# Bootloader, secure boot, factory reset, and recovery (F1.T3, F1.T4)

Design specification for the **cube firmware boot chain** and **user-facing recovery**. **Implementation** (signature verification code, partition flashing, recovery userspace) is **firmware / OS work** tracked with GettingStarted **Phase 12** and **F2.T3 / F2.T4**; this document satisfies **F1.T3.S1, F1.T3.S4, F1.T4.S1, F1.T4.S4** at the **spec** level and defers **F1.T3.S2–S3** and **F1.T4.S2–S3** to those phases.

---

## F1.T3 — Bootloader and secure boot chain

### F1.T3.S1 — Bootloader and secure boot approach (chosen direction)

| Layer | Choice | Notes |
|-------|--------|--------|
| **ROM / SoC** | Vendor **Root of Trust** | Chain starts in mask-ROM or on-die BootROM loading signed first-stage. |
| **First-stage bootloader** | Vendor **TF-A** (ARM) or equivalent + **U-Boot** (or vendor alternative) | Must support **A/B** slot selection, **recovery** partition boot, and **USB/UART** debug policy locked in production. |
| **Kernel + DTB** | Linux LTS aligned with Buildroot (`OS_PARTITIONS_AND_BUILDROOT.md`) | Boot from `boot_a`/`boot_b` or combined FIT in `rootfs` slot per SoC layout. |
| **Root filesystem integrity** | **Recommended:** **dm-verity** or signed **squashfs** for **read-only** system partitions | **Data** partition remains encrypted/mounted rw; verity for `rootfs_*` reduces persistent malware risk. |
| **OTA signing** | **Offline** key in build CI; **public key** in bootloader; **rotate** via dual-key scheme in F2.T3 | Private keys never on device. |

### F1.T3.S2 — Firmware signature verification (implementation scope)

- Bootloader verifies **each stage** before execution (hash + asymmetric signature per vendor APIs).
- **Deliverable:** implemented in **Phase 12** with **tests** on dev boards; not part of the Java cube repo.

### F1.T3.S3 — A/B firmware partitions (implementation scope)

- **Two** full system slots: **`system_a` / `system_b`** (names map to `rootfs_a` / `rootfs_b` in partition doc).
- **Metadata partition** records **active slot**, **boot attempts**, **rollback** flag (see F2.T3).
- **Deliverable:** partition table + bootloader logic in **image build**; Java cube reads **version/status** via future API if exposed.

### F1.T3.S4 — Boot sequence and failure modes (documentation)

**Happy path**

1. Power-on → BootROM → TF-A (or equivalent) → U-Boot reads **slot metadata** → loads **active** kernel + dtb → mounts **rootfs** (verified) → init → **supervisor** starts **Java cube** (`Cube.main` / gateway).

2. **Network / API** available only after **“ready”** state; LED shows **ready** (see below).

**Failure modes**

| Condition | Behaviour |
|-----------|-----------|
| **Invalid signature** on staged update | Refuse boot to new slot; **increment fail count**; after threshold **revert** to previous slot (F2.T3). |
| **Corrupt rootfs** (verity failure) | **Panic** into **recovery** partition or **previous slot** per F2.T4. |
| **Repeated boot failure** | Bootloader selects **fallback slot** or **recovery**; surface **LED pattern** + optional **audio chirp** (SKU-dependent). |
| **User-initiated recovery** | Hold **recovery combo** at power-on or from running system (F1.T4) → **recovery OS** minimal UI + **BLE** for app (F9.T2). |

---

## F1.T4 — Factory reset and recovery mode

### F1.T4.S1 — Physical interaction (button combo, LED feedback)

| Action | Interaction (baseline — **finalize in BOM**) |
|--------|-----------------------------------------------|
| **Enter recovery** | From **powered off**: hold **Recovery** + tap **Power** until **amber blink** (e.g. 2 Hz); release after **3 s** confirmation window OR hold **10 s** for **factory reset** path. |
| **Factory reset** | **Long-press Recovery** (e.g. **15 s**) while device on **charging** OR combo from power-off as above — **two-step** optional: first **recovery**, confirm in **app** to wipe (reduces accidental wipe). |
| **LED feedback** | **Solid blue** = normal ready; **pulsing blue** = listening; **amber blink** = recovery; **red blink** = error/OTA fail; **white breathe** = OTA in progress. |

### F1.T4.S2 — Factory reset: wipe user data, preserve base firmware (implementation scope)

- **Wipe:** `data` partition (profiles, secrets, models download cache), **behaviour logs**, Wi-Fi credentials, paired app keys.
- **Preserve:** **recovery** + **inactive system slot** as needed for **rollback**; **do not** erase BootROM/bootloader without dedicated **RMA** flow.
- **Deliverable:** **recovery userspace** + **API** hooks; **Phase 12**.

### F1.T4.S3 — Recovery mode UX for mobile app pairing (implementation scope)

- Recovery exposes **minimal GATT** or **provisioning Wi-Fi AP** (product choice) so **F9.T2** can run **key exchange** and **“restore from backup”** (F10.T3).
- **Deliverable:** firmware + app flows; **Phase 10–12**.

### F1.T4.S4 — Documentation for manuals and support

- **End-user:** one-page **quick start** path: *Recovery LED → open app → Connect to cube → Restore or Reset* (full copy in **F11.T2**).
- **Support:** table mapping **LED + button** to **state**; **no secret** engineering menus in v1.

---

## Traceability

| Ticket | Spec here | Implementation |
|--------|-----------|----------------|
| **F1.T3** | S1, S4 | S2, S3 → Phase 12 / board firmware |
| **F1.T4** | S1, S4 | S2, S3 → Phase 12 + app pairing |
| **F2.T3, F2.T4** | OTA / corruption recovery (overview) | `docs/OS_PARTITIONS_AND_BUILDROOT.md`; detailed OTA/recovery design in F2 docs when added |
