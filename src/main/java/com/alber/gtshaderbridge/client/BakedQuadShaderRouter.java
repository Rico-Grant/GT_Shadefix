package com.alber.gtshaderbridge.client;

import com.alber.gtshaderbridge.GTShaderBridge;
import com.alber.gtshaderbridge.GTShaderBridgeConfig;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public final class BakedQuadShaderRouter {
    private static final ThreadLocal<RenderContext> ACTIVE_CONTEXT = new ThreadLocal<RenderContext>();
    private static final ThreadLocal<Deque<AutoCloseable>> ADD_VERTEX_SCOPES = new ThreadLocal<Deque<AutoCloseable>>();
    private static final Set<String> LOGGED_ROUTES = Collections.synchronizedSet(new HashSet<String>());
    private static final Set<String> LOGGED_RENDER_MODELS = Collections.synchronizedSet(new HashSet<String>());

    private BakedQuadShaderRouter() {
    }

    public static void begin(Object renderer, IBlockAccess world, IBakedModel model, IBlockState state, BlockPos pos, long rand) {
        RenderContext parent = ACTIVE_CONTEXT.get();
        if (!GTShaderBridgeConfig.enabled || !GTShaderBridgeConfig.writeEntityData
            || !GTShaderBridgeConfig.routeAeOcBakedQuads || !isTargetState(state)) {
            ACTIVE_CONTEXT.set(new RenderContext(parent));
            return;
        }

        ACTIVE_CONTEXT.set(new RenderContext(parent, renderer, world, model, state, pos, rand));
    }

    public static void logRenderModelEntry(Object renderer, IBakedModel model, IBlockState state, BlockPos pos) {
        if (!GTShaderBridgeConfig.enabled || !GTShaderBridgeConfig.routeAeOcBakedQuads || !isTargetState(state)) {
            return;
        }

        String stateName = registryName(state);
        String modelClass = className(model);
        String key = stateName + "|" + modelClass;
        if (LOGGED_RENDER_MODELS.add(key)) {
            GTShaderBridge.LOGGER.info("GTShaderBridge: AE/OC BlockModelRenderer.renderModel detected, state={}, model={}, renderer={}, thread={}, pos={}",
                stateName, modelClass, className(renderer), Thread.currentThread().getName(), pos);
        }
    }

    public static void end() {
        RenderContext context = ACTIVE_CONTEXT.get();
        ACTIVE_CONTEXT.set(context == null ? null : context.parent);
    }

    public static void addVertexData(BufferBuilder buffer, int[] vertexData) {
        RenderContext context = ACTIVE_CONTEXT.get();
        if (context == null || !context.active) {
            addVertexDataRaw(buffer, vertexData);
            return;
        }

        BakedQuad quad = context.findQuad(vertexData);
        RouteDecision decision = routeMaterialId(context, quad);
        if (decision == null) {
            addVertexDataRaw(buffer, vertexData);
            return;
        }

        logRoute(decision, context, quad);
        GTShaderBridgeScope.addVertexDataWithEntityData(buffer, vertexData, decision.materialId);
    }

    public static void beginAddVertexData(BufferBuilder buffer, int[] vertexData) {
        AutoCloseable scope = null;
        try {
            RenderContext context = ACTIVE_CONTEXT.get();
            if (context != null && context.active) {
                BakedQuad quad = context.findQuad(vertexData);
                RouteDecision decision = routeMaterialId(context, quad);
                if (decision != null) {
                    logRoute(decision, context, quad);
                    scope = GTShaderBridgeScope.beginTemporaryEntityData(buffer, decision.materialId);
                }
            }
        } catch (Throwable e) {
            GTShaderBridge.LOGGER.warn("[GTShaderBridge] AE2/OpenComputers BakedQuad route failed; skipped this quad", e);
        }

        Deque<AutoCloseable> scopes = ADD_VERTEX_SCOPES.get();
        if (scopes == null) {
            scopes = new ArrayDeque<AutoCloseable>();
            ADD_VERTEX_SCOPES.set(scopes);
        }
        scopes.push(scope == null ? NoopCloseable.INSTANCE : scope);
    }

    public static void endAddVertexData() {
        Deque<AutoCloseable> scopes = ADD_VERTEX_SCOPES.get();
        if (scopes == null || scopes.isEmpty()) {
            return;
        }

        AutoCloseable scope = scopes.pop();
        if (scopes.isEmpty()) {
            ADD_VERTEX_SCOPES.remove();
        }

        try {
            scope.close();
        } catch (Throwable e) {
            GTShaderBridge.LOGGER.warn("[GTShaderBridge] Failed to restore temporary AE2/OpenComputers entity data", e);
        }
    }

    private static boolean isTargetState(IBlockState state) {
        if (state == null) {
            return false;
        }

        String namespace = registryNamespace(state);
        return "appliedenergistics2".equals(namespace) || "opencomputers".equals(namespace);
    }

    private static RouteDecision routeMaterialId(RenderContext context, BakedQuad quad) {
        if (quad == null) {
            return null;
        }

        String iconName = iconName(quad);

        if (iconName.startsWith("appliedenergistics2:")) {
            return routeAe2(iconName, quad, context);
        }

        if (iconName.startsWith("opencomputers:")) {
            if (shouldUseOcGenericEmission(iconName)) {
                return new RouteDecision(SemanticIds.OC_LED_BAKED, "oc_baked_high_saturation_pixel_mask", false);
            }
            return null;
        }

        return null;
    }

    private static RouteDecision routeAe2(String iconName, BakedQuad quad, RenderContext context) {
        if (iconName.endsWith("terminal_dark")) {
            return routeTerminalScreen(iconName, quad, "ae_terminal_dark");
        }
        if (iconName.endsWith("terminal_medium")) {
            return routeTerminalScreen(iconName, quad, "ae_terminal_medium");
        }
        if (iconName.endsWith("terminal_bright")) {
            return routeTerminalScreen(iconName, quad, "ae_terminal_bright");
        }
        if (iconName.endsWith("fluid_terminal/lights_dark")) {
            return routeTerminalScreen(iconName, quad, "ae_terminal_fluid_lights_dark");
        }
        if (iconName.endsWith("fluid_terminal/lights_medium")) {
            return routeTerminalScreen(iconName, quad, "ae_terminal_fluid_lights_medium");
        }
        if (iconName.endsWith("fluid_terminal/lights_bright")) {
            return routeTerminalScreen(iconName, quad, "ae_terminal_fluid_lights_bright");
        }
        if (iconName.endsWith("monitor_sides_status_off")) {
            return new RouteDecision(SemanticIds.AE_TERMINAL_DARK_LEGACY, "ae_terminal_status_off_no_lightmap", false);
        }
        if (iconName.endsWith("monitor_sides_status_on")) {
            return new RouteDecision(SemanticIds.AE_TERMINAL_MEDIUM_LEGACY, "ae_terminal_status_powered_no_channel", false);
        }
        if (iconName.endsWith("monitor_sides_status_has_channel")) {
            return new RouteDecision(SemanticIds.AE_TERMINAL_BRIGHT_LEGACY, "ae_terminal_status_powered_has_channel", false);
        }
        if (iconName.endsWith("controller_lights") || iconName.endsWith("controller_column_lights")
            || iconName.endsWith("controller_conflict") || iconName.endsWith("controller_column_conflict")) {
            return new RouteDecision(SemanticIds.AE_CONTROLLER_LIGHT, "ae_controller_precise_light_texture", false);
        }
        if (iconName.endsWith("drive_cell_states_emissive") || iconName.contains("drive_cell_states_emissive")) {
            return new RouteDecision(SemanticIds.AE_DRIVE_LED, "ae_drive_cell_states_emissive", false);
        }
        if (iconName.endsWith("drive_cell_states") || iconName.contains("drive_cell_states")) {
            return new RouteDecision(SemanticIds.AE_BODY, "ae_drive_cell_states_base_body", false);
        }

        boolean dense = iconName.contains("parts/cable/dense_smart/channels_");
        boolean smart = iconName.contains("parts/cable/smart/channels_");
        if (dense || smart) {
            int channelsIndex = parseChannelTextureIndex(iconName);
            if (channelsIndex == 0 || channelsIndex == 10) {
                return new RouteDecision(SemanticIds.AE_CABLE_IDLE, "ae_cable_channels_00_or_10_idle", false);
            }
            if (channelsIndex >= 1 && channelsIndex <= 4) {
                return new RouteDecision(SemanticIds.AE_CABLE_LOW_CHANNEL, "ae_cable_channels_01_04_low", false);
            }
            if (channelsIndex >= 11 && channelsIndex <= 14) {
                return new RouteDecision(SemanticIds.AE_CABLE_HIGH_CHANNEL, "ae_cable_channels_11_14_high", false);
            }
        }

        if (shouldUseAeGenericEmission(iconName)) {
            return new RouteDecision(SemanticIds.OC_LED_BAKED, "ae_generic_high_saturation_pixel_mask", false);
        }

        return null;
    }

    private static boolean shouldUseAeGenericEmission(String iconName) {
        return !iconName.contains("parts/cable/")
            && !iconName.contains("terminal")
            && !iconName.contains("monitor_sides_status")
            && !iconName.contains("drive_cell")
            && !iconName.contains("controller");
    }

    private static boolean shouldUseOcGenericEmission(String iconName) {
        return iconName.contains("blocks/printer_")
            || iconName.contains("blocks/charger_")
            || iconName.contains("blocks/redstone_")
            || iconName.contains("blocks/motionsensor_")
            || iconName.contains("blocks/overlay/charger_")
            || iconName.contains("blocks/overlay/disassembler_")
            || iconName.contains("blocks/overlay/geolyzer_")
            || iconName.contains("blocks/overlay/powerdistributor_")
            || iconName.contains("blocks/overlay/adapter_on")
            || iconName.contains("blocks/overlay/switch_side_on")
            || iconName.contains("blocks/overlay/transposer_on");
    }

    private static RouteDecision routeTerminalScreen(String iconName, BakedQuad quad, String reasonPrefix) {
        boolean poweredModel = hasLightmapElement(quad);
        int id = poweredModel ? SemanticIds.AE_TERMINAL_BRIGHT_LEGACY : SemanticIds.AE_TERMINAL_DARK_LEGACY;
        return new RouteDecision(id, reasonPrefix + (poweredModel ? "_uvl_powered" : "_off_no_uvl"), false);
    }

    private static int parseChannelTextureIndex(String iconName) {
        int index = iconName.lastIndexOf("channels_");
        if (index < 0) {
            return -1;
        }

        int start = index + "channels_".length();
        int end = Math.min(start + 2, iconName.length());
        try {
            return Integer.parseInt(iconName.substring(start, end));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static void logRoute(RouteDecision decision, RenderContext context, BakedQuad quad) {
        if (!GTShaderBridgeConfig.logAeOcBakedQuadRoutes) {
            return;
        }

        String iconName = iconName(quad);
        String stateName = registryName(context.state);
        String rendererName = className(context.renderer);
        String modelName = className(context.model);
        String key = stateName + "|" + rendererName + "|" + modelName + "|" + decision.materialId + "|" + iconName + "|" + decision.reason;
        if (LOGGED_ROUTES.add(key)) {
            GTShaderBridge.LOGGER.info("GTShaderBridge: semantic route detected, source=BakedQuad, state={}, renderer={}, modelClass={}, sprite={}, chosenId={}({}), selectionReason={}, legacyFallback={}",
                stateName, rendererName, modelName, iconName, decision.materialId, SemanticIds.nameOf(decision.materialId),
                decision.reason, decision.legacyFallback);
        }
    }

    private static String registryName(IBlockState state) {
        Object block = invoke(state, new String[] {"func_177230_c", "getBlock"});
        Object registryName = invoke(block, new String[] {"getRegistryName"});
        return registryName == null ? "" : String.valueOf(registryName);
    }

    private static String registryNamespace(IBlockState state) {
        Object block = invoke(state, new String[] {"func_177230_c", "getBlock"});
        Object registryName = invoke(block, new String[] {"getRegistryName"});
        Object namespace = invoke(registryName, new String[] {"func_110624_b", "getNamespace"});
        return namespace instanceof String ? (String) namespace : "";
    }

    private static String className(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    private static String iconName(BakedQuad quad) {
        Object sprite = invoke(quad, new String[] {"func_187508_a", "getSprite"});
        Object iconName = invoke(sprite, new String[] {"func_94215_i", "getIconName"});
        return iconName instanceof String ? (String) iconName : "";
    }

    private static int[] vertexData(BakedQuad quad) {
        Object value = invoke(quad, new String[] {"func_178209_a", "getVertexData"});
        return value instanceof int[] ? (int[]) value : null;
    }

    private static boolean hasLightmapElement(BakedQuad quad) {
        Object value = invoke(quad, new String[] {"func_178210_d", "getFormat"});
        if (!(value instanceof VertexFormat)) {
            return false;
        }

        VertexFormat format = (VertexFormat) value;
        int elements = intMethod(format, new String[] {"func_177345_h", "getElementCount"}, -1);
        if (elements <= 0) {
            return false;
        }

        for (int i = 0; i < elements; i++) {
            Object element = invoke(format, new String[] {"func_177348_c", "getElement"},
                new Class<?>[] {Integer.TYPE}, new Object[] {Integer.valueOf(i)});
            if (!(element instanceof VertexFormatElement)) {
                continue;
            }

            Object usage = invoke(element, new String[] {"func_177375_c", "getUsage"});
            int index = intMethod(element, new String[] {"func_177369_e", "getIndex"}, -1);
            if ("UV".equals(String.valueOf(usage)) && index == 1) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<BakedQuad> modelQuads(IBakedModel model, IBlockState state, EnumFacing face, long rand) {
        Object value = invoke(model, new String[] {"func_188616_a", "getQuads"},
            new Class<?>[] {IBlockState.class, EnumFacing.class, Long.TYPE},
            new Object[] {state, face, Long.valueOf(rand)});
        return value instanceof List<?> ? (List<BakedQuad>) value : Collections.<BakedQuad>emptyList();
    }

    private static void addVertexDataRaw(BufferBuilder buffer, int[] vertexData) {
        invoke(buffer, new String[] {"func_178981_a", "addVertexData"},
            new Class<?>[] {int[].class},
            new Object[] {vertexData});
    }

    private static Object invoke(Object target, String[] names) {
        return invoke(target, names, new Class<?>[0], new Object[0]);
    }

    private static int intMethod(Object target, String[] names, int defaultValue) {
        Object value = invoke(target, names);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
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

    private static final class RenderContext {
        private final RenderContext parent;
        private final boolean active;
        private final Object renderer;
        private final IBakedModel model;
        private final IBlockState state;
        private final long rand;
        private IdentityHashMap<int[], BakedQuad> quadsByVertexData;
        private List<BakedQuad> quads;

        private RenderContext(RenderContext parent) {
            this.parent = parent;
            this.active = false;
            this.renderer = null;
            this.model = null;
            this.state = null;
            this.rand = 0L;
        }

        private RenderContext(RenderContext parent, Object renderer, IBlockAccess world, IBakedModel model, IBlockState state, BlockPos pos, long rand) {
            this.parent = parent;
            this.active = world != null && pos != null && model != null && state != null;
            this.renderer = renderer;
            this.model = model;
            this.state = state;
            this.rand = rand;
        }

        private BakedQuad findQuad(int[] vertexData) {
            ensureQuadsLoaded();
            BakedQuad identityMatch = quadsByVertexData.get(vertexData);
            if (identityMatch != null) {
                return identityMatch;
            }

            for (int i = 0; i < quads.size(); i++) {
                BakedQuad quad = quads.get(i);
                int[] candidate = vertexData(quad);
                if (candidate != vertexData && sameVertexData(candidate, vertexData)) {
                    return quad;
                }
            }
            return null;
        }

        private void ensureQuadsLoaded() {
            if (quads != null) {
                return;
            }

            quads = new ArrayList<BakedQuad>();
            quadsByVertexData = new IdentityHashMap<int[], BakedQuad>();
            try {
                EnumFacing[] faces = EnumFacing.values();
                for (int i = 0; i < faces.length; i++) {
                    addQuads(modelQuads(model, state, faces[i], rand));
                }
                addQuads(modelQuads(model, state, null, rand));
            } catch (Throwable e) {
                GTShaderBridge.LOGGER.warn("[GTShaderBridge] Failed to inspect AE2/OpenComputers baked quads; routing skipped for this model", e);
                quads.clear();
                quadsByVertexData.clear();
            }
        }

        private void addQuads(List<BakedQuad> source) {
            if (source == null || source.isEmpty()) {
                return;
            }

            for (int i = 0; i < source.size(); i++) {
                BakedQuad quad = source.get(i);
                if (quad == null) {
                    continue;
                }
                quads.add(quad);
                int[] vertexData = vertexData(quad);
                if (vertexData != null) {
                    quadsByVertexData.put(vertexData, quad);
                }
            }
        }

        private static boolean sameVertexData(int[] left, int[] right) {
            if (left == right) {
                return true;
            }
            if (left == null || right == null || left.length != right.length) {
                return false;
            }
            for (int i = 0; i < left.length; i++) {
                if (left[i] != right[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class RouteDecision {
        private final int materialId;
        private final String reason;
        private final boolean legacyFallback;

        private RouteDecision(int materialId, String reason, boolean legacyFallback) {
            this.materialId = materialId;
            this.reason = reason;
            this.legacyFallback = legacyFallback;
        }
    }

    private enum NoopCloseable implements AutoCloseable {
        INSTANCE;

        @Override
        public void close() {
        }
    }
}
