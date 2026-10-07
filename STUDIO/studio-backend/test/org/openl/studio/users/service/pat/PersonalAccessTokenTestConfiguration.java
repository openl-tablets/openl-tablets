package org.openl.studio.users.service.pat;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.openl.rules.security.standalone.dao.PersonalAccessTokenDao;

/**
 * Test configuration for Personal Access Token service.
 */
@Configuration
public class PersonalAccessTokenTestConfiguration {

    @Bean
    public PersonalAccessTokenService personalAccessTokenService(PersonalAccessTokenDao tokenDao) {
        return new PersonalAccessTokenServiceImpl(tokenDao);
    }
}
