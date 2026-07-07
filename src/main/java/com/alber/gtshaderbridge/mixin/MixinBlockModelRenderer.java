package com.alber.gtshaderbridge.mixin;

import com.alber.gtshaderbridge.client.BakedQuadShaderRouter;
import java.lang.reflect.Method;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockModelRenderer.class, remap = false)
public abstract class MixinBlockModelRenderer {
    @Inject(
        method = "func_187493_a(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
        at = @At("HEAD"),
        require = 1
    )
    private void gtshaderbridge$logAeOcBakedModelEntry(
        IBlockAccess world,
        IBakedModel model,
        IBlockState state,
        BlockPos pos,
        BufferBuilder buffer,
        boolean checkSides,
        long rand,
        CallbackInfoReturnable<Boolean> cir
    ) {
        BakedQuadShaderRouter.logRenderModelEntry((BlockModelRenderer) (Object) this, model, state, pos);
    }

    @Redirect(
        method = "func_187493_a(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/BlockModelRenderer;func_187498_b(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z"
        ),
        require = 1
    )
    private boolean gtshaderbridge$routeSmoothBakedModel(
        BlockModelRenderer renderer,
        IBlockAccess world,
        IBakedModel model,
        IBlockState state,
        BlockPos pos,
        BufferBuilder buffer,
        boolean checkSides,
        long rand
    ) {
        return routeBakedModel(renderer, "func_187498_b", world, model, state, pos, buffer, checkSides, rand);
    }

    @Redirect(
        method = "func_187493_a(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/BlockModelRenderer;func_187497_c(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z"
        ),
        require = 1
    )
    private boolean gtshaderbridge$routeFlatBakedModel(
        BlockModelRenderer renderer,
        IBlockAccess world,
        IBakedModel model,
        IBlockState state,
        BlockPos pos,
        BufferBuilder buffer,
        boolean checkSides,
        long rand
    ) {
        return routeBakedModel(renderer, "func_187497_c", world, model, state, pos, buffer, checkSides, rand);
    }

    private static boolean routeBakedModel(
        BlockModelRenderer renderer,
        String methodName,
        IBlockAccess world,
        IBakedModel model,
        IBlockState state,
        BlockPos pos,
        BufferBuilder buffer,
        boolean checkSides,
        long rand
    ) {
        BakedQuadShaderRouter.begin(renderer, world, model, state, pos, rand);
        try {
            return invokeRenderModel(renderer, methodName, world, model, state, pos, buffer, checkSides, rand);
        } finally {
            BakedQuadShaderRouter.end();
        }
    }

    private static boolean invokeRenderModel(
        BlockModelRenderer renderer,
        String methodName,
        IBlockAccess world,
        IBakedModel model,
        IBlockState state,
        BlockPos pos,
        BufferBuilder buffer,
        boolean checkSides,
        long rand
    ) {
        try {
            Method method = findRenderModelMethod(renderer.getClass(), methodName);
            Object value = method.invoke(renderer, world, model, state, pos, buffer, Boolean.valueOf(checkSides), Long.valueOf(rand));
            return ((Boolean) value).booleanValue();
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException("Failed to invoke BlockModelRenderer " + methodName, e);
        }
    }

    private static Method findRenderModelMethod(Class<?> owner, String methodName) throws NoSuchMethodException {
        Class<?> type = owner;
        while (type != null) {
            try {
                Method method = type.getDeclaredMethod(methodName, IBlockAccess.class, IBakedModel.class, IBlockState.class,
                    BlockPos.class, BufferBuilder.class, Boolean.TYPE, Long.TYPE);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchMethodException(owner.getName() + "." + methodName);
    }
}
