/* ======================================
 * File: BiscuitPlugin.java
 * Date: 2026-10-10
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package biscuit;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Provider;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.tasks.TaskProvider;
import java.nio.file.Files;
import java.nio.file.Path;

public class BiscuitPlugin implements Plugin<Project> {
    private static final String REPO_URL = "https://libraries.minecraft.net";

    @Override
    public void apply(Project project) {
        BiscuitExtension ext = project.getExtensions().create("biscuit", BiscuitExtension.class, project);
    }
}
