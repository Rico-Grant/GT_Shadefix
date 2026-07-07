package com.alber.gtshaderbridge.client;

import com.alber.gtshaderbridge.GTShaderBridge;
import com.alber.gtshaderbridge.GTShaderBridgeConfig;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.ResourceLocation;

public final class SemanticTransportProbe {
    private static final ThreadLocal<RouteScope> ACTIVE_ROUTE = new ThreadLocal<RouteScope>();
    private static final ThreadLocal<EntityDataTop> ACTIVE_WRITE = new ThreadLocal<EntityDataTop>();
    private static final Set<String> LOGGED_ROUTES = Collections.synchronizedSet(new HashSet<String>());
    private static final Set<String> LOGGED_TRANSPORTS = Collections.synchronizedSet(new HashSet<String>());
    private static volatile boolean entityWriteDisabled;
    private static volatile boolean loggedEntityWriteDisabled;

    private SemanticTransportProbe() {
    }

    public static AutoCloseable beginOcTesrRoute(String renderer, String method, ResourceLocation overlay) {
        if (!GTShaderBridgeConfig.enabled || !GTShaderBridgeConfig.probeOcTesrTransport) {
            return NoopCloseable.INSTANCE;
        }

        RouteScope scope = new RouteScope(ACTIVE_ROUTE.get(), renderer, method, overlay, SemanticIds.OC_LED_TESR);
        ACTIVE_ROUTE.set(scope);
        logRoute(scope);
        return scope;
    }

    public static void beginBufferEndVertex(BufferBuilder buffer) {
        RouteScope scope = ACTIVE_ROUTE.get();
        if (scope == null || !GTShaderBridgeConfig.enabled || !GTShaderBridgeConfig.probeOcTesrTransport) {
            return;
        }

        scope.vertices++;
        TransportSummary summary = TransportSummary.capture(buffer);
        EntityDataTop entityDataTop = null;
        String writeResult = "not_attempted";
        String packedEntityWords = "unwritten";

        if (!GTShaderBridgeConfig.writeEntityData) {
            writeResult = "disabled_by_config";
        } else if (entityWriteDisabled) {
            writeResult = "disabled_after_error";
        } else if (!summary.hasEntitySlotCandidate) {
            writeResult = "no_entity_slot";
        } else {
            try {
                entityDataTop = EntityDataTop.capture(buffer);
                entityDataTop.set(packEntity(scope.materialId));
                ACTIVE_WRITE.set(entityDataTop);
                packedEntityWords = entityDataTop.packedWords();
                writeResult = "success";
            } catch (Throwable e) {
                writeResult = "write_failed";
                disableEntityWrites(e);
            }
        }

        logTransport(scope, summary, writeResult, packedEntityWords);
    }

    public static void endBufferEndVertex() {
        EntityDataTop entityDataTop = ACTIVE_WRITE.get();
        if (entityDataTop == null) {
            return;
        }

        ACTIVE_WRITE.remove();
        entityDataTop.restoreQuietly();
    }

    private static void logRoute(RouteScope scope) {
        if (!GTShaderBridgeConfig.logSemanticTransport) {
            return;
        }

        String key = scope.renderer + "|" + scope.method + "|" + scope.overlayName + "|" + scope.materialId;
        if (LOGGED_ROUTES.add(key)) {
            GTShaderBridge.LOGGER.info("GTShaderBridge: semantic route detected, source=OC_TESR, renderer={}, method={}, overlay={}, materialId={}",
                scope.renderer, scope.method, scope.overlayName, scope.materialId);
        }
    }

    private static void logTransport(RouteScope scope, TransportSummary summary, String writeResult, String packedEntityWords) {
        if (!GTShaderBridgeConfig.logSemanticTransport) {
            return;
        }

        String key = scope.renderer + "|" + scope.method + "|" + scope.overlayName + "|" + scope.materialId + "|"
            + summary.formatSignature + "|" + writeResult;
        if (LOGGED_TRANSPORTS.add(key)) {
            GTShaderBridge.LOGGER.info("GTShaderBridge: semantic transport detected, source=OC_TESR, renderer={}, method={}, sprite={}, modelClass=TESR, chosenId={}({}), selectionReason=oc_tesr_precise_renderer_route, legacyFallback=false, verticesSeen={}, writeResult={}, packedEntityWords={}, {}",
                scope.renderer, scope.method, scope.overlayName, scope.materialId, SemanticIds.nameOf(scope.materialId),
                scope.vertices, writeResult, packedEntityWords, summary.formatSignature);
        }
    }

    private static long packEntity(int materialId) {
        return (long) (materialId & 0xFFFF);
    }

    private static void disableEntityWrites(Throwable cause) {
        entityWriteDisabled = true;
        if (!loggedEntityWriteDisabled) {
            loggedEntityWriteDisabled = true;
            GTShaderBridge.LOGGER.warn("[GTShaderBridge] Disabled TESR semantic transport writes after BufferBuilder entity-data failure", cause);
        }
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

    private static int intMethod(Object target, String[] names) {
        Object value = invoke(target, names);
        return value instanceof Integer ? ((Integer) value).intValue() : -1;
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

    private static int optifineInt(String fieldName, int fallback) {
        try {
            Class<?> sVertexFormat = Class.forName("net.optifine.shaders.SVertexFormat", false,
                SemanticTransportProbe.class.getClassLoader());
            Field field = sVertexFormat.getField(fieldName);
            return field.getInt(null);
        } catch (Throwable e) {
            return fallback;
        }
    }

    private static String optifineFormatSummary() {
        try {
            Class<?> sVertexFormat = Class.forName("net.optifine.shaders.SVertexFormat", false,
                SemanticTransportProbe.class.getClassLoader());
            return "SVertexFormat{vertexSizeBlock=" + staticInt(sVertexFormat, "vertexSizeBlock")
                + ",offsetMidTexCoord=" + staticInt(sVertexFormat, "offsetMidTexCoord")
                + ",offsetTangent=" + staticInt(sVertexFormat, "offsetTangent")
                + ",offsetEntity=" + staticInt(sVertexFormat, "offsetEntity")
                + "}";
        } catch (Throwable e) {
            return "unavailable";
        }
    }

    private static int staticInt(Class<?> owner, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = owner.getField(fieldName);
        return field.getInt(null);
    }

    private static int currentGlProgram() {
        try {
            Class<?> gl11 = Class.forName("org.lwjgl.opengl.GL11", false, SemanticTransportProbe.class.getClassLoader());
            Method method = gl11.getMethod("glGetInteger", Integer.TYPE);
            Object value = method.invoke(null, Integer.valueOf(35725));
            return value instanceof Integer ? ((Integer) value).intValue() : -1;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static String shaderProgramSummary() {
        try {
            Class<?> shaders = Class.forName("net.optifine.shaders.Shaders", false, SemanticTransportProbe.class.getClassLoader());
            return "Shaders{activeProgram=" + staticFieldText(shaders, "activeProgram")
                + ",activeProgramID=" + staticFieldText(shaders, "activeProgramID")
                + ",isRenderingWorld=" + staticFieldText(shaders, "isRenderingWorld")
                + "}";
        } catch (Throwable e) {
            return "Shaders{unavailable}";
        }
    }

    private static String staticFieldText(Class<?> owner, String fieldName) {
        try {
            Field field = owner.getField(fieldName);
            return String.valueOf(field.get(null));
        } catch (Throwable ignored) {
            return "unavailable";
        }
    }

    private static final class RouteScope implements AutoCloseable {
        private final RouteScope parent;
        private final String renderer;
        private final String method;
        private final String overlayName;
        private final int materialId;
        private int vertices;

        private RouteScope(RouteScope parent, String renderer, String method, ResourceLocation overlay, int materialId) {
            this.parent = parent;
            this.renderer = renderer;
            this.method = method;
            this.overlayName = overlay == null ? "null" : String.valueOf(overlay);
            this.materialId = materialId;
        }

        @Override
        public void close() {
            ACTIVE_ROUTE.set(parent);
        }
    }

    private static final class TransportSummary {
        private final String formatSignature;
        private final boolean hasEntitySlotCandidate;

        private TransportSummary(String formatSignature, boolean hasEntitySlotCandidate) {
            this.formatSignature = formatSignature;
            this.hasEntitySlotCandidate = hasEntitySlotCandidate;
        }

        private static TransportSummary capture(BufferBuilder buffer) {
            Object format = invoke(buffer, new String[] {"func_178973_g", "getVertexFormat"});
            int integerSize = intMethod(format, new String[] {"func_181719_f", "getIntegerSize"});
            int vertexSize = intMethod(format, new String[] {"func_177338_f", "getSize"});
            int elementCount = intMethod(format, new String[] {"func_177345_h", "getElementCount"});
            int optifineEntityOffset = optifineInt("offsetEntity", -1);
            boolean sVertexBuilder = hasField(buffer, "sVertexBuilder");
            boolean hasEntitySlot = sVertexBuilder && optifineEntityOffset >= 0 && integerSize >= optifineEntityOffset + 2;

            StringBuilder builder = new StringBuilder(384);
            builder.append("buffer=");
            builder.append(buffer == null ? "null" : buffer.getClass().getName());
            builder.append(", sVertexBuilder=");
            builder.append(sVertexBuilder);
            builder.append(", optifine=");
            builder.append(optifineFormatSummary());
            builder.append(", formatClass=");
            builder.append(format == null ? "null" : format.getClass().getName());
            builder.append(", sizeBytes=");
            builder.append(vertexSize);
            builder.append(", intSize=");
            builder.append(integerSize);
            builder.append(", elements=");
            builder.append(elementCount);
            builder.append(", hasEntitySlotCandidate=");
            builder.append(hasEntitySlot);
            builder.append(", currentGlProgram=");
            builder.append(currentGlProgram());
            builder.append(", shaderProgram=");
            builder.append(shaderProgramSummary());
            return new TransportSummary(builder.toString(), hasEntitySlot);
        }
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

        String packedWords() {
            long packed = entityData[index];
            return "index=" + index
                + ",low16=" + (packed & 0xFFFFL)
                + ",high16=" + ((packed >>> 16) & 0xFFFFL)
                + ",raw=" + packed;
        }

        void restoreQuietly() {
            try {
                entityData[index] = previous;
            } catch (Throwable ignored) {
                // The next vertex should not inherit the temporary probe id.
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

    private enum NoopCloseable implements AutoCloseable {
        INSTANCE;

        @Override
        public void close() {
        }
    }
}
