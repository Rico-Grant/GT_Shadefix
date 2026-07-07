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
