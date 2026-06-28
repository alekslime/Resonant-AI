/**
 * RESONANT — Voice-First AI Learning Assistant
 * app.js — ChatGPT-style pixel fluid orb + LiveKit + full a11y
 */

/* ═══════════════════════════════════════════════════════
   1. ORB RENDERER — pixel-shader fluid simulation
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

    // Each state has its own speed + streak intensity
    this.PARAMS = {
      idle:      { speed: 0.006, warp: 0.22, streakStr: 0.72 },
      listening: { speed: 0.013, warp: 0.36, streakStr: 0.85 },
      thinking:  { speed: 0.028, warp: 0.55, streakStr: 0.55 },
      speaking:  { speed: 0.020, warp: 0.44, streakStr: 0.78 },
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

  _lerp(a, b, k) {
    return a + (b - a) * Math.min(1, Math.max(0, k));
  }

  _clamp(v) {
    return Math.min(255, Math.max(0, v));
  }

  setState(s) {
    this.state = s;
    this.tgt = { ...this.PARAMS[s] };
  }

  setEnergy(v) {
    this.targetEnergy = Math.min(1, Math.max(0, v));
  }

  _frame() {
    this.raf = requestAnimationFrame(() => this._frame());

    // Lerp params toward target
    const k = 0.05;
    this.cur.speed     = this._lerp(this.cur.speed,     this.tgt.speed,     k);
    this.cur.warp      = this._lerp(this.cur.warp,      this.tgt.warp,      k);
    this.cur.streakStr = this._lerp(this.cur.streakStr, this.tgt.streakStr, k);
    this.energy        = this._lerp(this.energy, this.targetEnergy, 0.1);

    this.t += this.cur.speed * (1 + this.energy * 1.2);

    const { S, t, ctx } = this;
    const R = S / 2;
    const img = ctx.createImageData(S, S);
    const d   = img.data;
    const warp = this.cur.warp + this.energy * 0.15;
    const ss   = this.cur.streakStr;

    for (let py = 0; py < S; py++) {
      for (let px = 0; px < S; px++) {
        const dx = (px - R) / R;
        const dy = (py - R) / R;
        const dist = Math.sqrt(dx * dx + dy * dy);

        if (dist > 1) {
          // Outside circle — pure white
          const i = (py * S + px) * 4;
          d[i] = 255; d[i+1] = 255; d[i+2] = 255; d[i+3] = 255;
          continue;
        }

        // Warp space with noise
        const nx = dx + this._noise(dx * 1.4, dy * 1.4, t) * warp;
        const ny = dy + this._noise(dx * 1.4 + 4.2, dy * 1.4 + 1.7, t * 0.8) * warp;

        // Fluid value 0–1
        const fluid = (
          this._noise(nx * 1.6, ny * 1.6, t * 0.9) +
          this._noise(nx * 2.8 - 1.1, ny * 2.4 + 0.5, t * 1.1) * 0.6 +
          this._noise(nx * 0.9 + 2.0, ny * 1.1 - 1.3, t * 0.6) * 0.4
        ) / 2 + 0.5;

        // Diagonal cream/white streak
        const streak = this._noise(nx * 1.0 + ny * 0.8 + 1.5, ny * 1.0 - nx * 0.5, t * 0.55) * 0.5 + 0.5;
        const streakMask = Math.pow(Math.max(0, 1 - Math.abs(streak - 0.62) * 5), 1.8);

        // Color map — deep blue → bright cyan → cream
        let cr, cg, cb;
        const f = fluid;
        if (f < 0.25) {
          const tt = f / 0.25;
          cr = this._lerp(10, 15, tt); cg = this._lerp(60, 100, tt); cb = this._lerp(200, 230, tt);
        } else if (f < 0.55) {
          const tt = (f - 0.25) / 0.30;
          cr = this._lerp(15, 50, tt); cg = this._lerp(100, 160, tt); cb = this._lerp(230, 255, tt);
        } else if (f < 0.78) {
          const tt = (f - 0.55) / 0.23;
          cr = this._lerp(50, 120, tt); cg = this._lerp(160, 210, tt); cb = this._lerp(255, 255, tt);
        } else {
          const tt = (f - 0.78) / 0.22;
          cr = this._lerp(120, 230, tt); cg = this._lerp(210, 240, tt); cb = this._lerp(255, 255, tt);
        }

        // Inject cream streak
        const si = streakMask * ss;
        cr = this._lerp(cr, 245, si);
        cg = this._lerp(cg, 240, si);
        cb = this._lerp(cb, 235, si);

        // Rim darkening → spherical feel
        const rim = Math.pow(dist, 2.8) * 0.55;
        cr *= (1 - rim);
        cg *= (1 - rim);
        cb *= (1 - rim * 0.7);

        // Smooth circular clip at edge
        const edge = Math.pow(Math.max(0, 1 - dist * dist), 0.18);

        const i = (py * S + px) * 4;
        d[i]   = this._clamp(cr * edge + (1 - edge) * 255);
        d[i+1] = this._clamp(cg * edge + (1 - edge) * 255);
        d[i+2] = this._clamp(cb * edge + (1 - edge) * 255);
        d[i+3] = 255;
      }
    }

    ctx.putImageData(img, 0, 0);
  }

  start() {
    if (!this.raf) this._frame();
  }

  stop() {
    if (this.raf) { cancelAnimationFrame(this.raf); this.raf = null; }
  }
}


/* ═══════════════════════════════════════════════════════
   2. AUDIO ANALYSER — mic FFT → orb energy
═══════════════════════════════════════════════════════ */
class AudioAnalyser {
  constructor(orb) {
    this.orb    = orb;
    this.stream = null;
    this.actx   = null;
    this.raf    = null;
  }

  async start() {
    try {
      this.stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      this.actx   = new (window.AudioContext || window.webkitAudioContext)();
      const src   = this.actx.createMediaStreamSource(this.stream);
      this.analyser = this.actx.createAnalyser();
      this.analyser.fftSize = 256;
      this.analyser.smoothingTimeConstant = 0.80;
      src.connect(this.analyser);
      this.buf = new Uint8Array(this.analyser.frequencyBinCount);
      this._tick();
      return true;
    } catch (e) {
      console.warn('Mic unavailable:', e);
      return false;
    }
  }

  _tick() {
    this.raf = requestAnimationFrame(() => this._tick());
    if (!this.analyser) return;
    this.analyser.getByteFrequencyData(this.buf);
    const avg = this.buf.reduce((a, b) => a + b, 0) / this.buf.length / 255;
    this.orb.setEnergy(avg * 3.0);
  }

  stop() {
    if (this.raf) cancelAnimationFrame(this.raf);
    if (this.stream) this.stream.getTracks().forEach(t => t.stop());
    if (this.actx) this.actx.close();
    this.orb.setEnergy(0);
    this.raf = null;
  }
}


/* ═══════════════════════════════════════════════════════
   3. LIVEKIT SESSION
═══════════════════════════════════════════════════════ */
class VoiceSession {
  constructor() { this.room = null; this.connected = false; }

  async connect(onState) {
    try {
      const res  = await fetch('http://localhost:5000/api/token');
      const data = await res.json();
      if (!data.token || !data.url) throw new Error('No token');

      if (typeof LivekitClient === 'undefined') {
        await this._loadScript('https://cdn.jsdelivr.net/npm/livekit-client@2/dist/livekit-client.umd.min.js');
      }

      this.room = new LivekitClient.Room({ adaptiveStream: true, dynacast: true });

      this.room.on(LivekitClient.RoomEvent.TrackSubscribed, (track) => {
        if (track.kind === LivekitClient.Track.Kind.Audio) {
          track.attach();
          onState('speaking');
        }
      });
      this.room.on(LivekitClient.RoomEvent.TrackUnsubscribed, () => onState('listening'));
      this.room.on(LivekitClient.RoomEvent.Disconnected, () => { this.connected = false; onState('idle'); });

      await this.room.connect(data.url, data.token);
      await this.room.localParticipant.setMicrophoneEnabled(true);
      this.connected = true;
      onState('listening');
      return true;
    } catch (e) {
      console.warn('LiveKit demo mode:', e);
      onState('listening');
      return false;
    }
  }

  async disconnect() {
    if (this.room) { await this.room.disconnect(); this.room = null; }
    this.connected = false;
  }

  async toggleMic(enabled) {
    if (this.room) await this.room.localParticipant.setMicrophoneEnabled(enabled);
  }

  _loadScript(src) {
    return new Promise((res, rej) => {
      const s = document.createElement('script');
      s.src = src; s.onload = res; s.onerror = rej;
      document.head.appendChild(s);
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

    this.orb      = new OrbRenderer(this.canvas);
    this.analyser = new AudioAnalyser(this.orb);
    this.session  = new VoiceSession();

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
      await this.analyser.start();
      this.micActive = true;
      this.btnMic.setAttribute('aria-pressed', 'true');
      this.btnMic.setAttribute('aria-label', 'Mute microphone');
      this.btnMic.querySelector('.icon-mic').style.display     = 'none';
      this.btnMic.querySelector('.icon-mic-off').style.display = '';
      if (!this.session.connected) {
        await this.session.connect(s => this._setState(s));
      } else {
        await this.session.toggleMic(true);
        this._setState('listening');
      }
    } else {
      this.analyser.stop();
      await this.session.toggleMic(false);
      this.micActive = false;
      this.btnMic.setAttribute('aria-pressed', 'false');
      this.btnMic.setAttribute('aria-label', 'Start listening');
      this.btnMic.querySelector('.icon-mic').style.display     = '';
      this.btnMic.querySelector('.icon-mic-off').style.display = 'none';
      this._setState('idle');
    }
  }

  async _endSession() {
    this.analyser.stop();
    await this.session.disconnect();
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

  _bindEvents() {
    this.btnMic.addEventListener('click',       () => this._toggleMic());
    this.btnEnd.addEventListener('click',       () => this._endSession());
    this.btnSettings.addEventListener('click',  () => this._openSettings());
    this.settingsClose.addEventListener('click',() => this._closeSettings());

    this.settingsPanel.addEventListener('click', e => {
      if (e.target === this.settingsPanel) this._closeSettings();
    });

    this.chkContrast.addEventListener('change', () => {
      document.body.classList.toggle('high-contrast', this.chkContrast.checked);
    });

    this.chkMotion.addEventListener('change', () => {
      document.body.classList.toggle('reduce-motion', this.chkMotion.checked);
      if (this.chkMotion.checked) this.orb.stop(); else this.orb.start();
    });

    document.addEventListener('keydown', e => {
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

    // Focus trap in settings
    this.settingsPanel.addEventListener('keydown', e => {
      if (e.key !== 'Tab') return;
      const els = [...this.settingsPanel.querySelectorAll('button,select,input,[tabindex]:not([tabindex="-1"])')];
      const first = els[0], last = els[els.length - 1];
      if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
      else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
    });
  }

  _checkReducedMotion() {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      this.chkMotion.checked = true;
      document.body.classList.add('reduce-motion');
    }
  }

  // Gentle idle energy drift so orb looks alive without mic
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
