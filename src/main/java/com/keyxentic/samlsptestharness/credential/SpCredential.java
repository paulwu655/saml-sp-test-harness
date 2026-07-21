package com.keyxentic.samlsptestharness.credential;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

/**
 * The SP's own signing keypair and self-signed certificate, used to sign
 * AuthnRequests/LogoutRequests and embedded in exported SP Metadata.
 */
public record SpCredential(KeyPair keyPair, X509Certificate certificate) {
}
