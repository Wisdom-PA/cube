# Cube – On-device assistant

On-device voice assistant (Java): STT, TTS, LLM, intent engine, device control, API gateway. Runs on the physical cube hardware or on host/CI with mocked audio and drivers.

- **Plan and tickets:** See Wisdom root (Plan.md, GettingStarted.md, Tickets.md).
- **API contract:** Consume from the [contracts](https://github.com/Wisdom-PA/contracts) repo (cube ↔ app).

**Build:** Maven (Java 17). From `cube/` run `.\mvnw.cmd verify` (Windows) or `./mvnw verify` (Git Bash). **No Maven install needed**—the Maven Wrapper downloads Maven on first run.

**Run the HTTP gateway (Phase 1):** after `mvn -q -DskipTests package`, start the contract stub on port 8080:

```bash
java -cp target/cube-0.1.0-SNAPSHOT.jar wisdom.cube.Cube --port 8080
```

Or set **`CUBE_PORT`** (e.g. `8080`) and run with no args (same as `Cube.main` with empty `args`). Stop with Ctrl+C (JVM shutdown hook stops the server).

**Optional: install Maven globally** (e.g. for other projects): [Maven – Download](https://maven.apache.org/download.cgi) (binary zip → unzip → add `bin` to PATH), or **choco install maven** if you use Chocolatey. (Maven is not in winget.)

**Phase 1 (complete):** scaffold, tests + coverage gates, core interfaces + logging types, OpenAPI-aligned **`HttpServerGateway`**, **`Cube`** entry point with **`--port` / `CUBE_PORT`**. **`GET /logs`** is served from an in-memory **`InMemoryBehaviourLogStore`**: successful **`PATCH /devices/{id}`** and **`POST /chat`** append stub chains (F8.T1.S2 starter; no disk or rotation yet).

**Phases 4–6 (scaffold in this repo):**

- **Phase 4 — Hardware + boot/recovery spec:** `docs/HARDWARE_SPEC.md` (**F1.T1**, **F1.T2**); `docs/BOOTLOADER_SECURE_BOOT_AND_RECOVERY.md` (**F1.T3**, **F1.T4** design; firmware implementation in Phase 12).
- **Phase 5 — OS (F2):** `docs/OS_PARTITIONS_AND_BUILDROOT.md` (**F2.T1**), `docs/CORE_SERVICES_AND_IPC.md` (**F2.T2.S1**), `docs/OTA_FIRMWARE_UPDATE.md` (**F2.T3**), `docs/FIRMWARE_CORRUPTION_RECOVERY.md` (**F2.T4**), `docs/TIME_SYNCHRONIZATION.md` (**F2.T5**), and `os/buildroot/README.md` (**no** flashable image in CI until SoC pinned). **Supervisor (F2.T2.S2):** Java **`DefaultServiceSupervisor`**, **`ManagedService`**, **`ApiGatewayManagedService`**.
- **Phase 6 — Voice pipeline (F3 stubs + tests):** `docs/AUDIO_PIPELINE_AND_NON_RETENTION.md` (**F3**). `wisdom.cube.audio` — capture/playback, **`AudioChunk` discard** (F3.T5), **`AudioPreprocessor`** / passthrough, **`AudioAwareStubSttService`** (drain capture → fixed transcript). `wakeword`, `vad`, **`VoiceTurnPipeline`**. **Not** included: real mic/speaker, Piper, production STT/LLM, or **`Cube.main`** mic loop.
- **Phase 7 (in progress):** **`wisdom.cube.device`** + **`docs/F4_INTENT_DIALOGUE_AND_MEMORY.md`** (**F4**) + **`docs/F5_CLOUD_AND_INTERNET.md`** (**F5**): **`internet`** package — **`DefaultInternetAccessGate`** (global offline, paranoid, session + voice consent), **`CloudFallbackLlmService`**, **`StubCloudLlmClient`**, **`InternetCallLogger`**, **`HttpServerGateway.isGlobalOffline` / `getDefaultPrivacyMode`**. **`VoiceTurnPipeline.processUtterance(..., allowCloud)`** for explicit cloud consent. **Not:** real Claude HTTP client, streaming to TTS, log rotation, Home Assistant, **`Cube.main`** mic loop.

See **GettingStarted.md** for the full target sequence; this PR lands **documented specs + Java reference** so hardware and native stacks can attach later.
