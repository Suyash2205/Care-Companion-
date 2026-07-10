-- CareCompanion full schema. `users` already exists (auth spike migration).
-- Bitmask for days: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64; every day = 127.

-- ── Elders ──────────────────────────────────────────────────────────────────
create table public.elders (
  id                 uuid primary key default gen_random_uuid(),
  name               text not null,
  photo_url          text,
  avatar_key         text,                    -- fallback icon/color when no photo
  dob                date,
  age                int,
  address            text,
  phone              text,                     -- the elder's own phone (E.164)
  phone_verified     boolean not null default false,
  verified_elder_uid text,                     -- Firebase UID captured at OTP verify; elder self-links on this
  is_active          boolean not null default true,
  created_by         uuid not null references public.users(id) on delete restrict,
  created_at         timestamptz not null default now(),
  updated_at         timestamptz not null default now()
);
create index on public.elders (created_by);
create index on public.elders (verified_elder_uid);

-- ── Guardian ↔ Elder links (owner / edit / view) ────────────────────────────
create table public.guardian_elder_links (
  id            uuid primary key default gen_random_uuid(),
  guardian_id   uuid references public.users(id) on delete cascade,      -- null while pending
  elder_id      uuid not null references public.elders(id) on delete cascade,
  access        text not null check (access in ('owner','edit','view')),
  status        text not null default 'active' check (status in ('pending','active')),
  invited_phone text,                                                     -- set while pending
  created_at    timestamptz not null default now(),
  unique (guardian_id, elder_id)
);
create index on public.guardian_elder_links (elder_id);
create index on public.guardian_elder_links (guardian_id);
create index on public.guardian_elder_links (invited_phone) where status = 'pending';

-- ── Contacts (emergency + quick-call + wheelchair services) ─────────────────
create table public.contacts (
  id           uuid primary key default gen_random_uuid(),
  elder_id     uuid not null references public.elders(id) on delete cascade,
  name         text not null,
  phone        text not null,
  photo_url    text,
  relation     text,
  is_emergency boolean not null default false,
  is_service   boolean not null default false,   -- wheelchair/emergency service, shown on assistance screen
  sort         int not null default 0,
  created_at   timestamptz not null default now()
);
create index on public.contacts (elder_id);

-- ── OTT / Video shortcuts (preset catalog or custom link) ───────────────────
create table public.ott_shortcuts (
  id             uuid primary key default gen_random_uuid(),
  elder_id       uuid not null references public.elders(id) on delete cascade,
  title          text not null,
  kind           text not null default 'preset' check (kind in ('preset','custom')),
  preset_key     text,                          -- youtube|hotstar|whatsapp|prime… (bundled logo)
  package_or_url text not null,                 -- android package name OR https url
  sort           int not null default 0,
  created_at     timestamptz not null default now()
);
create index on public.ott_shortcuts (elder_id);

-- ── Medicines ───────────────────────────────────────────────────────────────
create table public.medicines (
  id               uuid primary key default gen_random_uuid(),
  elder_id         uuid not null references public.elders(id) on delete cascade,
  name             text not null,
  dosage           text default '',
  form             text default 'Tablet',
  instructions     text default '',
  with_liquid      text check (with_liquid in ('water','milk')),
  meal             text check (meal in ('before','after')),
  is_active        boolean not null default true,
  pill_url         text,
  packet_front_url text,
  packet_back_url  text,
  created_at       timestamptz not null default now()
);
create index on public.medicines (elder_id);

-- ── Medicine schedules (the only source of medicine reminders) ──────────────
create table public.medicine_schedules (
  id          uuid primary key default gen_random_uuid(),
  medicine_id uuid not null references public.medicines(id) on delete cascade,
  elder_id    uuid not null references public.elders(id) on delete cascade,  -- denormalized for RLS
  label       text not null,                     -- Breakfast|Lunch|Dinner|Custom
  time        text not null,                     -- 'HH:MM' 24h
  days        int not null default 127 check (days between 1 and 127),
  enabled     boolean not null default true,
  created_at  timestamptz not null default now()
);
create index on public.medicine_schedules (elder_id);
create index on public.medicine_schedules (medicine_id);

-- ── Reminder categories (defaults are global; customs are per-elder) ────────
create table public.reminder_categories (
  id         uuid primary key default gen_random_uuid(),
  elder_id   uuid references public.elders(id) on delete cascade,  -- null = global default
  key        text,                               -- water|walk|vitals|medicine(virtual)|<custom>
  name       text not null,
  icon       text,
  is_default boolean not null default false,
  is_virtual boolean not null default false,     -- 'medicine' is virtual: read-only over schedules
  created_at timestamptz not null default now()
);
create index on public.reminder_categories (elder_id);

-- ── Reminders (non-medicine recurring prompts) ──────────────────────────────
create table public.reminders (
  id               uuid primary key default gen_random_uuid(),
  elder_id         uuid not null references public.elders(id) on delete cascade,
  category_id      uuid references public.reminder_categories(id) on delete set null,
  title            text not null,
  times            text[] not null default '{}', -- ['08:00','14:00']
  repeat_kind      text not null default 'daily' check (repeat_kind in ('daily','days','interval')),
  days             int not null default 127,
  interval_minutes int,
  enabled          boolean not null default true,
  created_at       timestamptz not null default now()
);
create index on public.reminders (elder_id);

-- ── Adherence logs (one row per occurrence, ever) ───────────────────────────
create table public.adherence_logs (
  id              uuid primary key default gen_random_uuid(),
  elder_id        uuid not null references public.elders(id) on delete cascade,
  source          text not null check (source in ('schedule','reminder')),
  source_id       uuid not null,
  occurrence_date date not null,
  due_at          timestamptz not null,
  status          text not null default 'pending' check (status in ('pending','taken','skipped','missed')),
  responded_at    timestamptz,
  created_at      timestamptz not null default now(),
  unique (source, source_id, due_at)
);
create index on public.adherence_logs (elder_id, occurrence_date);

-- ── Vitals ──────────────────────────────────────────────────────────────────
create table public.vitals (
  id         uuid primary key default gen_random_uuid(),
  elder_id   uuid not null references public.elders(id) on delete cascade,
  type       text not null check (type in ('bp','sugar','temp','pulse')),
  value_1    numeric not null,                   -- systolic | sugar | temp | pulse
  value_2    numeric,                            -- diastolic (bp only)
  context    text check (context in ('fasting','post_meal')),
  note       text,
  taken_at   timestamptz not null default now(),
  entered_by uuid references public.users(id) on delete set null,
  created_at timestamptz not null default now()
);
create index on public.vitals (elder_id, taken_at desc);

-- ── Vitals thresholds (global config; severity computed in app + here) ──────
create table public.vital_thresholds (
  type       text not null,
  context    text not null default 'default',
  normal_lo  numeric,
  normal_hi  numeric,
  warn_lo    numeric,
  warn_hi    numeric,
  unit       text,
  primary key (type, context)
);

-- ── SOS events ──────────────────────────────────────────────────────────────
create table public.sos_events (
  id           uuid primary key default gen_random_uuid(),
  elder_id     uuid not null references public.elders(id) on delete cascade,
  triggered_at timestamptz not null default now(),
  lat          double precision,
  lng          double precision,
  accuracy     double precision,
  address      text,
  status       text not null default 'active' check (status in ('active','cancelled','dismissed','resolved')),
  created_at   timestamptz not null default now()
);
create index on public.sos_events (elder_id, triggered_at desc);

-- ── Alerts (fanned out one row per guardian) ────────────────────────────────
create table public.alerts (
  id          uuid primary key default gen_random_uuid(),
  elder_id    uuid not null references public.elders(id) on delete cascade,
  guardian_id uuid not null references public.users(id) on delete cascade,
  kind        text not null check (kind in ('sos','missed_dose','reminder_missed','sos_resolved','dose_corrected')),
  ref_id      uuid,
  title       text not null,
  body        text,
  read        boolean not null default false,
  created_at  timestamptz not null default now()
);
create index on public.alerts (guardian_id, created_at desc);

-- ── Wheelchair / accessibility master data (admin-seeded, global) ───────────
create table public.wheelchair_places (
  id        uuid primary key default gen_random_uuid(),
  city      text not null default 'Mumbai',
  name      text not null,
  kind      text not null check (kind in ('service','place')),
  phone     text,
  maps_url  text,
  lat       double precision,
  lng       double precision,
  sort      int not null default 0
);

-- ── App config (server-tunable knobs: grace window, etc.) ───────────────────
create table public.app_config (
  key   text primary key,
  value text not null
);
insert into public.app_config (key, value) values
  ('grace_window_minutes', '60'),
  ('renag_minutes', '20');

-- updated_at trigger for elders
create or replace function public.touch_updated_at()
returns trigger language plpgsql as $$
begin new.updated_at = now(); return new; end $$;
create trigger elders_touch before update on public.elders
  for each row execute function public.touch_updated_at();
