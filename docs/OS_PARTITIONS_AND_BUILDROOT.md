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

## Scaffold in this repository

The folder `os/buildroot/` contains **README** instructions only—no full tree checked in—to avoid duplicating Buildroot upstream. CI in this repo validates **Java** only; image build jobs can be added when hardware is pinned.

## Traceability

- **Tickets:** F2.T1, F2.T3 (OTA later).
- **Supervisor (F2.T2.S2):** Java reference implementation `wisdom.cube.supervisor`.
