-- Auth spike: users table keyed by Firebase UID, RLS proving token-scoped access.
-- Firebase JWTs arrive via Supabase third-party auth; auth.jwt()->>'sub' is the Firebase UID.

create table public.users (
  id uuid primary key default gen_random_uuid(),
  firebase_uid text not null unique,
  phone text not null,
  role text not null check (role in ('guardian', 'elder')),
  name text,
  photo_url text,
  fcm_token text,
  language text not null default 'en',
  created_at timestamptz not null default now()
);

alter table public.users enable row level security;

-- Helper: current Firebase UID from the verified JWT.
create or replace function public.firebase_uid()
returns text
language sql stable
as $$
  select nullif(auth.jwt()->>'sub', '')
$$;

create policy "users can read own row"
  on public.users for select
  using (firebase_uid = public.firebase_uid());

create policy "users can insert own row"
  on public.users for insert
  with check (firebase_uid = public.firebase_uid());

create policy "users can update own row"
  on public.users for update
  using (firebase_uid = public.firebase_uid())
  with check (firebase_uid = public.firebase_uid());
