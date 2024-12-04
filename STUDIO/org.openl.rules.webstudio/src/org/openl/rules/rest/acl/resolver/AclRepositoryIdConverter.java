package org.openl.rules.rest.acl.resolver;

import java.util.Objects;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.core.convert.converter.Converter;

import org.openl.rules.rest.acl.model.AclRepositoryId;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.webstudio.security.SecureDesignTimeRepository;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.security.acl.repository.AclRepositoryType;

@ParametersAreNonnullByDefault
public class AclRepositoryIdConverter implements Converter<String, AclRepositoryId> {

    private final SecureDesignTimeRepository designTimeRepository;
    private final DeploymentManager deploymentManager;

    public AclRepositoryIdConverter(SecureDesignTimeRepository designTimeRepository,
                                    DeploymentManager deploymentManager) {
        this.designTimeRepository = designTimeRepository;
        this.deploymentManager = deploymentManager;
    }

    @Override
    public AclRepositoryId convert(String source) {
        var aclRepoId = AclRepositoryId.decode(source);
        if (aclRepoId.getId() != null) {
            validate(aclRepoId);
        }
        return aclRepoId;
    }

    private void validate(AclRepositoryId aclRepoId) {
        boolean notExists;
        if (aclRepoId.getType() == AclRepositoryType.DESIGN) {
            notExists = designTimeRepository.getRepository(aclRepoId.getId()) == null;
        } else if (aclRepoId.getType() == AclRepositoryType.DEPLOY_CONFIG) {
            notExists = !designTimeRepository.hasDeployConfigRepo()
                    || !Objects.equals(aclRepoId.getId(), designTimeRepository.getDeployConfigRepository().getId());
        } else if (aclRepoId.getType() == AclRepositoryType.PROD) {
            notExists = !deploymentManager.getRepositoryConfigNames().contains(aclRepoId.getId());
        } else {
            throw new IllegalArgumentException("Unsupported repository type: " + aclRepoId.getType());
        }
        if (notExists) {
            throw new NotFoundException("repository.message", aclRepoId.getId());
        }
    }

}
