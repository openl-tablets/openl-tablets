package org.openl.studio.repositories.service;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;

import org.openl.rules.rest.acl.model.AclRepositoryId;
import org.openl.rules.webstudio.security.SecureDeploymentRepositoryService;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.repository.DeploymentRepositoriesUtil;
import org.openl.security.acl.repository.AclRepositoryType;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.studio.repositories.model.RepositoryViewModel;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeploymentRepositoryServiceImpl implements DeploymentRepositoryService {

    private final SecureDeploymentRepositoryService deploymentRepositoryService;
    private final RepositoryAccessService repositoryAccessService;
    private final RepositoryAclServiceProvider aclServiceProvider;
    private final DeploymentManager deploymentManager;

    @Override
    public boolean canDeployToAnyRepository() {
        // Instantiating a production repository can fail (unreachable/misconfigured). This runs for every
        // project while listing, so a failure must degrade to "not deployable", never break the whole list.
        try {
            var prodRepoAclService = aclServiceProvider.getProdRepoAclService();
            return deploymentRepositoryService.getRepositories().stream()
                    .anyMatch(repo -> prodRepoAclService
                            .isGranted(repo.getId(), null, List.of(BasePermission.WRITE))
                            && !DeploymentRepositoriesUtil.isMainBranchProtected(
                                    deploymentManager.getDeployRepository(repo.getConfigName())));
        } catch (Exception e) {
            log.warn("Cannot determine deploy permissions; treating as not deployable.", e);
            return false;
        }
    }

    @Override
    public List<RepositoryViewModel> getRepositoryList() {
        return deploymentRepositoryService.getRepositories()
                .stream()
                .map(repo -> RepositoryViewModel.builder()
                        .aclId(AclRepositoryId.builder()
                                .id(repo.getId())
                                .type(AclRepositoryType.PROD)
                                .build())
                        .id(repo.getId())
                        .name(repo.getName())
                        .type(repo.getType())
                        .capabilities(repositoryAccessService.computeCapabilities(repo.getId(), AclRepositoryType.PROD))
                        .build())
                .collect(Collectors.toList());
    }
}
