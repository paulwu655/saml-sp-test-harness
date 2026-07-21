package com.keyxentic.samlsptestharness.idp;

import com.keyxentic.samlsptestharness.config.HarnessProperties;
import com.keyxentic.samlsptestharness.credential.SpCredential;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;

import java.io.ByteArrayInputStream;

/**
 * Builds the harness's SP↔IdP pairing from the SP's own identity plus, if one has been imported,
 * the currently active IdP Metadata. Used both by {@link HarnessSamlRegistrations} at request time
 * and by the IdP Metadata import flow to validate a candidate import before it's persisted.
 */
public class SamlRegistrationFactory {

    /**
     * @param idpMetadataXml the currently active IdP Metadata, or {@code null} if none has been
     *                       imported yet, in which case a placeholder IdP is used.
     * @throws org.springframework.security.saml2.Saml2Exception if idpMetadataXml is present but
     *                                                            not parseable as SAML metadata.
     */
    public RelyingPartyRegistration build(HarnessProperties properties, SpCredential spCredential, byte[] idpMetadataXml) {
        RelyingPartyRegistration.Builder builder = idpMetadataXml == null
                ? placeholderBuilder(properties)
                : RelyingPartyRegistrations.fromMetadata(new ByteArrayInputStream(idpMetadataXml));

        Saml2X509Credential signingCredential = Saml2X509Credential.signing(
                spCredential.keyPair().getPrivate(), spCredential.certificate());

        return builder
                .registrationId(HarnessProperties.REGISTRATION_ID)
                .entityId(properties.entityId())
                .assertionConsumerServiceLocation(properties.assertionConsumerServiceLocation())
                .singleLogoutServiceLocation(properties.singleLogoutServiceLocation())
                .signingX509Credentials(c -> c.add(signingCredential))
                .build();
    }

    private RelyingPartyRegistration.Builder placeholderBuilder(HarnessProperties properties) {
        return RelyingPartyRegistration.withRegistrationId(HarnessProperties.REGISTRATION_ID)
                .assertingPartyMetadata(idp -> idp
                        .entityId(properties.entityId() + "/no-idp-configured")
                        .singleSignOnServiceLocation(properties.entityId() + "/no-idp-configured")
                        .wantAuthnRequestsSigned(false));
    }
}
