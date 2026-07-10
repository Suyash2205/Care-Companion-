-- When an elder triggers SOS, fan out an alert to every linked guardian so it
-- surfaces in each guardian's alert feed. (SMS is sent client-side; this is the
-- in-app notification channel. FCM push is a future enhancement.)

create or replace function public.on_sos_created()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  insert into public.alerts (elder_id, guardian_id, kind, ref_id, title, body)
  select new.elder_id, gl.guardian_id, 'sos', new.id,
         'SOS Emergency', e.name || ' triggered an emergency alert' ||
         coalesce(' near ' || new.address, '')
  from public.elders e
  join public.guardian_elder_links gl
    on gl.elder_id = new.elder_id and gl.status = 'active' and gl.guardian_id is not null
  where e.id = new.elder_id;
  return new;
end $$;

create trigger sos_created after insert on public.sos_events
  for each row execute function public.on_sos_created();
