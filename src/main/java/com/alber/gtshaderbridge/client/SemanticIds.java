package com.alber.gtshaderbridge.client;

public final class SemanticIds {
    public static final int AE_BODY = 12100;
    public static final int AE_UVL_LIGHT = 12101;
    public static final int AE_CONTROLLER_LIGHT = 12102;
    public static final int AE_CABLE_IDLE = 12103;
    public static final int AE_CABLE_LOW_CHANNEL = 12104;
    public static final int AE_CABLE_HIGH_CHANNEL = 12105;
    public static final int AE_DRIVE_LED = 12106;
    public static final int AE_CRAFTING_LIGHT = 12107;
    public static final int AE_MONITOR_LIGHT = 12108;
    public static final int AE_TERMINAL_TRACE = 12109;
    public static final int OC_BODY = 12110;
    public static final int OC_LED_BAKED = 12111;
    public static final int OC_LED_TESR = 12112;

    private SemanticIds() {
    }

    public static String startupTable() {
        return "12100 AE_BODY, "
            + "12101 AE_UVL_LIGHT, "
            + "12102 AE_CONTROLLER_LIGHT, "
            + "12103 AE_CABLE_IDLE, "
            + "12104 AE_CABLE_LOW_CHANNEL, "
            + "12105 AE_CABLE_HIGH_CHANNEL, "
            + "12106 AE_DRIVE_LED, "
            + "12107 AE_CRAFTING_LIGHT, "
            + "12108 AE_MONITOR_LIGHT, "
            + "12109 AE_TERMINAL_TRACE, "
            + "12110 OC_BODY, "
            + "12111 OC_LED_BAKED, "
            + "12112 OC_LED_TESR";
    }

    public static String nameOf(int id) {
        switch (id) {
            case AE_BODY:
                return "AE_BODY";
            case AE_UVL_LIGHT:
                return "AE_UVL_LIGHT";
            case AE_CONTROLLER_LIGHT:
                return "AE_CONTROLLER_LIGHT";
            case AE_CABLE_IDLE:
                return "AE_CABLE_IDLE";
            case AE_CABLE_LOW_CHANNEL:
                return "AE_CABLE_LOW_CHANNEL";
            case AE_CABLE_HIGH_CHANNEL:
                return "AE_CABLE_HIGH_CHANNEL";
            case AE_DRIVE_LED:
                return "AE_DRIVE_LED";
            case AE_CRAFTING_LIGHT:
                return "AE_CRAFTING_LIGHT";
            case AE_MONITOR_LIGHT:
                return "AE_MONITOR_LIGHT";
            case AE_TERMINAL_TRACE:
                return "AE_TERMINAL_TRACE";
            case OC_BODY:
                return "OC_BODY";
            case OC_LED_BAKED:
                return "OC_LED_BAKED";
            case OC_LED_TESR:
                return "OC_LED_TESR";
            default:
                return "UNKNOWN";
        }
    }
}
