package com.alber.gtshaderbridge.mixin;

import com.alber.gtshaderbridge.client.SemanticTransportProbe;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "li.cil.oc.api.event.RackMountableRenderEvent$TileEntity", remap = false)
public abstract class MixinRackMountableRenderEventTileEntity {
    private static final ThreadLocal<AutoCloseable> GTSHADERBRIDGE_ROUTE = new ThreadLocal<AutoCloseable>();

    @Inject(
        method = "renderOverlayFromAtlas(Lnet/minecraft/util/ResourceLocation;FF)V",
        at = @At("HEAD"),
        require = 1
    )
    private void gtshaderbridge$beginRackOverlay(ResourceLocation overlay, float u, float v, CallbackInfo ci) {
        GTSHADERBRIDGE_ROUTE.set(SemanticTransportProbe.beginOcTesrRoute("RackMountableRenderEvent.TileEntity", "renderOverlayFromAtlas", overlay));
    }

    @Inject(
        method = "renderOverlayFromAtlas(Lnet/minecraft/util/ResourceLocation;FF)V",
        at = @At("RETURN"),
        require = 1
    )
    private void gtshaderbridge$endRackOverlay(ResourceLocation overlay, float u, float v, CallbackInfo ci) {
        closeRoute();
    }

    private static void closeRoute() {
        AutoCloseable route = GTSHADERBRIDGE_ROUTE.get();
        GTSHADERBRIDGE_ROUTE.remove();
        if (route == null) {
            return;
        }

        try {
            route.close();
        } catch (Throwable ignored) {
            // Route cleanup must never break tile rendering.
        }
    }
}
