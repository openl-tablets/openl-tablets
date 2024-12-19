package org.openl.rules.rest.acl;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.openl.rules.rest.acl.model.AclView;
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

    @Operation(summary = "Get a list of ACL rules for all repositories by criteria")
    @Parameters({
            @Parameter(name = "sid", in = ParameterIn.QUERY, required = true, schema = @Schema(implementation = String.class)),
            @Parameter(name = "principal", in = ParameterIn.QUERY, schema = @Schema(implementation = Boolean.class))
    })
    @GetMapping
    @JsonView(AclView.Repository.class)
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

    @Operation(summary = "Get a list of ALC rules for a single repository")
    @RepositoryManagementPermission
    @GetMapping(value = "/{repo-id}")
    @JsonView(AclView.Sid.class)
    public List<AclRepositoryModel> getAclRepositoryRulesForSid(@PathVariable("repo-id") AclRepositoryId aclRepoId) {
        return mapAclRepositoryModelForSid(aclRepoId.getType(), aclRepoId.getId())
                .collect(Collectors.toList());
    }

    @Operation(summary = "Update existing ACL rule for a single repository")
    @Parameters({
            @Parameter(name = "sid", in = ParameterIn.QUERY, required = true, schema = @Schema(implementation = String.class)),
            @Parameter(name = "principal", in = ParameterIn.QUERY, schema = @Schema(implementation = Boolean.class))
    })
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

    @Operation(summary = "Delete an ACL rule for the repository by the requested criteria")
    @Parameters({
            @Parameter(name = "sid", in = ParameterIn.QUERY, required = true, schema = @Schema(implementation = String.class)),
            @Parameter(name = "principal", in = ParameterIn.QUERY, schema = @Schema(implementation = Boolean.class))
    })
    @RepositoryManagementPermission
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(value = "/{repo-id}")
    public void deleteAclRepositoryRulesForSid(@PathVariable("repo-id") AclRepositoryId aclRepoId,
                                               @NotNull @SidExistsConstraint Sid sid) {
        var aclService = aclServiceProvider.getAclService(aclRepoId.getType().getType());
        aclService.removePermissions(aclRepoId.getId(), null, sid);
    }

    @Operation(summary = "Get ACL rules for all repository roots")
    @Parameters({
            @Parameter(name = "sid", in = ParameterIn.QUERY, required = true, schema = @Schema(implementation = String.class)),
            @Parameter(name = "principal", in = ParameterIn.QUERY, schema = @Schema(implementation = Boolean.class))
    })
    @AdminPrivilege
    @GetMapping(value = "/roots")
    @JsonView(AclView.Root.class)
    public List<AclRepositoryModel> getAclRepositoryRulesForRoot(@NotNull @SidExistsConstraint Sid sid) {
        return StreamUtils.concat(mapAclRepositoryModelForRoot(AclRepositoryType.DESIGN, sid),
                        mapAclRepositoryModelForRoot(AclRepositoryType.DEPLOY_CONFIG, sid),
                        mapAclRepositoryModelForRoot(AclRepositoryType.PROD, sid))
                .collect(Collectors.toList());
    }

    @Operation(summary = "Update ACL rule for a repository root")
    @Parameters({
            @Parameter(name = "sid", in = ParameterIn.QUERY, required = true, schema = @Schema(implementation = String.class)),
            @Parameter(name = "principal", in = ParameterIn.QUERY, schema = @Schema(implementation = Boolean.class))
    })
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
