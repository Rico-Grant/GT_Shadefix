package net.minecraftforge.common.config;

import java.io.File;

public class Configuration {
    public static final String CATEGORY_CLIENT = "client";

    public Configuration(File file) {
    }

    public void load() {
    }

    public boolean getBoolean(String name, String category, boolean defaultValue, String comment) {
        return defaultValue;
    }

    public int getInt(String name, String category, int defaultValue, int minValue, int maxValue, String comment) {
        return defaultValue;
    }

    public boolean hasChanged() {
        return false;
    }

    public void save() {
    }
}
