# AEOC Semantic Debug Shader

This folder documents the shader-side companion for `gtceu-optifine-shader-bridge-semantic-debug.jar`.

The generated debug zip is derived from the currently installed Supersymmetry shaderpack:

`ComplementaryReimagined_r5.8.1_SuSyIPBR_Master_AEOC_StateFix_V2_2_TerminalOC.zip`

Output:

`build/libs/ComplementaryReimagined_r5.8.1_SuSyIPBR_AEOC_SemanticDebug.zip`

Second debug output:

`build/libs/ComplementaryReimagined_r5.8.1_SuSyIPBR_AEOC_SemanticDebug_V2.zip`

Debug IDs:

- `12100`: AE body, dark slate.
- `12101`: AE UVL light, pale cyan.
- `12102`: AE controller light, orange.
- `12103`: AE cable idle, dark gray.
- `12104`: AE cable low-channel quads, cyan.
- `12105`: AE cable high-channel quads, yellow.
- `12106`: AE drive LED quads, green.
- `12107`: AE crafting light, red.
- `12108`: AE monitor light, blue.
- `12109`: AE terminal trace, purple.
- `12110`: OpenComputers baked housing/body, deep gray.
- `12111`: OpenComputers baked LED, pink.
- `12112`: OpenComputers TESR overlay transport probe, pure magenta.

These branches intentionally set `emission = 0.0`. The first debug pass is about proving semantic routing and TESR transport, not final bloom tuning.

The generated shaderpack also includes `AEOC_SEMANTIC_DEBUG_IDS.txt` at zip root.

V2 keeps the same color table and extends the bridge-side OC TESR route coverage to `RaidRenderer.renderSlot`, covering `raid_front_activity` and `raid_front_error` without changing `opencomputers:raid` baked body quads.
