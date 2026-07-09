package com.alber.gtshaderbridge.mixin;

import com.alber.gtshaderbridge.client.SemanticTransportProbe;
import li.cil.oc.common.tileentity.DiskDrive;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "li.cil.oc.client.renderer.tileentity.DiskDriveRenderer$", remap = false)
public abstract class MixinDiskDriveRenderer {
    private static final ThreadLocal<AutoCloseable> GTSHADERBRIDGE_ROUTE = new ThreadLocal<AutoCloseable>();
    private static final ResourceLocation DISK_DRIVE_ACTIVITY =
        new ResourceLocation("opencomputers", "blocks/overlay/diskdrive_front_activity");

    @Inject(
        method = "render(Lli/cil/oc/common/tileentity/DiskDrive;DDDFIF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;func_78381_a()V",
            shift = At.Shift.BEFORE
        ),
        require = 1
    )
    private void gtshaderbridge$beginDiskDriveActivity(
        DiskDrive drive, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo ci
    ) {
        GTSHADERBRIDGE_ROUTE.set(SemanticTransportProbe.beginOcTesrRoute("DiskDriveRenderer", "drawActivityOverlay", DISK_DRIVE_ACTIVITY));
    }

    @Inject(
        method = "render(Lli/cil/oc/common/tileentity/DiskDrive;DDDFIF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;func_78381_a()V",
            shift = At.Shift.AFTER
        ),
        require = 1
    )
    private void gtshaderbridge$endDiskDriveActivity(
        DiskDrive drive, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo ci
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
