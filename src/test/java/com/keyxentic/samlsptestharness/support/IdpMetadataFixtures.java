package com.keyxentic.samlsptestharness.support;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

/** Builds minimal, valid SAML 2.0 IdP Metadata XML for tests, independent of production code. */
public final class IdpMetadataFixtures {

    private IdpMetadataFixtures() {
    }

    public static String validIdpMetadata(String entityId, String ssoLocation, String sloLocation, X509Certificate signingCertificate) {
        String certBase64 = base64(signingCertificate);
        String sloElement = sloLocation == null ? "" : """
                <md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="%s"/>
                """.formatted(sloLocation);
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <md:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" entityID="%s">
                    <md:IDPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
                        <md:KeyDescriptor use="signing">
                            <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
                                <ds:X509Data>
                                    <ds:X509Certificate>%s</ds:X509Certificate>
                                </ds:X509Data>
                            </ds:KeyInfo>
                        </md:KeyDescriptor>
                        %s
                        <md:SingleSignOnService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="%s"/>
                    </md:IDPSSODescriptor>
                </md:EntityDescriptor>
                """.formatted(entityId, certBase64, sloElement, ssoLocation);
    }

    public static String malformedMetadata() {
        return "<this is not even close to valid SAML metadata>";
    }

    private static String base64(X509Certificate certificate) {
        try {
            return Base64.getEncoder().encodeToString(certificate.getEncoded());
        } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
