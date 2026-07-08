package com.alber.gtshaderbridge.mixin;

import com.alber.gtshaderbridge.client.SemanticTransportProbe;
import li.cil.oc.client.Textures;
import li.cil.oc.client.Textures$Block$;
import li.cil.oc.common.tileentity.Raid;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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
            target = "Lnet/minecraft/client/renderer/Tessellator;func_78381_a()V",
            shift = At.Shift.BEFORE
        ),
        require = 1
    )
    private void gtshaderbridge$beginRaidOverlayDraw(Raid raid, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo ci) {
        appendPresentDriveOverlays(raid);
        GTSHADERBRIDGE_ROUTE.set(SemanticTransportProbe.beginOcTesrRoute("RaidRenderer", "drawSlotOverlayBatch", RAID_SLOT_OVERLAYS));
    }

    @Inject(
        method = "render(Lli/cil/oc/common/tileentity/Raid;DDDFIF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;func_78381_a()V",
            shift = At.Shift.AFTER
        ),
        require = 1
    )
    private void gtshaderbridge$endRaidOverlayDraw(Raid raid, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo ci) {
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

    private static void appendPresentDriveOverlays(Raid raid) {
        if (raid == null) {
            return;
        }

        try {
            boolean[] presence = raid.presence();
            if (presence == null || presence.length == 0) {
                return;
            }

            TextureAtlasSprite icon = Textures.getSprite(Textures$Block$.MODULE$.RaidFrontActivity());
            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            int slots = Math.min(raid.func_70302_i_(), presence.length);
            for (int slot = 0; slot < slots; slot++) {
                if (presence[slot]) {
                    renderSlot(buffer, slot, icon);
                }
            }
        } catch (Throwable ignored) {
            // Keep the original OpenComputers renderer alive even if the extra presence overlay cannot be appended.
        }
    }

    private static void renderSlot(BufferBuilder buffer, int slot, TextureAtlasSprite icon) {
        float left = 2.0F / 16.0F + slot * 4.0F / 16.0F;
        float right = 2.0F / 16.0F + (slot + 1) * 4.0F / 16.0F;
        buffer.pos(left, 1.0D, 0.0D).tex(icon.getInterpolatedU(left * 16.0F), icon.getMaxV()).endVertex();
        buffer.pos(right, 1.0D, 0.0D).tex(icon.getInterpolatedU(right * 16.0F), icon.getMaxV()).endVertex();
        buffer.pos(right, 0.0D, 0.0D).tex(icon.getInterpolatedU(right * 16.0F), icon.getMinV()).endVertex();
        buffer.pos(left, 0.0D, 0.0D).tex(icon.getInterpolatedU(left * 16.0F), icon.getMinV()).endVertex();
    }
}
