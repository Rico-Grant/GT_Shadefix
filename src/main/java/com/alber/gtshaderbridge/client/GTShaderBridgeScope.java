package com.alber.gtshaderbridge.client;

import codechicken.lib.render.CCRenderState;
import com.alber.gtshaderbridge.GTShaderBridge;
import com.alber.gtshaderbridge.GTShaderBridgeConfig;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public final class GTShaderBridgeScope implements AutoCloseable {
    private static final ThreadLocal<GTShaderBridgeScope> ACTIVE_SCOPE = new ThreadLocal<GTShaderBridgeScope>();
    private static final Set<Long> LOGGED_CHUNKS = Collections.synchronizedSet(new HashSet<Long>());
    private static final Set<String> LOGGED_FORMATS = Collections.synchronizedSet(new HashSet<String>());
    private static final Object UNKNOWN = new Object();
    private static volatile boolean entityWriteDisabled;
    private static volatile boolean loggedEntityWrite;
    private static volatile boolean loggedEntityWriteDisabled;
    private static volatile boolean loggedBakedQuadEntityWriteSkipped;

    private final GTShaderBridgeScope parent;
    private final int materialGroup;
    private final long chunkKey;
    private int vertices;

    private GTShaderBridgeScope(int materialGroup, long chunkKey) {
        this.parent = ACTIVE_SCOPE.get();
        this.materialGroup = materialGroup;
        this.chunkKey = chunkKey;
        ACTIVE_SCOPE.set(this);
    }

    public static GTShaderBridgeScope begin(IBlockAccess world, BlockPos pos, int materialGroup) {
        return new GTShaderBridgeScope(materialGroup, chunkKey(world, pos));
    }

    public static void recordCclVertex(CCRenderState renderState) {
        GTShaderBridgeScope scope = ACTIVE_SCOPE.get();
        if (scope != null) {
            scope.vertices++;
            scope.logVertexFormat(renderState);
        }
    }

    public static Integer activeMaterialGroup() {
        GTShaderBridgeScope scope = ACTIVE_SCOPE.get();
        return scope == null ? null : Integer.valueOf(scope.materialGroup);
    }

    public static void endCclVertex(CCRenderState renderState, BufferBuilder buffer) {
        GTShaderBridgeScope scope = ACTIVE_SCOPE.get();
        if (scope == null || !GTShaderBridgeConfig.enabled || !GTShaderBridgeConfig.writeEntityData || entityWriteDisabled) {
            invokeEndVertex(buffer);
            return;
        }

        if (!isShaderBlockFormat(renderState, buffer)) {
            invokeEndVertex(buffer);
            return;
        }

        EntityDataTop entityDataTop;
        try {
            entityDataTop = EntityDataTop.capture(buffer);
            entityDataTop.set(packEntity(scope.materialGroup));
        } catch (Throwable e) {
            disableEntityWrites(e);
            invokeEndVertex(buffer);
            return;
        }

        if (!loggedEntityWrite) {
            loggedEntityWrite = true;
            GTShaderBridge.LOGGER.info("GTShaderBridge: writing OptiFine entity data on scoped CCL vertices, materialId={}", scope.materialGroup);
        }

        try {
            invokeEndVertex(buffer);
        } finally {
            if (entityDataTop != null) {
                entityDataTop.restoreQuietly();
            }
        }
    }

    public static void addVertexDataWithEntityData(BufferBuilder buffer, int[] vertexData, int materialId) {
        if (buffer == null || vertexData == null) {
            return;
        }

        if (!GTShaderBridgeConfig.enabled || !GTShaderBridgeConfig.writeEntityData || entityWriteDisabled) {
            invokeAddVertexData(buffer, vertexData);
            return;
        }

        if (!isShaderBlockFormat(null, buffer)) {
            if (!loggedBakedQuadEntityWriteSkipped) {
                loggedBakedQuadEntityWriteSkipped = true;
                GTShaderBridge.LOGGER.warn("[GTShaderBridge] BakedQuad entity-data write skipped: current BufferBuilder is not OptiFine shader block format");
            }
            invokeAddVertexData(buffer, vertexData);
            return;
        }

        EntityDataTop entityDataTop;
        int[] routedVertexData = vertexData.clone();
        try {
            entityDataTop = EntityDataTop.capture(buffer);
            entityDataTop.set(packEntity(materialId));
        } catch (Throwable e) {
            disableEntityWrites(e);
            invokeAddVertexData(buffer, vertexData);
            return;
        }

        if (!loggedEntityWrite) {
            loggedEntityWrite = true;
            GTShaderBridge.LOGGER.info("GTShaderBridge: writing OptiFine entity data on scoped BufferBuilder vertices, materialId={}", materialId);
        }

        try {
            invokeAddVertexData(buffer, routedVertexData);
        } finally {
            entityDataTop.restoreQuietly();
        }
    }

    public static AutoCloseable beginTemporaryEntityData(BufferBuilder buffer, int materialId) {
        if (buffer == null) {
            return null;
        }

        if (!GTShaderBridgeConfig.enabled || !GTShaderBridgeConfig.writeEntityData || entityWriteDisabled) {
            return null;
        }

        if (!isShaderBlockFormat(null, buffer)) {
            if (!loggedBakedQuadEntityWriteSkipped) {
                loggedBakedQuadEntityWriteSkipped = true;
                GTShaderBridge.LOGGER.warn("[GTShaderBridge] BakedQuad entity-data write skipped: current BufferBuilder is not OptiFine shader block format");
            }
            return null;
        }

        final EntityDataTop entityDataTop;
        try {
            entityDataTop = EntityDataTop.capture(buffer);
            entityDataTop.set(packEntity(materialId));
        } catch (Throwable e) {
            disableEntityWrites(e);
            return null;
        }

        if (!loggedEntityWrite) {
            loggedEntityWrite = true;
            GTShaderBridge.LOGGER.info("GTShaderBridge: writing OptiFine entity data on scoped BufferBuilder vertices, materialId={}", materialId);
        }

        return new AutoCloseable() {
            @Override
            public void close() {
                entityDataTop.restoreQuietly();
            }
        };
    }

    @Override
    public void close() {
        ACTIVE_SCOPE.set(parent);
        if (!GTShaderBridgeConfig.logVertexWriter || vertices <= 0) {
            return;
        }

        if (LOGGED_CHUNKS.add(Long.valueOf(chunkKey))) {
            GTShaderBridge.LOGGER.info("GTShaderBridge: scoped CCL vertex writer detected, materialId={}, vertices={}", materialGroup, vertices);
        }
    }

    private static long chunkKey(IBlockAccess world, BlockPos pos) {
        long x = (long) (coordinate(pos, "func_177958_n", "getX") >> 4) & 0xFFFFFFFFL;
        long z = (long) (coordinate(pos, "func_177952_p", "getZ") >> 4) & 0xFFFFFFFFL;
        return (x << 32) | z;
    }

    private static int coordinate(BlockPos pos, String runtimeName, String compileName) {
        try {
            Method method;
            try {
                method = pos.getClass().getMethod(runtimeName);
            } catch (NoSuchMethodException ignored) {
                method = pos.getClass().getMethod(compileName);
            }
            Object value = method.invoke(pos);
            return ((Integer) value).intValue();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private void logVertexFormat(CCRenderState renderState) {
        if (!GTShaderBridgeConfig.logVertexFormat || renderState == null) {
            return;
        }

        Object buffer = renderState.r;
        Object format = renderState.fmt;
        if (format == null && buffer != null) {
            format = invoke(buffer, new String[] {"func_178973_g", "getVertexFormat"});
        }

        String signature = vertexFormatSignature(buffer, format);
        if (LOGGED_FORMATS.add(signature)) {
            GTShaderBridge.LOGGER.info("GTShaderBridge: scoped CCL vertex format detected: {}", signature);
        }
    }

    private static String vertexFormatSignature(Object buffer, Object format) {
        StringBuilder builder = new StringBuilder(384);
        builder.append("buffer=");
        builder.append(buffer == null ? "null" : buffer.getClass().getName());
        builder.append(", sVertexBuilder=");
        builder.append(hasField(buffer, "sVertexBuilder"));
        builder.append(", activeMaterialGroup=");
        builder.append(activeMaterialGroup());
        builder.append(", optifine=");
        builder.append(optifineFormatSummary());

        if (format == null) {
            builder.append(", format=null");
            return builder.toString();
        }

        int integerSize = intMethod(format, new String[] {"func_181719_f", "getIntegerSize"});
        int vertexSize = intMethod(format, new String[] {"func_177338_f", "getSize"});
        int elementCount = intMethod(format, new String[] {"func_177345_h", "getElementCount"});
        builder.append(", formatClass=");
        builder.append(format.getClass().getName());
        builder.append(", sizeBytes=");
        builder.append(vertexSize);
        builder.append(", intSize=");
        builder.append(integerSize);
        builder.append(", elements=");
        builder.append(elementCount);

        int optifineEntityOffset = optifineInt("offsetEntity", -1);
        builder.append(", hasEntitySlotCandidate=");
        builder.append(integerSize >= optifineEntityOffset + 2 && optifineEntityOffset >= 0);

        for (int i = 0; i < elementCount; i++) {
            Object element = invoke(format, new String[] {"func_177348_c", "getElement"}, Integer.TYPE, Integer.valueOf(i));
            int offset = intMethod(format, new String[] {"func_181720_d", "getOffset"}, Integer.TYPE, Integer.valueOf(i));
            builder.append(" | #");
            builder.append(i);
            builder.append("@");
            builder.append(offset);
            builder.append(":");
            builder.append(vertexElementSummary(element));
        }

        return builder.toString();
    }

    private static String vertexElementSummary(Object element) {
        if (element == null || element == UNKNOWN) {
            return "unknown";
        }

        Object usage = invoke(element, new String[] {"func_177375_c", "getUsage"});
        Object type = invoke(element, new String[] {"func_177367_b", "getType"});
        int index = intMethod(element, new String[] {"func_177369_e", "getIndex"});
        int count = intMethod(element, new String[] {"func_177370_d", "getElementCount"});
        int size = intMethod(element, new String[] {"func_177368_f", "getSize"});
        return "usage=" + enumText(usage)
            + ",type=" + enumText(type)
            + ",index=" + index
            + ",count=" + count
            + ",size=" + size;
    }

    private static String optifineFormatSummary() {
        try {
            Class<?> sVertexFormat = Class.forName("net.optifine.shaders.SVertexFormat", false,
                GTShaderBridgeScope.class.getClassLoader());
            return "SVertexFormat{vertexSizeBlock=" + staticInt(sVertexFormat, "vertexSizeBlock")
                + ",offsetMidTexCoord=" + staticInt(sVertexFormat, "offsetMidTexCoord")
                + ",offsetTangent=" + staticInt(sVertexFormat, "offsetTangent")
                + ",offsetEntity=" + staticInt(sVertexFormat, "offsetEntity")
                + "}";
        } catch (Throwable e) {
            return "unavailable";
        }
    }

    private static int optifineInt(String fieldName, int fallback) {
        try {
            Class<?> sVertexFormat = Class.forName("net.optifine.shaders.SVertexFormat", false,
                GTShaderBridgeScope.class.getClassLoader());
            Field field = sVertexFormat.getField(fieldName);
            return field.getInt(null);
        } catch (Throwable e) {
            return fallback;
        }
    }

    private static boolean isShaderBlockFormat(CCRenderState renderState, BufferBuilder buffer) {
        Object format = renderState == null ? null : renderState.fmt;
        if (format == null && buffer != null) {
            format = invoke(buffer, new String[] {"func_178973_g", "getVertexFormat"});
        }

        int optifineVertexSize = optifineInt("vertexSizeBlock", -1);
        int optifineEntityOffset = optifineInt("offsetEntity", -1);
        int integerSize = intMethod(format, new String[] {"func_181719_f", "getIntegerSize"});
        return buffer != null
            && hasField(buffer, "sVertexBuilder")
            && optifineVertexSize == 14
            && optifineEntityOffset == 12
            && integerSize == optifineVertexSize
            && integerSize >= optifineEntityOffset + 2;
    }

    private static long packEntity(int materialId) {
        return (long) (materialId & 0xFFFF);
    }

    private static void invokeEndVertex(BufferBuilder buffer) {
        if (buffer == null) {
            return;
        }

        try {
            Method method;
            try {
                method = buffer.getClass().getMethod("func_181675_d");
            } catch (NoSuchMethodException ignored) {
                method = buffer.getClass().getMethod("endVertex");
            }
            method.invoke(buffer);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to invoke BufferBuilder.endVertex", e);
        }
    }

    private static void invokeAddVertexData(BufferBuilder buffer, int[] vertexData) {
        if (buffer == null || vertexData == null) {
            return;
        }

        Object result = invoke(buffer, new String[] {"func_178981_a", "addVertexData"}, int[].class, vertexData);
        if (result == UNKNOWN) {
            throw new RuntimeException("Failed to invoke BufferBuilder.addVertexData");
        }
    }

    private static void disableEntityWrites(Throwable cause) {
        entityWriteDisabled = true;
        if (!loggedEntityWriteDisabled) {
            loggedEntityWriteDisabled = true;
            GTShaderBridge.LOGGER.warn("[GTShaderBridge] Disabled: failed to write BufferBuilder-bound OptiFine entity data", cause);
        }
    }

    private static int staticInt(Class<?> owner, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = owner.getField(fieldName);
        return field.getInt(null);
    }

    private static boolean hasField(Object instance, String fieldName) {
        if (instance == null) {
            return false;
        }

        Class<?> type = instance.getClass();
        while (type != null) {
            try {
                type.getDeclaredField(fieldName);
                return true;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (Throwable ignored) {
                return false;
            }
        }
        return false;
    }

    private static int intMethod(Object target, String[] names) {
        Object value = invoke(target, names);
        return value instanceof Integer ? ((Integer) value).intValue() : -1;
    }

    private static int intMethod(Object target, String[] names, Class<?> parameterType, Object parameter) {
        Object value = invoke(target, names, parameterType, parameter);
        return value instanceof Integer ? ((Integer) value).intValue() : -1;
    }

    private static Object invoke(Object target, String[] names) {
        if (target == null) {
            return UNKNOWN;
        }

        for (int i = 0; i < names.length; i++) {
            try {
                Method method = target.getClass().getMethod(names[i]);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                // Try the next SRG/deobfuscated name.
            } catch (Throwable ignored) {
                return UNKNOWN;
            }
        }
        return UNKNOWN;
    }

    private static Object invoke(Object target, String[] names, Class<?> parameterType, Object parameter) {
        if (target == null) {
            return UNKNOWN;
        }

        for (int i = 0; i < names.length; i++) {
            try {
                Method method = target.getClass().getMethod(names[i], parameterType);
                return method.invoke(target, parameter);
            } catch (NoSuchMethodException ignored) {
                // Try the next SRG/deobfuscated name.
            } catch (Throwable ignored) {
                return UNKNOWN;
            }
        }
        return UNKNOWN;
    }

    private static String enumText(Object value) {
        if (value == null) {
            return "null";
        }
        if (value == UNKNOWN) {
            return "unknown";
        }
        if (value instanceof Enum<?>) {
            return ((Enum<?>) value).name();
        }
        return String.valueOf(value);
    }

    private static final class EntityDataTop {
        private final long[] entityData;
        private final int index;
        private final long previous;

        private EntityDataTop(long[] entityData, int index) {
            this.entityData = entityData;
            this.index = index;
            this.previous = entityData[index];
        }

        static EntityDataTop capture(Object buffer) throws ReflectiveOperationException {
            Object sVertexBuilder = readField(buffer, "sVertexBuilder");
            long[] entityData = (long[]) readField(sVertexBuilder, "entityData");
            int index = ((Integer) readField(sVertexBuilder, "entityDataIndex")).intValue();
            if (index < 0 || index >= entityData.length) {
                throw new IllegalStateException("OptiFine entityDataIndex out of bounds: " + index);
            }
            return new EntityDataTop(entityData, index);
        }

        void set(long packedEntity) {
            entityData[index] = packedEntity;
        }

        void restoreQuietly() {
            try {
                entityData[index] = previous;
            } catch (Throwable ignored) {
                // The next vertex should not inherit our temporary material id.
            }
        }

        private static Object readField(Object instance, String name) throws ReflectiveOperationException {
            if (instance == null) {
                throw new NoSuchFieldException(name);
            }

            Class<?> type = instance.getClass();
            while (type != null) {
                try {
                    Field field = type.getDeclaredField(name);
                    field.setAccessible(true);
                    return field.get(instance);
                } catch (NoSuchFieldException ignored) {
                    type = type.getSuperclass();
                }
            }
            throw new NoSuchFieldException(name);
        }
    }
}
