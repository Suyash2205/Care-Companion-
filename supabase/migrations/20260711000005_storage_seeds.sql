-- ── Storage buckets ─────────────────────────────────────────────────────────
-- Public-read (unguessable UUID paths); writes scoped to elders the caller manages.
-- Path convention: '{elder_id}/{uuid}.jpg'
insert into storage.buckets (id, name, public) values
  ('photos', 'photos', true),
  ('medicine-images', 'medicine-images', true)
on conflict (id) do nothing;

create policy "cc storage public read" on storage.objects for select
  using (bucket_id in ('photos','medicine-images'));

create policy "cc storage managed insert" on storage.objects for insert
  with check (
    bucket_id in ('photos','medicine-images')
    and public.can_manage_elder(((storage.foldername(name))[1])::uuid)
  );

create policy "cc storage managed update" on storage.objects for update
  using (
    bucket_id in ('photos','medicine-images')
    and public.can_manage_elder(((storage.foldername(name))[1])::uuid)
  );

create policy "cc storage managed delete" on storage.objects for delete
  using (
    bucket_id in ('photos','medicine-images')
    and public.can_manage_elder(((storage.foldername(name))[1])::uuid)
  );

-- ── Vitals thresholds (adult defaults; BP keyed by systolic/diastolic) ──────
insert into public.vital_thresholds (type, context, normal_lo, normal_hi, warn_lo, warn_hi, unit) values
  ('bp',    'systolic',  90, 120, 80, 139, 'mmHg'),
  ('bp',    'diastolic', 60,  80, 55,  89, 'mmHg'),
  ('sugar', 'fasting',   70, 100, 70, 125, 'mg/dL'),
  ('sugar', 'post_meal', 70, 140, 70, 199, 'mg/dL'),
  ('sugar', 'default',   70, 140, 70, 199, 'mg/dL'),
  ('temp',  'default',   97,  99, 95, 100.9, '°F'),
  ('pulse', 'default',   60, 100, 50, 110, 'bpm')
on conflict (type, context) do nothing;

-- ── Global default reminder categories (elder_id null) ──────────────────────
insert into public.reminder_categories (elder_id, key, name, icon, is_default, is_virtual) values
  (null, 'medicine', 'Medicine', 'pill',  true, true),
  (null, 'water',    'Water',    'drop',  true, false),
  (null, 'walk',     'Walk',     'walk',  true, false),
  (null, 'vitals',   'Vitals',   'heart', true, false);

-- ── Wheelchair / accessibility master data (Mumbai demo) ────────────────────
insert into public.wheelchair_places (city, name, kind, phone, maps_url, sort) values
  ('Mumbai', 'Ambulance (108 Emergency)',        'service', '108',          null, 0),
  ('Mumbai', 'Wheelchair Rental — Mobility Aids', 'service', '+912212345678', 'https://maps.google.com/?q=wheelchair+rental+mumbai', 1),
  ('Mumbai', 'Senior Citizen Helpline',           'service', '14567',        null, 2),
  ('Mumbai', 'Accessible Hospital — Wheelchair Access', 'place', '+912298765432', 'https://maps.google.com/?q=wheelchair+accessible+hospital+mumbai', 3);
