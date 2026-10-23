#!/bin/zsh
# Task 21 — backend round-trip latency, repeated trials.
URL="https://zijedzsoevhljankgvvj.supabase.co"
KEY=$(grep -o 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9[A-Za-z0-9._-]*' ../app/build.gradle.kts | head -1)
OUT="raw/backend_latency.csv"
echo "trial,endpoint,http_code,dns_s,tcp_s,tls_s,ttfb_s,total_s,bytes" > $OUT
typeset -A EPS
EPS[rest_root]="$URL/rest/v1/"
EPS[rls_denied_select]="$URL/rest/v1/elders?select=id&limit=1"
EPS[auth_settings]="$URL/auth/v1/settings"
EPS[edge_push_preflight]="$URL/functions/v1/push"
for t in $(seq 1 30); do
  for name in rest_root rls_denied_select auth_settings edge_push_preflight; do
    m=$(curl -s -o /dev/null -w "%{http_code},%{time_namelookup},%{time_connect},%{time_appconnect},%{time_starttransfer},%{time_total},%{size_download}" \
      -H "apikey: $KEY" -H "Authorization: Bearer $KEY" --max-time 20 "${EPS[$name]}")
    echo "$t,$name,$m" >> $OUT
  done
done
wc -l $OUT
