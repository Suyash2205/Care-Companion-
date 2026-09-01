// Voice intent for the elder's dose confirmation.
//
// The phone records a few seconds of speech and posts it here. This function calls
// Sarvam's Indian-language speech recogniser, then a language model constrained to a
// fixed set of intents, and returns both. The phone speaks the reply with the
// device's own text-to-speech, so no audio comes back over the wire.
//
// The API key lives here as a secret. It must never ship inside the APK: anyone can
// unzip an APK and read it, and the credits are drainable.
//
// Deliberately conservative: anything the model is not sure about comes back as
// "unclear", and the client falls back to its buttons. A medication record must
// never be written from a guess.

const SARVAM_KEY = Deno.env.get("SARVAM_API_KEY") ?? "";
const STT_URL = "https://api.sarvam.ai/speech-to-text";
const CHAT_URL = "https://api.sarvam.ai/v1/chat/completions";

const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

type Intent = "taken" | "not_taken" | "repeat" | "unclear";

const LANGS: Record<string, string> = {
  en: "en-IN", hi: "hi-IN", mr: "mr-IN", gu: "gu-IN",
};

// One reply per intent per language. Kept here rather than in the model so the
// wording is predictable and reviewable, and so a model change cannot alter what an
// elder is told about their medication.
const REPLIES: Record<string, Record<Intent, string>> = {
  en: {
    taken: "Good. Marked as taken.",
    not_taken: "Alright, marked as not taken.",
    repeat: "Let me say that again.",
    unclear: "Sorry, I did not catch that. Please use the buttons.",
  },
  hi: {
    taken: "बहुत अच्छा। दर्ज कर लिया।",
    not_taken: "ठीक है, नहीं ली — दर्ज कर लिया।",
    repeat: "मैं फिर से बताती हूँ।",
    unclear: "माफ़ कीजिए, समझ नहीं आया। कृपया बटन दबाएँ।",
  },
  mr: {
    taken: "छान. नोंद केली.",
    not_taken: "ठीक आहे, घेतले नाही — नोंद केली.",
    repeat: "मी पुन्हा सांगते.",
    unclear: "माफ करा, समजले नाही. कृपया बटण दाबा.",
  },
  gu: {
    taken: "સરસ. નોંધી લીધું.",
    not_taken: "ઠીક છે, લીધી નથી — નોંધી લીધું.",
    repeat: "હું ફરીથી કહું છું.",
    unclear: "માફ કરશો, સમજાયું નહીં. કૃપા કરીને બટન દબાવો.",
  },
};

const SYSTEM = `You classify what an elderly person said about taking a medicine.

Return exactly one intent:
- "taken": they confirm they have taken it.
- "not_taken": they say they have NOT taken it, or will take it later.
- "repeat": they ask you to say it again, or did not hear.
- "unclear": anything else, silence, or you are not confident.

Rules that matter:
- NEGATION DECIDES. "नहीं ली", "नाही घेतले", "લીધી નથી", "not yet", "later" are all
  not_taken, even though they contain the same verb as the positive form.
- Hedging ("I think so", "शायद") is unclear, not taken.
- If the transcript is empty, garbled, or off-topic, return unclear.
- Never guess. A wrong "taken" writes a false medication record.

Reply only with JSON.`;

const SCHEMA = {
  type: "json_schema",
  json_schema: {
    name: "dose_intent",
    schema: {
      type: "object",
      properties: {
        intent: { type: "string", enum: ["taken", "not_taken", "repeat", "unclear"] },
        confident: { type: "boolean" },
      },
      required: ["intent", "confident"],
      additionalProperties: false,
    },
  },
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...CORS, "Content-Type": "application/json" },
  });
}

/** Decode base64 into bytes without blowing the stack on a large string. */
function b64ToBytes(b64: string): Uint8Array {
  const bin = atob(b64);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}

async function transcribe(wav: Uint8Array, langCode: string): Promise<string> {
  const form = new FormData();
  form.append("file", new Blob([wav], { type: "audio/wav" }), "speech.wav");
  form.append("model", "saaras:v4");
  form.append("language_code", langCode);
  const res = await fetch(STT_URL, {
    method: "POST",
    headers: { "api-subscription-key": SARVAM_KEY },
    body: form,
  });
  if (!res.ok) throw new Error(`stt ${res.status}: ${(await res.text()).slice(0, 200)}`);
  const body = await res.json();
  return (body.transcript ?? "").trim();
}

async function classify(transcript: string, medicine: string): Promise<Intent> {
  if (!transcript) return "unclear";
  const res = await fetch(CHAT_URL, {
    method: "POST",
    headers: {
      "api-subscription-key": SARVAM_KEY,
      "Authorization": `Bearer ${SARVAM_KEY}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: "sarvam-105b",
      temperature: 0,
      max_tokens: 60,
      response_format: SCHEMA,
      messages: [
        { role: "system", content: SYSTEM },
        {
          role: "user",
          content: `Medicine: ${medicine || "a medicine"}\nThey said: "${transcript}"`,
        },
      ],
    }),
  });
  if (!res.ok) throw new Error(`llm ${res.status}: ${(await res.text()).slice(0, 200)}`);
  const body = await res.json();
  const raw = body?.choices?.[0]?.message?.content ?? "{}";
  const parsed = JSON.parse(raw);
  // An unconfident answer is treated as no answer. Better a second question than a
  // wrong entry in a medication record.
  if (!parsed.confident) return "unclear";
  const intent = parsed.intent as Intent;
  return ["taken", "not_taken", "repeat", "unclear"].includes(intent) ? intent : "unclear";
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: CORS });
  if (req.method !== "POST") return json({ error: "POST only" }, 405);

  if (!SARVAM_KEY) {
    // Not configured yet. Say so plainly so the app can fall back to its buttons
    // instead of appearing broken.
    return json({ intent: "unclear", transcript: "", reply: "", configured: false });
  }

  try {
    const { audio, lang = "en", medicine = "" } = await req.json();
    if (!audio) return json({ error: "audio required" }, 400);

    const langKey = LANGS[lang] ? lang : "en";
    const transcript = await transcribe(b64ToBytes(audio), LANGS[langKey]);
    const intent = await classify(transcript, medicine);

    return json({
      transcript,
      intent,
      reply: REPLIES[langKey][intent],
      configured: true,
    });
  } catch (e) {
    // Never surface a partial result as a confident one.
    return json({
      intent: "unclear",
      transcript: "",
      reply: "",
      configured: true,
      error: String(e).slice(0, 200),
    });
  }
});
