package org.openl.studio.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.web.PathPatternRequestMatcherBuilderFactoryBean;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;

import org.openl.rules.webstudio.web.servlet.StudioDispatcherServlet;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean(initMethod = "afterPropertiesSet", destroyMethod = "destroy")
    public FilterChainProxy filterChainProxy(List<SecurityFilterChain> securityFilterChains) {
        return new FilterChainProxy(securityFilterChains);
    }

    @Bean
    PathPatternRequestMatcherBuilderFactoryBean requestMatcherBuilder() {
        return new PathPatternRequestMatcherBuilderFactoryBean();
    }

    // Static resource patterns with no filters
    @Bean
    @Order(0)
    public SecurityFilterChain staticResourcesFilterChain(HttpSecurity http) throws Exception {

        return http
                .securityMatchers(matchers -> matchers
                        .requestMatchers(
                                "/favicon.ico",
                                "/favicon.svg",
                                "/application.properties",
                                "/api-docs",
                                "/icons/**",
                                "/assets/**",
                                "/licenses/**",
                                "/.well-known/**",
                                StudioDispatcherServlet.REST_PATH + "/public/**",
                                StudioDispatcherServlet.REST_PATH + "/settings",
                                StudioDispatcherServlet.REST_PATH + "/api-docs",
                                StudioDispatcherServlet.REST_PATH + "/openapi.json"
                        )
                        // A page of a guide is the application page, guarded as every other page.
                        .requestMatchers(RequestMatchers.userGuideFiles()))
                // Disable any configurers and authentications for the static-like resources.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .securityContext(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)
                .exceptionHandling(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .headers(AbstractHttpConfigurer::disable)
                .servletApi(AbstractHttpConfigurer::disable)
                .build();
    }
}
