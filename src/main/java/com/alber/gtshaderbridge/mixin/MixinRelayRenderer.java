package com.alber.gtshaderbridge.mixin;

import com.alber.gtshaderbridge.client.SemanticTransportProbe;
import li.cil.oc.common.tileentity.Relay;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "li.cil.oc.client.renderer.tileentity.RelayRenderer$", remap = false)
public abstract class MixinRelayRenderer {
    private static final ThreadLocal<AutoCloseable> GTSHADERBRIDGE_ROUTE = new ThreadLocal<AutoCloseable>();
    private static final ResourceLocation RELAY_ACTIVITY =
        new ResourceLocation("opencomputers", "blocks/overlay/switch_side_on");

    @Inject(
        method = "render(Lli/cil/oc/common/tileentity/Relay;DDDFIF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;func_78381_a()V",
            shift = At.Shift.BEFORE
        ),
        require = 1
    )
    private void gtshaderbridge$beginRelayActivity(
        Relay relay, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo ci
    ) {
        GTSHADERBRIDGE_ROUTE.set(SemanticTransportProbe.beginOcTesrRoute("RelayRenderer", "drawSwitchActivityOverlay", RELAY_ACTIVITY));
    }

    @Inject(
        method = "render(Lli/cil/oc/common/tileentity/Relay;DDDFIF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;func_78381_a()V",
            shift = At.Shift.AFTER
        ),
        require = 1
    )
    private void gtshaderbridge$endRelayActivity(
        Relay relay, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo ci
    ) {
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
