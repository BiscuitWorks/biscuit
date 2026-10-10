/* ======================================
 * File: FetchTask.java
 * Date: 2026-10-10
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package biscuit;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.io.File;

public abstract class FetchTask extends DefaultTask {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"; 

    @Input
    public abstract Property<String> getMcVersion();

    @OutputFile
    public abstract RegularFileProperty getManifestFile();

    @TaskAction
    public void fetchManifest() throws Exception {
        Path manifestPath = getManifestFile().getAsFile().get().toPath();
        Path tempPath = manifestPath.resolveSibling(manifestPath.getFileName() + ".tmp");
        URI uri = URI.create(MANIFEST_URL);

        HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tempPath));

        int status = response.statusCode();
        if (status == 200) {

            Files.move(tempPath, manifestPath, StandardCopyOption.REPLACE_EXISTING);
        } else {
            throw new GradleException("Could not download the version manifest from " + uri + ": server returned HTTP " + status + ", expected 200.");        
        }

        Files.deleteIfExists(tempPath);
        getLogger().lifecycle("Manifest Downloaded Successfully");
    }
}

