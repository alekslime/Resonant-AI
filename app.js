/**
 * RESONANT — Voice-First AI Learning Assistant
 * app.js — ChatGPT-style pixel fluid orb + local voice pipeline + full a11y
 * Colors: #f9952a (amber) → #f9c22a (yellow) with state animations
 */

/* ═══════════════════════════════════════════════════════
   1. ORB RENDERER — pixel-shader fluid + state animations
═══════════════════════════════════════════════════════ */
class OrbRenderer {
  constructor(canvas) {
    this.canvas = canvas;
    this.ctx    = canvas.getContext('2d');
    this.S      = 300;
    this.t      = 0;
    this.state  = 'idle';
    this.energy = 0;
    this.targetEnergy = 0;
    this.raf    = null;
    this.paused  = false;

    this.PARAMS = {
      idle:      { speed: 0.005, warp: 0.20, pulse: 0.00, turbulence: 0.0 },
      listening: { speed: 0.012, warp: 0.34, pulse: 0.06, turbulence: 0.0 },
      thinking:  { speed: 0.030, warp: 0.58, pulse: 0.00, turbulence: 0.4 },
      speaking:  { speed: 0.020, warp: 0.42, pulse: 0.10, turbulence: 0.1 },
    };

    this.cur = { ...this.PARAMS.idle };
    this.tgt = { ...this.PARAMS.idle };

    canvas.width  = this.S;
    canvas.height = this.S;
  }

  _noise(x, y, z) {
    return (
      Math.sin(x * 1.2 + z) * Math.cos(y * 0.9 + z * 0.7) +
      Math.sin(x * 2.3 - y * 1.1 + z * 1.3) * 0.5 +
      Math.cos(x * 0.7 + y * 2.1 + z * 0.9) * 0.5
    ) / 2;
  }

  _lerp(a, b, k) { return a + (b - a) * Math.min(1, Math.max(0, k)); }
  _clamp(v)       { return Math.min(255, Math.max(0, v)); }

  setState(s) {
    this.state = s;
    this.tgt   = { ...this.PARAMS[s] };
    this.canvas.className = `state-${s}`;
  }

  setEnergy(v) {
    this.targetEnergy = Math.min(1, Math.max(0, v));
  }

  _frame() {
    this.raf = requestAnimationFrame(() => this._frame());
    // Fix 8: skip heavy pixel loop when reduce-motion is active
    if (this.paused) return;

    // Smooth lerp between states
    const k = 0.04;
    this.cur.speed      = this._lerp(this.cur.speed,      this.tgt.speed,      k);
    this.cur.warp       = this._lerp(this.cur.warp,       this.tgt.warp,       k);
    this.cur.pulse      = this._lerp(this.cur.pulse,      this.tgt.pulse,      k);
    this.cur.turbulence = this._lerp(this.cur.turbulence, this.tgt.turbulence, k);
    this.energy         = this._lerp(this.energy, this.targetEnergy, 0.1);

    this.t += this.cur.speed * (1 + this.energy * 1.2);

    const { S, t, ctx, state } = this;
    const R = S / 2;

    // Breathe scale per state
    const breathe =
      state === 'idle'      ? 1 + Math.sin(t * 0.8)  * 0.025 :
      state === 'listening' ? 1 + Math.sin(t * 1.8)  * 0.040 :
      state === 'speaking'  ? 1 + Math.abs(Math.sin(t * 4.5)) * this.cur.pulse :
                              1 + Math.sin(t * 3.5)  * 0.030;

    const img = ctx.createImageData(S, S);
    const d   = img.data;

    for (let py = 0; py < S; py++) {
      for (let px = 0; px < S; px++) {
        const rawDx = (px - R) / R;
        const rawDy = (py - R) / R;
        const dx    = rawDx / breathe;
        const dy    = rawDy / breathe;
        const dist  = Math.sqrt(rawDx * rawDx + rawDy * rawDy);

        // Hard crisp circular clip — exactly like ChatGPT
        if (dist > 1) {
          const i = (py * S + px) * 4;
          d[i] = 0; d[i+1] = 0; d[i+2] = 0; d[i+3] = 0;
          continue;
        }

        const warp = this.cur.warp + this.energy * 0.15;

        // Extra turbulence for thinking
        const turb = this.cur.turbulence > 0.01
          ? this._noise(dx * 3.5 + t * 0.5, dy * 3.5 - t * 0.3, t * 2.1) * this.cur.turbulence
          : 0;

        const nx = dx + this._noise(dx * 1.4, dy * 1.4, t) * warp + turb;
        const ny = dy + this._noise(dx * 1.4 + 4.2, dy * 1.4 + 1.7, t * 0.8) * warp + turb;

        const fluid = (
          this._noise(nx * 1.6, ny * 1.6, t * 0.9) +
          this._noise(nx * 2.8 - 1.1, ny * 2.4 + 0.5, t * 1.1) * 0.6 +
          this._noise(nx * 0.9 + 2.0, ny * 1.1 - 1.3, t * 0.6) * 0.4
        ) / 2 + 0.5;

        // Small white streak — same as ChatGPT
        const streak     = this._noise(nx * 1.0 + ny * 0.8 + 1.5, ny * 1.0 - nx * 0.5, t * 0.55) * 0.5 + 0.5;
        const streakMask = Math.pow(Math.max(0, 1 - Math.abs(streak - 0.62) * 5), 1.8);

        // Color map: deep amber → #f9952a → #f9c22a → warm white
        let cr, cg, cb;
        const f = fluid;
        if (f < 0.25) {
          const tt = f / 0.25;
          cr = this._lerp(180, 249, tt); cg = this._lerp(80,  149, tt); cb = this._lerp(0,  42,  tt);
        } else if (f < 0.55) {
          const tt = (f - 0.25) / 0.30;
          cr = this._lerp(249, 249, tt); cg = this._lerp(149, 194, tt); cb = this._lerp(42, 42,  tt);
        } else if (f < 0.78) {
          const tt = (f - 0.55) / 0.23;
          cr = this._lerp(249, 253, tt); cg = this._lerp(194, 230, tt); cb = this._lerp(42, 160, tt);
        } else {
          const tt = (f - 0.78) / 0.22;
          cr = this._lerp(253, 255, tt); cg = this._lerp(230, 250, tt); cb = this._lerp(160, 230, tt);
        }

        // Small white streak
        const si = streakMask * 0.35;
        cr = this._lerp(cr, 255, si);
        cg = this._lerp(cg, 252, si);
        cb = this._lerp(cb, 230, si);

        // Speaking: color brightens on beat
        if (state === 'speaking') {
          const beat = Math.abs(Math.sin(t * 4.5)) * 0.18;
          cr = this._clamp(cr + beat * 30);
          cg = this._clamp(cg + beat * 15);
        }

        // Thinking: slight shift to deeper amber
        if (state === 'thinking') {
          cr = this._lerp(cr, 220, 0.15);
          cg = this._lerp(cg, 110, 0.15);
          cb = this._lerp(cb, 20,  0.15);
        }

        const i = (py * S + px) * 4;
        d[i]   = this._clamp(cr);
        d[i+1] = this._clamp(cg);
        d[i+2] = this._clamp(cb);
        d[i+3] = 255;
      }
    }

    ctx.putImageData(img, 0, 0);
  }

  pause()  { this.paused = true; }
  resume() { this.paused = false; }

  start() { if (!this.raf) this._frame(); }

  stop() {
    if (this.raf) { cancelAnimationFrame(this.raf); this.raf = null; }
  }
}


/* ═══════════════════════════════════════════════════════
   2. VOICE CONTROLLER — local mic capture + silence-VAD + backend turns
      Captures the mic once, drives the orb's energy from it, and uses a
      simple energy-based VAD to detect when an utterance ends. Each
      utterance is POSTed to the local Flask backend (/api/voice), which
      runs Whisper -> local LLM -> Piper and returns transcript + reply +
      a WAV to play back. No cloud realtime infra involved.
═══════════════════════════════════════════════════════ */
class VoiceController {
  constructor(orb, history) {
    this.orb      = orb;
    this.stream   = null;
    this.actx     = null;
    this.analyser = null;
    this.buf      = null;
    this.raf      = null;

    this.recorder = null;
    this.chunks   = [];

    this.active   = false;  // mic toggled on
    this.speaking = false;  // currently playing back a reply
    this.history  = history || [];  // shared with text chat if passed in

    // VAD tuning — tweak SILENCE_THRESHOLD if it cuts off too eagerly/late
    this.SILENCE_THRESHOLD = 0.02;
    this.SILENCE_MS        = 900;  // silence needed to end an utterance
    this.MIN_SPEECH_MS     = 300;  // ignore blips shorter than this
    this._speechStart  = null;
    this._silenceStart = null;
  }

  async start(onState) {
    this.onState = onState;
    try {
      this.stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    } catch (e) {
      console.warn('Mic unavailable:', e);
      return false;
    }

    this.actx = new (window.AudioContext || window.webkitAudioContext)();
    const src = this.actx.createMediaStreamSource(this.stream);
    this.analyser = this.actx.createAnalyser();
    this.analyser.fftSize = 512;
    this.analyser.smoothingTimeConstant = 0.75;
    src.connect(this.analyser);
    this.buf = new Uint8Array(this.analyser.frequencyBinCount);

    this.active = true;
    this._armRecorder();
    this._tick();
    this.onState('listening');
    return true;
  }

  stop() {
    this.active = false;
    if (this.raf) cancelAnimationFrame(this.raf);
    if (this.recorder && this.recorder.state !== 'inactive') this.recorder.stop();
    if (this.stream) this.stream.getTracks().forEach(t => t.stop());
    if (this.actx) this.actx.close();
    this.orb.setEnergy(0);
    this.raf = null;
    this.recorder = null;
  }

  _armRecorder() {
    this.chunks = [];
    this.recorder = new MediaRecorder(this.stream);
    this.recorder.ondataavailable = e => { if (e.data.size > 0) this.chunks.push(e.data); };
    this.recorder.onstop = () => this._onUtteranceEnd();
    this.recorder.start();
    this._speechStart  = null;
    this._silenceStart = null;
  }

  _tick() {
    this.raf = requestAnimationFrame(() => this._tick());
    if (!this.analyser) return;
    this.analyser.getByteFrequencyData(this.buf);
    const avg = this.buf.reduce((a, b) => a + b, 0) / this.buf.length / 255;
    this.orb.setEnergy(avg * 3.0);

    // Don't run VAD while the assistant is talking (avoid self-triggering)
    if (this.speaking || !this.active) return;

    const now = performance.now();
    const isSpeech = avg > this.SILENCE_THRESHOLD;

    if (isSpeech) {
      if (this._speechStart === null) this._speechStart = now;
      this._silenceStart = null;
    } else if (this._speechStart !== null) {
      if (this._silenceStart === null) this._silenceStart = now;
      const spokeLongEnough  = (now - this._speechStart) > this.MIN_SPEECH_MS;
      const silentLongEnough = (now - this._silenceStart) > this.SILENCE_MS;
      if (spokeLongEnough && silentLongEnough && this.recorder && this.recorder.state === 'recording') {
        this.recorder.stop(); // -> _onUtteranceEnd
      }
    }
  }

  async _onUtteranceEnd() {
    const blob = new Blob(this.chunks, { type: 'audio/webm' });
    this.chunks = [];

    if (blob.size < 2000) { // too short to be real speech — keep listening
      if (this.active) this._armRecorder();
      return;
    }

    this.onState('thinking');

    try {
      const form = new FormData();
      form.append('audio', blob, 'utterance.webm');
      form.append('history', JSON.stringify(this.history));

      const res  = await fetch('/api/voice', { method: 'POST', body: form });
      const data = await res.json();
      if (data.error) throw new Error(data.error);

      if (!data.transcript) {
        this.onState(this.active ? 'listening' : 'idle');
      } else {
        this.history.push({ role: 'user', content: data.transcript });
        this.history.push({ role: 'assistant', content: data.reply });
        await this._playReply(data.audio);
      }
    } catch (e) {
      console.warn('Voice turn failed:', e);
      this.onState('idle');
    }

    if (this.active) this._armRecorder();
  }

  _playReply(base64Wav) {
    return new Promise(resolve => {
      if (!base64Wav) { this.onState(this.active ? 'listening' : 'idle'); resolve(); return; }
      this.speaking = true;
      this.onState('speaking');
      const audio = new Audio('data:audio/wav;base64,' + base64Wav);
      const done = () => {
        this.speaking = false;
        this.onState(this.active ? 'listening' : 'idle');
        resolve();
      };
      audio.onended = done;
      audio.onerror = done;
      audio.play().catch(done);
    });
  }
}


/* ═══════════════════════════════════════════════════════
   4. UI — state machine + ARIA + keyboard
═══════════════════════════════════════════════════════ */
class ResonantUI {
  constructor() {
    this.canvas        = document.getElementById('orb-canvas');
    this.stateLabel    = document.getElementById('state-label');
    this.srLive        = document.getElementById('sr-live');
    this.btnMic        = document.getElementById('btn-mic');
    this.btnEnd        = document.getElementById('btn-end');
    this.btnSettings   = document.getElementById('btn-settings');
    this.settingsPanel = document.getElementById('settings-panel');
    this.settingsClose = document.getElementById('settings-close');
    this.chkContrast   = document.getElementById('high-contrast');
    this.chkMotion     = document.getElementById('reduce-motion');

    this.chatLog   = document.getElementById('chat-log');
    this.appRoot   = document.getElementById('voice-app');
    this.textForm  = document.getElementById('text-form');
    this.textInput = document.getElementById('text-input');
    this.btnSend   = document.getElementById('btn-send');

    this.orb     = new OrbRenderer(this.canvas);
    this.history = [];  // shared conversation history — text + voice both append here
    this.voice   = new VoiceController(this.orb, this.history);

    this.micActive    = false;
    this.settingsOpen = false;

    this._bindEvents();
    this._checkReducedMotion();
    this.orb.start();
    this._setState('idle');
    this._idlePulse();
  }

  _setState(state) {
    this.orb.setState(state);
    this.appRoot.dataset.state = state;
    const labels = { idle: 'Ready', listening: 'Listening', thinking: 'Thinking…', speaking: 'Speaking' };
    const label  = labels[state] || state;

    if (state === 'idle') {
      this.stateLabel.classList.remove('visible');
    } else {
      this.stateLabel.textContent = label;
      this.stateLabel.classList.add('visible');
    }

    this._announce(state === 'idle'
      ? 'Resonant ready. Press Space to begin.'
      : `Resonant is ${label}`);
  }

  _announce(msg) {
    this.srLive.textContent = '';
    requestAnimationFrame(() => { this.srLive.textContent = msg; });
  }

  async _toggleMic() {
    if (!this.micActive) {
      const ok = await this.voice.start(s => this._setState(s));
      this.micActive = ok;
      this.btnMic.setAttribute('aria-pressed', String(ok));
      this.btnMic.setAttribute('aria-label', ok ? 'Mute microphone' : 'Start listening');
      this.btnMic.querySelector('.icon-mic').style.display     = ok ? 'none' : '';
      this.btnMic.querySelector('.icon-mic-off').style.display = ok ? '' : 'none';
      if (!ok) this._announce('Microphone unavailable. Check browser permissions.');
    } else {
      this.voice.stop();
      this.micActive = false;
      this.btnMic.setAttribute('aria-pressed', 'false');
      this.btnMic.setAttribute('aria-label', 'Start listening');
      this.btnMic.querySelector('.icon-mic').style.display     = '';
      this.btnMic.querySelector('.icon-mic-off').style.display = 'none';
      this._setState('idle');
    }
  }

  _endSession() {
    this.voice.stop();
    this.micActive = false;
    this.btnMic.setAttribute('aria-pressed', 'false');
    this.btnMic.setAttribute('aria-label', 'Start listening');
    this.btnMic.querySelector('.icon-mic').style.display     = '';
    this.btnMic.querySelector('.icon-mic-off').style.display = 'none';
    this._setState('idle');
    this._announce('Session ended. Press Space to start again.');
  }

  _openSettings() {
    this.settingsOpen = true;
    this.settingsPanel.setAttribute('aria-hidden', 'false');
    this.settingsPanel.focus();
  }

  _closeSettings() {
    this.settingsOpen = false;
    this.settingsPanel.setAttribute('aria-hidden', 'true');
    this.btnSettings.focus();
  }

  _renderMessage(role, content) {
    this.appRoot.classList.add('chat-active');
    const div = document.createElement('div');
    div.className = `chat-msg chat-msg--${role}`;
    div.textContent = content;
    this.chatLog.appendChild(div);
    this.chatLog.scrollTop = this.chatLog.scrollHeight;
  }

  async _sendText() {
    const text = this.textInput.value.trim();
    if (!text) return;

    this.textInput.value = '';
    this.textInput.disabled = true;
    this.btnSend.disabled = true;

    this._renderMessage('user', text);
    this.history.push({ role: 'user', content: text });
    this._setState('thinking');

    try {
      const res  = await fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ history: this.history }),
      });
      const data = await res.json();
      if (data.error) throw new Error(data.error);

      this.history.push({ role: 'assistant', content: data.reply });
      this._renderMessage('assistant', data.reply);
      this._setState('idle');
    } catch (e) {
      console.warn('Text chat failed:', e);
      this._renderMessage('error', `Something went wrong: ${e.message}`);
      this._setState('idle');
    } finally {
      this.textInput.disabled = false;
      this.btnSend.disabled = false;
      this.textInput.focus();
    }
  }

  _bindEvents() {
    this.textForm.addEventListener('submit', e => {
      e.preventDefault();
      this._sendText();
    });

    this.btnMic.addEventListener('click',        () => this._toggleMic());
    this.btnEnd.addEventListener('click',        () => this._endSession());
    this.btnSettings.addEventListener('click',   () => this._openSettings());
    this.settingsClose.addEventListener('click', () => this._closeSettings());

    this.settingsPanel.addEventListener('click', e => {
      if (e.target === this.settingsPanel) this._closeSettings();
    });

    this.chkContrast.addEventListener('change', () => {
      document.body.classList.toggle('high-contrast', this.chkContrast.checked);
    });

    this.chkMotion.addEventListener('change', () => {
      document.body.classList.toggle('reduce-motion', this.chkMotion.checked);
      // Fix 8: pause pixel loop (not full stop) so the orb canvas stays visible
      if (this.chkMotion.checked) this.orb.pause(); else this.orb.resume();
    });

    document.addEventListener('keydown', e => {
      if (e.target === this.textInput) return; // let normal typing through
      if (this.settingsOpen && e.target !== document.body) return;
      switch (e.key) {
        case ' ':
        case 'Enter':
          if (e.target === document.body || e.target === this.btnMic) {
            e.preventDefault(); this._toggleMic();
          }
          break;
        case 'Escape':
          this.settingsOpen ? this._closeSettings() : this._endSession();
          break;
        case 's': case 'S':
          if (!this.settingsOpen) this._openSettings();
          break;
        case 'm': case 'M':
          this._toggleMic();
          break;
      }
    });

    this.settingsPanel.addEventListener('keydown', e => {
      if (e.key !== 'Tab') return;
      const els   = [...this.settingsPanel.querySelectorAll('button,select,input,[tabindex]:not([tabindex="-1"])')];
      const first = els[0], last = els[els.length - 1];
      if (e.shiftKey && document.activeElement === first)      { e.preventDefault(); last.focus(); }
      else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
    });
  }

  _checkReducedMotion() {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      this.chkMotion.checked = true;
      document.body.classList.add('reduce-motion');
    }
  }

  _idlePulse() {
    let pt = 0;
    const tick = () => {
      if (!this.micActive) {
        pt += 0.007;
        this.orb.setEnergy((Math.sin(pt) * 0.5 + 0.5) * 0.10);
      }
      requestAnimationFrame(tick);
    };
    tick();
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.resonant = new ResonantUI();
});
