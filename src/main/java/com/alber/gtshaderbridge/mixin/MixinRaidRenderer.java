package com.alber.gtshaderbridge.mixin;

import com.alber.gtshaderbridge.client.SemanticTransportProbe;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "li.cil.oc.client.renderer.tileentity.RaidRenderer$", remap = false)
public abstract class MixinRaidRenderer {
    private static final ThreadLocal<AutoCloseable> GTSHADERBRIDGE_ROUTE = new ThreadLocal<AutoCloseable>();
    private static final ResourceLocation RAID_SLOT_OVERLAYS =
        new ResourceLocation("opencomputers", "blocks/overlay/raid_front_error_or_activity");

    @Inject(
        method = "render(Lli/cil/oc/common/tileentity/Raid;DDDFIF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;draw()V",
            shift = At.Shift.BEFORE
        ),
        require = 1
    )
    private void gtshaderbridge$beginRaidOverlayDraw(CallbackInfo ci) {
        GTSHADERBRIDGE_ROUTE.set(SemanticTransportProbe.beginOcTesrRoute("RaidRenderer", "drawSlotOverlayBatch", RAID_SLOT_OVERLAYS));
    }

    @Inject(
        method = "render(Lli/cil/oc/common/tileentity/Raid;DDDFIF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;draw()V",
            shift = At.Shift.AFTER
        ),
        require = 1
    )
    private void gtshaderbridge$endRaidOverlayDraw(CallbackInfo ci) {
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
