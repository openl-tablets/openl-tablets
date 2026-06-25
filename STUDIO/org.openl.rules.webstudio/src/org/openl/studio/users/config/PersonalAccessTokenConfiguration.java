package org.openl.studio.users.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.openl.rules.security.standalone.dao.PersonalAccessTokenDao;
import org.openl.studio.users.service.pat.PersonalAccessTokenServiceImpl;

@Configuration
@ConditionalOnExpression("'${user.mode}' != 'single'")
public class PersonalAccessTokenConfiguration {

    @Bean
    public PersonalAccessTokenServiceImpl personalAccessTokenService(PersonalAccessTokenDao tokenDao) {
        return new PersonalAccessTokenServiceImpl(tokenDao);
    }

}
