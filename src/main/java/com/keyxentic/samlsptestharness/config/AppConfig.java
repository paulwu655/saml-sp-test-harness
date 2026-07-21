package com.keyxentic.samlsptestharness.config;

import com.keyxentic.samlsptestharness.credential.SpCredential;
import com.keyxentic.samlsptestharness.credential.SpCredentialService;
import com.keyxentic.samlsptestharness.idp.HarnessSamlRegistrations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;

@Configuration
@EnableConfigurationProperties(HarnessProperties.class)
public class AppConfig {

    @Bean
    public SpCredentialService spCredentialService() {
        return new SpCredentialService();
    }

    @Bean
    public SpCredential spCredential(SpCredentialService spCredentialService, HarnessProperties properties) {
        return spCredentialService.loadOrGenerate(properties.spCredentialPath());
    }

    @Bean
    public RelyingPartyRegistrationRepository samlRegistrations(
            HarnessProperties properties, SpCredential spCredential) {
        return new HarnessSamlRegistrations(properties, spCredential);
    }
}
