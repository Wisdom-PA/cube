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
- **Phase 5 — OS:** `docs/OS_PARTITIONS_AND_BUILDROOT.md` and `os/buildroot/README.md` (partition layout, Buildroot integration steps; **no** flashable image built in CI yet). **Supervisor (F2.T2.S2):** Java **`DefaultServiceSupervisor`**, **`ManagedService`**, **`ApiGatewayManagedService`**.
- **Phase 6 — Voice pipeline (stubs + tests):** `wisdom.cube.audio` (in-memory capture, silent playback), `wakeword`, `vad`, **`VoiceTurnPipeline`** (wake/STT → **`RuleBasedIntentClassifier`** → optional clarification → **`LlmPromptBuilder`** + **`LlmService`** → **`TtsService`**), **`DialogueManager`**, **`InMemoryMemoryStore`**. **Not** included: real mic/speaker drivers, Piper, production STT/LLM runtimes, or wiring voice turns to **`AutomationEngine`** (Phase 7).
- **Phase 7 (in progress):** **`wisdom.cube.device`** — registry + **`DefaultAutomationEngine`** (respects **`reachable`**; **`UNREACHABLE`** voice copy); **`VoiceTurnPipeline`** + **`VoicePipelineFactory`**. **`Cube.main`** shares one log + **`DeviceFixtureStore`** with **`HttpServerGateway`**. **`POST /devices/discover`** stub marks in-memory devices reachable. **`PATCH /devices/{id}`** returns **503** + **`DEVICE_UNREACHABLE`** when offline. **`MemoryStore.forgetKeyPrefix`** (F4.T4.S3). **Not** included: Home Assistant, real health probes, or a mic loop in **`Cube.main`**.

See **GettingStarted.md** for the full target sequence; this PR lands **documented specs + Java reference** so hardware and native stacks can attach later.
