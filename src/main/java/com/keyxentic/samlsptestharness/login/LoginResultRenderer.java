package com.keyxentic.samlsptestharness.login;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.Base64;

/** Renders a {@link ResultRecord} as the direct response to the ACS request — nothing is persisted. */
@Component
public class LoginResultRenderer {

    private final TemplateEngine templateEngine;

    public LoginResultRenderer(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public void render(HttpServletRequest request, HttpServletResponse response, ResultRecord result) throws IOException {
        Context context = new Context(request.getLocale());
        context.setVariable("result", result);
        String html = templateEngine.process("result", context);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(html);
    }

    public static String rawResponseXml(HttpServletRequest request) {
        String samlResponse = request.getParameter("SAMLResponse");
        if (samlResponse == null) {
            return null;
        }
        try {
            return new String(Base64.getDecoder().decode(samlResponse), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // Not valid Base64 at all — there's no raw XML to corroborate the failure with.
            return null;
        }
    }
}
