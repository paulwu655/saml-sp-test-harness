package com.keyxentic.samlsptestharness.config;

import com.keyxentic.samlsptestharness.login.HarnessAuthenticationFailureHandler;
import com.keyxentic.samlsptestharness.login.HarnessAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * No login screen, no request-level authorization: this harness is a disposable test tool for a
 * closed/local network, not a long-lived shared service. See docs/adr/0003-no-access-control.md.
 * <p>
 * {@code saml2Login()} handles both the SP-initiated AuthnRequest (GET /saml2/authenticate/{id})
 * and the ACS callback (POST /login/saml2/sso/{id}) — the latter also accepts unsolicited,
 * IdP-initiated Responses natively, since it doesn't require a matching InResponseTo.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            HarnessAuthenticationSuccessHandler successHandler,
                                            HarnessAuthenticationFailureHandler failureHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .saml2Metadata(Customizer.withDefaults())
                .saml2Login(saml2 -> saml2
                        .successHandler(successHandler)
                        .failureHandler(failureHandler));
        return http.build();
    }
}
