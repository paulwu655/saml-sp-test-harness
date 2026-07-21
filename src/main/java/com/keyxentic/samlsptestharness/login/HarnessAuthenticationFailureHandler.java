package com.keyxentic.samlsptestharness.login;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * A failed Login Test must still produce a readable Result Record — never an unhandled error
 * reaching the browser.
 */
@Component
public class HarnessAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final LoginResultRenderer renderer;

    public HarnessAuthenticationFailureHandler(LoginResultRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {
        String rawResponseXml = LoginResultRenderer.rawResponseXml(request);
        Saml2Error error = exception instanceof Saml2AuthenticationException saml2Exception
                ? saml2Exception.getSaml2Error() : null;

        ResultRecord result = ResultRecord.failure(
                signatureValidation(error),
                timeValidityValidation(error),
                rawResponseXml,
                describe(error, exception));

        renderer.render(request, response, result);
    }

    private ValidationOutcome signatureValidation(Saml2Error error) {
        if (error == null) {
            return ValidationOutcome.notEvaluated();
        }
        // Spring Security doesn't guarantee signature validation always runs before other
        // checks, so a non-signature error code doesn't prove the signature actually passed.
        return Saml2ErrorCodes.INVALID_SIGNATURE.equals(error.getErrorCode())
                ? ValidationOutcome.failed(error.getDescription())
                : ValidationOutcome.notEvaluated();
    }

    private ValidationOutcome timeValidityValidation(Saml2Error error) {
        if (error == null || Saml2ErrorCodes.INVALID_SIGNATURE.equals(error.getErrorCode())) {
            return ValidationOutcome.notEvaluated();
        }
        boolean looksTimeRelated = Saml2ErrorCodes.INVALID_ASSERTION.equals(error.getErrorCode())
                && error.getDescription() != null
                && (error.getDescription().toLowerCase().contains("notbefore")
                    || error.getDescription().toLowerCase().contains("notonorafter")
                    || error.getDescription().toLowerCase().contains("expired")
                    || error.getDescription().toLowerCase().contains("time"));
        return looksTimeRelated ? ValidationOutcome.failed(error.getDescription()) : ValidationOutcome.notEvaluated();
    }

    private String describe(Saml2Error error, AuthenticationException exception) {
        if (error != null) {
            return "[" + error.getErrorCode() + "] " + error.getDescription();
        }
        return exception.getMessage();
    }
}
