package com.alber.gtshaderbridge.mixin;

import com.alber.gtshaderbridge.client.SemanticTransportProbe;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "li.cil.oc.client.renderer.tileentity.RaidRenderer$", remap = false)
public abstract class MixinRaidRenderer {
    private static final ThreadLocal<AutoCloseable> GTSHADERBRIDGE_ROUTE = new ThreadLocal<AutoCloseable>();

    @Inject(
        method = "li$cil$oc$client$renderer$tileentity$RaidRenderer$$renderSlot(Lnet/minecraft/client/renderer/BufferBuilder;ILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V",
        at = @At("HEAD"),
        require = 1
    )
    private void gtshaderbridge$beginRaidOverlay(BufferBuilder buffer, int slot, TextureAtlasSprite icon, CallbackInfo ci) {
        GTSHADERBRIDGE_ROUTE.set(SemanticTransportProbe.beginOcTesrRoute("RaidRenderer", "renderSlot", overlayLocation(icon)));
    }

    @Inject(
        method = "li$cil$oc$client$renderer$tileentity$RaidRenderer$$renderSlot(Lnet/minecraft/client/renderer/BufferBuilder;ILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V",
        at = @At("RETURN"),
        require = 1
    )
    private void gtshaderbridge$endRaidOverlay(BufferBuilder buffer, int slot, TextureAtlasSprite icon, CallbackInfo ci) {
        closeRoute();
    }

    private static ResourceLocation overlayLocation(TextureAtlasSprite icon) {
        if (icon == null || icon.getIconName() == null) {
            return null;
        }

        return new ResourceLocation(icon.getIconName());
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
