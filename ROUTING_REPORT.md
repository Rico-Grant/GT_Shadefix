# GTShaderBridge AE2 / OpenComputers Semantic Routing Report

## Scope

This build keeps the existing GregTech CEu CCL routes and adds semantic debug routes for Applied Energistics 2 and OpenComputers.

Installed debug jar:

`gtceu-optifine-shader-bridge-semantic-debug.jar`

## Rendering Paths

- GregTech machine body: `MetaTileEntityRenderer.renderBlock` -> `MetaTileEntity.renderMetaTileEntity` -> CCL `CCRenderState.writeVert`.
- GregTech pipes/cables: `PipeRenderer.renderBlock` -> `PipeRenderer.renderPipeBlock` -> CCL `CCRenderState.writeVert`.
- AE2 cable bus, terminals, smart channels, and drive LEDs: vanilla/Forge `BlockModelRenderer` BakedQuad path.
- OpenComputers baked block bodies: vanilla/Forge `BlockModelRenderer` BakedQuad path.
- OpenComputers case, microcontroller, and rack overlay lamps: TESR overlay draw path, probed before formal bloom.

## Bridge Injection Points

- CCL path: `CCRenderState.writeVert`, wrapping `BufferBuilder.endVertex`.
- BakedQuad path: `BlockModelRenderer.renderModel` scopes the current block model, and `BufferBuilder.addVertexData(int[])` is wrapped while a routed quad is submitted.
- TESR path: `CaseRenderer.renderFrontOverlay`, `MicrocontrollerRenderer.renderFrontOverlay`, and `RackMountableRenderEvent.TileEntity.renderOverlayFromAtlas` open an OC overlay route, then `BufferBuilder.endVertex()` records the actual transport format and attempts the scoped semantic write only if an OptiFine entity slot exists.

## Dynamic Material IDs

- `12100`: AE2 body/backdrop/base quads.
- `12101`: AE2 UVL light quads.
- `12102`: AE2 controller light quads.
- `12103`: AE2 smart/dense cable inactive channel quads (`channels_00`, `channels_10`).
- `12104`: AE2 smart/dense cable low channel quads (`channels_01..04`).
- `12105`: AE2 smart/dense cable high channel quads (`channels_11..14`).
- `12106`: AE2 ME drive status LED quads.
- `12107`: AE2 crafting light quads.
- `12108`: AE2 monitor light quads.
- `12109`: AE2 terminal trace quads, debug-only.
- `12110`: OpenComputers non-emissive baked housing/body.
- `12111`: OpenComputers baked LED quads.
- `12112`: OpenComputers TESR overlay transport probe.

## Sprite Rules

AE2 routes are selected from the actual `TextureAtlasSprite.getIconName()` already chosen by AE2:

- `*terminal_dark`, `*terminal_medium`, `*terminal_bright`, and `fluid_terminal/lights_*` -> `12109` only.
- `*drive_cell_states_emissive*` -> `12106`.
- `*drive_cell_states` base/backdrop -> `12100`.
- `parts/cable/smart/channels_00` or `channels_10` -> `12103`.
- `parts/cable/smart/channels_01..04` -> `12104`.
- `parts/cable/smart/channels_11..14` -> `12105`.
- `parts/cable/dense_smart/channels_00` or `channels_10` -> `12103`.
- `parts/cable/dense_smart/channels_01..04` -> `12104`.
- `parts/cable/dense_smart/channels_11..14` -> `12105`.

5-8 channel states should produce both `12104` and `12105` because AE2 emits the odd/even channel layers as separate quads.

OpenComputers routes:

- Baked `opencomputers:*` model sprites -> `12110`.
- Known TESR overlay methods listed above -> attempted `12112`.
- There is intentionally no generic `_on`, `_light`, `_indicator`, or `blocks/overlay` string heuristic.

## Shader Rules

The semantic debug shaderpack maps `12100..12112` in:

`shaders/lib/materials/materialHandling/blockEntityIPBR.glsl`

`shaders/lib/materials/materialHandling/terrainIPBR.glsl`

No real emission or bloom is enabled in this debug pack. `12112` is pure magenta, `12110` is deep gray, and route colors are used only to prove semantic routing and transport.

## Runtime Verification

With `logAeOcBakedQuadRoutes=true`, `latest.log` should contain one line per unique routed baked sprite:

`GTShaderBridge: semantic route detected, source=BakedQuad, ..., modelClass=..., sprite=..., chosenId=...(...), selectionReason=..., legacyFallback=false`

With `probeOcTesrTransport=true`, OpenComputers TESR overlays should also log:

`GTShaderBridge: semantic transport detected, source=OC_TESR, renderer=..., overlay=..., writeResult=..., intSize=..., hasEntitySlotCandidate=...`
