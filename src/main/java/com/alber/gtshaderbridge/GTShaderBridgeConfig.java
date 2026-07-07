package com.alber.gtshaderbridge;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

public final class GTShaderBridgeConfig {
    public static boolean enabled = false;
    public static int machineBodyMaterialId = 11990;
    public static int pipeMaterialId = 11991;
    public static int cableMaterialId = 11992;
    public static int frameMaterialId = 11993;
    public static int panelMaterialId = 11994;
    public static boolean routeAeOcBakedQuads = true;
    public static boolean logAeOcBakedQuadRoutes = true;
    public static boolean probeOcTesrTransport = true;
    public static boolean logSemanticTransport = true;
    public static int aeCableIdleMaterialId = 12100;
    public static int aeCableLowChannelMaterialId = 12101;
    public static int aeCableHighChannelMaterialId = 12102;
    public static int aeDriveLedMaterialId = 12103;
    public static int aeTerminalDebugMaterialId = 12104;
    public static int ocBodyMaterialId = 12110;
    public static int ocLedTesrMaterialId = 12112;
    public static boolean logVertexWriter = true;
    public static boolean logVertexFormat = true;
    public static boolean writeEntityData = true;

    private GTShaderBridgeConfig() {
    }

    public static void load(File configDirectory) {
        File configFile = new File(configDirectory, "gt_shader_bridge.cfg");
        Configuration config = new Configuration(configFile);
        try {
            config.load();
            enabled = config.getBoolean("enabled", Configuration.CATEGORY_CLIENT, false,
                "Enable the client-only GregTech CEu CCL vertex-writer shader bridge.");
            machineBodyMaterialId = config.getInt("machineBodyMaterialId", Configuration.CATEGORY_CLIENT, 11990, 0, 65535,
                "OptiFine shader material id for GregTech MetaTileEntity machine body geometry.");
            pipeMaterialId = config.getInt("pipeMaterialId", Configuration.CATEGORY_CLIENT, 11991, 0, 65535,
                "OptiFine shader material id for GregTech fluid/item/optical/laser pipe and uninsulated wire body geometry.");
            cableMaterialId = config.getInt("cableMaterialId", Configuration.CATEGORY_CLIENT, 11992, 0, 65535,
                "OptiFine shader material id for GregTech insulated cable body geometry.");
            frameMaterialId = config.getInt("frameMaterialId", Configuration.CATEGORY_CLIENT, 11993, 0, 65535,
                "Reserved OptiFine shader material id for confirmed CCL-only dynamic frame geometry. Not used by the bridge yet.");
            panelMaterialId = config.getInt("panelMaterialId", Configuration.CATEGORY_CLIENT, 11994, 0, 65535,
                "Reserved OptiFine shader material id for future controller, screen, and dynamic panel adapters. Not used by the bridge yet.");
            routeAeOcBakedQuads = config.getBoolean("routeAeOcBakedQuads", Configuration.CATEGORY_CLIENT, true,
                "Route AE2/OpenComputers BakedQuad sprites to precise shader material ids. This only affects appliedenergistics2 and opencomputers block models.");
            logAeOcBakedQuadRoutes = config.getBoolean("logAeOcBakedQuadRoutes", Configuration.CATEGORY_CLIENT, true,
                "Log each AE2/OpenComputers BakedQuad sprite route once.");
            probeOcTesrTransport = config.getBoolean("probeOcTesrTransport", Configuration.CATEGORY_CLIENT, true,
                "Probe OpenComputers TESR overlay transport before enabling any formal OC LED emission.");
            logSemanticTransport = config.getBoolean("logSemanticTransport", Configuration.CATEGORY_CLIENT, true,
                "Log each unique semantic-ID route and TESR transport format once.");
            aeCableIdleMaterialId = config.getInt("aeCableIdleMaterialId", Configuration.CATEGORY_CLIENT, 12100, 0, 65535,
                "Semantic shader material id for inactive AE2 smart/dense cable channel quads.");
            aeCableLowChannelMaterialId = config.getInt("aeCableLowChannelMaterialId", Configuration.CATEGORY_CLIENT, 12101, 0, 65535,
                "Semantic shader material id for AE2 channels_01..04 quads.");
            aeCableHighChannelMaterialId = config.getInt("aeCableHighChannelMaterialId", Configuration.CATEGORY_CLIENT, 12102, 0, 65535,
                "Semantic shader material id for AE2 channels_11..14 quads.");
            aeDriveLedMaterialId = config.getInt("aeDriveLedMaterialId", Configuration.CATEGORY_CLIENT, 12103, 0, 65535,
                "Semantic shader material id for confirmed AE2 ME drive cell-status LED quads.");
            aeTerminalDebugMaterialId = config.getInt("aeTerminalDebugMaterialId", Configuration.CATEGORY_CLIENT, 12104, 0, 65535,
                "Debug-only semantic material id for audited AE2 terminal light quads; not used for formal emission yet.");
            ocBodyMaterialId = config.getInt("ocBodyMaterialId", Configuration.CATEGORY_CLIENT, 12110, 0, 65535,
                "Semantic shader material id for non-emissive OpenComputers baked model housing.");
            ocLedTesrMaterialId = config.getInt("ocLedTesrMaterialId", Configuration.CATEGORY_CLIENT, 12112, 0, 65535,
                "Semantic shader material id force-written by the OpenComputers TESR overlay transport probe.");
            logVertexWriter = config.getBoolean("logVertexWriter", Configuration.CATEGORY_CLIENT, true,
                "Log once per chunk when a scoped GregTech CCL renderer reaches the CCL vertex writer.");
            logVertexFormat = config.getBoolean("logVertexFormat", Configuration.CATEGORY_CLIENT, true,
                "Log distinct CCL/OptiFine vertex formats observed while a scoped GregTech CCL renderer is active.");
            writeEntityData = config.getBoolean("writeEntityData", Configuration.CATEGORY_CLIENT, true,
                "Write the active scoped material id into the current BufferBuilder's OptiFine sVertexBuilder entity-data slot while scoped GregTech CCL vertices are submitted.");
        } catch (RuntimeException e) {
            GTShaderBridge.LOGGER.warn("[GTShaderBridge] Failed to load gt_shader_bridge.cfg; bridge remains disabled", e);
            enabled = false;
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }
}
