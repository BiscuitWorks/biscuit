/* ======================================
 * File: FetchClientTask.java
 * Date: 2026-10-11
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package biscuit;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.file.RegularFileProperty;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

public abstract class FetchClientTask extends DefaultTask {

    @InputFile
    public abstract RegularFileProperty getVersionMetadata();
    
    @OutputFile
    public abstract RegularFileProperty getClientJar();

    @TaskAction
    public void fetchClientJar() {
        Path metadataPath = getVersionMetadata().getAsFile().get().toPath();

        Json metadata;
        try {
            metadata = JsonParser.parse(Files.readString(metadataPath));
        } catch (IOException e) {
            throw new GradleException("Failed to read metadata file.", e);
        } catch (RuntimeException e) {
            throw new GradleException("Failed to parse metadata file.", e);
        }

        JObject clientMeta = metadata.getObject("downloads.client");

        Path clientJarPath = getClientJar().getAsFile().get().toPath();
        Downloader.downloadVerified(clientMeta.getString("url"), clientJarPath, clientMeta.getString("sha1"));
        getLogger().lifecycle("Downloaded client jar successfully!");
    }
}

