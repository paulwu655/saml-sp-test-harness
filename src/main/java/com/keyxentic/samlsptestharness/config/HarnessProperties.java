package com.keyxentic.samlsptestharness.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * The harness's single-registration identity, derived entirely from {@code harness.base-url}
 * (see ADR-0002-adjacent decision in CONTEXT.md / the spec: a single BASE_URL, not per-field config).
 */
@ConfigurationProperties(prefix = "harness")
public record HarnessProperties(String baseUrl, String dataDir) {

    public static final String REGISTRATION_ID = "harness";

    public String entityId() {
        return normalize(baseUrl);
    }

    public String assertionConsumerServiceLocation() {
        return normalize(baseUrl) + "/login/saml2/sso/" + REGISTRATION_ID;
    }

    public String singleLogoutServiceLocation() {
        return normalize(baseUrl) + "/logout/saml2/slo";
    }

    public Path spCredentialPath() {
        return Path.of(dataDir).resolve("sp-credential.p12");
    }

    private static String normalize(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
