-- Column GRANTs don't reliably apply to Supabase third-party (Firebase) JWT roles,
-- so enforce owner-only identity columns with a trigger that uses the same
-- firebase_uid()-based owner check the RLS policies use. SECURITY DEFINER RPCs
-- still pass because auth.jwt() (hence can_own_elder) reflects the original caller.

-- Restore the normal table grant (the column-only grant was ineffective).
grant update on public.elders to authenticated;

create or replace function public.guard_elder_columns()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  if ( new.phone              is distinct from old.phone
    or new.phone_verified     is distinct from old.phone_verified
    or new.verified_elder_uid is distinct from old.verified_elder_uid
    or new.is_active          is distinct from old.is_active
    or new.created_by         is distinct from old.created_by )
     and not public.can_own_elder(old.id) then
    raise exception 'only the owner can change phone/verification/active';
  end if;
  return new;
end $$;

drop trigger if exists elders_guard_cols on public.elders;
create trigger elders_guard_cols before update on public.elders
  for each row execute function public.guard_elder_columns();
