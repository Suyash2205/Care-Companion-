-- Invite-code hardening: rate limiting and atomic redemption.
--
-- A reviewer of the ICISS submission observed that a six-digit code valid for seven
-- days needs attempt limits, authenticated rate limiting and atomic redemption, and
-- that none of these were specified. They were not specified because they did not
-- exist. This migration adds them.
--
-- Two properties are fixed here:
--
--   1. No attempt limit. The code space is 10^6. An authenticated caller could
--      previously guess without restriction.
--   2. Non-atomic redemption. Both redeem routines read the code row and updated it
--      later without locking, so two concurrent redemptions of the same code could
--      each observe it as unredeemed and both proceed.
--
-- One property is deliberately NOT changed, and the reasoning is recorded here so it
-- is a decision rather than an oversight. An already-redeemed code still returns a
-- message distinct from the unknown-code message, which is in principle an
-- enumeration oracle. Collapsing it would tell an older adult holding a used code
-- only "that code is not valid", removing the one hint that lets them recover without
-- help. With attempt limiting in place, enumerating a 10^6 space through that oracle
-- is no longer feasible, so the usability of the failure message is worth more than
-- the residual signal. Unknown and expired codes remain indistinguishable.

create table if not exists public.invite_code_attempts (
  id           bigserial primary key,
  user_id      uuid not null references public.users(id) on delete cascade,
  attempted_at timestamptz not null default now()
);

create index if not exists invite_code_attempts_user_time_idx
  on public.invite_code_attempts (user_id, attempted_at desc);

-- Written only by the definer functions below; no client may read or write it.
alter table public.invite_code_attempts enable row level security;

comment on table public.invite_code_attempts is
  'Failed invite-code redemption attempts, per account. Written only by '
  'rpc_redeem_invite_code and rpc_redeem_guardian_invite for rate limiting.';

-- Fails the caller when too many recent attempts have failed. Counts only failures:
-- a successful redemption clears the account's history, so a user who mistypes once
-- and then succeeds is not penalised later.
create or replace function public.assert_invite_attempts_available(p_user uuid)
returns void language plpgsql security definer set search_path = public as $$
declare
  v_recent int;
begin
  select count(*) into v_recent
    from public.invite_code_attempts
   where user_id = p_user
     and attempted_at > now() - interval '15 minutes';

  if v_recent >= 5 then
    raise exception 'Too many attempts. Please wait 15 minutes and try again.';
  end if;
end $$;

create or replace function public.record_invite_attempt(p_user uuid)
returns void language plpgsql security definer set search_path = public as $$
begin
  insert into public.invite_code_attempts (user_id) values (p_user);
end $$;

revoke execute on function public.assert_invite_attempts_available(uuid) from anon, authenticated;
revoke execute on function public.record_invite_attempt(uuid) from anon, authenticated;

-- ─────────────────────────────────────────────────────────────────────────────
-- Elder-device redemption, with rate limiting and a locked read.
-- ─────────────────────────────────────────────────────────────────────────────
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

  perform public.assert_invite_attempts_available(v_user);

  -- FOR UPDATE: hold the row for the rest of the transaction so a concurrent
  -- redemption of the same code blocks here and then observes redeemed_at set.
  select * into v_row from public.invite_codes
   where code = regexp_replace(coalesce(p_code, ''), '\D', '', 'g')
     and kind = 'elder'
   for update;

  if not found or v_row.expires_at <= now() then
    perform public.record_invite_attempt(v_user);
    raise exception 'That code is not valid. Ask your family for a new one.';
  end if;
  if v_row.redeemed_at is not null then
    perform public.record_invite_attempt(v_user);
    raise exception 'That code has already been used. Ask your family for a new one.';
  end if;

  select * into v_elder from public.elders where id = v_row.elder_id and is_active;
  if not found then
    perform public.record_invite_attempt(v_user);
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

  delete from public.invite_code_attempts where user_id = v_user;
  return v_elder;
end $$;

-- ─────────────────────────────────────────────────────────────────────────────
-- Caregiver-invite redemption, same treatment.
-- ─────────────────────────────────────────────────────────────────────────────
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

  perform public.assert_invite_attempts_available(v_user);

  select * into v_row from public.invite_codes
   where code = regexp_replace(coalesce(p_code, ''), '\D', '', 'g')
     and kind = 'guardian'
   for update;

  if not found or v_row.expires_at <= now() then
    perform public.record_invite_attempt(v_user);
    raise exception 'That code is not valid. Ask them for a new one.';
  end if;
  if v_row.redeemed_at is not null then
    perform public.record_invite_attempt(v_user);
    raise exception 'That code has already been used. Ask them for a new one.';
  end if;

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

  delete from public.invite_code_attempts where user_id = v_user;
  return v_link;
end $$;

grant execute on function public.rpc_redeem_invite_code(text) to anon, authenticated;
grant execute on function public.rpc_redeem_guardian_invite(text) to anon, authenticated;
