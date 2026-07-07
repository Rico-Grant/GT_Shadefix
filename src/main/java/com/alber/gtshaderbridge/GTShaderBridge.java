package com.alber.gtshaderbridge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = GTShaderBridge.MODID,
    name = GTShaderBridge.NAME,
    version = GTShaderBridge.VERSION,
    acceptedMinecraftVersions = "[1.12.2]",
    clientSideOnly = true,
    dependencies = "required-after:gregtech"
)
public final class GTShaderBridge {
    public static final String MODID = "gtshaderbridge";
    public static final String NAME = "GregTech CEu OptiFine Shader Bridge";
    public static final String VERSION = "1.7.0-semantic-debug";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        GTShaderBridgeConfig.load(event.getModConfigurationDirectory());
        LOGGER.info("[GTShaderBridge] Semantic debug bridge loaded; enabled={}, writeEntityData={}, machine={}, pipe={}, cable={}, aeOcBakedQuads={}, ocTesrProbe={}, aeCableIdle={}, aeCableLow={}, aeCableHigh={}, ocBody={}, ocLedTesr={}",
            GTShaderBridgeConfig.enabled, GTShaderBridgeConfig.writeEntityData,
            GTShaderBridgeConfig.machineBodyMaterialId, GTShaderBridgeConfig.pipeMaterialId,
            GTShaderBridgeConfig.cableMaterialId, GTShaderBridgeConfig.routeAeOcBakedQuads,
            GTShaderBridgeConfig.probeOcTesrTransport,
            GTShaderBridgeConfig.aeCableIdleMaterialId, GTShaderBridgeConfig.aeCableLowChannelMaterialId,
            GTShaderBridgeConfig.aeCableHighChannelMaterialId, GTShaderBridgeConfig.ocBodyMaterialId,
            GTShaderBridgeConfig.ocLedTesrMaterialId);
    }
}
