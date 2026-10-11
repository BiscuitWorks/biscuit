/* ======================================
 * File: FetchVersionTask.java
 * Date: 2026-10-11
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package biscuit;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

public abstract class FetchVersionTask extends DefaultTask {
    @Input
    public abstract Property<String> getMcVersion();

    @InputFile
    public abstract RegularFileProperty getManifestFile();

    @OutputFile
    public abstract RegularFileProperty getVersionFile();

    @TaskAction
    public void fetchVersion() {
        Path manifestPath = getManifestFile().getAsFile().get().toPath();
        String mcVersion = getMcVersion().get();

        Json manifest;
        try {
            manifest = JsonParser.parse(Files.readString(manifestPath));
        } catch (IOException e) {
            throw new GradleException("Failed to read manifest file.", e);
        } catch (RuntimeException e) {
            throw new GradleException("Failed to parse manifest file.", e);
        }

        JObject versionMeta = findVersion(manifest, mcVersion);
        System.out.println(getVersionFile().getAsFile().get().toPath());
        Downloader.downloadVerified(versionMeta.getString("url"), getVersionFile().getAsFile().get().toPath(), versionMeta.getString("sha1"));
        getLogger().lifecycle("Found Minecraft {}: {}, downloading version meta finished.", mcVersion, versionMeta.getString("url"));
    }

    private static JObject findVersion(Json manifest, String id) {
        for (Json item: manifest.getArray("versions").items()) {
            if (item instanceof JObject v && v.getString("id").equals(id)) {
                return v;
            }
        }

        throw new GradleException("Minecraft version " + id + " not found in manifest");
    }
}

