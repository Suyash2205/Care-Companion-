-- Security hardening from the adversarial review.

-- 1) elders: RLS is row-level only. Restrict which COLUMNS an authenticated
--    guardian may UPDATE directly, so identity/verification/active columns can
--    only change via the owner-gated SECURITY DEFINER RPCs.
revoke update on public.elders from authenticated;
grant update (name, photo_url, avatar_key, dob, age, address, updated_at)
  on public.elders to authenticated;

-- 2) rpc_invite_guardian must never downgrade the owner's own link.
create or replace function public.rpc_invite_guardian(p_elder uuid, p_phone text, p_access text)
returns public.guardian_elder_links language plpgsql security definer set search_path = public as $$
declare v_caller uuid := public.current_user_id(); v_target uuid; v_link public.guardian_elder_links;
begin
  if not public.can_own_elder(p_elder) then raise exception 'owner only'; end if;
  if p_access not in ('edit','view') then raise exception 'access must be edit or view'; end if;
  select id into v_target from public.users where phone = p_phone limit 1;
  if v_target is not null and v_target = v_caller then
    raise exception 'you are the owner and cannot change your own access';
  end if;
  insert into public.guardian_elder_links (guardian_id, elder_id, access, status, invited_phone)
  values (v_target, p_elder, p_access,
          case when v_target is null then 'pending' else 'active' end,
          case when v_target is null then p_phone else null end)
  on conflict (guardian_id, elder_id) do update
     set access = excluded.access, status = 'active'
     where guardian_elder_links.access <> 'owner'
  returning * into v_link;
  return v_link;
end $$;
