package com.alber.gtshaderbridge.client;

import com.alber.gtshaderbridge.GTShaderBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;

public final class OcTesrShaderFormatPoc {
    private static final ResourceLocation BLOCK_ATLAS = new ResourceLocation("textures/atlas/blocks.png");
    private static final Set<String> LOGGED = Collections.synchronizedSet(new HashSet<String>());
    private static volatile boolean loggedFailure;

    private OcTesrShaderFormatPoc() {
    }

    public static boolean renderCaseFrontOverlay(ResourceLocation overlay) {
        try {
            Object tessellator = invokeStatic("net.minecraft.client.renderer.Tessellator",
                new String[] {"func_178181_a", "getInstance"});
            Object buffer = invoke(tessellator, new String[] {"func_178180_c", "getBuffer"});
            Object minecraft = invokeStatic("net.minecraft.client.Minecraft", new String[] {"func_71410_x", "getMinecraft"});
            Object textureManager = invoke(minecraft, new String[] {"func_110434_K", "getTextureManager"});
            invoke(textureManager, new String[] {"func_110577_a", "bindTexture"},
                new Class<?>[] {ResourceLocation.class}, new Object[] {BLOCK_ATLAS});

            Object textureMap = invoke(minecraft, new String[] {"func_175602_ab", "getTextureMapBlocks"});
            Object sprite = invoke(textureMap, new String[] {"func_110572_b", "getAtlasSprite"},
                new Class<?>[] {String.class}, new Object[] {String.valueOf(overlay)});
            VertexFormat format = shaderTexturedFormat();
            if (tessellator == null || buffer == null || sprite == null || format == null) {
                logFailure(null);
                return false;
            }

            invoke(buffer, new String[] {"func_181668_a", "begin"},
                new Class<?>[] {Integer.TYPE, VertexFormat.class}, new Object[] {Integer.valueOf(7), format});

            double minU = floatValue(sprite, new String[] {"func_94209_e", "getMinU"});
            double maxU = floatValue(sprite, new String[] {"func_94212_f", "getMaxU"});
            double minV = floatValue(sprite, new String[] {"func_94206_g", "getMinV"});
            double maxV = floatValue(sprite, new String[] {"func_94210_h", "getMaxV"});

            addVertex((BufferBuilder) buffer, 0.0D, 1.0D, 0.0D, minU, maxV);
            addVertex((BufferBuilder) buffer, 1.0D, 1.0D, 0.0D, maxU, maxV);
            addVertex((BufferBuilder) buffer, 1.0D, 0.0D, 0.0D, maxU, minV);
            addVertex((BufferBuilder) buffer, 0.0D, 0.0D, 0.0D, minU, minV);

            invoke(tessellator, new String[] {"func_78381_a", "draw"});
            logSuccess(overlay, format);
            return true;
        } catch (Throwable e) {
            logFailure(e);
            return false;
        }
    }

    private static void addVertex(BufferBuilder buffer, double x, double y, double z, double u, double v) {
        invoke(buffer, new String[] {"func_181662_b", "pos"},
            new Class<?>[] {Double.TYPE, Double.TYPE, Double.TYPE}, new Object[] {Double.valueOf(x), Double.valueOf(y), Double.valueOf(z)});
        invoke(buffer, new String[] {"func_181669_b", "color"},
            new Class<?>[] {Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE},
            new Object[] {Integer.valueOf(255), Integer.valueOf(255), Integer.valueOf(255), Integer.valueOf(255)});
        invoke(buffer, new String[] {"func_187315_a", "tex"},
            new Class<?>[] {Double.TYPE, Double.TYPE}, new Object[] {Double.valueOf(u), Double.valueOf(v)});
        invoke(buffer, new String[] {"func_187314_a", "lightmap"},
            new Class<?>[] {Integer.TYPE, Integer.TYPE}, new Object[] {Integer.valueOf(240), Integer.valueOf(240)});
        invoke(buffer, new String[] {"func_181663_c", "normal"},
            new Class<?>[] {Float.TYPE, Float.TYPE, Float.TYPE}, new Object[] {Float.valueOf(0.0F), Float.valueOf(0.0F), Float.valueOf(1.0F)});
        invoke(buffer, new String[] {"func_181675_d", "endVertex"});
    }

    private static VertexFormat shaderTexturedFormat() throws ReflectiveOperationException {
        Class<?> owner = Class.forName("net.optifine.shaders.SVertexFormat", false,
            OcTesrShaderFormatPoc.class.getClassLoader());
        Field field = owner.getField("defVertexFormatTextured");
        return (VertexFormat) field.get(null);
    }

    private static double floatValue(Object target, String[] names) {
        Object value = invoke(target, names);
        return value instanceof Float ? ((Float) value).doubleValue() : 0.0D;
    }

    private static void logSuccess(ResourceLocation overlay, VertexFormat format) {
        String key = String.valueOf(overlay) + "|" + format;
        if (LOGGED.add(key)) {
            GTShaderBridge.LOGGER.info("GTShaderBridge: OC Case TESR shader-format POC submitted, overlay={}, chosenId={}({}), vertexStrideBytes={}, vertexIntSize={}",
                overlay, SemanticIds.OC_LED_TESR, SemanticIds.nameOf(SemanticIds.OC_LED_TESR),
                intMethod(format, new String[] {"func_177338_f", "getSize"}),
                intMethod(format, new String[] {"func_181719_f", "getIntegerSize"}));
        }
    }

    private static void logFailure(Throwable cause) {
        if (!loggedFailure) {
            loggedFailure = true;
            GTShaderBridge.LOGGER.warn("[GTShaderBridge] OC Case TESR shader-format POC failed; falling back to original POSITION_TEX draw", cause);
        }
    }

    private static Object invokeStatic(String className, String[] names) {
        try {
            Class<?> owner = Class.forName(className, false, OcTesrShaderFormatPoc.class.getClassLoader());
            for (int i = 0; i < names.length; i++) {
                try {
                    Method method = owner.getMethod(names[i]);
                    method.setAccessible(true);
                    return method.invoke(null);
                } catch (NoSuchMethodException ignored) {
                    // Try the next SRG/deobfuscated name.
                }
            }
        } catch (Throwable ignored) {
            return null;
        }
        return null;
    }

    private static Object invoke(Object target, String[] names) {
        return invoke(target, names, new Class<?>[0], new Object[0]);
    }

    private static Object invoke(Object target, String[] names, Class<?>[] parameterTypes, Object[] arguments) {
        if (target == null) {
            return null;
        }

        Class<?> type = target.getClass();
        while (type != null) {
            for (int i = 0; i < names.length; i++) {
                try {
                    Method method = type.getMethod(names[i], parameterTypes);
                    method.setAccessible(true);
                    return method.invoke(target, arguments);
                } catch (NoSuchMethodException ignored) {
                    // Try declared/private methods or the next runtime/deobfuscated name.
                } catch (Throwable ignored) {
                    return null;
                }

                try {
                    Method method = type.getDeclaredMethod(names[i], parameterTypes);
                    method.setAccessible(true);
                    return method.invoke(target, arguments);
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

    private static int intMethod(Object target, String[] names) {
        Object value = invoke(target, names);
        return value instanceof Integer ? ((Integer) value).intValue() : -1;
    }
}
