package com.keyxentic.samlsptestharness.web;

import com.keyxentic.samlsptestharness.support.IdpMetadataFixtures;
import com.keyxentic.samlsptestharness.support.TestCertificates;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class IdpMetadataControllerTest {

    @TempDir
    static Path dataDir;

    @DynamicPropertySource
    static void harnessProperties(DynamicPropertyRegistry registry) {
        registry.add("harness.base-url", () -> "https://sp.example.test");
        registry.add("harness.data-dir", dataDir::toString);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void importingPastedXmlUpdatesTheActiveIdpAndShowsAConfirmation() throws Exception {
        String metadata = idpMetadata("https://idp-one.example.test", "https://idp-one.example.test/sso", null);

        MvcResult result = mockMvc.perform(multipart("/idp-metadata").param("metadataText", metadata))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        followRedirect(result)
                .andExpect(content().string(containsString("https://idp-one.example.test")));
    }

    @Test
    void importingAnUploadedFileUpdatesTheActiveIdp() throws Exception {
        String metadata = idpMetadata("https://idp-file.example.test", "https://idp-file.example.test/sso", null);
        MockMultipartFile file = new MockMultipartFile(
                "file", "idp-metadata.xml", "application/xml", metadata.getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc.perform(multipart("/idp-metadata").file(file))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        followRedirect(result)
                .andExpect(content().string(containsString("https://idp-file.example.test")));
    }

    @Test
    void aNewImportReplacesThePreviousIdpOutright() throws Exception {
        mockMvc.perform(multipart("/idp-metadata").param("metadataText",
                idpMetadata("https://idp-v1.example.test", "https://idp-v1.example.test/sso", null)));

        mockMvc.perform(multipart("/idp-metadata").param("metadataText",
                idpMetadata("https://idp-v2.example.test", "https://idp-v2.example.test/sso", null)));

        mockMvc.perform(get("/"))
                .andExpect(content().string(containsString("https://idp-v2.example.test")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("https://idp-v1.example.test"))));
    }

    @Test
    void malformedMetadataIsRejectedAndDoesNotOverwriteTheCurrentIdp() throws Exception {
        mockMvc.perform(multipart("/idp-metadata").param("metadataText",
                idpMetadata("https://idp-good.example.test", "https://idp-good.example.test/sso", null)));

        MvcResult result = mockMvc.perform(multipart("/idp-metadata")
                        .param("metadataText", IdpMetadataFixtures.malformedMetadata()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        followRedirect(result)
                .andExpect(content().string(containsString("Import failed")));

        mockMvc.perform(get("/"))
                .andExpect(content().string(containsString("https://idp-good.example.test")));
    }

    private org.springframework.test.web.servlet.ResultActions followRedirect(MvcResult result) throws Exception {
        return mockMvc.perform(get("/").flashAttrs(extractFlashAttributes(result)));
    }

    private java.util.Map<String, Object> extractFlashAttributes(MvcResult result) {
        var flashMap = result.getFlashMap();
        if (flashMap == null) {
            throw new IllegalStateException("Expected a flash attribute to be set on the redirect");
        }
        return new java.util.HashMap<>(flashMap);
    }

    private String idpMetadata(String entityId, String ssoLocation, String sloLocation) {
        KeyPair keyPair = TestCertificates.generateRsaKeyPair();
        X509Certificate certificate = TestCertificates.selfSigned(keyPair, "CN=test-idp");
        return IdpMetadataFixtures.validIdpMetadata(entityId, ssoLocation, sloLocation, certificate);
    }
}
