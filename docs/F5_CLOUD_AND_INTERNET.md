# F5 — Cloud LLM and internet permission (scaffold)

Optional online reasoning with **global offline**, **privacy mode**, **session consent**, and **per-turn voice consent**. **No real HTTP** to Anthropic in this repo yet (F5.T1.S1); use **`StubCloudLlmClient`** in tests.

---

## F5.T1 — Cloud LLM

| Subtask | In-repo |
|---------|---------|
| **S1** | `CloudLlmClient` + `StubCloudLlmClient`; production client later (retries/timeouts). |
| **S2** | `CloudLlmPrompts` — system instructions (minimal metadata, short answers, standard failure line). |
| **S3** | Single-shot `complete` only; streaming/chunked TTS integration deferred. |
| **S4** | `CloudFallbackLlmService` falls back to on-device `LlmService` when blocked or on error. |

**Standard user-facing failure (Plan / Tickets):**  
`CloudLlmPrompts.CLOUD_UNAVAILABLE_SPOKEN_LINE`

---

## F5.T2 — Internet permission

| Subtask | In-repo |
|---------|---------|
| **S1** | `VoiceTurnPipeline.processUtterance(..., allowCloudForThisTurn)` + `VoiceCloudConsent` slot read by gate. |
| **S2** | `SessionInternetConsent` (time-bounded allow, e.g. after app grants session). |
| **S3** | `ProfileInternetPolicy` interface + `DefaultProfileInternetPolicy` (stub: all profiles allowed when not offline). |
| **S4** | `HttpServerGateway` **`global_offline`** enforced via `DefaultInternetAccessGate` suppliers. |

---

## F5.T3 — Internet activity logging

| Subtask | In-repo |
|---------|---------|
| **S1** | `InternetCallRecord` → `BehaviourLogSchema.InternetCallEntry` (chain UUID per call, call index, endpoint, result, errors). |
| **S2** | `InternetCallLogger` used from `CloudFallbackLlmService` on cloud attempts. |
| **S3** | Rotation / 7-day retention: still **F8** / storage (in-memory log today). |

---

## Wiring

- Pass `Optional.of(cloudConsentRef)` into `VoiceTurnPipeline` and the same ref into `DefaultInternetAccessGate` + `CloudFallbackLlmService` when cloud is enabled.
- `VoicePipelineFactory` can wrap the app-supplied `LlmService` with `CloudFallbackLlmService` once a real `CloudLlmClient` exists; until then optional cloud stays empty (pure on-device).
