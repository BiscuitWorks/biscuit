/* ======================================
 * File: BiscuitPlugin.java
 * Date: 2026-10-10
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package biscuit;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class BiscuitPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getExtensions().create("biscuit", BiscuitExtension.class, project);
    }
}
