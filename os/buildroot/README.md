# Buildroot integration (scaffold)

**Phase 5.1** expects a Buildroot out-of-tree board or br2-external package. This directory is the anchor for that work.

## Suggested steps (when SoC is fixed)

1. Create `br2-external-wisdom` with `external.desc` and `external.mk`.
2. Add package `wisdom-cube`:
   - `WISDOM_CUBE_VERSION` = git tag or file from Maven `mvn -q -Dexec.skip=true package`.
   - Install `target/cube-*-SNAPSHOT.jar` to `/opt/wisdom/cube.jar`.
3. Add `wisdom-cube.service` (systemd) or `S99wisdom` init script:
   - `ExecStart=/usr/bin/java -jar /opt/wisdom/cube.jar --port 8080` (or env file for `CUBE_PORT`).
4. Enable partitions per `docs/OS_PARTITIONS_AND_BUILDROOT.md`.

## CI

Full image builds are **not** run in the cube repo today. Add a workflow that runs only when `os/buildroot/**` changes, using a container with Buildroot dependencies, if/when the team maintains a defconfig here.
