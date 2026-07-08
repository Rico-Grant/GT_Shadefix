# GT Shadefix

GT Shadefix is a small **client-side Minecraft 1.12.2 Forge mod** that helps OptiFine shaderpacks recognize dynamic modded blocks.

It was made for a Supersymmetry / GregTech CEu setup where many important blocks are not rendered like normal vanilla blocks. Without a bridge, a shaderpack may see them as one generic material, or may not see their useful state at all.

This mod does not add visual effects by itself. It only sends extra semantic material IDs to OptiFine shaders. A compatible shaderpack must read those IDs and decide how to render reflections, generated normals, LEDs, bloom, and other effects.

## What it is for

GT Shadefix is mainly used to route:

- GregTech CEu machine bodies
- GregTech pipes, wires, and insulated cables
- selected Applied Energistics 2 baked quads
- selected OpenComputers baked or TESR overlay passes

The goal is to let the shader distinguish things such as:

- machine body vs. pipe vs. cable
- AE controller light vs. cable channel light
- ME drive LED vs. ME drive body
- OC body vs. OC LED overlay

## Current status

This is still an experimental semantic/debug build.

Confirmed focus areas:

- GregTech CEu CCL-rendered machines and pipes
- AE2 baked model routing
- OpenComputers overlay transport tests

Some AE2 and OC routes are still being tuned. For example, terminal state routing and OC LED bloom may require both bridge-side routing fixes and shaderpack-side material rules.

## Requirements

- Minecraft 1.12.2
- Forge
- OptiFine for 1.12.2 shaders
- GregTech CEu
- A compatible shaderpack that uses the material IDs listed below

The mod is client-side only.

## Installation

1. Build or download the bridge jar.
2. Put the jar in the client `mods/` folder.
3. Remove older GT Shadefix / GT Shader Bridge test jars from `mods/`.
4. Start the game once to generate the config file.
5. Open:

```text
config/gt_shader_bridge.cfg
```

6. Set:

```properties
enabled=true
writeEntityData=true
```

7. Restart the game.
8. Use a shaderpack that knows the IDs below.

## Build output

The expected jar is:

```text
build/libs/gtceu-optifine-shader-bridge-semantic-debug.jar
```

## Important config options

```properties
enabled=false
writeEntityData=true

machineBodyMaterialId=11990
pipeMaterialId=11991
cableMaterialId=11992
frameMaterialId=11993
panelMaterialId=11994

routeAeOcBakedQuads=true
probeOcTesrTransport=true
caseTesrShaderFormatPoc=true

logAeOcBakedQuadRoutes=true
logSemanticTransport=true
logVertexWriter=true
logVertexFormat=true
```

Recommended for testing:

```properties
enabled=true
writeEntityData=true
logAeOcBakedQuadRoutes=true
logSemanticTransport=true
```

Recommended after routes are confirmed:

```properties
logAeOcBakedQuadRoutes=false
logSemanticTransport=false
logVertexWriter=false
logVertexFormat=false
```

## Material ID map

### GregTech CEu

| ID | Meaning |
|---:|---|
| 11990 | GregTech machine body |
| 11991 | GregTech pipe / uninsulated wire body |
| 11992 | GregTech insulated cable body |
| 11993 | reserved frame / structure route |
| 11994 | reserved panel / screen route |

### Applied Energistics 2

| ID | Meaning |
|---:|---|
| 5072 | legacy AE terminal dark state |
| 5073 | legacy AE terminal medium state |
| 5074 | legacy AE terminal bright state |
| 12100 | AE body / base quads |
| 12101 | AE UVL light |
| 12102 | AE controller light |
| 12103 | AE cable idle channel texture |
| 12104 | AE cable low-channel texture |
| 12105 | AE cable high-channel texture |
| 12106 | AE drive LED |
| 12107 | AE crafting light |
| 12108 | AE monitor light |
| 12109 | AE terminal trace / debug route |

### OpenComputers

| ID | Meaning |
|---:|---|
| 12110 | OC body / housing |
| 12111 | OC baked LED candidate |
| 12112 | OC TESR overlay transport probe |

## How it works

GT Shadefix does three main things:

1. It wraps selected GregTech CEu renderers while they submit vertices.
2. It writes a semantic material ID into OptiFine's shader entity-data slot.
3. It lets the shaderpack use that ID to apply custom material logic.

For AE2 and OpenComputers, it also inspects baked quad sprites or selected overlay render paths and routes them to more specific IDs.

## Shaderpack side

The shaderpack should read the incoming entity or block-entity ID and branch on it.

Example idea:

```glsl
if (entityId == 11990) {
    // GregTech machine body: generated normals + metallic reflection
}

if (entityId == 12106) {
    // ME drive LED: HDR emission + bloom
}
```

Exact variable names depend on the shaderpack.

## Troubleshooting

### Nothing changes in game

Check that:

- this jar is in `mods/`
- old bridge jars were removed
- `enabled=true`
- the compatible shaderpack is selected
- the shaderpack has rules for the material IDs above

### GT machines work, but AE2 or OC does not

Check the log for lines like:

```text
GTShaderBridge: semantic route detected
```

If those lines do not appear for AE2 or OC blocks, the current route is not reaching that renderer.

### The shader reports `Invalid program gbuffers_block`

That usually means the shaderpack failed to compile. The bridge may still be loaded, but the shader cannot use the routed IDs until the shader compile error is fixed.

### Performance notes

The GregTech path is usually lightweight. AE2 and OC debug routing can be more expensive because it inspects baked quads and logs routes. Turn off debug logging after testing.

## Development notes

This bridge deliberately avoids direct OpenGL rendering changes. It should not call `glUseProgram`, bind buffers, or edit already-submitted vertex data. The intended design is to tag vertices during normal rendering, then let the shaderpack decide the visual result.

Use the log output to verify routing before changing shader brightness or bloom values.
