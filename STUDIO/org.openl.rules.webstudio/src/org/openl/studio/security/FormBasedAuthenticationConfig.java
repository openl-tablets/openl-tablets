package org.openl.studio.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

@Configuration
@ConditionalOnExpression("'${user.mode}' == 'ad' || '${user.mode}' == 'multi'")
public class FormBasedAuthenticationConfig {

    // REST endpoints
    @Bean
    @Order(1)
    public SecurityFilterChain restEndpointsFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/rest/**", "/web/**")
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .httpBasic(basic -> basic.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .build();
    }

    // All other patterns - catch-all
    @Bean
    @Order(2)
    public SecurityFilterChain defaultFilterChain(
            @Qualifier("loginUrl") String loginUrl,
            @Qualifier("logoutUrl") String logoutUrl,
            HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(form -> form.loginPage(loginUrl).permitAll())
                .logout(logout -> logout.logoutUrl(logoutUrl).permitAll())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .build();
    }

    @Bean
    public String loginUrl() {
        return "/login";
    }

    @Bean
    public String logoutUrl() {
        return "/logout";
    }

    @Bean
    public LogoutHandler logoutHandler() {
        return new SecurityContextLogoutHandler();
    }
}
