package org.openl.rules.webstudio.security;

import java.util.function.Predicate;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.security.acls.model.Permission;
import org.springframework.stereotype.Component;

@Component("aclProductionRepositoryPermissionTestFactory")
public class AclProductionRepositoryPermissionTestFactory implements FactoryBean<Predicate<Permission>> {

    private final SecureDeploymentRepositoryService secureDeploymentRepositoryService;

    public AclProductionRepositoryPermissionTestFactory(SecureDeploymentRepositoryService secureDeploymentRepositoryService) {
        this.secureDeploymentRepositoryService = secureDeploymentRepositoryService;
    }

    @Override
    public Predicate<Permission> getObject() {
        return secureDeploymentRepositoryService::hasPermission;
    }

    @Override
    public Class<?> getObjectType() {
        return Predicate.class;
    }
}
