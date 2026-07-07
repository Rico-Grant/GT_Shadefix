package com.alber.gtshaderbridge.mixin;

import com.alber.gtshaderbridge.client.SemanticTransportProbe;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "li.cil.oc.client.renderer.tileentity.CaseRenderer$", remap = false)
public abstract class MixinCaseRenderer {
    private static final ThreadLocal<AutoCloseable> GTSHADERBRIDGE_ROUTE = new ThreadLocal<AutoCloseable>();

    @Inject(
        method = "renderFrontOverlay(Lnet/minecraft/util/ResourceLocation;)V",
        at = @At("HEAD"),
        require = 1
    )
    private void gtshaderbridge$beginCaseOverlay(ResourceLocation overlay, CallbackInfo ci) {
        GTSHADERBRIDGE_ROUTE.set(SemanticTransportProbe.beginOcTesrRoute("CaseRenderer", "renderFrontOverlay", overlay));
    }

    @Inject(
        method = "renderFrontOverlay(Lnet/minecraft/util/ResourceLocation;)V",
        at = @At("RETURN"),
        require = 1
    )
    private void gtshaderbridge$endCaseOverlay(ResourceLocation overlay, CallbackInfo ci) {
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
