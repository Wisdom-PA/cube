# Firmware corruption detection and recovery (F2.T4)

Design for **failed boots**, **bad partitions**, and **recovery mode** aligned with **F1.T4** (factory reset / recovery UX) and **OTA** (F2.T3).

---

## F2.T4.S1 — Detect failed boots and mark partition bad

- **Bootloader** maintains **per-slot counters**: failed boot attempts, **watchdog** resets, **kernel panic** fingerprint (if reported).  
- After **N** failures (e.g. 3) on slot **B**, mark **B = bad** in **metadata**; **force boot from A** or **recovery**.  
- **dm-verity** I/O errors → **treat as corrupt**; do not remount rw system partition blindly.

---

## F2.T4.S2 — Boot fallback partition when corruption detected

- **Order:** try **alternate A/B slot** → if both bad or verify fails → **`recovery` partition** (minimal rootfs).  
- **Recovery** provides **BLE provisioning** and **USB serial** (debug builds only) per product policy.

---

## F2.T4.S3 — Recovery mode: restore from mobile backup

- **Flow:** User selects **Restore** in app → encrypted backup transferred per **F10.T3** → cube validates **manifest** → applies **profiles/routines/devices** per chosen merge policy.  
- **Does not** automatically reflash **rootfs** unless **bundled recovery image** path is explicitly supported (optional SKU).

---

## F2.T4.S4 — Factory reset from recovery mode

- **Wipe** `data` partition (user content, Wi-Fi, paired keys); **preserve** bootloader and **both** system slots **or** reflash **gold** image from recovery **only** if OTA stream available (product decision).  
- **LED + audio** confirmation before **destructive** wipe (F1.T4.S1).  
- After reset: **ready-to-pair** state (GettingStarted onboarding).

---

## Traceability

| Ticket | Covered |
|--------|---------|
| **F2.T4** (S1–S4) | This document (design) |
| **F1.T3, F1.T4** | Boot chain and physical recovery |
| **F10.T3** | Restore validation and UX |
