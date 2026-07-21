package com.keyxentic.samlsptestharness.idp;

import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;

/** What to show the operator after a successful IdP Metadata import. */
public record IdpImportResult(String entityId, String singleSignOnServiceLocation, String singleLogoutServiceLocation) {

    public static IdpImportResult from(RelyingPartyRegistration registration) {
        var idp = registration.getAssertingPartyMetadata();
        return new IdpImportResult(idp.getEntityId(), idp.getSingleSignOnServiceLocation(), idp.getSingleLogoutServiceLocation());
    }
}
