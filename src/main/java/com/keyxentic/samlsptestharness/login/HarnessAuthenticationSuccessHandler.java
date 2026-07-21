package com.keyxentic.samlsptestharness.login;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class HarnessAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final LoginResultRenderer renderer;

    public HarnessAuthenticationSuccessHandler(LoginResultRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) authentication.getPrincipal();
        String rawResponseXml = LoginResultRenderer.rawResponseXml(request);

        ResultRecord result = ResultRecord.success(
                principal.getName(),
                NameIdFormatExtractor.extract(rawResponseXml),
                principal.getAttributes(),
                rawResponseXml);

        renderer.render(request, response, result);
    }
}
