# Standalone repo, not a module shared with keyperdemobackend

The request to "extract SAML SP functionality" could have meant pulling it into a module that `keyperdemobackend` still depends on. We chose instead to build `saml-sp-test-harness` as a fully independent repo that shares no code with `keyperdemobackend`. The two have different concerns: `keyperdemobackend`'s SAML SP is Keyper-specific and hardcodes assumptions about its own IdP, while this harness needs to be IdP-agnostic, disposable, and configured per-test-run via `.env`. Coupling them via a shared module would tie the test tool's evolution to the production app's, and vice versa.

**Consequence**: `keyperdemobackend`'s existing `SAMLController` is untouched by this work and continues to carry its own bugs (disabled signature validation, no IdP metadata parsing) independently of anything built here.
