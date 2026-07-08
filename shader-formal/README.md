# AEOC Formal Shaderpacks

## OCBloom V1

Output:

`build/libs/ComplementaryReimagined_r5.8.1_SuSyIPBR_AEOC_OCBloom_V1.zip`

Runtime install target:

`Supersymmetry/shaderpacks/ComplementaryReimagined_r5.8.1_SuSyIPBR_AEOC_OCBloom_V1.zip`

Formal emission is enabled only for:

- `12101 AE_UVL_LIGHT`
- `12104 AE_CABLE_LOW_CHANNEL`
- `12105 AE_CABLE_HIGH_CHANNEL`
- `12106 AE_DRIVE_LED`
- `12111 OC_LED_BAKED`
- `12112 OC_LED_TESR`

Formal emission is disabled for:

- `12100 AE_BODY`
- `12102 AE_CONTROLLER_LIGHT`
- `12103 AE_CABLE_IDLE`
- `12107 AE_CRAFTING_LIGHT`
- `12108 AE_MONITOR_LIGHT`
- `12109 AE_TERMINAL_TRACE`
- `12110 OC_BODY`
- legacy fallback IDs `5072..5082`

`12112 OC_LED_TESR` preserves the original overlay texture color and uses conservative emission strength `4.5`. It does not perform color-threshold LED guessing and does not replace OpenComputers' original animation or blinking cadence.
