package org.openl.studio.security;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;

import org.openl.rules.security.SimpleUser;
import org.openl.studio.security.ad.LdapToOpenLUserDetailsMapper;

@Configuration
@ConditionalOnExpression("'${user.mode}' == 'ad'")
public class AdSecurityConfig {

    // ============================ Define needed beans for dependencies ======================================

    @Bean
    public Boolean canCreateInternalUsers() {
        return Boolean.FALSE;
    }

    // ================================= Authentication =======================================

    @Bean
    public ActiveDirectoryLdapAuthenticationProvider adAuthenticationProvider(
            @Value("${security.ad.domain}") String domain,
            @Value("${security.ad.server-url}") String serverUrl,
            @Value("${security.ad.search-filter:#{null}}") String searchFilter,
            @Qualifier("userDetailsContextMapper") LdapToOpenLUserDetailsMapper userDetailsContextMapper) {

        var provider = new ActiveDirectoryLdapAuthenticationProvider(domain, serverUrl);
        if (searchFilter != null) {
            provider.setSearchFilter(searchFilter);
        }
        provider.setUserDetailsContextMapper(userDetailsContextMapper);
        return provider;
    }

    @Bean
    public LdapToOpenLUserDetailsMapper userDetailsContextMapper(@Qualifier("syncUserData") Consumer<SimpleUser> syncUserData,
                                                                 @Qualifier("privilegeMapper") BiFunction<String, Collection<? extends GrantedAuthority>, Collection<GrantedAuthority>> privilegeMapper,
                                                                 Environment environment) {
        var delegate = new LdapUserDetailsMapper();
        return new LdapToOpenLUserDetailsMapper(
                delegate,
                syncUserData,
                environment,
                privilegeMapper
        );
    }
}
