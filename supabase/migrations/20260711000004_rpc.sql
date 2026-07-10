-- Privileged operations run as SECURITY DEFINER RPCs that enforce their own
-- authorization (owner checks) and perform multi-row bootstraps atomically.

-- Create an elder + the owner link in one transaction. Caller becomes owner.
-- p_verified_uid: the elder's Firebase UID captured via secondary-auth OTP at
-- setup (null => profile saved Unverified).
create or replace function public.rpc_create_elder(
  p_name text, p_phone text default null, p_photo_url text default null,
  p_avatar_key text default null, p_dob date default null, p_age int default null,
  p_address text default null, p_verified_uid text default null
) returns public.elders language plpgsql security definer set search_path = public as $$
declare
  v_caller uuid := public.current_user_id();
  v_elder  public.elders;
begin
  if v_caller is null then raise exception 'not authenticated'; end if;
  insert into public.elders (name, phone, photo_url, avatar_key, dob, age, address,
                             phone_verified, verified_elder_uid, created_by)
  values (p_name, p_phone, p_photo_url, p_avatar_key, p_dob, p_age, p_address,
          p_verified_uid is not null, p_verified_uid, v_caller)
  returning * into v_elder;

  insert into public.guardian_elder_links (guardian_id, elder_id, access, status)
  values (v_caller, v_elder.id, 'owner', 'active');
  return v_elder;
end $$;

-- Mark an existing profile verified (completing an Unverified profile, or re-verify).
create or replace function public.rpc_verify_elder_phone(p_elder uuid, p_verified_uid text)
returns public.elders language plpgsql security definer set search_path = public as $$
declare v_elder public.elders;
begin
  if not public.can_own_elder(p_elder) then raise exception 'owner only'; end if;
  update public.elders
     set phone_verified = true, verified_elder_uid = p_verified_uid
   where id = p_elder returning * into v_elder;
  return v_elder;
end $$;

-- Change the elder's phone number; requires fresh OTP (new verified uid) or resets to Unverified.
create or replace function public.rpc_change_elder_phone(p_elder uuid, p_new_phone text, p_verified_uid text default null)
returns public.elders language plpgsql security definer set search_path = public as $$
declare v_elder public.elders;
begin
  if not public.can_own_elder(p_elder) then raise exception 'owner only'; end if;
  update public.elders
     set phone = p_new_phone,
         phone_verified = p_verified_uid is not null,
         verified_elder_uid = p_verified_uid
   where id = p_elder returning * into v_elder;
  return v_elder;
end $$;

-- Soft deactivate / reactivate. Owner-only.
create or replace function public.rpc_set_elder_active(p_elder uuid, p_active boolean)
returns public.elders language plpgsql security definer set search_path = public as $$
declare v_elder public.elders;
begin
  if not public.can_own_elder(p_elder) then raise exception 'owner only'; end if;
  update public.elders set is_active = p_active where id = p_elder returning * into v_elder;
  return v_elder;
end $$;

-- Invite another guardian by phone. Owner-only. access in (edit,view).
-- If a user with that phone exists, link immediately active; else pending.
create or replace function public.rpc_invite_guardian(p_elder uuid, p_phone text, p_access text)
returns public.guardian_elder_links language plpgsql security definer set search_path = public as $$
declare v_target uuid; v_link public.guardian_elder_links;
begin
  if not public.can_own_elder(p_elder) then raise exception 'owner only'; end if;
  if p_access not in ('edit','view') then raise exception 'access must be edit or view'; end if;
  select id into v_target from public.users where phone = p_phone limit 1;
  insert into public.guardian_elder_links (guardian_id, elder_id, access, status, invited_phone)
  values (v_target, p_elder, p_access,
          case when v_target is null then 'pending' else 'active' end,
          case when v_target is null then p_phone else null end)
  on conflict (guardian_id, elder_id) do update set access = excluded.access, status = 'active'
  returning * into v_link;
  return v_link;
end $$;

create or replace function public.rpc_set_member_access(p_elder uuid, p_guardian uuid, p_access text)
returns void language plpgsql security definer set search_path = public as $$
begin
  if not public.can_own_elder(p_elder) then raise exception 'owner only'; end if;
  if p_access not in ('edit','view') then raise exception 'access must be edit or view'; end if;
  update public.guardian_elder_links set access = p_access
   where elder_id = p_elder and guardian_id = p_guardian and access <> 'owner';
end $$;

create or replace function public.rpc_remove_member(p_elder uuid, p_guardian uuid)
returns void language plpgsql security definer set search_path = public as $$
begin
  if not public.can_own_elder(p_elder) then raise exception 'owner only'; end if;
  delete from public.guardian_elder_links
   where elder_id = p_elder and guardian_id = p_guardian and access <> 'owner';
end $$;

-- Called at guardian login: activate any pending links addressed to the caller's phone.
create or replace function public.rpc_resolve_pending_links()
returns int language plpgsql security definer set search_path = public as $$
declare v_caller uuid := public.current_user_id(); v_phone text; v_count int;
begin
  if v_caller is null then return 0; end if;
  select phone into v_phone from public.users where id = v_caller;
  update public.guardian_elder_links
     set guardian_id = v_caller, status = 'active', invited_phone = null
   where status = 'pending' and invited_phone = v_phone;
  get diagnostics v_count = row_count;
  return v_count;
end $$;

grant execute on all functions in schema public to authenticated, anon;
