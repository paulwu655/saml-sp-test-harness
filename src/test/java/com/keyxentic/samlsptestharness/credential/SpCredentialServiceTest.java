package com.keyxentic.samlsptestharness.credential;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

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
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        X500Name subject = new X500Name("CN=operator-supplied");
        Instant notBefore = Instant.now();
        BigInteger serial = new BigInteger(128, new SecureRandom());
        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(subject, serial,
                Date.from(notBefore), Date.from(notBefore.plus(Duration.ofDays(30))), subject, publicKeyInfo);
        X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(
                builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate())));

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
