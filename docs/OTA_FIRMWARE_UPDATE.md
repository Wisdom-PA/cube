# Firmware update system — OTA A/B (F2.T3)

Design for **over-the-air** updates with **A/B slots**, **boot-success handshake**, and **rollback**. Complements **`OS_PARTITIONS_AND_BUILDROOT.md`** and the **F1** boot-chain spec when present in `docs/BOOTLOADER_SECURE_BOOT_AND_RECOVERY.md`.

---

## F2.T3.S1 — OTA protocol (app ↔ cube)

| Step | Description |
|------|----------------|
| 1. **Check** | App calls **version/status** endpoint (contract TBD); cube reports **active slot**, **available update**, **download URL** or **chunked manifest**. |
| 2. **Download** | Cube fetches **signed artifact** (HTTPS); **verify signature** before write (same key chain as bootloader). |
| 3. **Stage** | Write to **inactive** slot (`rootfs_b` if `A` active); **dual-bank** metadata records **pending** version. |
| 4. **Activate** | On user confirm or maintenance window, set **try boot** flag for new slot **once**. |
| 5. **Commit** | After **successful boot** + **health checks** (gateway up, filesystem rw), mark slot **permanent**; else **rollback** (F2.T3.S3). |

**Transport:** Prefer **LAN** from app as **control plane**; payload may be **direct from cube to CDN** to avoid phone memory limits.

---

## F2.T3.S2 — Write to inactive partition

- **Streaming write** with **hash** verification per block; **no** partial slot marked bootable until **complete**.  
- **Power loss:** resume from manifest **chunk index**; **never** boot incomplete slot.  
- **Disk space:** preflight **free space** on `data` if download is staged there before flash.

---

## F2.T3.S3 — Boot-success detection and automatic rollback

| Signal | Action |
|--------|--------|
| **Bootloader** boots new slot | Increment **try count** (max 1–2 attempts). |
| **Userspace health** (e.g. **systemd `WisdomHealth`**) | Within **T** minutes: gateway responds, **no** kernel panic. |
| **Success** | Clear try counter; set **active_slot = B**. |
| **Failure** | Bootloader on next reset loads **previous slot**; report **OTA failed** via LED + app (F11 error copy class). |

---

## F2.T3.S4 — Update status and changelog hooks for app

- Expose **JSON**: `current_version`, `pending_version`, `last_result`, `changelog_url` (optional).  
- **Behaviour log** (F8): record **OTA start/complete/fail** as **system actions** (no PII).

---

## Traceability

| Ticket | Covered |
|--------|---------|
| **F2.T3** (S1–S4) | This document (design); **implementation** Phase 12 + contracts |
| **F1.T3** | Secure boot / slot selection |
| **F10** | Backup before major OTA optional UX |
