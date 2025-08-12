package org.openl.studio.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import org.openl.rules.security.Privileges;

@Configuration
@ConditionalOnExpression("'${user.mode}' == 'single'")
public class SingleSecurityConfig {

    @Bean
    public Boolean canCreateInternalUsers() {
        return Boolean.FALSE;
    }

    @Bean
    // Create security filter chain for /** pattern with filters
    public SecurityFilterChain defaultFilterChain(HttpSecurity http,
                                                  @Value("${security.single.username}") String singleUsername) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .anonymous(anon -> anon
                        .principal(singleUsername)
                        .authorities(Privileges.ADMIN.getAuthority()))
                .build();

    }

}
