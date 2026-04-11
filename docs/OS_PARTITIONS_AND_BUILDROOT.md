# OS, partitions, and Buildroot (GettingStarted Phase 5.1)

This document satisfies **Phase 5.1 (F2.T1)** at the **design + scaffold** level. Producing flashable images requires a board defconfig, vendor BSP, and CI runners with cross-build capacity—tracked as follow-up work.

## Partition layout (A/B, data, logs, recovery)

| Partition | Role | Typical size (indicative) |
|-----------|------|---------------------------|
| `boot` | Bootloader / boot firmware | Vendor-specific |
| `boot_a` / `boot_b` | Kernel + DTB (if separate from rootfs) | 64–128 MB each |
| `rootfs_a` / `rootfs_b` | Squashfs or ext4 root; **Java runtime + cube JAR + models** | 2–4 GB each |
| `data` | Read-write: config, profiles, encrypted secrets, downloaded models | Remainder minus below |
| `log` | Behaviour and system logs; **7-day rotation** (Plan) | 512 MB–2 GB |
| `recovery` | Minimal rootfs for factory reset / OTA recovery | 512 MB–1 GB |

**A/B policy:** One slot marked active; updates write the inactive slot; boot-success handshake before committing (F2.T3).

## Buildroot (F2.T1.S1–S3)

- **Base:** Buildroot LTS aligned with kernel LTS used by the SoC vendor.
- **Cube application:** Install the **cube** fat JAR (or layered modules) under `/opt/wisdom`, started by **supervisor** (Phase 5.2 — see `DefaultServiceSupervisor` in Java; on device this may be `systemd` or a small init wrapper calling the JVM).
- **Java:** Use a **custom Buildroot package** `wisdom-cube` that depends on OpenJDK 17 (or Temurin build) and ships the artifact from this repo’s Maven build.

## F2.T1.S3 — Initial image build pipeline (design)

When the **SoC and board defconfig** are fixed:

1. **Container image** (e.g. Debian bookworm) with Buildroot deps (`build-essential`, `libncurses5-dev`, `wget`, `cvs`, `subversion`, `mercurial`, `git`, `python3`, `unzip`, `rsync`, `bc`, `cpio`, `file`, `ccache` — trim per Buildroot manual).
2. **CI job** (optional workflow, `paths: os/buildroot/**` + `docs/OS_PARTITIONS_AND_BUILDROOT.md`): checkout **br2-external** + **vendor BSP** (submodule or scripted fetch), run `make wisdom_defconfig && make`, publish **`sdcard.img`** or **`wic`** as **artifact** (not on every PR until runners are sized).
3. **Signing:** OTA payloads signed **out-of-band**; keys **not** in CI logs (F2.T3).
4. **Versioning:** embed **`cube` Maven version** + **git SHA** in `/etc/wisdom-release` for app display.

Until then, **`mvnw verify`** remains the **required** gate for this repository.

## Scaffold in this repository

The folder `os/buildroot/` contains **README** instructions only—no full tree checked in—to avoid duplicating Buildroot upstream. CI in this repo validates **Java** only; image build jobs follow **F2.T1.S3** above when hardware is pinned.

## Related design docs (F2)

| Ticket | Document |
|--------|----------|
| **F2.T2.S1** | `docs/CORE_SERVICES_AND_IPC.md` |
| **F2.T3** | `docs/OTA_FIRMWARE_UPDATE.md` |
| **F2.T4** | `docs/FIRMWARE_CORRUPTION_RECOVERY.md` |
| **F2.T5** | `docs/TIME_SYNCHRONIZATION.md` |

## Traceability

- **Tickets:** **F2.T1** (S1–S3 design), **F2.T3**–**F2.T5** (linked docs); flashable images = follow-up.
- **Supervisor (F2.T2.S2):** Java reference implementation `wisdom.cube.supervisor`.
