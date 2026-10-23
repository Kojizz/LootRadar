package dev.lootradar.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
public class SecurityConfig {
    @Bean SecurityFilterChain security(HttpSecurity http,
            @Value("${GOOGLE_CLIENT_ID:}") String clientId,
            @Value("${GOOGLE_CLIENT_SECRET:}") String secret,
            @Value("${APP_BASE_URL:http://127.0.0.1:5173}") String base) throws Exception {
        http.authorizeHttpRequests(a -> a.anyRequest().permitAll());
        http.requestCache(c -> c.disable());
        http.logout(l -> l.logoutUrl("/api/auth/logout").logoutSuccessHandler((req,res,auth) -> res.setStatus(204)));
        if (!clientId.isBlank() && !secret.isBlank()) {
            var google = CommonOAuth2Provider.GOOGLE.getBuilder("google").clientId(clientId).clientSecret(secret)
                .scope("openid", "profile", "email").redirectUri(base + "/login/oauth2/code/google").build();
            http.oauth2Login(o -> o.clientRegistrationRepository(new InMemoryClientRegistrationRepository(google))
                .successHandler((req,res,auth) -> res.sendRedirect(base + "/"))
                .failureHandler((req,res,error) -> res.sendRedirect(base + "/?login=failed")));
        }
        return http.build();
    }
}
