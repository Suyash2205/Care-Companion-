-- Missed-dose detection + alert fan-out, plus visible corrections when an elder
-- responds after being marked missed (decision #5).

create or replace function public.scan_missed_doses()
returns void language plpgsql security definer set search_path = public as $$
declare
  v_grace int;
  v_today date := (now() at time zone 'utc')::date;
  v_bit   int  := (1 << (extract(isodow from now())::int - 1));  -- Mon=bit0 … Sun=bit6
begin
  select coalesce(max(value::int), 60) into v_grace from app_config where key = 'grace_window_minutes';

  -- 1) mark overdue, unanswered occurrences as missed (never overwrite taken/skipped)
  insert into adherence_logs (elder_id, source, source_id, occurrence_date, due_at, status)
  select s.elder_id, 'schedule', s.id, v_today,
         (v_today::text || 'T' || s.time || ':00+00')::timestamptz, 'missed'
  from medicine_schedules s
  join medicines m on m.id = s.medicine_id
  join elders e on e.id = s.elder_id
  where s.enabled and m.is_active and e.is_active
    and (s.days & v_bit) <> 0
    and now() > (v_today::text || 'T' || s.time || ':00+00')::timestamptz + (v_grace || ' minutes')::interval
  on conflict (source, source_id, due_at) do update
    set status = case when adherence_logs.status = 'pending' then 'missed' else adherence_logs.status end;

  -- 2) fan out one missed_dose alert per linked guardian, once
  insert into alerts (elder_id, guardian_id, kind, ref_id, title, body)
  select l.elder_id, gl.guardian_id, 'missed_dose', l.id,
         'Missed medicine', e.name || ' missed a dose at ' || to_char(l.due_at, 'HH24:MI')
  from adherence_logs l
  join elders e on e.id = l.elder_id
  join guardian_elder_links gl on gl.elder_id = l.elder_id and gl.status = 'active' and gl.guardian_id is not null
  where l.occurrence_date = v_today and l.status = 'missed'
    and not exists (
      select 1 from alerts a
      where a.kind = 'missed_dose' and a.ref_id = l.id and a.guardian_id = gl.guardian_id
    );
end $$;

-- Correction: elder responds after a missed verdict → notify guardians (never silent).
create or replace function public.on_adherence_corrected()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  if old.status = 'missed' and new.status in ('taken', 'skipped') then
    insert into alerts (elder_id, guardian_id, kind, ref_id, title, body)
    select new.elder_id, gl.guardian_id, 'dose_corrected', new.id,
           'Dose update', e.name || ' actually ' || new.status || ' the ' ||
           to_char(new.due_at, 'HH24:MI') || ' dose'
    from elders e
    join guardian_elder_links gl on gl.elder_id = new.elder_id and gl.status = 'active' and gl.guardian_id is not null
    where e.id = new.elder_id;
  end if;
  return new;
end $$;

create trigger adherence_corrected after update on public.adherence_logs
  for each row execute function public.on_adherence_corrected();

-- Schedule the scan every 15 minutes.
create extension if not exists pg_cron;
select cron.schedule('cc-missed-doses', '*/15 * * * *', $$select public.scan_missed_doses()$$);
