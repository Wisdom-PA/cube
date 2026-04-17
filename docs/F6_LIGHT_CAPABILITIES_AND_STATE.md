# F6 — Light capabilities and normalised state (scaffold)

Implements a **uniform light abstraction layer** (F6.T1.S4) inside `wisdom.cube.device` without changing the public HTTP contract yet.

## Types

- `LightCapability`: `ON_OFF`, `DIMMABLE`, `COLOR_TEMP`, `COLOR_RGB`
- `LightCapabilities`: a `Set<LightCapability>` plus `inferFrom(LightDevice)`
- `NormalisedLightState`: `power` + `brightness01` clamped to \([0, 1]\)
- `LightDeviceModel`: bundles `LightDevice` + inferred capabilities + normalised state

## Notes

- Current OpenAPI `DeviceSummary` only requires `power` and `brightness` fields for lights; capabilities can be exposed later by extending contracts + app UI in the same change set.

## Traceability

- Tickets: **F6.T1.S4**

