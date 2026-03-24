# Cube – On-device assistant

On-device voice assistant (Java): STT, TTS, LLM, intent engine, device control, API gateway. Runs on the physical cube hardware or on host/CI with mocked audio and drivers.

- **Plan and tickets:** See Wisdom root (Plan.md, GettingStarted.md, Tickets.md).
- **API contract:** Consume from the [contracts](https://github.com/Wisdom-PA/contracts) repo (cube ↔ app).

**Build:** Maven (Java 17). From `cube/` run `.\mvnw.cmd verify` (Windows) or `./mvnw verify` (Git Bash). **No Maven install needed**—the Maven Wrapper downloads Maven on first run.

**Optional: install Maven globally** (e.g. for other projects): [Maven – Download](https://maven.apache.org/download.cgi) (binary zip → unzip → add `bin` to PATH), or **choco install maven** if you use Chocolatey. (Maven is not in winget.)

Phase 1: scaffold ✓, test config ✓, core interfaces ✓, API contract (contracts repo) ✓, skeleton gateway ✓. Next: Phase 2 (app scaffold, test + Storybook, mock cube, base screens).
