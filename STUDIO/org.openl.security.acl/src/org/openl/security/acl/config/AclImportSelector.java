package org.openl.security.acl.config;

import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;

/**
 * Import selector for ACL configuration.
 */
public class AclImportSelector implements ImportSelector, EnvironmentAware {

    private Environment environment;

    @Override
    @NonNull
    public String[] selectImports(@NonNull AnnotationMetadata metadata) {
        if (isAclDisabled()) {
            return new String[]{DisabledAclConfiguration.class.getName()};
        } else {
            return new String[]{EnabledAclConfiguration.class.getName()};
        }
    }

    private boolean isAclDisabled() {
        return "single".equals(environment.getProperty("user.mode"));
    }

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }
}
