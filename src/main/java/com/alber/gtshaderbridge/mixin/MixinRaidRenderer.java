package com.alber.gtshaderbridge.mixin;

import com.alber.gtshaderbridge.client.SemanticTransportProbe;
import java.lang.reflect.Method;
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
        Object iconName = invoke(icon, new String[] {"func_94215_i", "getIconName"});
        if (!(iconName instanceof String)) {
            return null;
        }

        return new ResourceLocation((String) iconName);
    }

    private static Object invoke(Object target, String[] names) {
        if (target == null) {
            return null;
        }

        Class<?> type = target.getClass();
        while (type != null) {
            for (int i = 0; i < names.length; i++) {
                try {
                    Method method = type.getMethod(names[i]);
                    method.setAccessible(true);
                    return method.invoke(target);
                } catch (NoSuchMethodException ignored) {
                    // Try declared/private methods or the next runtime/deobfuscated name.
                } catch (Throwable ignored) {
                    return null;
                }

                try {
                    Method method = type.getDeclaredMethod(names[i]);
                    method.setAccessible(true);
                    return method.invoke(target);
                } catch (NoSuchMethodException ignored) {
                    // Try the next runtime/deobfuscated name.
                } catch (Throwable ignored) {
                    return null;
                }
            }
            type = type.getSuperclass();
        }
        return null;
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
