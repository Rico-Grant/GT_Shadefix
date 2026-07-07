package com.alber.gtshaderbridge.mixin;

import codechicken.lib.render.CCRenderState;
import com.alber.gtshaderbridge.client.GTShaderBridgeScope;
import net.minecraft.client.renderer.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CCRenderState.class, remap = false)
public abstract class MixinCCRenderState {
    @Inject(method = "writeVert()V", at = @At("HEAD"))
    private void gtshaderbridge$countCclMteVertex(CallbackInfo ci) {
        GTShaderBridgeScope.recordCclVertex((CCRenderState) (Object) this);
    }

    @Redirect(
        method = "writeVert()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/BufferBuilder;func_181675_d()V"
        )
    )
    private void gtshaderbridge$endCclMteVertexWithEntityData(BufferBuilder buffer) {
        GTShaderBridgeScope.endCclVertex((CCRenderState) (Object) this, buffer);
    }
}
