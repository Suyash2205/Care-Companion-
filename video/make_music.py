"""
make_music.py — the advertisement's score, synthesised from scratch.

Written here rather than licensed, so the film carries no third-party audio. A warm
pad on a slow four-chord cycle, a low drone, and a soft bell that marks each section.

    python video/make_music.py <seconds> <out.wav>
"""
import sys, math, wave, struct
import numpy as np

SR = 48000

def adsr(n, a, d, s, r, peak=1.0, sustain=0.72):
    env = np.zeros(n)
    ai, di, ri = int(a * SR), int(d * SR), int(r * SR)
    si = max(n - ai - di - ri, 0)
    env[:ai] = np.linspace(0, peak, ai)
    env[ai:ai + di] = np.linspace(peak, peak * sustain, di)
    env[ai + di:ai + di + si] = peak * sustain
    env[ai + di + si:] = np.linspace(peak * sustain, 0, n - ai - di - si)
    return env

def note(freq, dur, partials=(1, 2, 3, 4, 5), gains=(1, .42, .2, .1, .05), detune=0.0015):
    n = int(dur * SR)
    t = np.arange(n) / SR
    out = np.zeros(n)
    for p, g in zip(partials, gains):
        out += g * np.sin(2 * np.pi * freq * p * t)
        out += g * 0.55 * np.sin(2 * np.pi * freq * p * (1 + detune) * t + 0.7)
    return out / (sum(gains) * 1.55)

def bell(freq, dur):
    n = int(dur * SR)
    t = np.arange(n) / SR
    sig = (np.sin(2 * np.pi * freq * t) * np.exp(-3.1 * t)
           + 0.4 * np.sin(2 * np.pi * freq * 2.76 * t) * np.exp(-5.2 * t)
           + 0.2 * np.sin(2 * np.pi * freq * 5.4 * t) * np.exp(-7.5 * t))
    return sig * np.minimum(1.0, t * 400)

def reverb(x, mix=0.3):
    """Cheap, smooth tail: a handful of decaying delayed copies."""
    out = x.copy()
    for delay_ms, gain in ((37, .34), (71, .26), (113, .2), (173, .15), (251, .11)):
        d = int(delay_ms * SR / 1000)
        tail = np.zeros_like(x)
        tail[d:] = x[:-d] * gain
        out += tail * mix
    return out

def lowpass(x, cutoff=2600):
    """A short Gaussian FIR: smooth, phase-linear, and fast enough for a whole film."""
    sigma = SR / (2 * math.pi * cutoff)
    half = max(2, int(sigma * 3))
    k = np.exp(-0.5 * (np.arange(-half, half + 1) / sigma) ** 2)
    k /= k.sum()
    return np.convolve(x, k, mode="same")


NOTE = lambda n: 440.0 * (2 ** ((n - 69) / 12))      # MIDI number to Hz

def build(total):
    n = int(total * SR)
    mix = np.zeros(n + SR)
    # Four-chord cycle, 8 s each: Am(9) - F(maj7) - C - G(sus2). Warm and unresolved.
    chords = [[57, 64, 69, 72, 76], [53, 60, 65, 69, 72],
              [48, 60, 64, 67, 72], [55, 62, 67, 71, 74]]
    bar = 8.0
    i = 0
    while i * bar < total:
        ch = chords[i % len(chords)]
        start = int(i * bar * SR)
        dur = bar * 1.25                              # let each chord bleed into the next
        seg = np.zeros(int(dur * SR))
        for k, m in enumerate(ch):
            voice = note(NOTE(m), dur) * adsr(int(dur * SR), 1.5, 2.0, 0, 3.2,
                                              peak=0.9 - 0.1 * k)
            seg += voice
        seg /= len(ch)
        end = min(start + len(seg), len(mix))
        if end > start:
            mix[start:end] += seg[:end - start] * 0.9
        # a single soft bell on the downbeat of every other bar
        if i % 2 == 0:
            b = bell(NOTE(ch[-1] + 12), 3.0) * 0.16
            e2 = min(start + len(b), len(mix))
            if e2 > start:
                mix[start:e2] += b[:e2 - start]
        i += 1
    # low drone, one octave under the tonic
    t = np.arange(len(mix)) / SR
    mix += 0.1 * np.sin(2 * np.pi * NOTE(33) * t) * (0.6 + 0.4 * np.sin(2 * np.pi * t / 17))
    # a breath of air
    rng = np.random.default_rng(11)
    air = rng.normal(0, 1, len(mix))
    air = np.convolve(air, np.ones(900) / 900, mode="same")
    mix += air * 0.05

    mix = lowpass(mix, 2400)
    mix = reverb(mix, 0.33)
    mix = mix[:n]
    # fade the ends, then normalise to a level that sits under a voice
    f = int(2.2 * SR)
    mix[:f] *= np.linspace(0, 1, f)
    mix[-f:] *= np.linspace(1, 0, f)
    mix /= max(abs(mix).max(), 1e-9)
    return mix * 0.52

def write(path, mono):
    st = np.stack([mono, mono], axis=1)
    data = (np.clip(st, -1, 1) * 32767).astype("<i2").tobytes()
    with wave.open(path, "wb") as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(data)

if __name__ == "__main__":
    total = float(sys.argv[1]); out = sys.argv[2]
    write(out, build(total))
    print("Wrote:", out, f"{total:.1f}s")
