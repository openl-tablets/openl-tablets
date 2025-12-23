package org.openl.studio.security.pat.config;

import java.time.Clock;
import java.util.Collection;
import java.util.function.BiFunction;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.openl.rules.security.standalone.dao.PersonalAccessTokenDao;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.webstudio.service.AdminUsers;
import org.openl.rules.webstudio.service.ExternalGroupService;
import org.openl.studio.security.pat.filter.PatAuthenticationFilter;
import org.openl.studio.security.pat.service.PatAuthService;
import org.openl.studio.security.pat.service.PatAuthServiceImpl;
import org.openl.studio.security.pat.service.PatGeneratorServiceImpl;
import org.openl.studio.security.pat.service.PatUserInfoUserDetailsServiceImpl;
import org.openl.studio.security.pat.service.PatValidationService;
import org.openl.studio.security.pat.service.PatValidationServiceImpl;
import org.openl.studio.users.service.pat.PersonalAccessTokenService;

/**
 * Spring Security configuration for Personal Access Token (PAT) authentication.
 * <p>
 * This configuration is only active when the user mode is OAuth2 or SAML. It configures
 * all beans required for PAT authentication including services and filters.
 * </p>
 * <p>
 * The PAT authentication flow:
 * <ol>
 *   <li>{@link PatAuthenticationFilter} extracts and parses PAT from Authorization header</li>
 *   <li>{@link PatValidationService} validates the token cryptographically</li>
 *   <li>{@link PatAuthService} converts valid token to Spring Security authentication</li>
 *   <li>{@link PatUserInfoUserDetailsServiceImpl} loads user details with external groups</li>
 * </ol>
 * </p>
 *
 * @since 6.0.0
 */
@Configuration
@ConditionalOnExpression("'${user.mode}' == 'oauth2' || '${user.mode}' == 'saml'")
public class PatSecurityConfiguration {

    /**
     * Creates the PAT authentication service bean.
     *
     * @param validator          the token validation service
     * @param userDetailsService the user details service (should be patUserInfoUserDetailsService)
     * @return configured PAT authentication service
     */
    @Bean
    public PatAuthServiceImpl patAuthService(PatValidationService validator,
                                             UserDetailsService userDetailsService) {
        return new PatAuthServiceImpl(validator, userDetailsService);
    }

    /**
     * Creates the PAT generator service bean.
     *
     * @param crudService the PAT CRUD service
     * @param passwordEncoder the password encoder for hashing secrets
     * @param clock the clock for generating timestamps
     * @return configured PAT generator service
     */
    @Bean
    public PatGeneratorServiceImpl patGeneratorService(PersonalAccessTokenService crudService,
                                                       PasswordEncoder passwordEncoder,
                                                       Clock clock) {
        return new PatGeneratorServiceImpl(crudService, passwordEncoder, clock);
    }

    /**
     * Creates the PAT validation service bean.
     *
     * @param tokenDao the DAO for accessing stored tokens
     * @param passwordEncoder the password encoder for verifying secrets
     * @param clock the clock for checking expiration
     * @return configured PAT validation service
     */
    @Bean
    public PatValidationServiceImpl patValidationService(PersonalAccessTokenDao tokenDao,
                                                         PasswordEncoder passwordEncoder,
                                                         Clock clock) {
        return new PatValidationServiceImpl(tokenDao, passwordEncoder, clock);
    }

    /**
     * Creates the PAT-specific UserDetailsService bean.
     * <p>
     * This service loads user details with external group privileges, ensuring that
     * PAT-authenticated users have the same authorities as OAuth2/SAML-authenticated users.
     * </p>
     *
     * @param userDao the user DAO
     * @param adminUsersInitializer the admin users initializer
     * @param privilegeMapper the privilege mapping function
     * @param externalGroupService the external group service
     * @return configured UserDetailsService for PAT authentication
     */
    @Bean
    public PatUserInfoUserDetailsServiceImpl patUserInfoUserDetailsService(@Qualifier("openlUserDao") UserDao userDao,
                                                                           @Qualifier("adminUsersInitializer") AdminUsers adminUsersInitializer,
                                                                           @Qualifier("privilegeMapper") BiFunction<String, Collection<? extends GrantedAuthority>, Collection<GrantedAuthority>> privilegeMapper,
                                                                           ExternalGroupService externalGroupService) {
        return new PatUserInfoUserDetailsServiceImpl(userDao, adminUsersInitializer, privilegeMapper, externalGroupService);
    }

    /**
     * Creates the PAT authentication filter bean.
     * <p>
     * This filter processes requests with "Authorization: Token &lt;pat&gt;" header
     * and should be placed before bearer token authentication in the security filter chain.
     * </p>
     *
     * @param patAuthService the PAT authentication service
     * @return configured PAT authentication filter
     */
    @Bean
    public PatAuthenticationFilter patAuthenticationFilter(PatAuthService patAuthService) {
        return new PatAuthenticationFilter(patAuthService);
    }

}
