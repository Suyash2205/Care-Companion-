// Sends an FCM push to the target guardian when an `alerts` row is inserted.
// Triggered by a Postgres pg_net webhook (see migration). SA key is a secret.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.0";

const SA = JSON.parse(Deno.env.get("FCM_SA_JSON")!);
const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const PROJECT_ID = SA.project_id as string;

function b64url(bytes: Uint8Array): string {
  let s = btoa(String.fromCharCode(...bytes));
  return s.replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
}
function b64urlStr(s: string): string {
  return btoa(s).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
}
function pemToBuf(pem: string): ArrayBuffer {
  const body = pem.replace(/-----BEGIN PRIVATE KEY-----/, "")
    .replace(/-----END PRIVATE KEY-----/, "").replace(/\s+/g, "");
  const bin = atob(body);
  const buf = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) buf[i] = bin.charCodeAt(i);
  return buf.buffer;
}

async function getAccessToken(): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const header = b64urlStr(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claim = b64urlStr(JSON.stringify({
    iss: SA.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now, exp: now + 3600,
  }));
  const unsigned = `${header}.${claim}`;
  const key = await crypto.subtle.importKey(
    "pkcs8", pemToBuf(SA.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"],
  );
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned));
  const jwt = `${unsigned}.${b64url(new Uint8Array(sig))}`;
  const resp = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`,
  });
  const data = await resp.json();
  if (!data.access_token) throw new Error("no access_token: " + JSON.stringify(data));
  return data.access_token;
}

Deno.serve(async (req) => {
  try {
    const payload = await req.json();
    const alert = payload.record ?? payload; // pg_net sends {record} or the row directly
    const guardianId = alert.guardian_id;
    if (!guardianId) return new Response("no guardian", { status: 200 });

    const sb = createClient(SUPABASE_URL, SERVICE_ROLE);
    const { data: user } = await sb.from("users").select("fcm_token").eq("id", guardianId).single();
    const token = user?.fcm_token;
    if (!token) return new Response("no token", { status: 200 });

    const accessToken = await getAccessToken();
    const message = {
      message: {
        token,
        notification: { title: alert.title ?? "Care Companion", body: alert.body ?? "" },
        android: { priority: "HIGH", notification: { channel_id: "cc_alerts", sound: "default" } },
        data: { kind: String(alert.kind ?? ""), elder_id: String(alert.elder_id ?? "") },
      },
    };
    const fcmResp = await fetch(
      `https://fcm.googleapis.com/v1/projects/${PROJECT_ID}/messages:send`,
      { method: "POST", headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" }, body: JSON.stringify(message) },
    );
    const result = await fcmResp.text();
    return new Response(result, { status: fcmResp.ok ? 200 : 500 });
  } catch (e) {
    return new Response("error: " + (e as Error).message, { status: 500 });
  }
});
