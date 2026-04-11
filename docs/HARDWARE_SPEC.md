# Wisdom cube — hardware specification (GettingStarted Phase 4)

This document satisfies **Phase 4.1 (F1.T1)** targets for software bring-up. Values are engineering baselines for OS, audio, and on-device LLM sizing; they should be revised when a SoC is chosen.

## F1.T1 — Platform

| Area | Target | Notes |
|------|--------|--------|
| **CPU** | 4+ Armv8-A cores @ ≥ 1.5 GHz (application processors) | Runs JVM gateway, STT/TTS workers, LLM runtime (quantized). |
| **NPU / GPU** | Optional but recommended | Accelerate INT8/FP16 LLM and optional STT; fallback CPU acceptable at higher latency. |
| **RAM** | **8 GB LPDDR4x minimum** (16 GB preferred) | LLM + STT + OS + buffers; leave headroom for future models. |
| **Storage** | **64 GB eUFS minimum** | Rootfs A/B (~2× 2–4 GB), data, logs partition, recovery, model store. |
| **Microphones** | **4-mic linear or circular array** | Beamforming and AEC-friendly geometry; SNR and spacing per acoustic sim. |
| **Speaker** | Full-range driver + Class-D amp | Loud enough for kitchen counter at arm’s length; volume curve in software. |
| **Connectivity** | Wi-Fi 6, Bluetooth 5.x; optional Ethernet | LAN API to app; BLE for provisioning. |
| **Controls** | RGB status LED, mic mute, recovery combo | Match F1.T4 recovery story; LED patterns documented in OS manual. |

## F1.T2 — Power and thermal (Phase 4.2)

| Topic | Target |
|-------|--------|
| **Worst-case power** | Budget for all cores + NPU active + speaker peak; wall adapter headroom 2× sustained average. |
| **Cooling** | Passive preferred; define max TDP before thermal throttle of voice/STT/LLM bundle. |
| **Low-power** | Wake-word path on sensor hub or low-power core; define mW target for listen-only state. |
| **Throttle** | On sustained thermal event: degrade LLM quality / lengthen STT first; voice prompts per Plan error copy. |

## Traceability

- **Tickets:** F1.T1, F1.T2 in `Tickets.md`.
- **Next:** Phase 5 OS images and partition layout (`docs/OS_PARTITIONS_AND_BUILDROOT.md`).
