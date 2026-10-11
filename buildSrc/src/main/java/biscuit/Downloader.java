/* ======================================
 * File: Downloader.java
 * Date: 2026-10-11
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package biscuit;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

public class Downloader {
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void downloadVerified(String url, Path path, String sha1) {
        if (Files.exists(path) && sha1(path).equalsIgnoreCase(sha1)) return;
        Path temp = path.resolveSibling(path.getFileName() + ".part");
        try {
            String actual = download(url, temp);
            if (!actual.equalsIgnoreCase(sha1)) {
                throw new IOException(
                    "SHA-1 mismatch for '" + url + "'. Expected " + sha1
                    + ", got " + actual + ". Downloaded file: " + temp
                );
            }
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException(
                "Failed to download verified file from '" + url
                + "' to '" + path + "' (temporary file: '" + temp + "'). "
                + e.getMessage(),
                e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                "Interrupted while downloading '" + url + "' to '" + path + "'.", e
            );
        }
    }

    public static void downloadUnverified(String url, Path path) {
        Path temp = path.resolveSibling(path.getFileName() + ".part");
        try {
            download(url, temp);
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException(
                "Failed to download file from '" + url
                + "' to '" + path + "' (temporary file: '" + temp + "'). "
                + e.getMessage(),
                e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                "Interrupted while downloading '" + url + "' to '" + path + "'.", e
            );
        }
    }

    private static String download(String url, Path path) throws IOException, InterruptedException {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofMinutes(2))
            .GET()
            .build();
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid download URL: '" + url + "'.", e);
        }
        HashingBodySubscriber subscriber = new HashingBodySubscriber(path);
        HttpResponse<Path> response;
        try {
            response = client.send(request, info ->
                info.statusCode() == 200
                ? subscriber
                : HttpResponse.BodySubscribers.replacing(Path.of("/NULL"))
            );
        } catch (IOException e) {
            throw new IOException(
                "HTTP request failed for '" + url + "' while downloading to '" + path
                + "': " + e.getMessage(),
                e
            );
        }
        if (response.statusCode() != 200) {
            throw new IOException(
                "Download failed for '" + url + "': HTTP "
                + response.statusCode() + " (expected 200). Destination: '" + path + "'."
            );
        }
        return subscriber.getSha1();
    }

    private static String sha1(Path path) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(path))) {
                byte[] buf = new byte[512];
                int len;
                while ((len = in.read(buf, 0, 512)) != -1) {
                    md.update(buf, 0, len);
                }
                return HexFormat.of().formatHex(md.digest());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash file " + path, e);
        }
    }
}

class HashingBodySubscriber implements HttpResponse.BodySubscriber<Path> {
    private final HttpResponse.BodySubscriber<Path> inner;
    private final MessageDigest md;
    private String sha1;

    public HashingBodySubscriber(Path path) {
        try {
            this.inner = HttpResponse.BodySubscribers.ofFile(path);
        } catch (UncheckedIOException e) {
            throw new UncheckedIOException(
                "Failed to open temporary download file '" + path + "'. "
                + e.getMessage(),
                e.getCause()
            );
        }
        try {
            this.md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-1 is required to be supported by every JVM", e);
        }
    }    

    public String getSha1() {
        return this.sha1;
    }

    @Override public void onSubscribe(Flow.Subscription s) { inner.onSubscribe(s); }
    @Override public void onNext(List<ByteBuffer> items) {
        for (ByteBuffer b : items) md.update(b.duplicate());
        inner.onNext(items);
    }
    @Override public void onError(Throwable t) { inner.onError(t); }
    @Override public void onComplete() {
        byte[] hash = md.digest();
        sha1 = HexFormat.of().formatHex(hash);
        inner.onComplete(); 
    }
    @Override public CompletionStage<Path> getBody() { return inner.getBody(); }
}
