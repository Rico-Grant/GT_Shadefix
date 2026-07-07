# GregTech CEu OptiFine Shader Bridge

Semantic debug build for the Supersymmetry HMCL instance.

This version writes semantic material ids into selected GregTech CEu CCL body renderers, audited AE2 baked quads, and OpenComputers TESR overlay transport probes. It uses the current `BufferBuilder`'s OptiFine `sVertexBuilder` entity-data stack and does not call any global shader entity stack.

## Output

`build/libs/gtceu-optifine-shader-bridge-semantic-debug.jar`

Do not use the older jars:

- `gtceu-optifine-shader-bridge-debug-1.0.0.jar`
- `gtceu-optifine-shader-bridge-release-1.1.0.jar`
- `gtceu-optifine-shader-bridge-debug-1.1.0.jar`
- `gtceu-optifine-shader-bridge-diagnostic-1.2.0.jar`
- `gtceu-optifine-shader-bridge-diagnostic-1.3.0.jar`
- `gtceu-optifine-shader-bridge-write-debug-1.4.0.jar`
- `gtceu-optifine-shader-bridge-multiroute-debug-1.5.0.jar`
- `gtceu-optifine-shader-bridge-routeprobe-debug-1.6.4.jar`

## What It Does

During:

`MetaTileEntityRenderer.renderBlock(...) -> MetaTileEntity.renderMetaTileEntity(...)`

it opens a `ThreadLocal` diagnostic scope:

`activeMaterialGroup = machineBodyMaterialId`

During:

`PipeRenderer.renderBlock(...) -> PipeRenderer.renderPipeBlock(...)`

it opens:

- `activeMaterialGroup = pipeMaterialId` for non-cable pipe body geometry
- `activeMaterialGroup = pipeMaterialId` for `CableRenderer` states whose registry path starts with `wire_`
- `activeMaterialGroup = cableMaterialId` for insulated `CableRenderer` states whose registry path starts with `cable_`

Then it counts calls to:

`codechicken.lib.render.CCRenderState.writeVert()`

It logs distinct vertex format signatures seen at that point, including:

- current `BufferBuilder` class
- whether a `sVertexBuilder` field exists on the buffer
- `SVertexFormat.vertexSizeBlock`
- `SVertexFormat.offsetEntity`
- current `VertexFormat` byte size, int size, and element list

When CCL reaches its final `BufferBuilder.endVertex()` call, this build:

1. verifies the active format matches OptiFine's 14-int shader block layout;
2. verifies `SVertexFormat.offsetEntity == 12`;
3. reads the current buffer's `sVertexBuilder.entityData[entityDataIndex]`;
4. temporarily replaces that top value with `debugMaterialId`;
5. calls the original `BufferBuilder.endVertex()`;
6. restores the previous top value in `finally`.

No global `Shaders` entity stack is touched. No OpenGL state is touched. No already-submitted vertex buffer data is edited after `endVertex()`.

## Config

Created at:

`config/gt_shader_bridge.cfg`

Defaults:

```properties
enabled=false
machineBodyMaterialId=11990
pipeMaterialId=11991
cableMaterialId=11992
frameMaterialId=11993
panelMaterialId=11994
logVertexWriter=true
logVertexFormat=true
writeEntityData=true
```

Set `enabled=true` for the diagnostic test.

Expected log after a Basic Electrolyzer or other `gregtech:machine` MTE body is rebuilt:

```text
GTShaderBridge: CCL MTE vertex writer detected, vertices=N
GTShaderBridge: CCL MTE vertex format detected: buffer=..., sVertexBuilder=..., optifine=SVertexFormat{...}, formatClass=..., sizeBytes=..., intSize=..., elements=...
GTShaderBridge: writing OptiFine entity data on CCL MTE vertices, materialId=11990
```

AE2/OpenComputers semantic debug routes use:

- `12100`: AE cable idle (`channels_00`, `channels_10`).
- `12101`: AE cable low channel (`channels_01..04`).
- `12102`: AE cable high channel (`channels_11..14`).
- `12103`: AE drive LED.
- `12104`: AE terminal debug-only route.
- `12110`: OpenComputers baked housing/body.
- `12112`: OpenComputers TESR overlay transport probe.

For 5-8 channel smart cables, AE2 emits both low and high channel quads; the bridge routes those quads independently.

## Reverse Findings

Current jars inspected:

- `preview_OptiFine_1.12.2_HD_U_G6_pre1.jar`
- `gregtech-1.12.2-2.8.10-beta.jar`
- `CodeChickenLib-1.12.2-3.2.3.358-universal.jar`

GregTech path:

`MetaTileEntityRenderer.renderBlock(...)`

calls:

`MetaTileEntity.renderMetaTileEntity(CCRenderState, Matrix4, IVertexOperation[])`

CCL vertex path:

`CCRenderState.render()`

loops model vertices and calls:

`CCRenderState.writeVert()`

`writeVert()` writes the current vertex by calling `BufferBuilder` methods for each current `VertexFormat` element, then calls:

`BufferBuilder.endVertex()` / SRG `func_181675_d()`

This write-debug build injects at `CCRenderState.writeVert()` HEAD for logging and redirects only the `BufferBuilder.endVertex()` call inside `CCRenderState.writeVert()`.

The write path is skipped unless the runtime format matches OptiFine's shader block vertex layout and exposes the entity-data slot.

Pipe path confirmed in `gregtech-1.12.2-2.8.10-beta.jar`:

`PipeRenderer.renderBlock(IBlockAccess, BlockPos, IBlockState, BufferBuilder)`

binds the provided `BufferBuilder` to `CCRenderState`, builds a `PipeRenderContext`, calls:

`PipeRenderer.renderPipeBlock(CCRenderState, PipeRenderContext)`

then renders optional pipe frames and covers separately. This bridge wraps only `renderPipeBlock`; it does not wrap `renderFrame`, `renderCovers`, block damage rendering, item rendering, GUI, TESR, or FastTESR.

## Deliberately Not Used

This build does not call:

- `net.optifine.shaders.Shaders.pushEntity(...)`
- `net.optifine.shaders.Shaders.popEntity()`
- `net.optifine.shaders.SVertexBuilder.pushEntity(...)`
- `net.optifine.shaders.SVertexBuilder.popEntity(...)`
- any `glVertexAttrib`, `glUseProgram`, or `glBindBuffer` path

The companion semantic debug shaderpack maps `12112` to pure magenta and `12110` to deep gray with no HDR emission.
