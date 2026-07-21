package com.keyxentic.samlsptestharness.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * No login screen, no request-level authorization: this harness is a disposable test tool for a
 * closed/local network, not a long-lived shared service. See docs/adr/0003-no-access-control.md.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .saml2Metadata(Customizer.withDefaults());
        return http.build();
    }
}
