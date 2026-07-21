package com.keyxentic.samlsptestharness.idp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the harness's single active IdP Metadata, persisted to a file so it survives restarts.
 * At most one IdP is ever configured — {@link #replace} overwrites whatever was there before.
 */
public class IdpRegistrationStore {

    private final Path metadataFile;
    private final AtomicReference<byte[]> current = new AtomicReference<>();

    public IdpRegistrationStore(Path metadataFile) {
        this.metadataFile = metadataFile;
        if (Files.exists(metadataFile)) {
            current.set(readFile(metadataFile));
        }
    }

    public Optional<byte[]> current() {
        return Optional.ofNullable(current.get());
    }

    public void replace(byte[] idpMetadataXml) {
        writeFile(metadataFile, idpMetadataXml);
        current.set(idpMetadataXml);
    }

    private static byte[] readFile(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeFile(Path path, byte[] content) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
