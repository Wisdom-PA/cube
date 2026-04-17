# F6 — Light capabilities and normalised state (scaffold)

Implements a **uniform light abstraction layer** (F6.T1.S4) inside `wisdom.cube.device` without changing the public HTTP contract yet.

## Types

- `LightCapability`: `ON_OFF`, `DIMMABLE`, `COLOR_TEMP`, `COLOR_RGB`
- `LightCapabilities`: a `Set<LightCapability>` plus `inferFrom(LightDevice)`
- `NormalisedLightState`: `power` + `brightness01` clamped to \([0, 1]\)
- `LightDeviceModel`: bundles `LightDevice` + inferred capabilities + normalised state

## Notes

- OpenAPI `DeviceSummary` includes `reachable` (F6.T3); capabilities remain internal until the contract is extended.

## Device health (F6.T3.S1)

- `DeviceHealthScheduler` runs `DeviceDiscoveryService.refreshDiscoveries` on a fixed interval when enabled.
- `HttpServerGateway` starts that scheduler when constructed with a positive **device health period** (seconds). `Cube` reads **`CUBE_DEVICE_HEALTH_SEC`**; unset or non‑positive means no background health task.
- The default stub `NoOpDeviceDiscoveryService` marks in-memory devices reachable on each refresh (integration code replaces this).

## Traceability

- Tickets: **F6.T1.S4**, **F6.T3.S1**

