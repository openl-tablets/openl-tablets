package org.openl.rules.rest.acl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
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
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.rest.acl.model.AclRepositoryId;
import org.openl.rules.rest.acl.model.AclRepositoryModel;
import org.openl.rules.rest.acl.model.AclSubject;
import org.openl.rules.rest.acl.model.AclView;
import org.openl.rules.rest.acl.model.SetAclRoleModel;
import org.openl.rules.rest.acl.service.AclRoleAuditor;
import org.openl.rules.rest.acl.service.AclRoleAuditor.Place;
import org.openl.rules.rest.acl.validation.SidExistsConstraint;
import org.openl.rules.webstudio.security.SecureDeploymentRepositoryService;
import org.openl.rules.webstudio.security.SecureDesignTimeRepository;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.AclRepositoryType;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.studio.security.AdminPrivilege;

@Validated
@RestController
@RequestMapping(value = "/acls/repositories", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "ACL Management: Repositories", description = "ACL Management API for Repositories")
public class AclRepositoriesController {

    private final RepositoryAclServiceProvider aclServiceProvider;
    private final TransactionTemplate txTemplate;
    private final SecureDesignTimeRepository designTimeRepository;
    private final SecureDeploymentRepositoryService deploymentRepositoryService;
    private final AclRoleAuditor aclRoleAuditor;

    public AclRepositoriesController(RepositoryAclServiceProvider aclServiceProvider,
                                     PlatformTransactionManager txManager,
                                     SecureDesignTimeRepository designTimeRepository,
                                     SecureDeploymentRepositoryService deploymentRepositoryService,
                                     AclRoleAuditor aclRoleAuditor) {
        this.aclServiceProvider = aclServiceProvider;
        this.aclRoleAuditor = aclRoleAuditor;
        this.txTemplate = new TransactionTemplate(txManager);
        this.designTimeRepository = designTimeRepository;
        this.deploymentRepositoryService = deploymentRepositoryService;
    }

    @Operation(summary = "Get a list of ACL rules for all repositories by criteria")
    @Parameter(name = "sid", in = ParameterIn.QUERY, required = true, schema = @Schema(implementation = String.class))
    @Parameter(name = "principal", in = ParameterIn.QUERY, schema = @Schema(implementation = Boolean.class))
    @GetMapping
    @JsonView(AclView.Repository.class)
    public List<AclRepositoryModel> getAclRepositoryRules(@NotNull @SidExistsConstraint Sid sid) {
        var aclRepoModels = designTimeRepository.getManageableRepositories().stream()
                .flatMap(repo -> mapAclRepositoryModel(AclRepositoryType.DESIGN, repo.getId(), sid));
        return Stream.concat(aclRepoModels,
                        deploymentRepositoryService.getManageableRepositories().stream()
                                .flatMap(repo -> mapAclRepositoryModel(AclRepositoryType.PROD, repo.getId(), sid)))
                .toList();
    }

    @Operation(summary = "Get a list of ALC rules for a single repository")
    @RepositoryManagementPermission
    @GetMapping("/{repo-id}")
    @JsonView(AclView.Sid.class)
    public List<AclRepositoryModel> getAclRepositoryRulesForSid(@PathVariable("repo-id") AclRepositoryId aclRepoId) {
        return mapAclRepositoryModelForSid(aclRepoId.getType(), aclRepoId.getId())
                .toList();
    }

    @Operation(summary = "Update existing ACL rule for a single repository")
    @Parameter(name = "sid", in = ParameterIn.QUERY, required = true, schema = @Schema(implementation = String.class))
    @Parameter(name = "principal", in = ParameterIn.QUERY, schema = @Schema(implementation = Boolean.class))
    @RepositoryManagementPermission
    @PutMapping(value = "/{repo-id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateAclRepositoryRulesForSid(@PathVariable("repo-id") AclRepositoryId aclRepoId,
                                               @NotNull @SidExistsConstraint Sid sid,
                                               @Valid @RequestBody SetAclRoleModel requestBody) {
        setRepositoryRole(aclRepoId, sid, requestBody.getRole());
    }

    @Operation(summary = "Delete an ACL rule for the repository by the requested criteria")
    @Parameter(name = "sid", in = ParameterIn.QUERY, required = true, schema = @Schema(implementation = String.class))
    @Parameter(name = "principal", in = ParameterIn.QUERY, schema = @Schema(implementation = Boolean.class))
    @RepositoryManagementPermission
    @DeleteMapping("/{repo-id}")
    public void deleteAclRepositoryRulesForSid(@PathVariable("repo-id") AclRepositoryId aclRepoId,
                                               @NotNull @SidExistsConstraint Sid sid) {
        setRepositoryRole(aclRepoId, sid, null);
    }

    @Operation(summary = "Get ACL rules for all repository roots")
    @Parameter(name = "sid", in = ParameterIn.QUERY, required = true, schema = @Schema(implementation = String.class))
    @Parameter(name = "principal", in = ParameterIn.QUERY, schema = @Schema(implementation = Boolean.class))
    @AdminPrivilege
    @GetMapping("/roots")
    @JsonView(AclView.Root.class)
    public List<AclRepositoryModel> getAclRepositoryRulesForRoot(@NotNull @SidExistsConstraint Sid sid) {
        return Stream.concat(mapAclRepositoryModelForRoot(AclRepositoryType.DESIGN, sid),
                        mapAclRepositoryModelForRoot(AclRepositoryType.PROD, sid))
                .toList();
    }

    @Operation(summary = "Update ACL rule for a repository root")
    @Parameter(name = "sid", in = ParameterIn.QUERY, required = true, schema = @Schema(implementation = String.class))
    @Parameter(name = "principal", in = ParameterIn.QUERY, schema = @Schema(implementation = Boolean.class))
    @AdminPrivilege
    @PutMapping(value = "/roots/{root-id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateAclRepositoryRulesForRoot(@PathVariable("root-id") AclRepositoryId aclRepoId,
                                                @Valid @RequestBody SetAclRoleModel requestBody,
                                                @NotNull @SidExistsConstraint Sid sid) {
        setRootRole(aclRepoId.getType(), sid, requestBody.getRole());
    }

    @Operation(summary = "Delete an ACL rule for the repository root by the requested criteria")
    @Parameter(name = "sid", in = ParameterIn.QUERY, required = true, schema = @Schema(implementation = String.class))
    @Parameter(name = "principal", in = ParameterIn.QUERY, schema = @Schema(implementation = Boolean.class))
    @AdminPrivilege
    @DeleteMapping("/roots/{root-id}")
    public void deleteAclRepositoryRulesForRoot(@PathVariable("root-id") AclRepositoryId aclRepoId,
                                               @NotNull @SidExistsConstraint Sid sid) {
        setRootRole(aclRepoId.getType(), sid, null);
    }

    /**
     * Gives a subject a role on a repository in place of the one it held, and announces the change.
     *
     * @param role the role to give, or {@code null} to take the role away
     */
    private void setRepositoryRole(AclRepositoryId aclRepoId, Sid sid, @Nullable AclRole role) {
        var aclService = aclServiceProvider.getAclService(aclRepoId.getType().getType());
        txTemplate.executeWithoutResult(status -> {
            var before = AclRoleAuditor.roleOf(aclService.listPermissions(aclRepoId.getId(), null, sid));
            aclService.removePermissions(aclRepoId.getId(), null, sid);
            if (role != null) {
                aclService.addPermissions(aclRepoId.getId(), null, sid, role.getCumulativePermission());
            }
            aclRoleAuditor.announce(Place.repository(aclRepoId.getType(), aclRepoId.getId()), sid, before,
                    Optional.ofNullable(role).map(AclRole::name).orElse(null));
        });
    }

    /**
     * Gives a subject a role on all the design or all the production repositories in place of the one it held, and
     * announces the change.
     *
     * @param role the role to give, or {@code null} to take the role away
     */
    private void setRootRole(AclRepositoryType repositoryType, Sid sid, @Nullable AclRole role) {
        var aclService = aclServiceProvider.getAclService(repositoryType.getType());
        txTemplate.executeWithoutResult(status -> {
            var before = AclRoleAuditor.roleOf(aclService.listRootPermissions(sid));
            aclService.removeRootPermissions(sid);
            if (role != null) {
                aclService.addRootPermissions(sid, role.getCumulativePermission());
            }
            aclRoleAuditor.announce(Place.root(repositoryType), sid, before,
                    Optional.ofNullable(role).map(AclRole::name).orElse(null));
        });
    }

    private Stream<AclRepositoryModel> mapAclRepositoryModelForRoot(AclRepositoryType type, @NonNull Sid sid) {
        return aclServiceProvider.getAclService(type.getType()).listRootPermissions(sid).stream()
                .map(permission -> AclRepositoryModel.rootRepositoryBuilder()
                        .id(AclRepositoryId.builder().type(type).build())
                        .type(type)
                        .role(AclRole.getRole(permission.getMask()))
                        .build());
    }

    private Stream<AclRepositoryModel> mapAclRepositoryModelForSid(AclRepositoryType type, @NonNull String repoId) {
        return aclServiceProvider.getAclService(type.getType()).listPermissions(repoId, null).entrySet().stream()
                .map(entry -> Pair.of(AclSubject.of(entry.getKey()), entry.getValue()))
                .flatMap(entry -> entry.getValue().stream()
                        .map(permission -> AclRepositoryModel.sidRepositoryBuilder()
                                .sid(entry.getKey())
                                .role(AclRole.getRole(permission.getMask()))
                                .build()));
    }

    private Stream<AclRepositoryModel> mapAclRepositoryModel(AclRepositoryType type, @NonNull String repoId, @NonNull Sid sid) {
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
