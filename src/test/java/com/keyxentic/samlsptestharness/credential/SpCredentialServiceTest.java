package com.keyxentic.samlsptestharness.credential;

import com.keyxentic.samlsptestharness.support.TestCertificates;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;

class SpCredentialServiceTest {

    @Test
    void generatesAndPersistsACredentialWhenNoneExistsYet(@TempDir Path dataDir) throws Exception {
        Path keystorePath = dataDir.resolve("sp-credential.p12");
        SpCredentialService service = new SpCredentialService();

        SpCredential credential = service.loadOrGenerate(keystorePath);

        assertThat(Files.exists(keystorePath)).isTrue();
        assertThat(credential.certificate()).isInstanceOf(X509Certificate.class);
        assertThat(credential.keyPair().getPrivate()).isNotNull();
        assertThat(credential.keyPair().getPublic()).isEqualTo(credential.certificate().getPublicKey());
    }

    @Test
    void loadsTheExistingCredentialInsteadOfGeneratingANewOneOnSubsequentBoots(@TempDir Path dataDir) throws Exception {
        Path keystorePath = dataDir.resolve("sp-credential.p12");
        SpCredential first = new SpCredentialService().loadOrGenerate(keystorePath);

        // a second, independent service instance simulates a container restart
        SpCredential second = new SpCredentialService().loadOrGenerate(keystorePath);

        assertThat(second.certificate().getSerialNumber()).isEqualTo(first.certificate().getSerialNumber());
        assertThat(second.keyPair().getPrivate()).isEqualTo(first.keyPair().getPrivate());
    }

    @Test
    void loadsAnOperatorSuppliedCredentialWhenOnePreexistsAtThePath(@TempDir Path dataDir) throws Exception {
        Path keystorePath = dataDir.resolve("sp-credential.p12");
        // built independently of SpCredentialService, simulating an operator mounting their own
        // credential into the volume before first boot, rather than one the harness generated itself
        X509Certificate operatorCertificate = writeIndependentlyBuiltKeystore(keystorePath);

        SpCredential loaded = new SpCredentialService().loadOrGenerate(keystorePath);

        assertThat(loaded.certificate()).isEqualTo(operatorCertificate);
    }

    private X509Certificate writeIndependentlyBuiltKeystore(Path keystorePath) throws Exception {
        KeyPair keyPair = TestCertificates.generateRsaKeyPair();
        X509Certificate certificate = TestCertificates.selfSigned(keyPair, "CN=operator-supplied");

        Files.createDirectories(keystorePath.getParent());
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        char[] password = "sp-credential".toCharArray();
        keyStore.setKeyEntry("sp", keyPair.getPrivate(), password, new Certificate[]{certificate});
        try (OutputStream out = Files.newOutputStream(keystorePath)) {
            keyStore.store(out, password);
        }
        return certificate;
    }
}
