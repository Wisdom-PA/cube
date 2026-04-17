# F6 — Routines: model, tick scheduler, execution, logging (F6.T4.S1–S4)

## Model (S1)

- **`RoutineDefinition`**: `routineId`, `name`, `ownerProfileId`, `editorProfileIds`, `triggers`, `conditions`, `actions`.
- **`RoutineTrigger`** + **`RoutineTriggerKind`**: `TIME`, `SUN`, `DEVICE_EVENT`, `VOICE_PHRASE`, `PRESENCE` with opaque `payload` (cron-like for `TIME`).
- **`RoutineCondition`** + **`RoutineConditionKind`**: `TIME_WINDOW` (JSON `start` / `end` as `HH:mm`), `PRESENCE` / `DEVICE_STATE` (stubs: always pass).
- **`RoutineAction`** + **`RoutineActionKind`**: `DEVICE_STATE` (JSON `device_id`, optional `power`, `brightness`), `DELAY` (stub: deferred, no real sleep), `NOTIFICATION` (stub).

## Tick scheduler (S2)

- **`RoutineTickProcessor`**: on each tick, evaluates **TIME** triggers with **`RoutineCronEvaluator`** (`M H * * *` daily subset), then **`RoutineConditionEvaluator`**, then runs actions.
- **`RoutineTickScheduler`**: daemon `scheduleAtFixedRate` (seconds in production; tests may use smaller `TimeUnit`).
- **`HttpServerGateway`**: optional **`routineTickPeriodSeconds`** (`> 0` starts the scheduler). **`Cube`** reads **`CUBE_ROUTINE_TICK_SEC`** (unset or non-positive = off).

## Execution (S3)

- **`RoutineRunner`**: runs actions **in order**; a failed step does not stop later steps (partial success). Device actions respect **`reachable`** on the registry.

## Logging (S4)

- **`InMemoryBehaviourLogStore.recordRoutineRun`**: one chain with intent type **`routine.timer`** and one **`ActionEntry`** per step (`ok` / `error` + codes). Surfaced via existing **`GET /logs`**.

## HTTP

- **`GET /routines`**: **`RoutineList`** from **`RoutineCatalog#listSummariesJson()`**.
- **`GET /routines/history`**: **`RoutineRunHistory`** (newest first) from **`InMemoryBehaviourLogStore#toRoutineRunHistoryJson`**, fed whenever **`recordRoutineRun`** runs.
- **`PATCH /routines/{routineId}`** with **`{"name":"…"}`**: updates display name when the catalog supports it (**`MutableRoutineCatalog`** is the default in **`HttpServerGateway`**); read-only **`FixtureRoutineCatalog`** returns **501**.

## Catalogs

- **`FixtureRoutineCatalog`**: two sample routines (evening 18:00; morning 07:00 with time window); immutable.
- **`MutableRoutineCatalog`**: same starting data; supports **`patchRoutineDisplayName`** for companion sync (F6.T5).

## Traceability

- Tickets: **F6.T4.S1–S4**; **F6.T5** history + name patch + app wiring.
