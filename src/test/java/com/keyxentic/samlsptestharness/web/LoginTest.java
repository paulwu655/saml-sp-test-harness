package com.keyxentic.samlsptestharness.web;

import com.keyxentic.samlsptestharness.support.IdpMetadataFixtures;
import com.keyxentic.samlsptestharness.support.SignedSamlResponseFixtures;
import com.keyxentic.samlsptestharness.support.TestCertificates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class LoginTest {

    private static final String SP_ENTITY_ID = "https://sp.example.test";
    private static final String ACS_URL = "https://sp.example.test/login/saml2/sso/harness";
    private static final String IDP_ENTITY_ID = "https://idp.example.test";

    @TempDir
    static Path dataDir;

    @DynamicPropertySource
    static void harnessProperties(DynamicPropertyRegistry registry) {
        registry.add("harness.base-url", () -> SP_ENTITY_ID);
        registry.add("harness.data-dir", dataDir::toString);
    }

    @Autowired
    private MockMvc mockMvc;

    private KeyPair idpKeyPair;
    private X509Certificate idpCertificate;

    @BeforeEach
    void importTheIdp() throws Exception {
        idpKeyPair = TestCertificates.generateRsaKeyPair();
        idpCertificate = TestCertificates.selfSigned(idpKeyPair, "CN=test-idp");
        String idpMetadata = IdpMetadataFixtures.validIdpMetadata(
                IDP_ENTITY_ID, "https://idp.example.test/sso", null, idpCertificate);
        mockMvc.perform(multipart("/idp-metadata").param("metadataText", idpMetadata));
    }

    @Test
    void triggeringALoginTestRedirectsToTheIdpWithAnAuthnRequest() throws Exception {
        mockMvc.perform(get("/saml2/authenticate/harness"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("https://idp.example.test/sso")));
    }

    @Test
    void aValidSpInitiatedResponseProducesASuccessfulResultRecord() throws Exception {
        String samlResponse = SignedSamlResponseFixtures.signedResponse(
                SignedSamlResponseFixtures.Spec.validFor(IDP_ENTITY_ID, idpKeyPair, idpCertificate, SP_ENTITY_ID, ACS_URL));

        mockMvc.perform(post("/login/saml2/sso/harness").param("SAMLResponse", samlResponse))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("test-user@example.test")))
                .andExpect(content().string(containsString("Signature: pass")))
                .andExpect(content().string(containsString("Time validity: pass")))
                .andExpect(content().string(containsString("email")));
    }

    @Test
    void anUnsolicitedResponseIsAcceptedAsAnIdpInitiatedLogin() throws Exception {
        // No prior GET /saml2/authenticate/harness in this test — simulates the IdP starting the flow unprompted.
        String samlResponse = SignedSamlResponseFixtures.signedResponse(
                SignedSamlResponseFixtures.Spec.validFor(IDP_ENTITY_ID, idpKeyPair, idpCertificate, SP_ENTITY_ID, ACS_URL));

        mockMvc.perform(post("/login/saml2/sso/harness").param("SAMLResponse", samlResponse))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("test-user@example.test")));
    }

    @Test
    void theResultRecordIncludesTheRawResponseXml() throws Exception {
        String samlResponse = SignedSamlResponseFixtures.signedResponse(
                SignedSamlResponseFixtures.Spec.validFor(IDP_ENTITY_ID, idpKeyPair, idpCertificate, SP_ENTITY_ID, ACS_URL));

        mockMvc.perform(post("/login/saml2/sso/harness").param("SAMLResponse", samlResponse))
                .andExpect(content().string(containsString("&lt;saml2:Assertion")));
    }

    @Test
    void anInvalidSignatureProducesAFailedResultRecordWithoutAStackTrace() throws Exception {
        KeyPair wrongKeyPair = TestCertificates.generateRsaKeyPair();
        X509Certificate wrongCertificate = TestCertificates.selfSigned(wrongKeyPair, "CN=wrong-key");
        String samlResponse = SignedSamlResponseFixtures.signedResponse(
                SignedSamlResponseFixtures.Spec.validFor(IDP_ENTITY_ID, idpKeyPair, idpCertificate, SP_ENTITY_ID, ACS_URL)
                        .signedWith(wrongKeyPair, wrongCertificate));

        mockMvc.perform(post("/login/saml2/sso/harness").param("SAMLResponse", samlResponse))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Signature: fail")));
    }

    @Test
    void anExpiredAssertionProducesAFailedResultRecord() throws Exception {
        String samlResponse = SignedSamlResponseFixtures.signedResponse(
                SignedSamlResponseFixtures.Spec.validFor(IDP_ENTITY_ID, idpKeyPair, idpCertificate, SP_ENTITY_ID, ACS_URL)
                        .expired());

        mockMvc.perform(post("/login/saml2/sso/harness").param("SAMLResponse", samlResponse))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Result: failed")));
    }
}
