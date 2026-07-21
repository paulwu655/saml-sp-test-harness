package com.keyxentic.samlsptestharness.idp;

import com.keyxentic.samlsptestharness.config.HarnessProperties;
import com.keyxentic.samlsptestharness.credential.SpCredential;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;

/**
 * Builds the harness's single SP↔IdP pairing from the SP's own identity
 * ({@link HarnessProperties}, {@link SpCredential}) plus whatever IdP is currently configured.
 * <p>
 * No IdP Metadata import exists yet (that's a separate, later ticket) — until one is imported,
 * a placeholder IdP is used so SP Metadata export still works. This class is the seam that
 * ticket will extend to source the real IdP details instead of the placeholder.
 * <p>
 * Implements Spring Security's {@link RelyingPartyRegistrationRepository} SPI, whose vocabulary
 * ("relying party") CONTEXT.md deliberately avoids elsewhere in this codebase — that naming is
 * confined to this integration point.
 */
public class HarnessSamlRegistrations implements RelyingPartyRegistrationRepository {

    private final HarnessProperties properties;
    private final SpCredential spCredential;

    public HarnessSamlRegistrations(HarnessProperties properties, SpCredential spCredential) {
        this.properties = properties;
        this.spCredential = spCredential;
    }

    @Override
    public RelyingPartyRegistration findByRegistrationId(String registrationId) {
        if (!HarnessProperties.REGISTRATION_ID.equals(registrationId)) {
            return null;
        }
        return build();
    }

    private RelyingPartyRegistration build() {
        Saml2X509Credential signingCredential = Saml2X509Credential.signing(
                spCredential.keyPair().getPrivate(), spCredential.certificate());

        return RelyingPartyRegistration.withRegistrationId(HarnessProperties.REGISTRATION_ID)
                .entityId(properties.entityId())
                .assertionConsumerServiceLocation(properties.assertionConsumerServiceLocation())
                .singleLogoutServiceLocation(properties.singleLogoutServiceLocation())
                .signingX509Credentials(c -> c.add(signingCredential))
                .assertingPartyMetadata(idp -> idp
                        .entityId(properties.entityId() + "/no-idp-configured")
                        .singleSignOnServiceLocation(properties.entityId() + "/no-idp-configured")
                        .wantAuthnRequestsSigned(false))
                .build();
    }
}
