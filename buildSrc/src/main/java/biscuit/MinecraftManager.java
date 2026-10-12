/* ======================================
 * File: MinecraftManager.java
 * Date: 2026-10-12
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package biscuit;

import org.gradle.api.Project;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.inject.Inject;

public class MinecraftManager {
    private static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"; 
    private static final String REPO_URL = "https://libraries.minecraft.net";

    private final Project project;
    private final Path cacheDir;

    @Inject
    public MinecraftManager(Project project) {
        this.project = project;
        this.cacheDir = project.getGradle().getGradleUserHomeDir().toPath().resolve("caches/biscuit");
    }

    public void setup(MinecraftConfig config) {
        try {
            Files.createDirectories(cacheDir);
        } catch (Exception e) {
            throw new GradleException("Could not create cache directory for biscuit plugin.", e);
        }

        config.verify();

        DepConfigs configs = createConfigurations();
        JObject versionMeta = getVersionMeta(config.getVersion().get());
        
        addRepositories();
        addMinecraftDependencies(configs, versionMeta.getObject("downloads.client"));
        addLibraryDependencies(configs, versionMeta.getArray("libraries"));
        registerTasks();
    }

    private void registerTasks() {
    }

    private void addRepositories() {
        project.getRepositories().maven(repo -> repo.setUrl(REPO_URL));
        project.getRepositories().ivy(repo -> {
            repo.setName("mojangPissTon");
            repo.setUrl("https://piston-meta.mojang.com");
            repo.patternLayout(l -> l.artifact("v1/objects/[revision]/[module].[ext]"));
            repo.metadataSources(m -> m.artifact());
            repo.content(c -> c.includeGroup("net.minecraft"));
        });
    }

    private void addMinecraftDependencies(DepConfigs configs, JObject clientInfo) {
        project.getDependencies().add(configs.mc().getName(), "net.minecraft:client:" + clientInfo.getString("sha1") + "@jar");
    }

    private void addLibraryDependencies(DepConfigs configs, JArray libraries) {
        String libsName = configs.libraries().getName();
        String nativesName = configs.natives().getName();
        OSMeta os = getOSMeta();

        for (Json raw: libraries.items()) {
            if (!(raw instanceof JObject library)) {
                throw new GradleException(
                    "Expected a library object, got " + raw.getClass().getSimpleName()
                );
            }

            String configName = libsName;
            String libName = library.getString("name");

            if (library.containsKey("rules")) {
                if (!libAllowedOnOS(os, library)) continue;
                configName = nativesName;
            }

            project.getDependencies().add(configName, libName);
        }
    }

    private boolean libAllowedOnOS(OSMeta os, JObject library) {
        boolean allowed = false;
        for (Json raw : library.getArray("rules").items()) {
            JObject rule = (JObject) raw;
            if (!ruleMatches(rule, os)) continue;
            allowed = rule.getString("action").equals("allow");
        }
        return allowed;
    }

    private boolean ruleMatches(JObject rule, OSMeta os) {
        if (!rule.containsKey("os")) return true;
        JObject osInfo = rule.getObject("os");
        if (osInfo.containsKey("name") && !osInfo.getString("name").equals(os.name())) return false;
        if (osInfo.containsKey("arch") && !osInfo.getString("arch").equals(os.arch())) return false;
        return true;
    }

    // uses new gradle convention of using dependencyScopes to collect deps, resolvable to resolve deps
    private DepConfigs createConfigurations() {
        ConfigurationContainer configurations = project.getConfigurations();
        Configuration mcDeps = configurations.dependencyScope("minecraftDeps").get(); 
        Configuration libs = configurations.dependencyScope("minecraftLibraries").get(); 
        Configuration natives = configurations.dependencyScope("minecraftNativeDeps").get(); 
        configurations.resolvable("minecraft").get().extendsFrom(mcDeps);
        configurations.resolvable("minecraftNatives").get().extendsFrom(natives);
        configurations.getByName("implementation").extendsFrom(mcDeps, libs);
        return new DepConfigs(mcDeps, libs, natives);
    }

    private JObject getVersionMeta(String version) {
        Path metaFile = cacheDir.resolve(version + ".json");

        if (!Files.exists(metaFile)) {
            Path manifestFile = cacheDir.resolve("version_manifest_v2.json");
            Downloader.downloadUnverifiedCached(MANIFEST_URL, manifestFile, true);
            Json manifest = JsonParser.parse(getFileContent(manifestFile));
            JObject versionEntry = null;
            for (Json item : manifest.getArray("versions").items()) {
                JObject candidate = (JObject) item;
                if (candidate.getString("id").equals(version)) {
                    versionEntry = candidate;
                    break;
                }
            }

            if (versionEntry == null) {
                throw new GradleException("Could not find details in manifest for version " + version);
            }

            String metaUrl = versionEntry.getString("url");
            String metaSha1 = versionEntry.getString("sha1");
            Downloader.downloadCached(metaUrl, metaFile, metaSha1);
        }

        return (JObject) JsonParser.parse(getFileContent(metaFile));
    }

    private String getFileContent(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception e) {
            throw new GradleException("Could not read " + path, e);
        }
    }

    private OSMeta getOSMeta() {
        String name = System.getProperty("os.name").toLowerCase();

        if (name.contains("win")) {
            name = "windows";
        } else if (name.contains("mac")) {
            name = "osx";
        } else if (name.contains("nix") || name.contains("nux") || name.contains("aix")) {
            name = "linux";
        } else {
            throw new GradleException("Unsupported OS '" + name + "' to fetch libraries for.");
        }

        String arch = System.getProperty("os.arch");

        return new OSMeta(name, arch);
    }
}

record DepConfigs(Configuration mc, Configuration libraries, Configuration natives) {}
record OSMeta(String name, String arch) {}
