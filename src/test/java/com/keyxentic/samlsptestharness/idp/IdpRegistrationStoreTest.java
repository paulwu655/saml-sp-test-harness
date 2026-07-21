package com.keyxentic.samlsptestharness.idp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class IdpRegistrationStoreTest {

    @Test
    void startsEmptyWhenNoMetadataFileExistsYet(@TempDir Path dataDir) {
        IdpRegistrationStore store = new IdpRegistrationStore(dataDir.resolve("idp-metadata.xml"));

        assertThat(store.current()).isEmpty();
    }

    @Test
    void replaceUpdatesTheCurrentIdpAndPersistsToTheFile(@TempDir Path dataDir) {
        Path metadataFile = dataDir.resolve("idp-metadata.xml");
        IdpRegistrationStore store = new IdpRegistrationStore(metadataFile);
        byte[] xml = "<idp-metadata-v1/>".getBytes(StandardCharsets.UTF_8);

        store.replace(xml);

        assertThat(store.current()).contains(xml);
    }

    @Test
    void aNewImportReplacesThePreviousOneOutright(@TempDir Path dataDir) {
        Path metadataFile = dataDir.resolve("idp-metadata.xml");
        IdpRegistrationStore store = new IdpRegistrationStore(metadataFile);
        store.replace("<idp-metadata-v1/>".getBytes(StandardCharsets.UTF_8));

        byte[] v2 = "<idp-metadata-v2/>".getBytes(StandardCharsets.UTF_8);
        store.replace(v2);

        assertThat(store.current()).contains(v2);
    }

    @Test
    void survivesARestartEquivalentReload(@TempDir Path dataDir) {
        Path metadataFile = dataDir.resolve("idp-metadata.xml");
        byte[] xml = "<idp-metadata/>".getBytes(StandardCharsets.UTF_8);
        new IdpRegistrationStore(metadataFile).replace(xml);

        // a second, independent store instance simulates a container restart
        IdpRegistrationStore reloaded = new IdpRegistrationStore(metadataFile);

        assertThat(reloaded.current()).contains(xml);
    }
}
