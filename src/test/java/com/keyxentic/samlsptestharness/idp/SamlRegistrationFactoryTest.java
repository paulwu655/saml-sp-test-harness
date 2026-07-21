package com.keyxentic.samlsptestharness.idp;

import com.keyxentic.samlsptestharness.config.HarnessProperties;
import com.keyxentic.samlsptestharness.credential.SpCredential;
import com.keyxentic.samlsptestharness.support.IdpMetadataFixtures;
import com.keyxentic.samlsptestharness.support.TestCertificates;
import org.junit.jupiter.api.Test;
import org.springframework.security.saml2.Saml2Exception;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SamlRegistrationFactoryTest {

    private final HarnessProperties properties = new HarnessProperties("https://sp.example.test", "/data");
    private final SpCredential spCredential = generateSpCredential();
    private final SamlRegistrationFactory factory = new SamlRegistrationFactory();

    @Test
    void buildsAPlaceholderRegistrationWhenNoIdpIsConfigured() {
        RelyingPartyRegistration registration = factory.build(properties, spCredential, null);

        assertThat(registration.getEntityId()).isEqualTo("https://sp.example.test");
        assertThat(registration.getAssertingPartyMetadata().getEntityId()).contains("no-idp-configured");
    }

    @Test
    void buildsARegistrationFromImportedIdpMetadata() {
        KeyPair idpKeyPair = TestCertificates.generateRsaKeyPair();
        X509Certificate idpCertificate = TestCertificates.selfSigned(idpKeyPair, "CN=test-idp");
        byte[] metadata = IdpMetadataFixtures.validIdpMetadata(
                "https://idp.example.test", "https://idp.example.test/sso", "https://idp.example.test/slo", idpCertificate)
                .getBytes(StandardCharsets.UTF_8);

        RelyingPartyRegistration registration = factory.build(properties, spCredential, metadata);

        assertThat(registration.getEntityId()).isEqualTo("https://sp.example.test");
        assertThat(registration.getAssertingPartyMetadata().getEntityId()).isEqualTo("https://idp.example.test");
        assertThat(registration.getAssertingPartyMetadata().getSingleSignOnServiceLocation()).isEqualTo("https://idp.example.test/sso");
        assertThat(registration.getAssertingPartyMetadata().getSingleLogoutServiceLocation()).isEqualTo("https://idp.example.test/slo");
    }

    @Test
    void rejectsMalformedIdpMetadata() {
        byte[] malformed = IdpMetadataFixtures.malformedMetadata().getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> factory.build(properties, spCredential, malformed))
                .isInstanceOf(Saml2Exception.class);
    }

    private static SpCredential generateSpCredential() {
        KeyPair keyPair = TestCertificates.generateRsaKeyPair();
        X509Certificate certificate = TestCertificates.selfSigned(keyPair, "CN=test-sp");
        return new SpCredential(keyPair, certificate);
    }
}
