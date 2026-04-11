# Audio pipeline, wake word, STT/TTS, and non-retention (F3)

Design and **in-repo Java scaffolds** for Phase 6. **Production** mic/speaker drivers, wake engine, Piper, and streaming STT binaries attach later per **F1** / **F3** hardware choices.

---

## F3.T1 — Capture and output

| Subtask | In this repo | Production follow-up |
|---------|--------------|----------------------|
| **S1** | `AudioCapture`, `InMemoryAudioRingBuffer`, `AudioChunk` | ALSA/PipeWire JNI or native service feeding ring buffer |
| **S2** | `AudioPreprocessor` + `PassthroughAudioPreprocessor` (no-op) | Native beamformer when array geometry is fixed (**HARDWARE_SPEC**) |
| **S3** | Documented here; preprocessor hook reserved | AEC with reference tap from playback path |
| **S4** | `AudioPlayback.setVolume`, `SilentAudioPlayback` | HAL mixer / Piper output gain |

---

## F3.T2 — Wake word

| Subtask | In this repo |
|---------|----------------|
| **S1–S4** | `WakeWordDetector`, `StubWakeWordDetector`, `VoiceWakeCoordinator` — engine TBD (Porcupine, openWakeWord, vendor). Indicators: LED/audio spec in **F1** boot/recovery doc and hardware manual (**F11.T2**). |

---

## F3.T3 — STT

| Subtask | In this repo |
|---------|----------------|
| **S1** | Model/runtime TBD; document English-first + locale list in Plan when chosen |
| **S2** | `AudioAwareStubSttService` drains `AudioCapture` through `AudioPreprocessor` then returns a fixed transcript (tests / demos) |
| **S3** | `VoiceActivityDetector`, `StubVoiceActivityDetector` |
| **S4** | `SttService` → `VoiceTurnPipeline` (existing) |

---

## F3.T4 — TTS

| Subtask | In this repo |
|---------|----------------|
| **S1–S3** | **Piper** target; not bundled — `TtsService` + mocks / `SilentAudioPlayback` |
| **S4** | Fallback: shorter phrase or text-only via app (copy in Plan **F11.T1**); implement with real engine |

---

## F3.T5 — Voice data non-retention

| Subtask | Approach |
|---------|----------|
| **S1** | **Audit:** `wisdom.cube.audio` must not use `java.nio.file` / `FileOutputStream` for PCM; behaviour logs carry **text only**, not raw audio (**F8**). |
| **S2** | `AudioChunk.discardPayload()` zero-fills PCM bytes; `InMemoryAudioRingBuffer.close()` discards queued chunks; consumers should discard after use. |
| **S3** | Unit tests: discard semantics + no persistence helpers in audio package. |
| **S4** | `AudioPrivacySettings` — **`allowRawPersistence`** is **false** for production defaults; telemetry must not ship raw samples. |

---

## Traceability

| Ticket | Where |
|--------|--------|
| **F3.T1–T5** | This doc + `wisdom.cube.audio`, `wisdom.cube.voice`, `wisdom.cube.wakeword`, `wisdom.cube.vad` |
| **F4** | `VoiceTurnPipeline` downstream of STT |
| **Plan** | Paranoid mode, no audio retention |
