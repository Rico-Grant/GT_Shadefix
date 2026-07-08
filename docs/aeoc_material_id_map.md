# AE2 / OpenComputers Semantic Material ID Map

This table is the fixed source of truth for Java routing, runtime logs, and the semantic debug shaderpack.

## Fixed Semantic IDs

| ID | Symbol | Meaning |
|---:|---|---|
| 12100 | `AE_BODY` | AE2 normal body, backdrop, base, cartridge, and non-light quads |
| 12101 | `AE_UVL_LIGHT` | AE2 confirmed UVL light quads |
| 12102 | `AE_CONTROLLER_LIGHT` | AE2 controller or controller-like light quads |
| 12103 | `AE_CABLE_IDLE` | Smart/dense cable `channels_00` and `channels_10` quads |
| 12104 | `AE_CABLE_LOW_CHANNEL` | Smart/dense cable `channels_01..04` quads |
| 12105 | `AE_CABLE_HIGH_CHANNEL` | Smart/dense cable `channels_11..14` quads |
| 12106 | `AE_DRIVE_LED` | ME Drive `Cell Light` / `drive_cell_states_emissive` quads |
| 12107 | `AE_CRAFTING_LIGHT` | Crafting CPU light quads |
| 12108 | `AE_MONITOR_LIGHT` | Crafting monitor light quads |
| 12109 | `AE_TERMINAL_TRACE` | Terminal debug trace quads only |
| 12110 | `OC_BODY` | OpenComputers normal body and housing quads |
| 12111 | `OC_LED_BAKED` | Confirmed OpenComputers baked LED quads |
| 12112 | `OC_LED_TESR` | OpenComputers Case/Rack/Microcontroller/Raid TESR overlay quads |

## Required AE Routing Rules

- `channels_00` and `channels_10` -> `12103 AE_CABLE_IDLE`.
- `channels_01..04` -> `12104 AE_CABLE_LOW_CHANNEL`.
- `channels_11..14` -> `12105 AE_CABLE_HIGH_CHANNEL`.
- `drive_cell_states` base/backdrop -> `12100 AE_BODY`.
- `drive_cell_states_emissive` / Cell Light element -> `12106 AE_DRIVE_LED`.
- `terminal_dark`, `terminal_medium`, `terminal_bright` -> `12109 AE_TERMINAL_TRACE` only.
- `terminal_*` must never route to any cable ID.
- `drive_cell_states` base/backdrop must never route to `12103` or `12106`.

## OC TESR Rule

Case/Rack/Microcontroller/Raid overlays are precise renderer routes, not generic filename guesses. If the renderer submits `POSITION_TEX`, the bridge must not hard-write entity data into the 5-int vertex format. The verified transport is:

- `transportMode=uniform_override_scope`
- `entitySlotWrite=no_entity_slot`
- `uniformOverride=success`

`no_entity_slot` is expected for OpenComputers `POSITION_TEX` overlays. Do not expand or stride-shift those vertices to fake an OptiFine entity slot.

Every OC TESR overlay scope must log `previousBlockEntityId`, `activeBlockEntityId`, `restoreResult`, `renderer`, `sprite`, `shaderPass`, and `transportMode`. A failed uniform restore is an `ERROR`.

## Parallel Release Strategy

OC and AE2 now move in parallel. OC may enter the first formal bloom shader once overlay leakage checks pass. AE2 remains debug-first except for confirmed low-risk IDs: `12104 AE_CABLE_LOW_CHANNEL`, `12105 AE_CABLE_HIGH_CHANNEL`, `12106 AE_DRIVE_LED`, and explicit `12101 AE_UVL_LIGHT` faces. `12109 AE_TERMINAL_TRACE` must not emit in the formal shader.
