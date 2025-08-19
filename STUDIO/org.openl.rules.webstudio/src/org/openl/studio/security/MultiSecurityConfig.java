package org.openl.studio.security;

import java.util.Collection;
import java.util.function.BiFunction;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.webstudio.service.AdminUsers;
import org.openl.rules.webstudio.service.UserInfoUserDetailsServiceImpl;

@Configuration
@ConditionalOnExpression("'${user.mode}' == 'multi'")
public class MultiSecurityConfig {

    @Bean
    public Boolean canCreateInternalUsers() {
        return Boolean.TRUE;
    }

    // ========================== Internal users authentication ===============================

    @Bean
    public UserDetailsService userDetailsService(
            @Qualifier("openlUserDao") UserDao userDao,
            @Qualifier("adminUsersInitializer") AdminUsers adminUsersInitializer,
            @Qualifier("privilegeMapper") BiFunction<String, Collection<? extends GrantedAuthority>, Collection<GrantedAuthority>> privilegeMapper) {

        return new UserInfoUserDetailsServiceImpl(userDao, adminUsersInitializer, privilegeMapper);
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
            @Qualifier("passwordEncoder") PasswordEncoder passwordEncoder,
            @Qualifier("userDetailsService") UserDetailsService userDetailsService) {

        var provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

}
