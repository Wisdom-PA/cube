# F6 — Routines: data model (scaffold, F6.T4.S1)

This document and the `wisdom.cube.routine` package capture the **routine definition shape** before the scheduler, execution graph, and inspection APIs land (F6.T4.S2–S4, F6.T5).

## Model (Java)

- **`RoutineDefinition`**: `routineId`, `name`, `ownerProfileId`, `editorProfileIds` (who may edit), `triggers`, `conditions`, `actions`.
- **`RoutineTrigger`** + **`RoutineTriggerKind`**: `TIME`, `SUN`, `DEVICE_EVENT`, `VOICE_PHRASE`, `PRESENCE` with an opaque `payload` string (e.g. cron, phrase, device ref).
- **`RoutineCondition`** + **`RoutineConditionKind`**: optional guards (`TIME_WINDOW`, `PRESENCE`, `DEVICE_STATE`) with payload.
- **`RoutineAction`** + **`RoutineActionKind`**: `DEVICE_STATE`, `DELAY`, `NOTIFICATION` with payload (e.g. JSON for device patch, delay ms).

## HTTP (current)

- **`GET /routines`** still returns **`RoutineList`** (`id` + `name` per item) from `RoutineCatalog#listSummariesJson()`. Full definitions stay internal until contracts expose them.

## Fixture

- **`FixtureRoutineCatalog`** holds the two dev routines previously inlined in `HttpServerGateway`.

## Traceability

- Tickets: **F6.T4.S1**
