# F4 — Intent schema, dialogue, context chain, memory

Java scaffolds for on-device NLU, dialogue states, short-term follow-ups, and per-profile memory. **LLM runtime / quantization** remain future work (**F4.T1.S1–S2, S4**).

---

## F4.T2 — Intent and NLU

| Subtask | In-repo |
|---------|---------|
| **S1** | This doc + `AutomationEngine.Intent` (`type`, `targets`, `parameters`); see **Intent types** below |
| **S2** | `RuleBasedIntentClassifier`, `SingleDeviceFallbackIntentClassifier` |
| **S3** | Room slugs aligned with `LightDeviceRegistry`; single-device home fallback |
| **S4** | `NeedsClarification` + `Unknown`; pronoun follow-ups via `VoiceContextChain` |

**Intent types (v1 scaffold):** `set_light`, `set_brightness` — extend for scenes, routines, settings in later tickets.

---

## F4.T3 — Dialogue

| Subtask | In-repo |
|---------|---------|
| **S1** | `DialogueState` includes **EXECUTING**; `DialogueManager` **5s listen deadline** after wake (`listenDeadlineExceeded`) |
| **S2** | Existing one-shot clarification in `VoiceTurnPipeline` + app suggestion copy |
| **S3** | `SensitiveActionConfirmationPolicy` + `DefaultSensitiveActionConfirmationPolicy` (locks/doors on; lights off); pipeline blocks with spoken hint when required |
| **S4** | Device/LLM failure paths + TTS + return toward **IDLE** via `onSpokenResponse` |
| **S5** | `VoiceContextChain` — last successful automation (**60s** window); “turn it off”, “brighter”, etc. |

---

## F4.T4 — Memory

| Subtask | In-repo |
|---------|---------|
| **S1** | Key-value per profile; day-scoped keys under `mem/<yyyy-MM-dd>/` |
| **S2** | Encryption deferred (**F7** / storage layer) |
| **S3** | `remember` / `forget` / `forgetDay` / `forgetKeyPrefix` |
| **S4** | `exportProfile` |

---

## Traceability

| Ticket | Location |
|--------|----------|
| **F4** | `wisdom.cube.intent`, `dialogue`, `voice`, `memory` |
