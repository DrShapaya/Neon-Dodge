// Generates the game's audio assets without third-party tools.
// Sound effects stay as WAV for low latency. The 20-second music loop is
// generated as WAV into tools/audio-src and then encoded to OGG by
// tools/encode-audio.ps1, which cuts the APK by about 600 KB.
// Usage: node tools/make_audio.js && pwsh tools/encode-audio.ps1
const fs = require('fs');
const path = require('path');

const OUT = path.join(__dirname, '..', 'app', 'src', 'main', 'res', 'raw');
const SRC = path.join(__dirname, 'audio-src');
fs.mkdirSync(SRC, { recursive: true });

function writeWav(file, samples, sampleRate) {
  const n = samples.length;
  const buf = Buffer.alloc(44 + n * 2);
  buf.write('RIFF', 0);
  buf.writeUInt32LE(36 + n * 2, 4);
  buf.write('WAVE', 8);
  buf.write('fmt ', 12);
  buf.writeUInt32LE(16, 16);
  buf.writeUInt16LE(1, 20);          // PCM
  buf.writeUInt16LE(1, 22);          // mono
  buf.writeUInt32LE(sampleRate, 24);
  buf.writeUInt32LE(sampleRate * 2, 28);
  buf.writeUInt16LE(2, 32);
  buf.writeUInt16LE(16, 34);
  buf.write('data', 36);
  buf.writeUInt32LE(n * 2, 40);
  for (let i = 0; i < n; i++) {
    let s = Math.max(-1, Math.min(1, samples[i]));
    if (s > 0) s = Math.min(1, s);
    buf.writeInt16LE(Math.round(s * 32767), 44 + i * 2);
  }
  fs.writeFileSync(file, buf);
  console.log(`${path.basename(file)}: ${(buf.length / 1024).toFixed(1)} KB, ${(n / sampleRate).toFixed(2)} s`);
}

function normalize(samples, peak) {
  let max = 0;
  for (const s of samples) max = Math.max(max, Math.abs(s));
  if (max < 1e-9) return;
  const k = peak / max;
  for (let i = 0; i < samples.length; i++) samples[i] *= k;
}

// ---------------------------------------------------------------- coin ding
function makeCoin() {
  const sr = 44100;
  const dur = 0.80;
  const n = Math.floor(sr * dur);
  const out = new Float32Array(n);
  // A soft major-third bell: C6 + E6 with a light shimmer.
  const partials = [
    { f: 1046.50, a: 0.55, tau: 0.42, p: 0.0 },
    { f: 1318.51, a: 0.40, tau: 0.35, p: 0.7 },
    { f: 2093.00, a: 0.16, tau: 0.26, p: 1.3 },
    { f: 2637.02, a: 0.09, tau: 0.19, p: 2.1 },
    { f: 3951.07, a: 0.04, tau: 0.13, p: 0.4 },
  ];
  for (let i = 0; i < n; i++) {
    const t = i / sr;
    const attack = 1 - Math.exp(-t / 0.0035);
    let s = 0;
    for (const q of partials) {
      const env = Math.exp(-t / q.tau);
      s += q.a * env * Math.sin(2 * Math.PI * q.f * t + q.p);
      // A quiet octave partial makes the bell sound rounder.
      s += q.a * 0.13 * env * Math.sin(4 * Math.PI * q.f * t + q.p * 1.7);
    }
    // Tiny second strike gives the coin a lively "tick-ding".
    const t2 = t - 0.055;
    if (t2 > 0) {
      const e2 = Math.exp(-t2 / 0.22);
      s += 0.16 * e2 * Math.sin(2 * Math.PI * 1760.0 * t2);
    }
    const fade = Math.min(1, (dur - t) / 0.12);
    out[i] = s * attack * Math.max(0, fade);
  }
  normalize(out, 0.82);
  writeWav(path.join(OUT, 'coin.wav'), out, sr);
}

// ---------------------------------------------------------------- music loop
function makeMusic(outputName = 'music.wav', variant = 0) {
  const sr = 44100;
  const bpm = variant ? 112 : 96;
  const beat = 60 / bpm;             // 0.625 s
  const bars = 8;
  const dur = bars * 4 * beat;       // 20 s
  const n = Math.floor(sr * dur);
  // Buffer includes extra time for natural note release tail, which is wrapped back for a seamless loop.
  const tailSec = 2.0;
  const totalSamples = n + Math.floor(sr * tailSec);
  const raw = new Float32Array(totalSamples);

  const A2 = 110.00, C3 = 130.81, E3 = 164.81;
  const F2 = 87.31, G2 = 98.00, B2 = 123.47, D3 = 146.83;
  const chords = variant ? [
    { bass: 73.42, notes: [D3, 185.00, 220.00] }, // Dm
    { bass: 55.00, notes: [A2, C3, E3] },         // Am
    { bass: 65.41, notes: [C3, E3, 196.00] },     // C
    { bass: 49.00, notes: [G2, B2, D3] },         // G
    { bass: 73.42, notes: [D3, 185.00, 220.00] },
    { bass: 55.00, notes: [A2, C3, E3] },
    { bass: 65.41, notes: [C3, E3, 196.00] },
    { bass: 49.00, notes: [G2, B2, D3] },
  ] : [
    { bass: 55.00, notes: [A2, C3, E3] },  // Am
    { bass: 43.65, notes: [F2, A2, C3] },  // F
    { bass: 65.41, notes: [C3, E3, 196.00] }, // C
    { bass: 49.00, notes: [G2, B2, D3] },  // G
    { bass: 55.00, notes: [A2, C3, E3] },  // Am
    { bass: 43.65, notes: [F2, A2, C3] },  // F
    { bass: 65.41, notes: [C3, E3, 196.00] }, // C
    { bass: 49.00, notes: [G2, B2, D3] },  // G
  ];

  const barLen = 4 * beat;

  function addPad() {
    for (let b = 0; b < bars; b++) {
      const chord = chords[b];
      const start = b * barLen;
      // Allow slight crossfade between bars for smooth transition without click
      const crossfade = 0.25;
      const len = barLen + crossfade;
      for (const f of chord.notes) {
        for (const det of [-0.002, 0.002]) {
          const freq = f * (1 + det);
          for (let i = Math.floor(start * sr); i < Math.min(totalSamples, (start + len) * sr); i++) {
            const t = i / sr;
            const local = t - start;
            // Smooth attack and smooth release to zero
            const attack = Math.min(1, local / 0.50);
            const release = Math.max(0, Math.min(1, (len - local) / crossfade));
            const env = attack * release;
            raw[i] += 0.045 * env * Math.sin(2 * Math.PI * freq * local);
            raw[i] += 0.012 * env * Math.sin(4 * Math.PI * freq * local);
          }
        }
      }
    }
  }

  function addBass() {
    for (let b = 0; b < bars; b++) {
      const f = chords[b].bass;
      for (let beatIx = 0; beatIx < 4; beatIx++) {
        const start = b * barLen + beatIx * beat;
        const len = beat * 0.95;
        const releaseTime = 0.06;
        const accent = beatIx % 2 === 0 ? 1 : 0.65;
        for (let i = Math.floor(start * sr); i < Math.min(totalSamples, (start + len) * sr); i++) {
          const t = i / sr;
          const local = t - start;
          const attack = Math.min(1, local / 0.015);
          const decay = Math.exp(-local / 0.38);
          const release = Math.max(0, Math.min(1, (len - local) / releaseTime));
          const env = attack * decay * release;
          // Zero-phase start eliminates clicks
          raw[i] += 0.090 * accent * env * Math.sin(2 * Math.PI * f * local);
          raw[i] += 0.018 * accent * env * Math.sin(4 * Math.PI * f * local);
        }
      }
    }
  }

  function addArp() {
    for (let b = 0; b < bars; b++) {
      const chord = chords[b];
      const pattern = variant
        ? [0, 2, 1, 2, 0, 1, 2, 1]
        : [0, 1, 2, 1, 2, 0, 1, 2];
      for (let step = 0; step < 8; step++) {
        const start = b * barLen + step * beat * 0.5;
        const idx = pattern[step] % chord.notes.length;
        const octave = step >= 4 ? 4 : 2;
        const f = chord.notes[idx] * octave;
        const len = beat * 0.48;
        const releaseTime = 0.04;
        for (let i = Math.floor(start * sr); i < Math.min(totalSamples, (start + len) * sr); i++) {
          const t = i / sr;
          const local = t - start;
          const attack = Math.min(1, local / 0.008);
          const decay = Math.exp(-local / 0.18);
          const release = Math.max(0, Math.min(1, (len - local) / releaseTime));
          const env = attack * decay * release;
          raw[i] += 0.048 * env * Math.sin(2 * Math.PI * f * local);
          raw[i] += 0.010 * env * Math.sin(4 * Math.PI * f * local);
        }
      }
    }
  }

  function addShimmer() {
    const events = [
      ...(variant
        ? [[0.95, 1, 587.33], [3.1, 2, 739.99], [5.6, 3, 880.0],
          [8.1, 4, 659.25], [10.6, 5, 783.99], [13.1, 6, 987.77],
          [15.6, 7, 880.0], [17.2, 8, 1174.66]]
        : [[1.25, 1, 660.0], [3.75, 2, 523.25], [6.25, 3, 783.99],
          [8.75, 4, 659.25], [11.25, 5, 523.25], [13.75, 6, 783.99],
          [16.25, 7, 659.25], [18.25, 8, 880.00]]),
    ];
    for (const [start, bar, f] of events) {
      const gain = 0.028 + (bar % 2) * 0.005;
      const len = 1.6;
      const releaseTime = 0.25;
      for (let i = Math.floor(start * sr); i < Math.min(totalSamples, (start + len) * sr); i++) {
        const t = i / sr;
        const local = t - start;
        const attack = Math.min(1, local / 0.20);
        const decay = Math.exp(-local / 0.75);
        const release = Math.max(0, Math.min(1, (len - local) / releaseTime));
        const env = attack * decay * release;
        raw[i] += gain * env * Math.sin(2 * Math.PI * f * local);
        raw[i] += gain * 0.28 * env * Math.sin(4 * Math.PI * f * local);
      }
    }
  }

  addPad();
  addBass();
  addArp();
  addShimmer();

  // Wrap the release tail onto the start of the loop for seamless looping
  const out = new Float32Array(n);
  for (let i = 0; i < n; i++) {
    out[i] = raw[i];
  }
  for (let i = n; i < totalSamples; i++) {
    const wrapIndex = i - n;
    if (wrapIndex < n) {
      out[wrapIndex] += raw[i];
    }
  }

  // Smooth two-pole low-pass filter to remove digital harshness
  let lp1 = 0, lp2 = 0;
  const cutoff = 4800;
  const alpha = 1 - Math.exp(-2 * Math.PI * cutoff / sr);
  for (let i = 0; i < n; i++) {
    lp1 += alpha * (out[i] - lp1);
    lp2 += alpha * (lp1 - lp2);
    out[i] = lp2;
  }

  normalize(out, 0.75);
  writeWav(path.join(SRC, outputName), out, sr);
}

makeCoin();
makeMusic('music.wav', 0);
makeMusic('music_alt.wav', 1);
