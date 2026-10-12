/* ======================================
 * File: BiscuitExtension.java
 * Date: 2026-10-12
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package biscuit;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;

public class BiscuitExtension {
    private final Project project;
    private boolean configured = false;
    
    public BiscuitExtension(Project project) {
        this.project = project;
    }

    public void minecraft(Action<MinecraftConfig> action) {
        if (configured) throw new GradleException("biscuit.minecraft { } can only be configured once per project");
        configured = true;

        MinecraftConfig config = project.getObjects().newInstance(MinecraftConfig.class);
        action.execute(config);
        project.getObjects().newInstance(MinecraftManager.class).setup(config);
    }
}
