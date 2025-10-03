package org.openl.rules.ruleservice.spring;

import com.example.auth.BasicAuthChecker;
import com.example.auth.ClientAccessDeniedHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class AuthExtensionConfiguration {
    @Bean
    @Order(1)
    public BasicAuthChecker basicAuthChecker(@Value("${example.auth.username}") String username, @Value("${example.auth.password}") String password) {
        return new BasicAuthChecker(username, password);
    }
    
    @Bean
    @Order(0)
    public ClientAccessDeniedHandler clientAccessDeniedHandler() {
        return new ClientAccessDeniedHandler();
    }
}
