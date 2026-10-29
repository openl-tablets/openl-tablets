package org.openl.studio.session;

import java.time.Duration;
import jakarta.servlet.ServletContext;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import org.openl.rules.webstudio.web.admin.security.AuthenticationSettings;
import org.openl.rules.webstudio.web.admin.security.UserMode;

/**
 * Registers the scope of one client of OpenL Studio, {@link ClientSessionScope}.
 */
@Configuration
public class ClientSessionConfig {

    /**
     * The state kept for the credentials of stateless requests, released after the idle time an HTTP session
     * of this application has. In single-user mode, it is the state of every request. The listeners hear of the
     * clients that start and end, as the audit log does.
     */
    @Bean
    public ClientSessions clientSessions(ServletContext servletContext,
                                         Environment environment,
                                         ObjectProvider<ClientSessionListener> listeners) {
        var singleUser = UserMode.SINGLE.getValue().equals(environment.getProperty(AuthenticationSettings.USER_MODE));
        return new ClientSessions(Duration.ofMinutes(servletContext.getSessionTimeout()), singleUser, listeners);
    }

    @Bean
    public static CustomScopeConfigurer clientSessionScopeConfigurer(ClientSessions clientSessions) {
        var configurer = new CustomScopeConfigurer();
        configurer.addScope(ClientSessions.NAME, clientSessions);
        return configurer;
    }
}
