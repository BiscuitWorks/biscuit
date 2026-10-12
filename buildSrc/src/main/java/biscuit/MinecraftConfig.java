/* ======================================
 * File: MinecraftConfig.java
 * Date: 2026-10-12
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package biscuit;

import org.gradle.api.provider.Property;

public abstract class MinecraftConfig {
    public abstract Property<String> getVersion();

    public void verify() {
        if (!getVersion().isPresent()) {
            throw error("minecraft.version must be set");
        }
    }

    public RuntimeException error(String msg) {
        return new IllegalArgumentException(msg);
    }
}

