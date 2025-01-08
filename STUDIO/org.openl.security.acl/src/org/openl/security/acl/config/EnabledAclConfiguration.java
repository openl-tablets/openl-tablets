package org.openl.security.acl.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.ConsoleAuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionFactory;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
import org.springframework.security.acls.jdbc.BasicLookupStrategy;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.openl.security.acl.AclAuthorizationStrategyImpl;
import org.openl.security.acl.JdbcMutableAclService;
import org.openl.security.acl.MaskPermissionGrantingStrategy;
import org.openl.security.acl.oid.AclObjectIdentityProviderImpl;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.RepositoryAclServiceImpl;
import org.openl.security.acl.repository.SimpleRepositoryAclService;
import org.openl.security.acl.repository.SimpleRepositoryAclServiceImpl;

/**
 * Configuration for ACL services that are enabled.
 * Imported by {@link AclImportSelector}.
 */
@Configuration
@ImportResource("classpath:META-INF/standalone/spring/security-hibernate-beans.xml")
public class EnabledAclConfiguration {

    private static final String ACL_CACHE_NAME = "aclCache";

    private static final boolean ALC_CLASS_ID_SUPPORTED = true;

    private static final GrantedAuthoritySid RELEVANT_SYSTEM_WIDE_SID = new GrantedAuthoritySid("ADMIN");

    private static final String DESIGN_REPO_ROOT_ID = "1";
    private static final String PROD_REPO_ROOT_ID = "3";

    @Bean
    public PermissionGrantingStrategy maskPermissionGrantingStrategy() {
        return new MaskPermissionGrantingStrategy(new ConsoleAuditLogger());
    }

    @Bean
    public AclCache aclCache(CacheManager cacheManager,
                             AclAuthorizationStrategy aclAuthorizationStrategy,
                             PermissionGrantingStrategy permissionGrantingStrategy) {
        var cache = cacheManager.getCache(ACL_CACHE_NAME);

        return new SpringCacheBasedAclCache(cache,
                permissionGrantingStrategy,
                aclAuthorizationStrategy);
    }

    @Bean
    public AclAuthorizationStrategy aclAuthorizationStrategy(SidRetrievalStrategy sidRetrievalStrategy) {
        var strategy = new AclAuthorizationStrategyImpl(new SimpleGrantedAuthority("ADMIN"));
        strategy.setSidRetrievalStrategy(sidRetrievalStrategy);
        return strategy;
    }

    @Bean
    public SidRetrievalStrategy sidRetrievalStrategy(RoleHierarchy roleHierarchy) {
        return new SidRetrievalStrategyImpl(roleHierarchy);
    }

    @Bean
    public JdbcMutableAclService repositoryJdbcMutableAclService(DataSource openlDataSource,
                                                                 AclCache aclCache,
                                                                 AclAuthorizationStrategy aclAuthorizationStrategy,
                                                                 PermissionGrantingStrategy permissionGrantingStrategy,
                                                                 DefaultPermissionFactory repositoryPermissionFactory,
                                                                 @Value("${db.url}") String jdbcUrl) {

        var lookupStrategy = createRepositoryAclLookupStrategy(openlDataSource,
                aclCache,
                aclAuthorizationStrategy,
                repositoryPermissionFactory,
                permissionGrantingStrategy);
        var mutableAclService = new JdbcMutableAclService(openlDataSource,
                lookupStrategy,
                aclCache,
                RELEVANT_SYSTEM_WIDE_SID);
        mutableAclService.setAclClassIdSupported(ALC_CLASS_ID_SUPPORTED);

        if (jdbcUrl.startsWith("jdbc:postgresql")) {
            mutableAclService.setClassIdentityQuery("select currval(pg_get_serial_sequence('acl_class', 'id'))");
            mutableAclService.setSidIdentityQuery("select currval(pg_get_serial_sequence('acl_sid', 'id'))");
        } else if (jdbcUrl.startsWith("jdbc:mysql")) {
            mutableAclService.setClassIdentityQuery("select @@IDENTITY");
            mutableAclService.setSidIdentityQuery("select @@IDENTITY");
        } else if (jdbcUrl.startsWith("jdbc:oracle")) {
            mutableAclService.setClassIdentityQuery("select acl_class_sequence.currval from dual");
            mutableAclService.setSidIdentityQuery("select acl_sid_sequence.currval from dual");
        } else if (jdbcUrl.startsWith("jdbc:sqlserver")) {
            mutableAclService.setClassIdentityQuery("select ident_current('acl_class')");
            mutableAclService.setSidIdentityQuery("select ident_current('acl_sid')");
        } else if (jdbcUrl.startsWith("jdbc:h2")) {
            mutableAclService.setClassIdentityQuery("select currval('acl_class_sequence')");
            mutableAclService.setSidIdentityQuery("select currval('acl_sid_sequence')");
        }

        return mutableAclService;
    }

    private BasicLookupStrategy createRepositoryAclLookupStrategy(DataSource openlDataSource,
                                                                  AclCache aclCache,
                                                                  AclAuthorizationStrategy aclAuthorizationStrategy,
                                                                  DefaultPermissionFactory repositoryPermissionFactory,
                                                                  PermissionGrantingStrategy permissionGrantingStrategy) {
        var strategy = new BasicLookupStrategy(openlDataSource,
                aclCache,
                aclAuthorizationStrategy,
                permissionGrantingStrategy);
        strategy.setAclClassIdSupported(ALC_CLASS_ID_SUPPORTED);
        strategy.setPermissionFactory(repositoryPermissionFactory);
        return strategy;
    }

    @Bean
    public DefaultPermissionFactory repositoryPermissionFactory() {
        return new DefaultPermissionFactory(AclPermission.class);
    }

    @Bean
    public AclPermissionEvaluator aclPermissionEvaluator(JdbcMutableAclService repositoryJdbcMutableAclService,
                                                         DefaultPermissionFactory repositoryPermissionFactory,
                                                         SidRetrievalStrategy sidRetrievalStrategy,
                                                         DefaultMethodSecurityExpressionHandler expressionHandler) {
        var permissionEvaluator = new AclPermissionEvaluator(repositoryJdbcMutableAclService);
        permissionEvaluator.setPermissionFactory(repositoryPermissionFactory);
        permissionEvaluator.setSidRetrievalStrategy(sidRetrievalStrategy);

        expressionHandler.setPermissionEvaluator(permissionEvaluator);
        return permissionEvaluator;
    }

    @Bean
    public RepositoryAclService designRepositoryAclService(AclCache aclCache,
                                                           JdbcMutableAclService repositoryJdbcMutableAclService,
                                                           SidRetrievalStrategy sidRetrievalStrategy) {
        var oidProvider = new AclObjectIdentityProviderImpl(org.openl.security.acl.repository.ProjectArtifact.class,
                DESIGN_REPO_ROOT_ID);
        return new RepositoryAclServiceImpl(aclCache,
                repositoryJdbcMutableAclService,
                RELEVANT_SYSTEM_WIDE_SID,
                sidRetrievalStrategy,
                oidProvider);
    }

    @Bean
    public SimpleRepositoryAclService productionRepositoryAclService(AclCache aclCache,
                                                                     JdbcMutableAclService repositoryJdbcMutableAclService,
                                                                     SidRetrievalStrategy sidRetrievalStrategy) {
        var oidProvider = new AclObjectIdentityProviderImpl(org.openl.security.acl.repository.RepositoryObjectIdentity.class,
                PROD_REPO_ROOT_ID);
        return new SimpleRepositoryAclServiceImpl(aclCache,
                repositoryJdbcMutableAclService,
                RELEVANT_SYSTEM_WIDE_SID,
                sidRetrievalStrategy,
                oidProvider);
    }

}
