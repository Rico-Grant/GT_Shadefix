package com.alber.gtshaderbridge.mixin;

import com.alber.gtshaderbridge.client.SemanticTransportProbe;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "li.cil.oc.client.renderer.tileentity.MicrocontrollerRenderer$", remap = false)
public abstract class MixinMicrocontrollerRenderer {
    private static final ThreadLocal<AutoCloseable> GTSHADERBRIDGE_ROUTE = new ThreadLocal<AutoCloseable>();

    @Inject(
        method = "renderFrontOverlay(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/BufferBuilder;)V",
        at = @At("HEAD"),
        require = 1
    )
    private void gtshaderbridge$beginMicrocontrollerOverlay(ResourceLocation overlay, BufferBuilder buffer, CallbackInfo ci) {
        GTSHADERBRIDGE_ROUTE.set(SemanticTransportProbe.beginOcTesrRoute("MicrocontrollerRenderer", "renderFrontOverlay", overlay));
    }

    @Inject(
        method = "renderFrontOverlay(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/BufferBuilder;)V",
        at = @At("RETURN"),
        require = 1
    )
    private void gtshaderbridge$endMicrocontrollerOverlay(ResourceLocation overlay, BufferBuilder buffer, CallbackInfo ci) {
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
