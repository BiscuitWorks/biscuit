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

    @Override
    public void apply(Project project) {
        String name = project.getName();
        ProjectLayout layout = project.getLayout();
        Directory cacheDir = layout.getProjectDirectory().dir("." + name);
        Provider<String> mcVersion = project.getProviders().gradleProperty("mc_version"); 

        TaskProvider<FetchTask> fetchManifest = project.getTasks().register("fetchManifest", FetchTask.class, task -> {
            task.setGroup("biscuit");
            task.setDescription("Downloads the version manifest file");
            task.getMcVersion().set(mcVersion);
            task.getManifestFile().set(cacheDir.file("version_manifest_v2.json"));
        });

        TaskProvider<FetchVersionTask> fetchVersion = project.getTasks().register("fetchVersion", FetchVersionTask.class, task -> {
            task.setGroup("biscuit");
            task.setDescription("Downloads the version metadata file");
            task.getMcVersion().set(mcVersion);
            task.getManifestFile().set(fetchManifest.flatMap(t -> t.getManifestFile()));
            task.getVersionFile().set(cacheDir.file(mcVersion.get() + ".json"));
        });

        TaskProvider<FetchClientTask> fetchClient = project.getTasks().register("fetchClient", FetchClientTask.class, task -> {
            task.setGroup("biscuit");
            task.setDescription("Downloads the minecraft client file");

            task.getVersionMetadata().set(fetchVersion.flatMap(t -> t.getVersionFile()));
            task.getClientJar().set(cacheDir.file("minecraft-" + mcVersion.get() + ".jar"));
        });

        TaskProvider<FetchLibrariesTask> fetchLibs = project.getTasks().register("fetchLibraries", FetchLibrariesTask.class, task -> {
            task.setGroup("biscuit");
            task.setDescription("Downloads minecraft dependencies");

            task.getVersionMetadata().set(fetchVersion.flatMap(t -> t.getVersionFile()));
            task.getLibsDirectory().set(cacheDir.dir("libs"));
        });
    }
}

