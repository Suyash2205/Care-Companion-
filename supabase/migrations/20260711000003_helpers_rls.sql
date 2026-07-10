-- ── Helper functions (SECURITY DEFINER so they bypass RLS on internal reads,
--    preventing policy recursion; STABLE; locked search_path) ────────────────

create or replace function public.current_user_id()
returns uuid language sql stable security definer set search_path = public as $$
  select id from public.users where firebase_uid = public.firebase_uid()
$$;

-- access rank: owner=3, edit=2, view=1
create or replace function public.is_guardian_of(p_elder uuid, p_min_access text)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (
    select 1
    from public.guardian_elder_links l
    join public.users u on u.id = l.guardian_id
    where l.elder_id = p_elder
      and l.status = 'active'
      and u.firebase_uid = public.firebase_uid()
      and case l.access when 'owner' then 3 when 'edit' then 2 when 'view' then 1 else 0 end
          >= case p_min_access when 'owner' then 3 when 'edit' then 2 when 'view' then 1 else 0 end
  )
$$;

create or replace function public.is_elder_self(p_elder uuid)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (
    select 1 from public.elders e
    where e.id = p_elder
      and e.is_active
      and e.phone_verified
      and e.verified_elder_uid is not null
      and e.verified_elder_uid = public.firebase_uid()
  )
$$;

create or replace function public.can_read_elder(p_elder uuid)
returns boolean language sql stable security definer set search_path = public as $$
  select public.is_guardian_of(p_elder, 'view') or public.is_elder_self(p_elder)
$$;

create or replace function public.can_manage_elder(p_elder uuid)
returns boolean language sql stable security definer set search_path = public as $$
  select public.is_guardian_of(p_elder, 'edit')
$$;

create or replace function public.can_own_elder(p_elder uuid)
returns boolean language sql stable security definer set search_path = public as $$
  select public.is_guardian_of(p_elder, 'owner')
$$;

-- ── Enable RLS everywhere ────────────────────────────────────────────────────
alter table public.elders               enable row level security;
alter table public.guardian_elder_links enable row level security;
alter table public.contacts             enable row level security;
alter table public.ott_shortcuts        enable row level security;
alter table public.medicines            enable row level security;
alter table public.medicine_schedules   enable row level security;
alter table public.reminder_categories  enable row level security;
alter table public.reminders            enable row level security;
alter table public.adherence_logs       enable row level security;
alter table public.vitals               enable row level security;
alter table public.vital_thresholds     enable row level security;
alter table public.sos_events           enable row level security;
alter table public.alerts               enable row level security;
alter table public.wheelchair_places    enable row level security;
alter table public.app_config           enable row level security;

-- ── elders ───────────────────────────────────────────────────────────────────
-- read: linked guardians + the elder themselves + the creator (covers the
-- moment right after rpc_create_elder before caches settle)
create policy elders_read on public.elders for select
  using (can_read_elder(id) or created_by = current_user_id());
-- update care fields: owner or edit. (phone/verified/is_active go via RPC that asserts owner.)
create policy elders_update on public.elders for update
  using (can_manage_elder(id)) with check (can_manage_elder(id));
-- no direct INSERT/DELETE: creation is via rpc_create_elder; deactivation via rpc_set_elder_active.

-- ── guardian_elder_links ─────────────────────────────────────────────────────
-- read links for elders you can read (so co-guardians are visible), or your own pending invite
create policy links_read on public.guardian_elder_links for select
  using (can_read_elder(elder_id) or guardian_id = current_user_id());
-- writes go through RPCs (owner-gated); no direct policies granted.

-- ── generic care tables: read=can_read_elder, write=can_manage_elder ─────────
create policy contacts_read on public.contacts for select using (can_read_elder(elder_id));
create policy contacts_write on public.contacts for all
  using (can_manage_elder(elder_id)) with check (can_manage_elder(elder_id));

create policy ott_read on public.ott_shortcuts for select using (can_read_elder(elder_id));
create policy ott_write on public.ott_shortcuts for all
  using (can_manage_elder(elder_id)) with check (can_manage_elder(elder_id));

create policy meds_read on public.medicines for select using (can_read_elder(elder_id));
create policy meds_write on public.medicines for all
  using (can_manage_elder(elder_id)) with check (can_manage_elder(elder_id));

create policy sched_read on public.medicine_schedules for select using (can_read_elder(elder_id));
create policy sched_write on public.medicine_schedules for all
  using (can_manage_elder(elder_id)) with check (can_manage_elder(elder_id));

create policy remcat_read on public.reminder_categories for select
  using (elder_id is null or can_read_elder(elder_id));
create policy remcat_write on public.reminder_categories for all
  using (elder_id is not null and can_manage_elder(elder_id))
  with check (elder_id is not null and can_manage_elder(elder_id));

create policy rem_read on public.reminders for select using (can_read_elder(elder_id));
create policy rem_write on public.reminders for all
  using (can_manage_elder(elder_id)) with check (can_manage_elder(elder_id));

-- ── adherence_logs: elder writes own; guardians (edit) may correct; all read ─
create policy adh_read on public.adherence_logs for select using (can_read_elder(elder_id));
create policy adh_insert on public.adherence_logs for insert
  with check (is_elder_self(elder_id) or can_manage_elder(elder_id));
create policy adh_update on public.adherence_logs for update
  using (is_elder_self(elder_id) or can_manage_elder(elder_id))
  with check (is_elder_self(elder_id) or can_manage_elder(elder_id));

-- ── vitals: elder or guardian(edit) enter; all linked read ──────────────────
create policy vit_read on public.vitals for select using (can_read_elder(elder_id));
create policy vit_insert on public.vitals for insert
  with check (is_elder_self(elder_id) or can_manage_elder(elder_id));
create policy vit_update on public.vitals for update
  using (is_elder_self(elder_id) or can_manage_elder(elder_id))
  with check (is_elder_self(elder_id) or can_manage_elder(elder_id));

-- ── sos_events: only the elder triggers; any linked guardian or elder resolves ─
create policy sos_read on public.sos_events for select using (can_read_elder(elder_id));
create policy sos_insert on public.sos_events for insert with check (is_elder_self(elder_id));
create policy sos_update on public.sos_events for update
  using (can_read_elder(elder_id)) with check (can_read_elder(elder_id));

-- ── alerts: each guardian sees & marks only their own rows (insert = server) ──
create policy alerts_read on public.alerts for select using (guardian_id = current_user_id());
create policy alerts_update on public.alerts for update
  using (guardian_id = current_user_id()) with check (guardian_id = current_user_id());

-- ── global read-only reference data ─────────────────────────────────────────
create policy thresholds_read on public.vital_thresholds for select using (true);
create policy wheelchair_read on public.wheelchair_places for select using (true);
create policy config_read on public.app_config for select using (true);
