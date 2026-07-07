package com.alber.gtshaderbridge.mixin;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.alber.gtshaderbridge.GTShaderBridgeConfig;
import com.alber.gtshaderbridge.client.GTShaderBridgeScope;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.client.renderer.handler.MetaTileEntityRenderer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MetaTileEntityRenderer.class, remap = false)
public abstract class MixinMetaTileEntityRenderer {
    @Redirect(
        method = "renderBlock(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/client/renderer/BufferBuilder;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/metatileentity/MetaTileEntity;renderMetaTileEntity(Lcodechicken/lib/render/CCRenderState;Lcodechicken/lib/vec/Matrix4;[Lcodechicken/lib/render/pipeline/IVertexOperation;)V"
        )
    )
    private void gtshaderbridge$renderMteBodyWithDiagnosticScope(
        MetaTileEntity metaTileEntity,
        CCRenderState renderState,
        Matrix4 matrix,
        IVertexOperation[] operations,
        IBlockAccess world,
        BlockPos pos,
        IBlockState state,
        BufferBuilder buffer
    ) {
        if (!GTShaderBridgeConfig.enabled) {
            metaTileEntity.renderMetaTileEntity(renderState, matrix, operations);
            return;
        }

        GTShaderBridgeScope scope = GTShaderBridgeScope.begin(world, pos, GTShaderBridgeConfig.machineBodyMaterialId);
        try {
            metaTileEntity.renderMetaTileEntity(renderState, matrix, operations);
        } finally {
            scope.close();
        }
    }
}
