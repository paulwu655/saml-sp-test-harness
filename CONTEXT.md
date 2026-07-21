# SAML SP Test Harness

A standalone, dockerized SAML 2.0 Service Provider used to test how a given Identity Provider (IdP) behaves in an SSO exchange — export this SP's metadata, import an IdP's metadata, run a login, and inspect the result. It exists independently of `keyperdemobackend`; it does not share code with it and is not a replacement for `keyperdemobackend`'s own SAML implementation.

## Language

**SP (Service Provider)**:
This harness itself, acting as the SAML Service Provider under test. There is exactly one SP identity per running instance, derived from `BASE_URL`.
_Avoid_: Relying Party (framework-internal term, not used in this project's vocabulary), client

**IdP (Identity Provider)**:
The external system being tested against (e.g. Okta, Azure AD, Keycloak, Keyper). The harness holds metadata for exactly one IdP at a time.
_Avoid_: Identity server, auth provider

**IdP Metadata**:
The XML document describing an IdP's SSO/SLO endpoints and signing certificate, supplied by the user via file upload or pasted text. Importing new IdP Metadata replaces whatever was previously configured — there is no history of prior imports.
_Avoid_: IdP config, IdP descriptor

**SP Metadata**:
The XML document this harness generates describing its own entityId, ACS endpoint, and signing certificate, for the user to hand to the IdP being tested. Exported on demand, never uploaded/imported.

**BASE_URL**:
The single environment variable that anchors this SP's identity — its entityId, ACS URL, and SLO URL are all derived from it. Must be a URL the IdP can actually reach back to (a real domain, or a tunneling URL like ngrok for local testing).

**SP Credential**:
The RSA key pair and self-signed certificate this SP uses to sign AuthnRequests/LogoutRequests. Generated automatically on first boot and persisted to the mounted volume thereafter; can be overridden by supplying a credential via `.env`/volume instead. Never exposed for download through the UI.

**Login Test**:
A full SP-initiated or IdP-initiated SSO round trip against the currently configured IdP, ending in a Result Record. This is the harness's core function — metadata exchange alone does not validate that a login actually works.

**Result Record**:
The outcome of a single Login Test: pass/fail status, the decoded NameID, signature validation outcome, time-validity (IssueInstant/NotOnOrAfter) validation outcome, the IdP's returned Attributes, and the raw Response XML. Not persisted across restarts.
