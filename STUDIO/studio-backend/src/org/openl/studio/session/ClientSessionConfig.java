package org.openl.studio.session;

import java.time.Duration;
import jakarta.servlet.ServletContext;

import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the scope of one client of OpenL Studio, {@link ClientSessionScope}.
 */
@Configuration
public class ClientSessionConfig {

    /**
     * The state kept for the credentials of stateless requests, released after the idle time an HTTP session
     * of this application has.
     */
    @Bean
    public ClientSessions clientSessions(ServletContext servletContext) {
        return new ClientSessions(Duration.ofMinutes(servletContext.getSessionTimeout()));
    }

    @Bean
    public static CustomScopeConfigurer clientSessionScopeConfigurer(ClientSessions clientSessions) {
        var configurer = new CustomScopeConfigurer();
        configurer.addScope(ClientSessions.NAME, clientSessions);
        return configurer;
    }
}
