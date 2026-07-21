package com.keyxentic.samlsptestharness.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class HomeControllerTest {

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
    void homePageLinksToSpMetadataExport() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/saml2/service-provider-metadata/harness")))
                .andExpect(content().string(containsString("https://sp.example.test")));
    }
}
