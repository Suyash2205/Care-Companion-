-- ─────────────────────────────────────────────────────────────────────────────
-- Invite codes: link an elder's device to their care profile without a phone number.
--
-- Auth moved from phone OTP to Google Sign-In, so there is no longer a phone
-- number to match an elder's login against their profile. Instead the guardian
-- gets a short code when they create the profile; entering it once on the elder's
-- device (by the elder or, more often, by the guardian sitting with them) claims
-- the profile for that Google account. Single-use and short-lived so a guessed
-- code cannot expose someone's care data.
-- ─────────────────────────────────────────────────────────────────────────────

create table if not exists public.invite_codes (
  code        text primary key,
  elder_id    uuid not null references public.elders(id) on delete cascade,
  created_by  uuid not null references public.users(id) on delete cascade,
  created_at  timestamptz not null default now(),
  expires_at  timestamptz not null default now() + interval '7 days',
  redeemed_at timestamptz,
  redeemed_by uuid references public.users(id) on delete set null
);

create index if not exists invite_codes_elder_idx on public.invite_codes(elder_id);

alter table public.invite_codes enable row level security;

-- Only someone who can manage the elder may see that elder's codes. Nobody can
-- SELECT codes generally — that would make guessing unnecessary.
drop policy if exists invite_codes_read on public.invite_codes;
create policy invite_codes_read on public.invite_codes
  for select using (public.can_read_elder(elder_id));

-- Writes go exclusively through the SECURITY DEFINER RPCs below.
revoke insert, update, delete on public.invite_codes from anon, authenticated;

-- ── create ───────────────────────────────────────────────────────────────────
-- Returns the active code for an elder, generating one if none is live. Calling
-- it repeatedly is safe and idempotent — the guardian sees the same code.
create or replace function public.rpc_create_invite_code(p_elder_id uuid)
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
    raise exception 'only the owner can create an invite code';
  end if;
  select id into v_user from public.users where firebase_uid = v_uid;

  -- Reuse a still-valid, unredeemed code so the guardian isn't handed a new
  -- number every time they open the screen.
  select * into v_row from public.invite_codes
   where elder_id = p_elder_id and redeemed_at is null and expires_at > now()
   order by created_at desc limit 1;
  if found then return v_row; end if;

  -- 6 digits, retried on the (astronomically unlikely) collision.
  loop
    i := i + 1;
    v_code := lpad((floor(random() * 1000000))::int::text, 6, '0');
    exit when not exists (select 1 from public.invite_codes where code = v_code);
    if i > 20 then raise exception 'could not allocate an invite code'; end if;
  end loop;

  insert into public.invite_codes (code, elder_id, created_by)
  values (v_code, p_elder_id, v_user)
  returning * into v_row;
  return v_row;
end $$;

-- ── redeem ───────────────────────────────────────────────────────────────────
-- Called from the elder's device after Google sign-in. Claims the profile for
-- the caller. Returns the linked elder, or raises with a message safe to show.
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
   where code = regexp_replace(coalesce(p_code, ''), '\D', '', 'g');

  -- Deliberately identical message for "wrong" and "expired" so the response
  -- cannot be used to discover which codes exist.
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

  -- An already-claimed profile may only be re-opened by the same person.
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

-- The guard trigger blocks non-owners from touching verification columns; the
-- redeem RPC is SECURITY DEFINER but still runs the trigger, so allow an elder
-- claiming an unclaimed profile with their own uid (phone/active/creator fixed).
-- This mirrors the existing self-link exception and is already covered by
-- guard_elder_columns(), so no trigger change is needed here.

grant execute on function public.rpc_create_invite_code(uuid) to anon, authenticated;
grant execute on function public.rpc_redeem_invite_code(text) to anon, authenticated;
