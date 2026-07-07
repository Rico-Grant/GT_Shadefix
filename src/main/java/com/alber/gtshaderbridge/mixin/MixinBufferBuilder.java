package com.alber.gtshaderbridge.mixin;

import com.alber.gtshaderbridge.client.BakedQuadShaderRouter;
import com.alber.gtshaderbridge.client.SemanticTransportProbe;
import net.minecraft.client.renderer.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BufferBuilder.class, remap = false)
public abstract class MixinBufferBuilder {
    @Inject(
        method = "func_181675_d()V",
        at = @At("HEAD"),
        require = 1
    )
    private void gtshaderbridge$beginSemanticEndVertex(CallbackInfo ci) {
        SemanticTransportProbe.beginBufferEndVertex((BufferBuilder) (Object) this);
    }

    @Inject(
        method = "func_181675_d()V",
        at = @At("RETURN"),
        require = 1
    )
    private void gtshaderbridge$endSemanticEndVertex(CallbackInfo ci) {
        SemanticTransportProbe.endBufferEndVertex();
    }

    @Inject(
        method = "func_178981_a([I)V",
        at = @At("HEAD"),
        require = 1
    )
    private void gtshaderbridge$beginBakedQuadEntityData(int[] vertexData, CallbackInfo ci) {
        BakedQuadShaderRouter.beginAddVertexData((BufferBuilder) (Object) this, vertexData);
    }

    @Inject(
        method = "func_178981_a([I)V",
        at = @At("RETURN"),
        require = 1
    )
    private void gtshaderbridge$endBakedQuadEntityData(int[] vertexData, CallbackInfo ci) {
        BakedQuadShaderRouter.endAddVertexData();
    }
}
