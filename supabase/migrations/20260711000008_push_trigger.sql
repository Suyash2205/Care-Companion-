-- Call the `push` edge function (FCM v1) whenever an alert row is inserted.
-- The anon key below is public (also embedded in the app); the function runs with
-- --no-verify-jwt and uses its own service-role env internally.
create extension if not exists pg_net;

create or replace function public.notify_push()
returns trigger language plpgsql security definer set search_path = public as $$
declare v_anon text := 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InppamVkenNvZXZobGphbmtndnZqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODM3MDU4ODQsImV4cCI6MjA5OTI4MTg4NH0.CCay92d4k_wN01yFF5oF3kIC1WuNLZf3PA4XiE9TM5A';
begin
  perform net.http_post(
    url := 'https://zijedzsoevhljankgvvj.supabase.co/functions/v1/push',
    headers := jsonb_build_object('Content-Type', 'application/json', 'Authorization', 'Bearer ' || v_anon),
    body := to_jsonb(NEW)
  );
  return NEW;
end $$;

create trigger alerts_push after insert on public.alerts
  for each row execute function public.notify_push();
