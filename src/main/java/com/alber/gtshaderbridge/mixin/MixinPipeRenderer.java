package com.alber.gtshaderbridge.mixin;

import codechicken.lib.render.CCRenderState;
import com.alber.gtshaderbridge.GTShaderBridgeConfig;
import com.alber.gtshaderbridge.client.GTShaderBridgeScope;
import gregtech.client.renderer.pipe.CableRenderer;
import gregtech.client.renderer.pipe.PipeRenderer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = PipeRenderer.class, remap = false)
public abstract class MixinPipeRenderer {
    @Redirect(
        method = "renderBlock(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/client/renderer/BufferBuilder;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/client/renderer/pipe/PipeRenderer;renderPipeBlock(Lcodechicken/lib/render/CCRenderState;Lgregtech/client/renderer/pipe/PipeRenderer$PipeRenderContext;)V"
        )
    )
    private void gtshaderbridge$renderPipeBodyWithMaterialScope(
        PipeRenderer renderer,
        CCRenderState renderState,
        PipeRenderer.PipeRenderContext context,
        IBlockAccess world,
        BlockPos pos,
        IBlockState state,
        BufferBuilder buffer
    ) {
        if (!GTShaderBridgeConfig.enabled) {
            renderer.renderPipeBlock(renderState, context);
            return;
        }

        int materialId = getMaterialId(renderer, state);
        GTShaderBridgeScope scope = GTShaderBridgeScope.begin(world, pos, materialId);
        try {
            renderer.renderPipeBlock(renderState, context);
        } finally {
            scope.close();
        }
    }

    private static int getMaterialId(PipeRenderer renderer, IBlockState state) {
        if (!(renderer instanceof CableRenderer)) {
            return GTShaderBridgeConfig.pipeMaterialId;
        }

        String stateText = String.valueOf(state);
        return stateText.contains("gregtech:wire_")
            ? GTShaderBridgeConfig.pipeMaterialId
            : GTShaderBridgeConfig.cableMaterialId;
    }
}
