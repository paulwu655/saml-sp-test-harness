package com.keyxentic.samlsptestharness.support;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Self-signed RSA keypairs/certificates for tests, built independently of production code. */
public final class TestCertificates {

    private TestCertificates() {
    }

    public static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048, new SecureRandom());
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static X509Certificate selfSigned(KeyPair keyPair, String subjectDn) {
        try {
            X500Name subject = new X500Name(subjectDn);
            Instant notBefore = Instant.now();
            BigInteger serial = new BigInteger(128, new SecureRandom());
            SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
            X509v3CertificateBuilder builder = new X509v3CertificateBuilder(subject, serial,
                    Date.from(notBefore), Date.from(notBefore.plus(Duration.ofDays(30))), subject, publicKeyInfo);
            return new JcaX509CertificateConverter().getCertificate(
                    builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
