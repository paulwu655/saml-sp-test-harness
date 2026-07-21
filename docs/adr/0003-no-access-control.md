# No access control on the harness UI/API

The harness's frontend can export SP metadata, accept IdP metadata, and trigger login tests, but has no authentication in front of it. This is a deliberate choice, not an oversight: the SP credential the harness generates is a self-signed test certificate, not a production trust root, and the harness is meant to be started with `docker-compose up`, used for a test session, and torn down again on a developer machine or an isolated test network — not left running as a shared, long-lived service. Adding login would work against the "one command, immediately usable" experience this tool is meant to have.

**Consequence**: this harness must not be deployed on a network reachable by untrusted parties. If a shared/long-lived deployment is ever needed, access control should be added before that happens — it was left out for the single-user/local-test use case, not because it's unnecessary in general.
