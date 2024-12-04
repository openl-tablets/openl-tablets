package org.openl.rules.rest.acl;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.acls.model.Sid;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.rest.acl.model.AclRepositoryId;
import org.openl.rules.rest.acl.model.AclRepositoryModel;
import org.openl.rules.rest.acl.model.AclSidModel;
import org.openl.rules.rest.acl.model.SetAclRoleModel;
import org.openl.rules.rest.acl.validation.SidExistsConstraint;
import org.openl.rules.security.AdminPrivilege;
import org.openl.rules.webstudio.security.SecureDeploymentRepositoryService;
import org.openl.rules.webstudio.security.SecureDesignTimeRepository;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.AclRepositoryType;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.util.StreamUtils;

@Validated
@RestController
@RequestMapping(value = "/acls/repositories", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "ACL Management: Repositories", description = "ACL Management API for Repositories")
@Hidden
public class AclRepositoriesController {

    private final RepositoryAclServiceProvider aclServiceProvider;
    private final TransactionTemplate txTemplate;
    private final SecureDesignTimeRepository designTimeRepository;
    private final SecureDeploymentRepositoryService deploymentRepositoryService;

    public AclRepositoriesController(RepositoryAclServiceProvider aclServiceProvider,
                                     PlatformTransactionManager txManager,
                                     SecureDesignTimeRepository designTimeRepository,
                                     SecureDeploymentRepositoryService deploymentRepositoryService) {
        this.aclServiceProvider = aclServiceProvider;
        this.txTemplate = new TransactionTemplate(txManager);
        this.designTimeRepository = designTimeRepository;
        this.deploymentRepositoryService = deploymentRepositoryService;
    }

    @GetMapping
    public List<AclRepositoryModel> getAclRepositoryRules(@NotNull @SidExistsConstraint Sid sid) {
        var aclRepoModels = designTimeRepository.getManageableRepositories().stream()
                .flatMap(repo -> mapAclRepositoryModel(AclRepositoryType.DESIGN, repo.getId(), sid));
        if (designTimeRepository.hasDeployConfigRepo()) {
            var deployConfigRepo = designTimeRepository.getManageableDeployConfigRepository();
            if (deployConfigRepo != null) {
                aclRepoModels = Stream.concat(aclRepoModels,
                        mapAclRepositoryModel(AclRepositoryType.DEPLOY_CONFIG, deployConfigRepo.getId(), sid));
            }
        }
        return Stream.concat(aclRepoModels,
                        deploymentRepositoryService.getManageableRepositories().stream()
                                .flatMap(repo -> mapAclRepositoryModel(AclRepositoryType.PROD, repo.getId(), sid)))
                .collect(Collectors.toList());
    }

    @RepositoryManagementPermission
    @GetMapping(value = "/{repo-id}")
    public List<AclRepositoryModel> getAclRepositoryRulesForSid(@PathVariable("repo-id") AclRepositoryId aclRepoId) {
        return mapAclRepositoryModelForSid(aclRepoId.getType(), aclRepoId.getId())
                .collect(Collectors.toList());
    }

    @RepositoryManagementPermission
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping(value = "/{repo-id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateAclRepositoryRulesForSid(@PathVariable("repo-id") AclRepositoryId aclRepoId,
                                               @NotNull @SidExistsConstraint Sid sid,
                                               @Valid @RequestBody SetAclRoleModel requestBody) {
        var aclService = aclServiceProvider.getAclService(aclRepoId.getType().getType());
        txTemplate.execute(status -> {
            aclService.removePermissions(aclRepoId.getId(), null, sid);
            aclService.addPermissions(aclRepoId.getId(), null, sid, requestBody.getRole().getCumulativePermission());
            return null;
        });
    }

    @RepositoryManagementPermission
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(value = "/{repo-id}")
    public void deleteAclRepositoryRulesForSid(@PathVariable("repo-id") AclRepositoryId aclRepoId,
                                               @NotNull @SidExistsConstraint Sid sid) {
        var aclService = aclServiceProvider.getAclService(aclRepoId.getType().getType());
        aclService.removePermissions(aclRepoId.getId(), null, sid);
    }

    @AdminPrivilege
    @GetMapping(value = "/roots")
    public List<AclRepositoryModel> getAclRepositoryRulesForRoot(@NotNull @SidExistsConstraint Sid sid) {
        return StreamUtils.concat(mapAclRepositoryModelForRoot(AclRepositoryType.DESIGN, sid),
                        mapAclRepositoryModelForRoot(AclRepositoryType.DEPLOY_CONFIG, sid),
                        mapAclRepositoryModelForRoot(AclRepositoryType.PROD, sid))
                .collect(Collectors.toList());
    }

    @AdminPrivilege
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping(value = "/roots/{root-id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateAclRepositoryRulesForRoot(@PathVariable("root-id") AclRepositoryId aclRepoId,
                                                @Valid @RequestBody SetAclRoleModel requestBody,
                                                @NotNull @SidExistsConstraint Sid sid) {
        var aclService = aclServiceProvider.getAclService(aclRepoId.getType().getType());
        txTemplate.execute(status -> {
            aclService.removeRootPermissions(sid);
            aclService.addRootPermissions(sid, requestBody.getRole().getCumulativePermission());
            return null;
        });
    }

    private Stream<AclRepositoryModel> mapAclRepositoryModelForRoot(AclRepositoryType type, @Nonnull Sid sid) {
        return aclServiceProvider.getAclService(type.getType()).listRootPermissions(sid).stream()
                .map(permission -> AclRepositoryModel.rootRepositoryBuilder()
                        .id(AclRepositoryId.builder().type(type).build())
                        .type(type)
                        .role(AclRole.getRole(permission.getMask()))
                        .build());
    }

    private Stream<AclRepositoryModel> mapAclRepositoryModelForSid(AclRepositoryType type, @Nonnull String repoId) {
        return aclServiceProvider.getAclService(type.getType()).listPermissions(repoId, null).entrySet().stream()
                .map(entry -> Pair.of(AclSidModel.of(entry.getKey()), entry.getValue()))
                .flatMap(entry -> entry.getValue().stream()
                        .map(permission -> AclRepositoryModel.sidRepositoryBuilder()
                                .sid(entry.getKey())
                                .role(AclRole.getRole(permission.getMask()))
                                .build()));
    }

    private Stream<AclRepositoryModel> mapAclRepositoryModel(AclRepositoryType type, @Nonnull String repoId, @Nonnull Sid sid) {
        return aclServiceProvider.getAclService(type.getType()).listPermissions(repoId, null, sid).stream()
                .map(permission -> AclRepositoryModel.repositoryBuilder()
                        .id(AclRepositoryId.builder()
                                .type(type)
                                .id(repoId)
                                .build())
                        .name(repoId)
                        .type(type)
                        .role(AclRole.getRole(permission.getMask()))
                        .build());
    }

}
