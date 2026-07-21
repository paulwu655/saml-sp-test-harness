package com.keyxentic.samlsptestharness.web;

import com.keyxentic.samlsptestharness.config.HarnessProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SpMetadataIntegrationTest {

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
    void exportsSpMetadataReflectingTheConfiguredBaseUrlAndTheSpCredential() throws Exception {
        mockMvc.perform(get("/saml2/service-provider-metadata/{registrationId}", HarnessProperties.REGISTRATION_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/samlmetadata+xml"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("entityID=\"https://sp.example.test\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Location=\"https://sp.example.test/login/saml2/sso/" + HarnessProperties.REGISTRATION_ID + "\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Location=\"https://sp.example.test/logout/saml2/slo\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("X509Certificate")));
    }
}
