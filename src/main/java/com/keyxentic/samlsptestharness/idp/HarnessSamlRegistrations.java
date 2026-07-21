package com.keyxentic.samlsptestharness.idp;

import com.keyxentic.samlsptestharness.config.HarnessProperties;
import com.keyxentic.samlsptestharness.credential.SpCredential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;

/**
 * Resolves the harness's single SP↔IdP pairing on every request, from the SP's own identity
 * ({@link HarnessProperties}, {@link SpCredential}) plus whatever IdP is currently active in the
 * {@link IdpRegistrationStore} — or a placeholder IdP if none has been imported yet.
 * <p>
 * Implements Spring Security's {@link RelyingPartyRegistrationRepository} SPI, whose vocabulary
 * ("relying party") CONTEXT.md deliberately avoids elsewhere in this codebase — that naming is
 * confined to this integration point.
 */
public class HarnessSamlRegistrations implements RelyingPartyRegistrationRepository {

    private final HarnessProperties properties;
    private final SpCredential spCredential;
    private final IdpRegistrationStore idpRegistrationStore;
    private final SamlRegistrationFactory registrationFactory;

    public HarnessSamlRegistrations(HarnessProperties properties, SpCredential spCredential,
                                     IdpRegistrationStore idpRegistrationStore, SamlRegistrationFactory registrationFactory) {
        this.properties = properties;
        this.spCredential = spCredential;
        this.idpRegistrationStore = idpRegistrationStore;
        this.registrationFactory = registrationFactory;
    }

    @Override
    public RelyingPartyRegistration findByRegistrationId(String registrationId) {
        if (!HarnessProperties.REGISTRATION_ID.equals(registrationId)) {
            return null;
        }
        byte[] idpMetadataXml = idpRegistrationStore.current().orElse(null);
        return registrationFactory.build(properties, spCredential, idpMetadataXml);
    }
}
