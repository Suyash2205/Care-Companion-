-- ─────────────────────────────────────────────────────────────────────────────
-- Guardian invite codes: add a second family member to an elder's circle.
--
-- "Invite by mobile" was stranded by the move from phone OTP to Google Sign-In.
-- rpc_invite_guardian matched the invitee on users.phone, and the app now writes
-- an empty phone for every Google account, so the lookup never matched: every
-- invite was filed as 'pending' and rpc_resolve_pending_links compared
-- invited_phone against '' forever. Nothing was ever delivered either — there is
-- no SMS or email integration anywhere in the project.
--
-- This replaces it with the mechanism that already works for linking an elder's
-- device: a short code the owner shares however they like. Same table, extended
-- with the kind of invite and the access it grants.
-- ─────────────────────────────────────────────────────────────────────────────

alter table public.invite_codes
  add column if not exists kind   text not null default 'elder',
  add column if not exists access text;

do $$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'invite_codes_kind_check'
  ) then
    alter table public.invite_codes
      add constraint invite_codes_kind_check check (kind in ('elder', 'guardian'));
  end if;
  if not exists (
    select 1 from pg_constraint where conname = 'invite_codes_access_check'
  ) then
    alter table public.invite_codes
      add constraint invite_codes_access_check
      check (access is null or access in ('view', 'edit'));
  end if;
end $$;

-- ── create ───────────────────────────────────────────────────────────────────
-- A fresh code per access level, so switching View Only <-> Can Edit doesn't
-- hand back a code that grants the other one. Idempotent within an access level.
create or replace function public.rpc_create_guardian_invite(p_elder_id uuid, p_access text)
returns public.invite_codes language plpgsql security definer set search_path = public as $$
declare
  v_uid  text := public.firebase_uid();
  v_user uuid;
  v_row  public.invite_codes;
  v_code text;
  i int := 0;
begin
  if v_uid is null then raise exception 'not signed in'; end if;
  if not public.can_own_elder(p_elder_id) then
    raise exception 'only the owner can invite family members';
  end if;
  if p_access not in ('view', 'edit') then
    raise exception 'access must be view or edit';
  end if;
  select id into v_user from public.users where firebase_uid = v_uid;

  select * into v_row from public.invite_codes
   where elder_id = p_elder_id and kind = 'guardian' and access = p_access
     and redeemed_at is null and expires_at > now()
   order by created_at desc limit 1;
  if found then return v_row; end if;

  loop
    i := i + 1;
    v_code := lpad((floor(random() * 1000000))::int::text, 6, '0');
    exit when not exists (select 1 from public.invite_codes where code = v_code);
    if i > 20 then raise exception 'could not allocate an invite code'; end if;
  end loop;

  insert into public.invite_codes (code, elder_id, created_by, kind, access)
  values (v_code, p_elder_id, v_user, 'guardian', p_access)
  returning * into v_row;
  return v_row;
end $$;

-- ── redeem ───────────────────────────────────────────────────────────────────
-- Called by the invited guardian after Google sign-in. Joins them to the elder's
-- circle at the access level the code carries.
create or replace function public.rpc_redeem_guardian_invite(p_code text)
returns public.guardian_elder_links language plpgsql security definer set search_path = public as $$
declare
  v_uid   text := public.firebase_uid();
  v_user  uuid;
  v_row   public.invite_codes;
  v_link  public.guardian_elder_links;
  v_owner boolean;
begin
  if v_uid is null then raise exception 'not signed in'; end if;
  select id into v_user from public.users where firebase_uid = v_uid;
  if v_user is null then raise exception 'no account'; end if;

  select * into v_row from public.invite_codes
   where code = regexp_replace(coalesce(p_code, ''), '\D', '', 'g')
     and kind = 'guardian';

  -- Same message for wrong and expired, so responses can't be used to probe
  -- which codes exist.
  if not found or v_row.expires_at <= now() then
    raise exception 'That code is not valid. Ask them for a new one.';
  end if;
  if v_row.redeemed_at is not null then
    raise exception 'That code has already been used. Ask them for a new one.';
  end if;

  -- The owner redeeming their own code would demote themselves to view/edit.
  select exists (
    select 1 from public.guardian_elder_links
     where elder_id = v_row.elder_id and guardian_id = v_user and access = 'owner'
  ) into v_owner;
  if v_owner then
    raise exception 'You already own this profile.';
  end if;

  insert into public.guardian_elder_links (guardian_id, elder_id, access, status)
  values (v_user, v_row.elder_id, v_row.access, 'active')
  on conflict (guardian_id, elder_id) do update
     set access = excluded.access, status = 'active'
   where guardian_elder_links.access <> 'owner'
  returning * into v_link;

  update public.invite_codes
     set redeemed_at = now(), redeemed_by = v_user
   where code = v_row.code;

  return v_link;
end $$;

grant execute on function public.rpc_create_guardian_invite(uuid, text) to anon, authenticated;
grant execute on function public.rpc_redeem_guardian_invite(text) to anon, authenticated;

-- Elder-device codes must not be redeemable as guardian codes or vice versa;
-- rpc_redeem_invite_code predates the column, so pin it to elder codes.
create or replace function public.rpc_redeem_invite_code(p_code text)
returns public.elders language plpgsql security definer set search_path = public as $$
declare
  v_uid   text := public.firebase_uid();
  v_user  uuid;
  v_row   public.invite_codes;
  v_elder public.elders;
begin
  if v_uid is null then raise exception 'not signed in'; end if;
  select id into v_user from public.users where firebase_uid = v_uid;
  if v_user is null then raise exception 'no account'; end if;

  select * into v_row from public.invite_codes
   where code = regexp_replace(coalesce(p_code, ''), '\D', '', 'g')
     and kind = 'elder';

  if not found or v_row.expires_at <= now() then
    raise exception 'That code is not valid. Ask your family for a new one.';
  end if;
  if v_row.redeemed_at is not null then
    raise exception 'That code has already been used. Ask your family for a new one.';
  end if;

  select * into v_elder from public.elders where id = v_row.elder_id and is_active;
  if not found then
    raise exception 'That code is not valid. Ask your family for a new one.';
  end if;

  if v_elder.verified_elder_uid is not null and v_elder.verified_elder_uid <> v_uid then
    raise exception 'This profile is already set up on another phone.';
  end if;

  update public.elders
     set verified_elder_uid = v_uid, phone_verified = true
   where id = v_elder.id
  returning * into v_elder;

  update public.invite_codes
     set redeemed_at = now(), redeemed_by = v_user
   where code = v_row.code;

  return v_elder;
end $$;

grant execute on function public.rpc_redeem_invite_code(text) to anon, authenticated;
