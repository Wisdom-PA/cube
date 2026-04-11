# Wisdom cube — hardware specification (F1.T1, F1.T2)

Engineering baselines for OS bring-up, audio, and on-device LLM sizing. Revise when a SoC is chosen. This document **closes the specification track** for **F1.T1** and **F1.T2** in-repo; bootloader/factory-reset **behaviour** is specified in `BOOTLOADER_SECURE_BOOT_AND_RECOVERY.md` (F1.T3, F1.T4).

---

## F1.T1 — Define hardware specification

### F1.T1.S1 — CPU / NPU / GPU requirements

| Requirement | Target | Rationale |
|-------------|--------|-----------|
| Application CPU | **4+ Armv8-A cores @ ≥ 1.5 GHz** | JVM gateway, concurrent STT/TTS/LLM workers, OS services. |
| ISA | AArch64 for application processors | OpenJDK 17+ Tier-1 on arm64. |
| NPU / GPU | **Recommended**, not mandatory for v1 | INT8/FP16 LLM and optional STT acceleration; CPU-only path allowed at higher latency and power. |
| Vector / SIMD | NEON minimum | Audio DSP and lightweight ML helpers on CPU. |

### F1.T1.S2 — RAM and storage (LLM, STT, logs, backups)

| Resource | Minimum | Preferred | Notes |
|----------|---------|-----------|--------|
| **RAM** | **8 GB LPDDR4x** | **16 GB** | Budget: OS ~1–2 GB, JVM heap for gateway + services ~1–2 GB, STT/TTS runtimes ~1–2 GB, LLM working set (quantized 7–8B class) ~4–6 GB; leave headroom for spikes and future models. |
| **Storage (eUFS)** | **64 GB** | **128 GB** | **rootfs A/B** (~2 × 2–4 GB), **recovery** (~512 MB–1 GB), **log** partition (512 MB–2 GB, 7-day rotation), **data** (config, encrypted secrets, **model store** 10–30+ GB depending on model line-up), margin for backups staged from app. |

### F1.T1.S3 — Microphone array

| Parameter | Target |
|-----------|--------|
| Topology | **4 microphones**, **linear end-fire** or **circular** array (choose with mechanical ID). |
| Spacing (indicative) | Linear: **35–45 mm** edge spacing; circular: **40–60 mm** aperture for beamforming/AEC-friendly capture at ~50 cm–2 m. |
| Acoustic | Aim **SNR ≥ 60 dB** at 1 kHz reference, **AOP** suitable for close-field loud speech; wind/pop mesh in mechanical stack. |
| Wake word | Array feeds **low-power listen path** (sensor hub or dedicated DSP) per F1.T2 low-power targets; geometry validated in acoustic sim before EVT. |

### F1.T1.S4 — Speaker, amplifier, and acoustic targets

| Parameter | Target |
|-----------|--------|
| Driver | Full-range speaker appropriate for **voice-first** output (TTS, prompts, alerts). |
| Amplifier | **Class-D**, efficient idle; hardware volume control or I2S/digital volume to software. |
| SPL (indicative) | Comfortable intelligibility at **arm’s length on a kitchen counter** (~0.5 m); define **max SPL** and **THD budget** at 85% of max volume in DVT. |
| Echo reference | **Reference tap** to AEC path (digital or analog) per F3.T1. |

### F1.T1.S5 — Connectivity

| Interface | Requirement |
|-----------|-------------|
| **Wi-Fi** | **802.11ax (Wi-Fi 6)** dual-band; STA mode for home LAN; soft AP optional for captive provisioning only if product requires it. |
| **Bluetooth** | **5.2+** for provisioning, optional LE Audio later; stable BLE stack for app-driven setup (F9.T2). |
| **Ethernet** | **Optional** RJ45 or USB-C dock; product SKU may omit. |

### F1.T1.S6 — Physical controls (LEDs, mic mute, recovery)

| Control | Behaviour |
|---------|-----------|
| **RGB status LED** | Patterns for **boot/ready**, **listening**, **error**, **OTA**, **recovery** — detailed timing in `BOOTLOADER_SECURE_BOOT_AND_RECOVERY.md` and future hardware manual (F11.T2). |
| **Mic mute** | Hardware or firm software latch: **no uplink** to STT/cloud when muted; state visible on LED. |
| **Recovery / factory reset** | **Combo** (e.g. hold **Recovery** + **Power** 10 s, or long-press single **Recovery** from power-off) — exact combo fixed in mechanical/electrical BOM; **LED feedback** during countdown. |

---

## F1.T2 — Power, thermal, and always-on audio design

### F1.T2.S1 — Worst-case power draw (all pipelines)

| Mode | Budget (indicative) |
|------|---------------------|
| **Peak** (all CPU cores busy + accelerator + speaker near max) | Size **DC input** and **PMIC** for **~15–25 W** short bursts (refine with SoC datasheet). |
| **Sustained** (LLM + STT active, typical TTS) | **Wall adapter** rated **≥ 2×** expected sustained average for thermal and margin (e.g. 30–45 W class supply if sustained ~12–18 W). |
| **Listen-only** (wake path) | See F1.T2.S3. |

### F1.T2.S2 — Passive cooling and thermal budget

- **Preferred:** passive heatsink (define **min effective area** after SoC pick).
- **Max TDP** before voice/STT/LLM bundle must throttle: set per silicon **junction temp** (e.g. 85 °C) with **30 s / 5 min** averaging windows.
- **DVT:** validate voice quality under throttle (no clipping on TTS due to undervoltage).

### F1.T2.S3 — Low-power modes and wake-word standby

| State | Target |
|-------|--------|
| **Deep listen** (wake engine only, network mostly down) | **≤ 0.5 W** system budget (indicative — **revise** with measured SoC + PMIC); goal is always-on without unreasonable idle draw. |
| **Idle after interaction** | Ramp down non-wake CPUs; resume **< 500 ms** to full listen on wake hit. |

### F1.T2.S4 — Thermal throttling and shutdown

1. **Graded throttle:** reduce LLM decode throughput / batch size → lengthen STT windows → reduce non-essential background work **before** muting speaker.
2. **User feedback:** voice prompts align with **Plan** standard error copy (thermal not user-facing as jargon; “Please try again in a moment” class).
3. **Hard shutdown:** only on **unsafe junction** or **PMIC fault**; log reason to **secure / minimal** store for support.

---

## Traceability

| Ticket | Covered here |
|--------|----------------|
| **F1.T1** (S1–S6) | Sections above |
| **F1.T2** (S1–S4) | Sections above |
| **F1.T3, F1.T4** | `docs/BOOTLOADER_SECURE_BOOT_AND_RECOVERY.md` |
| **F2.T1** | `docs/OS_PARTITIONS_AND_BUILDROOT.md` |
| **Tickets.md** (Wisdom root) | Snapshot row for F1 spec — update when syncing docs |
