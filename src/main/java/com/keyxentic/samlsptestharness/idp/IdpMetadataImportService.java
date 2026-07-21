package com.keyxentic.samlsptestharness.idp;

import com.keyxentic.samlsptestharness.config.HarnessProperties;
import com.keyxentic.samlsptestharness.credential.SpCredential;
import org.springframework.security.saml2.Saml2Exception;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;

/**
 * Validates a candidate IdP Metadata import before it's allowed to become the active IdP
 * Registration — a malformed submission must never overwrite what's currently configured.
 */
public class IdpMetadataImportService {

    private final HarnessProperties properties;
    private final SpCredential spCredential;
    private final IdpRegistrationStore store;
    private final SamlRegistrationFactory registrationFactory;

    public IdpMetadataImportService(HarnessProperties properties, SpCredential spCredential,
                                     IdpRegistrationStore store, SamlRegistrationFactory registrationFactory) {
        this.properties = properties;
        this.spCredential = spCredential;
        this.store = store;
        this.registrationFactory = registrationFactory;
    }

    /**
     * @throws InvalidIdpMetadataException if idpMetadataXml can't be parsed as SAML metadata —
     *                                     the currently active IdP Registration is left untouched.
     */
    public IdpImportResult importMetadata(byte[] idpMetadataXml) {
        RelyingPartyRegistration registration;
        try {
            registration = registrationFactory.build(properties, spCredential, idpMetadataXml);
        } catch (Saml2Exception e) {
            throw new InvalidIdpMetadataException("Could not parse the supplied IdP Metadata: " + e.getMessage(), e);
        }
        store.replace(idpMetadataXml);
        return IdpImportResult.from(registration);
    }
}
