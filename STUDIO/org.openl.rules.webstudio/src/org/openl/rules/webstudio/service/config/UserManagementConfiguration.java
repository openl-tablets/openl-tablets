package org.openl.rules.webstudio.service.config;

import org.openl.rules.security.standalone.dao.ExternalGroupDao;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.webstudio.service.ExternalGroupService;
import org.openl.rules.webstudio.service.ExternalGroupServiceImpl;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.security.acl.JdbcMutableAclService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    public ExternalGroupService externalGroupService(ExternalGroupDao externalGroupDao) {
        return new ExternalGroupServiceImpl(externalGroupDao);
    }

    @Bean("groupManagementService")
    public GroupManagementService groupManagementService(GroupDao groupDao,
                                                         @Autowired(required = false) JdbcMutableAclService aclService) {
        return new GroupManagementService(groupDao, aclService);
    }

}
