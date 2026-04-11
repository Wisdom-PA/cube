# Core service architecture and IPC (F2.T2.S1)

Defines **process boundaries** and **IPC** between STT, TTS, LLM, automation, and the API gateway. **F2.T2.S2** (supervisor with restart policies) has a **Java reference** in this repo (`DefaultServiceSupervisor`); on-device, processes may map to **separate OS services** with the same logical roles.

---

## Service roles

| Service | Responsibility | Typical runtime |
|---------|----------------|-----------------|
| **API gateway** | HTTP/WebSocket (or similar) to the mobile app; OpenAPI contract | JVM (**`HttpServerGateway`**) |
| **Automation engine** | Device commands, reachability, routines (future) | JVM or native bridge |
| **STT** | Streaming speech-to-text | Native worker (C/C++), **no** raw audio to disk |
| **TTS** | Synthesis playback | Native worker (e.g. Piper) + audio HAL |
| **LLM** | On-device inference | Native runtime + optional NPU; **small IPC** messages |
| **Wake word** | Always-on listen path | Low-power core / DSP; **event** to STT pipeline |

---

## IPC patterns (v1 target)

1. **Control plane (signalling)**  
   - **Unix domain sockets** or **gRPC over UDS** between services on the same SoC.  
   - Messages: **start/stop session**, **intent JSON**, **device command results**, **health ping**.  
   - **Secrets** (tokens) never cross IPC in plaintext; use **fd passing** or **kernel keyring** where applicable.

2. **Audio plane**  
   - **Shared-memory ring buffers** or **PipeWire / PulseAudio**-style graph (product choice).  
   - **Wake** → **STT** stream is **in-memory only**; align with **F3.T5** (no retention).

3. **Java ↔ native boundary**  
   - **JNI** or **subprocess** with JSON/stdin for **LLM/STT** if JVM hosts orchestration only.  
   - Prefer **bounded queues** and **timeouts** on every call; surface failures to **dialogue** and **behaviour log** (F8).

4. **Gateway ↔ automation**  
   - In current Java scaffold: **in-process** calls (`DefaultAutomationEngine`).  
   - On device: may remain **in-process** in one JVM **or** split if memory isolation is required — same **logical** API.

---

## Health and lifecycle

- **Supervisor** (systemd or `DefaultServiceSupervisor` pattern): **restart** on crash with **backoff**; **circuit-break** repeated failures and notify **LED/voice** (Plan error copy).  
- **Dependency order:** network → gateway → automation → optional cloud connectors.  
- **STT/TTS/LLM** restarted independently without tearing down gateway if possible.

---

## Traceability

| Ticket | Covered |
|--------|---------|
| **F2.T2.S1** | This document |
| **F2.T2.S2** | Java: `wisdom.cube.supervisor` |
| **F2.T2.S3** | `BehaviourLogWriter`, `BehaviourLogSchema` (write path; storage F8.T1) |
| **F3, F4** | Voice and intent pipelines consume this layout |
