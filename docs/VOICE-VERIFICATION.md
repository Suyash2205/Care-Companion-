# Spoken dose confirmation — verification record

Run against the deployed `voice` edge function with real audio, not mocks. Utterances
were synthesised with the macOS `Lekha` Hindi voice and sent as 16 kHz mono WAV, the
same format the app records.

## Results

| Said (Hindi) | Meaning | Expected | Got |
|---|---|---|---|
| हाँ, मैंने दवा ले ली | Yes, I took it | taken | **taken** |
| हाँ जी, मैंने अपनी दवा ले ली है | Yes, I have taken my medicine | taken | **taken** |
| नहीं, मैंने अभी तक दवा नहीं ली | No, I haven't taken it yet | not_taken | **not_taken** |
| अभी नहीं, थोड़ी देर बाद लूँगी | Not now, I'll take it later | not_taken | **not_taken** |
| पता नहीं, शायद ली होगी | Don't know, maybe I took it | unclear | **unclear** |
| क्या कहा आपने, फिर से बताइए | What did you say, tell me again | repeat | **repeat** |
| आज मौसम बहुत अच्छा है | The weather is nice today | unclear | **unclear** |

Transcription was exact in every case.

The last three matter most. Hedging and off-topic speech must not be forced into an
answer, because a wrong "taken" writes a false medication record that the guardian
then trusts.

## Two things this found

**The classifier model reasons before answering, and the reasoning is billed and
counted as output.** With `max_tokens: 60` the entire budget went on thinking and the
reply came back `finish_reason: "length"` with `content: null` — every utterance
classified as unclear while transcription was perfect.

**Structured JSON output was unreliable from that model**, returning truncated or
absent objects. Asking for a single bare word (`TAKEN` / `NOT_TAKEN` / `REPEAT` /
`UNCLEAR`) is robust because one token always terminates cleanly. The parser reads the
*last* label in the reply, since a reasoning model names several while weighing them.

## Not verified

- **Marathi and Gujarati.** macOS ships no voice for either, so no test audio could be
  synthesised. The language codes are wired but unproven.
- **Elderly speech.** These are synthetic utterances from a clean TTS voice. Real
  speakers of the target age, with regional accents and background noise, will be
  harder, and no published benchmark covers that population. This is the gap the
  planned user study should close.
- **On-device end to end.** The pipeline is verified from the server side; recording
  and playback on a physical handset are not.
