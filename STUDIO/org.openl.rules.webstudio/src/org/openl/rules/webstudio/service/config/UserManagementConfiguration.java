package org.openl.rules.webstudio.service.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.openl.rules.security.Privileges;
import org.openl.rules.security.standalone.dao.ExternalGroupDao;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.webstudio.service.AdminUsers;
import org.openl.rules.webstudio.service.ExternalGroupService;
import org.openl.rules.webstudio.service.ExternalGroupServiceImpl;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.security.acl.JdbcMutableAclService;
import org.openl.studio.security.GetUserPrivileges;

/**
 * User management configuration beans.
 *
 * @author Vladyslav Pikus
 */
@Configuration
@ImportResource("classpath:META-INF/standalone/spring/security-hibernate-beans.xml")
public class UserManagementConfiguration {

    @Bean("userManagementService")
    public UserManagementService userManagementService(UserDao userDao,
                                                       GroupDao groupDao,
                                                       SessionRegistry sessionRegistry,
                                                       PasswordEncoder passwordEncoder,
                                                       @Autowired(required = false) JdbcMutableAclService aclService) {
        return new UserManagementService(userDao, groupDao, sessionRegistry, passwordEncoder, aclService);
    }

    @Bean("externalGroupService")
    public ExternalGroupService externalGroupService(ExternalGroupDao externalGroupDao,
                                                     LockRegistry lockRegistry,
                                                     PlatformTransactionManager txManager) {
        return new ExternalGroupServiceImpl(externalGroupDao, lockRegistry, new TransactionTemplate(txManager));
    }

    @Bean("groupManagementService")
    public GroupManagementService groupManagementService(GroupDao groupDao,
                                                         @Autowired(required = false) JdbcMutableAclService aclService) {
        return new GroupManagementService(groupDao, aclService);
    }

    @Bean("privilegeMapper")
    @ConditionalOnExpression("'${user.mode}' != 'multi' and '${user.mode}' != 'single'")
    public BiFunction<String, Collection<? extends GrantedAuthority>, Collection<GrantedAuthority>> groupPrivilegeMapper(
            UserManagementService userManagementService,
            GroupManagementService groupManagementService) {
        return new GetUserPrivileges(userManagementService, groupManagementService);
    }

    @Bean("privilegeMapper")
    @ConditionalOnExpression("'${user.mode}' == 'multi'")
    public BiFunction<String, Collection<? extends GrantedAuthority>, Collection<GrantedAuthority>> multiUserPrivilegeMapper(
            AdminUsers adminUsers) {
        return (user, authorities) -> {
            if (adminUsers.isSuperuser(user)) {
                return List.of(Privileges.ADMIN);
            }
            return Collections.emptyList();
        };
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

}
