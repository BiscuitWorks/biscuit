/* ======================================
 * File: FetchLibrariesTask.java
 * Date: 2026-10-11
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package biscuit;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.file.DirectoryProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class FetchLibrariesTask extends DefaultTask {
    @InputFile
    public abstract RegularFileProperty getVersionMetadata();

    @OutputDirectory
    public abstract DirectoryProperty getLibsDirectory();

    @TaskAction
    public void fetchLibraries() {
        Path metadataPath = getVersionMetadata().getAsFile().get().toPath();
        Path libsDirectory = getLibsDirectory().getAsFile().get().toPath();

        getLogger().lifecycle("Fetching libraries into {}", libsDirectory);
        getLogger().info("Reading version metadata from {}", metadataPath);

        Json metadata;

        try {
            metadata = JsonParser.parse(Files.readString(metadataPath));
        } catch (IOException e) {
            throw new GradleException("Failed to read metadata file: " + metadataPath, e);
        } catch (RuntimeException e) {
            throw new GradleException("Failed to parse metadata file: " + metadataPath, e);
        }

        String osName = getOSName();
        getLogger().info("Detected operating system: {}", osName);

        int total = metadata.getArray("libraries").items().size();
        int fetched = 0;
        int skipped = 0;

        getLogger().info("Found {} libraries in metadata.", total);

        for (Json raw : metadata.getArray("libraries").items()) {
            if (!(raw instanceof JObject library)) {
                throw new GradleException(
                    "Expected a library object, got " + raw.getClass().getSimpleName()
                );
            }

            if (!supportsOS(library, osName)) {
                skipped++;
                getLogger().info("Skipping library '{}' (unsupported on {}).",
                    library.getString("name"), osName);
                continue;
            }

            JObject artifact = library.getObject("downloads.artifact");
            Path artifactPath = libsDirectory.resolve(artifact.getString("path"));

            try {
                Files.createDirectories(temp.toAbsolutePath().getParent());
            } catch (IOException e) {
                throw new UncheckedIOException(
                    "Failed to create download directory for '" + temp + "': " + e.getMessage(),
                    e
                );
            }
            getLogger().info("Fetching library '{}'.", library.getString("name"));
            getLogger().debug("Library URL: {}", artifact.getString("url"));
            getLogger().debug("Library destination: {}", artifactPath);

            Downloader.downloadVerified(
                artifact.getString("url"),
                artifactPath,
                artifact.getString("sha1")
            );

            fetched++;
        }

        getLogger().lifecycle(
            "Library fetching complete: {} fetched, {} skipped, {} total.",
            fetched, skipped, total
        );
    }

    private boolean supportsOS(JObject library, String osName) {
        if (!library.containsKey("rules")) return true;

        for (Json raw : library.getArray("rules").items()) {
            if (!(raw instanceof JObject rule)) {
                throw new GradleException(
                    "Expected a rule object, got " + raw.getClass().getSimpleName()
                );
            }

            if (!rule.getString("os.name").equals(osName)) {
                getLogger().debug(
                    "Library '{}' rejected by OS rule: expected '{}', detected '{}'.",
                    library.getString("name"),
                    rule.getString("os.name"),
                    osName
                );
                return false;
            }
        }

        return true;
    }

    private String getOSName() {
        String name = System.getProperty("os.name").toLowerCase();

        if (name.contains("win")) {
            return "windows";
        } else if (name.contains("mac")) {
            return "osx";
        } else if (name.contains("nix") || name.contains("nux") || name.contains("aix")) {
            return "linux";
        }

        throw new GradleException("Unsupported OS '" + name + "' to fetch libraries for.");
    }
}
