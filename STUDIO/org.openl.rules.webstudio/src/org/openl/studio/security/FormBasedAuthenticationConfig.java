package org.openl.studio.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import org.openl.studio.security.pat.filter.PatAuthenticationFilter;

@Configuration
@ConditionalOnExpression("'${user.mode}' == 'ad' || '${user.mode}' == 'multi'")
public class FormBasedAuthenticationConfig {

    // REST endpoints - also accept Personal Access Tokens
    @Bean
    @Order(1)
    public SecurityFilterChain restEndpointsFilterChain(HttpSecurity http,
                                                        PatAuthenticationFilter patAuthenticationFilter) throws Exception {
        return http
                .securityMatcher("/rest/**")
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .httpBasic(basic -> basic.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(patAuthenticationFilter, BasicAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .build();
    }

    // Web endpoints
    @Bean
    @Order(2)
    public SecurityFilterChain webEndpointsFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/web/**")
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .httpBasic(basic -> basic.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .build();
    }

    // All other patterns - catch-all
    @Bean
    @Order(3)
    public SecurityFilterChain defaultFilterChain(
            HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(form -> form.loginPage("/login").permitAll())
                .logout(logout -> logout.logoutUrl("/logout").permitAll())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .build();
    }
}
