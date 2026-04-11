# Time synchronization service (F2.T5)

Design for **correct wall-clock time** on a headless device: **primary** path via **mobile app** after pairing; **optional** NTP when internet is allowed; **sanity** checks for drift and invalid time.

---

## F2.T5.S1 — Time sync from mobile app (Bluetooth / Wi-Fi)

- After **secure channel** is up (F7.T3 / F9.T2), app sends **`unix_epoch_ms`** + **timezone** (IANA string) + optional **DST** rule version.  
- Cube applies via **`timedatectl`** equivalent or **kernel** `settimeofday` **only** if jump is within **policy** (e.g. ±10 years rejected).  
- **Log** (structured, no PII): **source=app**, **delta_ms** applied.

---

## F2.T5.S2 — Optional NTP when internet is allowed

- If **global offline** is false and **profile policy** allows, run **SNTP** client to **pool.ntp.org** or **DHCP-provided** NTP server.  
- **Paranoid mode:** NTP **off** unless user explicitly allows **“network time”** (separate from cloud LLM).  
- **Rate-limit** queries; **battery / thermal** irrelevant for wall-powered cube but avoid **storm** on reconnect.

---

## F2.T5.S3 — Time correction from internet responses

- HTTP **Date** headers from **allowed** endpoints may **nudge** clock if drift **< threshold** (e.g. 5 s) — **no** large steps without confirmation.  
- **LLM/STT** pipelines use **monotonic** clock for **timeouts**; wall clock for **logs** and **routines** (F6.T4).

---

## F2.T5.S4 — Clock drift and invalid time edge cases

| Case | Handling |
|------|----------|
| **Time jumps backward** | Reject or **smear** (adjtime) for **small** deltas; **large** backward jump requires **adult** confirmation in app. |
| **RTC battery failure** | Boot at **epoch**; refuse **TLS** until time synced (show **set time** in app). |
| **Dual source conflict** | **Priority:** app-provisioned > NTP > HTTP nudge; **never** oscillate: **hysteresis** 30 s. |

---

## Traceability

| Ticket | Covered |
|--------|---------|
| **F2.T5** (S1–S4) | This document (design) |
| **F5.T2** | Global offline / internet gating |
| **F6.T4** | Scheduler uses wall clock |
