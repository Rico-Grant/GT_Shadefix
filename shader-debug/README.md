# AEOC Semantic Debug Shader

This folder documents the shader-side companion for `gtceu-optifine-shader-bridge-semantic-debug.jar`.

The generated debug zip is derived from the currently installed Supersymmetry shaderpack:

`ComplementaryReimagined_r5.8.1_SuSyIPBR_Master_AEOC_StateFix_V2_2_TerminalOC.zip`

Output:

`build/libs/ComplementaryReimagined_r5.8.1_SuSyIPBR_AEOC_SemanticDebug.zip`

Debug IDs:

- `12100`: AE cable idle, dark gray.
- `12101`: AE cable low-channel quads, cyan.
- `12102`: AE cable high-channel quads, yellow.
- `12103`: AE drive LED quads, green.
- `12104`: AE terminal debug-only quads, purple.
- `12110`: OpenComputers baked housing/body, deep gray.
- `12112`: OpenComputers TESR overlay transport probe, pure magenta.

These branches intentionally set `emission = 0.0`. The first debug pass is about proving semantic routing and TESR transport, not final bloom tuning.
