/*
 * RESONANT — app.js
 * No canvas, no pixel shader. State lives on data-state and CSS does the rest.
 */

/* ═══════════════════════════════════════
   1. VOICE CONTROLLER
═══════════════════════════════════════ */
class VoiceController {
  constructor(history) {
    this.stream   = null;
    this.actx     = null;
    this.recorder = null;
    this.chunks   = [];
    this.active   = false;
    this.speaking = false;
    this.history  = history || [];

    this.SILENCE_THRESHOLD = 0.02;
    this.SILENCE_MS        = 900;
    this.MIN_SPEECH_MS     = 300;
    this._speechStart  = null;
    this._silenceStart = null;
    this._analyser     = null;
    this._buf          = null;
    this._raf          = null;
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
    this._analyser = this.actx.createAnalyser();
    this._analyser.fftSize = 512;
    this._analyser.smoothingTimeConstant = 0.75;
    src.connect(this._analyser);
    this._buf = new Uint8Array(this._analyser.frequencyBinCount);

    this.active = true;
    this._armRecorder();
    this._tick();
    this.onState('listening');
    return true;
  }

  stop() {
    this.active = false;
    if (this._raf) cancelAnimationFrame(this._raf);
    if (this.recorder && this.recorder.state !== 'inactive') this.recorder.stop();
    if (this.stream) this.stream.getTracks().forEach(t => t.stop());
    if (this.actx) this.actx.close();
    this._raf = null; this.recorder = null;
  }

  _armRecorder() {
    this.chunks = [];
    this.recorder = new MediaRecorder(this.stream);
    this.recorder.ondataavailable = e => { if (e.data.size > 0) this.chunks.push(e.data); };
    this.recorder.onstop = () => this._onUtteranceEnd();
    this.recorder.start();
    this._speechStart = null; this._silenceStart = null;
  }

  _tick() {
    this._raf = requestAnimationFrame(() => this._tick());
    if (!this._analyser) return;
    this._analyser.getByteFrequencyData(this._buf);
    const avg = this._buf.reduce((a, b) => a + b, 0) / this._buf.length / 255;
    if (this.speaking || !this.active) return;

    const now = performance.now();
    const isSpeech = avg > this.SILENCE_THRESHOLD;
    if (isSpeech) {
      if (this._speechStart === null) this._speechStart = now;
      this._silenceStart = null;
    } else if (this._speechStart !== null) {
      if (this._silenceStart === null) this._silenceStart = now;
      const spokeLong   = (now - this._speechStart)  > this.MIN_SPEECH_MS;
      const silentLong  = (now - this._silenceStart) > this.SILENCE_MS;
      if (spokeLong && silentLong && this.recorder && this.recorder.state === 'recording') {
        this.recorder.stop();
      }
    }
  }

  async _onUtteranceEnd() {
    const blob = new Blob(this.chunks, { type: 'audio/webm' });
    this.chunks = [];
    if (blob.size < 2000) { if (this.active) this._armRecorder(); return; }

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
        this.history.push({ role: 'user',      content: data.transcript });
        this.history.push({ role: 'assistant', content: data.reply });
        await this._playReply(data.audio, data.transcript, data.reply);
      }
    } catch (e) {
      console.warn('Voice turn failed:', e);
      this.onState('idle');
    }
    if (this.active) this._armRecorder();
  }

  _playReply(base64Wav, transcript, reply) {
    return new Promise(resolve => {
      if (!base64Wav) { this.onState(this.active ? 'listening' : 'idle'); resolve(); return; }
      this.speaking = true;
      this.onState('speaking');
      const audio = new Audio('data:audio/wav;base64,' + base64Wav);
      const done = () => { this.speaking = false; this.onState(this.active ? 'listening' : 'idle'); resolve(); };
      audio.onended = done; audio.onerror = done;
      audio.play().catch(done);
    });
  }
}


/* ═══════════════════════════════════════
   2. UI
═══════════════════════════════════════ */
class ResonantUI {
  constructor() {
    this.app          = document.getElementById('voice-app');
    this.srLive       = document.getElementById('sr-live');
    this.stateLabel   = document.getElementById('state-label');
    this.btnOrb       = document.getElementById('btn-orb');
    this.btnSettings  = document.getElementById('btn-settings');
    this.btnEnd       = document.getElementById('btn-end');
    this.settingsPanel= document.getElementById('settings-panel');
    this.settingsClose= document.getElementById('settings-close');
    this.chkContrast  = document.getElementById('high-contrast');
    this.chkMotion    = document.getElementById('reduce-motion');
    this.chatLog      = document.getElementById('chat-log');
    this.textForm     = document.getElementById('text-form');
    this.textInput    = document.getElementById('text-input');
    this.btnSend      = document.getElementById('btn-send');

    this.history      = [];
    this.voice        = new VoiceController(this.history);
    this.micActive    = false;
    this.settingsOpen = false;

    this._bindEvents();
    this._checkReducedMotion();
    this._setState('idle');
  }

  _setState(state) {
    this.app.dataset.state = state;
    const labels = { idle: '', listening: 'Listening', thinking: 'Thinking', speaking: 'Speaking' };
    this.stateLabel.textContent = labels[state] || '';

    const announcements = {
      idle:      'Resonant ready. Tap the orb or press Space to begin.',
      listening: 'Resonant is listening.',
      thinking:  'Resonant is thinking.',
      speaking:  'Resonant is speaking.',
    };
    this._announce(announcements[state] || '');

    // Show/hide end button once chat has started
    if (this.app.classList.contains('chat-active')) {
      this.btnEnd.style.display = state === 'idle' ? 'none' : '';
    }
  }

  _announce(msg) {
    this.srLive.textContent = '';
    requestAnimationFrame(() => { this.srLive.textContent = msg; });
  }

  async _toggleMic() {
    if (!this.micActive) {
      const ok = await this.voice.start(s => this._setState(s));
      this.micActive = ok;
      this.btnOrb.setAttribute('aria-pressed', String(ok));
      this.btnOrb.setAttribute('aria-label', ok ? 'Stop listening' : 'Start listening');
      if (!ok) this._announce('Microphone unavailable. Check browser permissions.');
    } else {
      this.voice.stop();
      this.micActive = false;
      this.btnOrb.setAttribute('aria-pressed', 'false');
      this.btnOrb.setAttribute('aria-label', 'Start listening');
      this._setState('idle');
    }
  }

  _endSession() {
    this.voice.stop();
    this.micActive = false;
    this.btnOrb.setAttribute('aria-pressed', 'false');
    this.btnOrb.setAttribute('aria-label', 'Start listening');
    this._setState('idle');
    this._announce('Session ended. Tap the orb or press Space to start again.');
  }

  _renderMessage(role, content) {
    this.app.classList.add('chat-active');
    this.btnEnd.style.display = '';
    const div = document.createElement('div');
    div.className = `chat-msg chat-msg--${role}`;
    div.textContent = content;
    this.chatLog.appendChild(div);
    this.chatLog.scrollTop = this.chatLog.scrollHeight;
    if (role === 'assistant') this._announce(content);
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
    this.btnOrb.addEventListener('click', () => this._toggleMic());
    this.btnEnd.addEventListener('click', () => this._endSession());
    this.btnSettings.addEventListener('click', () => this._openSettings());
    this.settingsClose.addEventListener('click', () => this._closeSettings());
    this.settingsPanel.addEventListener('click', e => {
      if (e.target === this.settingsPanel) this._closeSettings();
    });
    this.textForm.addEventListener('submit', e => { e.preventDefault(); this._sendText(); });

    this.chkContrast.addEventListener('change', () => {
      document.body.classList.toggle('high-contrast', this.chkContrast.checked);
    });
    this.chkMotion.addEventListener('change', () => {
      document.body.classList.toggle('reduce-motion', this.chkMotion.checked);
    });

    document.addEventListener('keydown', e => {
      if (e.target === this.textInput) return;
      if (this.settingsOpen) return;
      switch (e.key) {
        case ' ': case 'Enter':
          if (e.target === document.body || e.target === this.btnOrb) {
            e.preventDefault(); this._toggleMic();
          }
          break;
        case 'Escape': this.settingsOpen ? this._closeSettings() : this._endSession(); break;
        case 's': case 'S': this._openSettings(); break;
      }
    });

    this.settingsPanel.addEventListener('keydown', e => {
      if (e.key !== 'Tab') return;
      const els = [...this.settingsPanel.querySelectorAll('button, input, select, [tabindex]:not([tabindex="-1"])')];
      const first = els[0], last = els[els.length - 1];
      if (e.shiftKey && document.activeElement === first)      { e.preventDefault(); last.focus(); }
      else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
    });
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

  _checkReducedMotion() {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      this.chkMotion.checked = true;
      document.body.classList.add('reduce-motion');
    }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.resonant = new ResonantUI();
});
