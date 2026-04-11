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

**Phase 1 (complete):** scaffold, tests + coverage gates, core interfaces + logging types, OpenAPI-aligned **`HttpServerGateway`**, **`Cube`** entry point with **`--port` / `CUBE_PORT`**. **`GET /logs`** is served from an in-memory **`InMemoryBehaviourLogStore`**: successful **`PATCH /devices/{id}`** and **`POST /chat`** append stub chains (F8.T1.S2 starter; no disk or rotation yet). Next: Phase 4+ (hardware, OS, voice) per **GettingStarted.md**.
