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

    @TaskAction
    public void fetchVersion() {
        Path manifestPath = getManifestFile().getAsFile().get().toPath();
        String src;
        try {
            src = Files.readString(manifestPath);
        } catch (IOException e) {
            throw new GradleException("Failed to read manifest file.", e.getCause());
        }
        String mcVersion = getMcVersion().get();
        Json json = JsonParser.parse(src);
        JObject versionMeta = null;

        for (Json item: json.getArray("versions").items()) {
            if (item instanceof JObject v && v.getString("id").equals(mcVersion)) {
                versionMeta = v;
                break;
            }
        }

        if (versionMeta == null) {
            throw new GradleException("Failed to find metadata for provided version " + mcVersion);
        }
        
        System.out.println(versionMeta);
    }
}

