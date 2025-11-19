package org.openl.rules.webstudio.repositories.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import org.openl.rules.rest.acl.model.AclRepositoryId;
import org.openl.rules.rest.model.RepositoryViewModel;
import org.openl.rules.webstudio.security.SecureDeploymentRepositoryService;
import org.openl.security.acl.repository.AclRepositoryType;

@Service
public class DeploymentRepositoryServiceImpl implements DeploymentRepositoryService {

    private final SecureDeploymentRepositoryService deploymentRepositoryService;

    public DeploymentRepositoryServiceImpl(SecureDeploymentRepositoryService deploymentRepositoryService) {
        this.deploymentRepositoryService = deploymentRepositoryService;
    }

    @Override
    public List<RepositoryViewModel> getRepositoryList() {
        return deploymentRepositoryService.getRepositories()
                .stream()
                .map(repo -> RepositoryViewModel.builder()
                        .id(repo.getId())
                        .name(repo.getName())
                        .aclId(AclRepositoryId.builder()
                                .id(repo.getId())
                                .type(AclRepositoryType.PROD)
                                .build())
                        .build())
                .collect(Collectors.toList());
    }
}
