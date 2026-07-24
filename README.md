# SAML SP Test Harness

A standalone, dockerized SAML 2.0 Service Provider used to test how a given Identity Provider (IdP) behaves in an SSO exchange — export this SP's metadata, import an IdP's metadata, run a login, and inspect the result.

It exists independently of `keyperdemobackend`; it shares no code or deployment with it (see `docs/adr/0002-standalone-repo-not-shared-module.md`).

Domain vocabulary (SP, IdP, IdP Metadata, SP Metadata, `BASE_URL`, SP Credential, Login Test, Result Record) is defined in [`CONTEXT.md`](./CONTEXT.md).

## Prerequisites

- Docker and Docker Compose
- A way for the IdP under test to reach this SP over HTTP(S) — either a shared network (e.g. both containers on the same host/Docker network) or a tunneling URL (e.g. [ngrok](https://ngrok.com)) if the IdP is an external SaaS provider (Okta, Azure AD, etc.) and this harness is running on your local machine.

## Quick start

1. **Configure `BASE_URL`**

   ```bash
   cp harness.env.example harness.env
   ```

   Edit `harness.env` and set `BASE_URL` to a URL the IdP can actually reach back to. This single value derives the SP's entityId, ACS URL, and SLO URL — everything else is optional.

   - Testing against an IdP on the same machine/network (e.g. a local Keycloak): use whatever host/port the IdP can resolve.
   - Testing against an external IdP (Okta, Azure AD, Keyper, ...): `localhost` won't work — use a tunnel URL instead, e.g. `BASE_URL=https://xxxx.ngrok.io`.

   `PORT` only controls the host-side port mapping; the container always listens on `8080` internally.

   The env file is deliberately named `harness.env`, not `.env` — Compose only auto-loads a file literally named `.env`, and this repo may end up living alongside other `docker-compose.yml` projects that each want their own env file without colliding. Because of that, every `docker compose` command below needs `--env-file harness.env` explicitly; without it, Compose will refuse to start (`BASE_URL` has no default).

2. **Start the harness**

   ```bash
   docker compose --env-file harness.env up --build
   ```

   On first boot, an SP Credential (RSA keypair + self-signed certificate) is generated automatically and persisted at `sp-credential.p12`, alongside the imported IdP Metadata. Subsequent restarts reuse the same credential — the SP Metadata you hand to the IdP stays valid across restarts. To supply your own credential instead, place a PKCS12 keystore at that path (alias `sp`, password `sp-credential`) before first boot; the harness loads it instead of generating a new one.

   By default this state lives in a Docker-managed named volume (`harness-data`), which isn't a path you can point at directly on the host. To control (or migrate) exactly where it lives on disk, set `DATA_PATH` in `harness.env` to an absolute host path instead — the directory is created automatically on first boot. To move an existing named volume's contents into that path first: `docker run --rm -v harness-data:/from -v /your/chosen/path:/to alpine sh -c "cp -a /from/. /to/"`.

3. **Open the UI**

   Visit `BASE_URL` in a browser. The home page shows the SP's Entity ID and ACS URL, and a link to export SP Metadata.

4. **Register this SP with the IdP**

   Click **Export SP Metadata** and hand the resulting XML to the IdP's administrator to register.

5. **Import the IdP's metadata**

   Get the IdP's metadata (a file, or pasted XML) and use the **Import IdP Metadata** form on the home page — either upload a file or paste the XML text. Importing new metadata replaces whatever IdP was previously configured; only one IdP is held at a time.

6. **Run a Login Test**

   Once an IdP is configured, a **Start Login Test** link appears:

   - **SP-initiated**: click it to kick off the standard flow — you're redirected to the IdP, log in, and are redirected back to the ACS endpoint.
   - **IdP-initiated**: instead of using that link, point the IdP's own SSO portal directly at this SP's ACS URL — the same endpoint accepts unsolicited (IdP-initiated) responses natively.

   Either way, the result is a **Result Record**: pass/fail status, decoded NameID, signature validation outcome, time-validity validation outcome, the IdP's returned Attributes, and the raw Response XML (collapsible). Failed logins (bad signature, expired assertion, malformed response) also produce a Result Record explaining the failure, rather than an error page.

## Notes

- No login is required to use the harness's UI or API — this is a deliberate scope decision for a disposable, single-user test tool, not an oversight. See `docs/adr/0003-no-access-control.md`. **Do not deploy this on a network reachable by untrusted parties.**
- State (the current IdP Registration and the SP Credential) is file/volume-based — there is no database.
- Further design background lives in `docs/adr/`.

## Development

Run tests:

```bash
mvn test
```

Tests use a `MockMvc`-driven integration seam with signed SAML fixtures standing in for a real IdP — no live IdP or browser automation is needed.
