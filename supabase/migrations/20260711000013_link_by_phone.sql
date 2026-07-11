-- Robust elder linking: an elder who logs in with their own OTP (proving they own
-- the phone) auto-links to any elder profile created for that same phone. This
-- replaces the fragile "verify the elder's phone during guardian setup" flow.

-- Allow an elder to self-link an UNCLAIMED profile to their own uid (the guard
-- trigger otherwise blocks changes to verification columns by non-owners).
create or replace function public.guard_elder_columns()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  if ( new.phone              is distinct from old.phone
    or new.phone_verified     is distinct from old.phone_verified
    or new.verified_elder_uid is distinct from old.verified_elder_uid
    or new.is_active          is distinct from old.is_active
    or new.created_by         is distinct from old.created_by )
     and not public.can_own_elder(old.id)
     -- exception: an elder claiming an as-yet-unclaimed profile with THEIR OWN uid,
     -- changing nothing else (phone/active/creator stay put)
     and not (
       old.verified_elder_uid is null
       and new.verified_elder_uid = public.firebase_uid()
       and new.phone      is not distinct from old.phone
       and new.is_active  is not distinct from old.is_active
       and new.created_by is not distinct from old.created_by
     )
  then
    raise exception 'only the owner can change phone/verification/active';
  end if;
  return new;
end $$;

-- The elder app calls this at login. It links the caller to an active elder
-- profile whose phone matches the caller's own (verified) phone, if not already
-- claimed by someone else. Returns the linked elder, or null if none matched.
create or replace function public.rpc_link_elder_by_phone()
returns public.elders language plpgsql security definer set search_path = public as $$
declare v_uid text := public.firebase_uid(); v_phone text; v_elder public.elders;
begin
  if v_uid is null then return null; end if;
  select phone into v_phone from public.users where firebase_uid = v_uid;
  if v_phone is null then return null; end if;

  -- already linked?
  select * into v_elder from public.elders
   where verified_elder_uid = v_uid and is_active limit 1;
  if found then return v_elder; end if;

  -- claim a matching, unclaimed, active profile
  update public.elders
     set verified_elder_uid = v_uid, phone_verified = true
   where phone = v_phone and is_active and verified_elder_uid is null
   returning * into v_elder;
  return v_elder;
end $$;

grant execute on function public.rpc_link_elder_by_phone() to authenticated, anon;
