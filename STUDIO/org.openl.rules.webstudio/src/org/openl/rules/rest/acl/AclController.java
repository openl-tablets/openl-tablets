package org.openl.rules.rest.acl;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.model.AccessControlEntry;
import org.openl.rules.rest.acl.model.AclResourceAccess;
import org.openl.rules.rest.acl.model.AclResourceRef;
import org.openl.rules.rest.acl.model.AclSubject;
import org.openl.rules.rest.acl.model.BulkAclOverwriteRequest;
import org.openl.rules.rest.acl.service.BulkAclOverwriteService;
import org.openl.rules.security.AdminPrivilege;
import org.openl.rules.webstudio.security.SecureDeploymentRepositoryService;
import org.openl.rules.webstudio.security.SecureDesignTimeRepository;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.AclRepositoryType;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.util.CollectionUtils;

@Validated
@RestController
@RequestMapping("/acls")
@Tag(name = "ACL Management", description = "ACL Management Bulk API")
@ConditionalOnExpression("'${user.mode}' != 'single'")
public class AclController {

    private final RepositoryAclServiceProvider aclServiceProvider;
    private final SecureDesignTimeRepository designTimeRepository;
    private final SecureDeploymentRepositoryService deploymentRepositoryService;
    private final BulkAclOverwriteService bulkAclOverwriteService;

    public AclController(RepositoryAclServiceProvider aclServiceProvider,
                         SecureDesignTimeRepository designTimeRepository,
                         SecureDeploymentRepositoryService deploymentRepositoryService,
                         BulkAclOverwriteService bulkAclOverwriteService) {
        this.aclServiceProvider = aclServiceProvider;
        this.designTimeRepository = designTimeRepository;
        this.deploymentRepositoryService = deploymentRepositoryService;
        this.bulkAclOverwriteService = bulkAclOverwriteService;
    }

    @GetMapping
    @AdminPrivilege
    @Operation(summary = "Get ACL configuration for all resources")
    public List<AclResourceAccess> getAclResourceAccess() {
        var roots = Stream.of(convertRootToResourceAccess(AclRepositoryType.DESIGN),
                convertRootToResourceAccess(AclRepositoryType.PROD));
        var designRepos = designTimeRepository.getRepositories().stream().map(this::convertToAclResourceAccess);
        var designProjects = designTimeRepository.getProjects().stream().map(this::convertProjectToAclResourceAccess);
        var deployRepos = deploymentRepositoryService.getRepositories().stream().map(this::convertDeployRepoToAclResourceAccess);
        return Stream.of(roots, designRepos, designProjects, deployRepos)
                .flatMap(Function.identity())
                .filter(resourceAccess -> CollectionUtils.isNotEmpty(resourceAccess.getAces()))
                .sorted(Comparator.comparing(AclResourceAccess::getResourceRef, AclResourceRef.COMPARATOR))
                .toList();
    }

    private AclResourceAccess convertDeployRepoToAclResourceAccess(RepositoryConfiguration deployRepository) {
        var resourceAccess = AclResourceAccess.builder()
                .resourceRef(AclResourceRef.builder()
                        .repositoryType(AclRepositoryType.PROD)
                        .repositoryId(deployRepository.getId())
                        .build());
        var aclService = aclServiceProvider.getProdRepoAclService();
        var aces = aclService.listPermissions(deployRepository.getId(), null).entrySet().stream()
                .flatMap(entry -> convertToAce(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(AccessControlEntry::getSub, AclSubject.COMPARATOR))
                .toList();
        return resourceAccess
                .aces(aces)
                .build();
    }

    private AclResourceAccess convertProjectToAclResourceAccess(AProject project) {
        var resourceAccess = AclResourceAccess.builder()
                .resourceRef(AclResourceRef.builder()
                        .repositoryType(AclRepositoryType.DESIGN)
                        .repositoryId(project.getRepository().getId())
                        .projectName(project.getName())
                        .build());
        var aclService = aclServiceProvider.getDesignRepoAclService();
        var aces = aclService.listPermissions(project).entrySet().stream()
                .flatMap(entry -> convertToAce(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(AccessControlEntry::getSub, AclSubject.COMPARATOR))
                .toList();
        return resourceAccess
                .aces(aces)
                .build();
    }

    private AclResourceAccess convertToAclResourceAccess(Repository repository) {
        var resourceAccess = AclResourceAccess.builder()
                .resourceRef(AclResourceRef.builder()
                        .repositoryType(AclRepositoryType.DESIGN)
                        .repositoryId(repository.getId())
                        .build());
        var aclService = aclServiceProvider.getDesignRepoAclService();
        var aces = aclService.listPermissions(repository.getId(), null).entrySet().stream()
                .flatMap(entry -> convertToAce(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(AccessControlEntry::getSub, AclSubject.COMPARATOR))
                .toList();
        return resourceAccess
                .aces(aces)
                .build();
    }

    private AclResourceAccess convertRootToResourceAccess(AclRepositoryType type) {
        var resourceAccess = AclResourceAccess.builder()
                .resourceRef(AclResourceRef.builder()
                        .repositoryType(type)
                        .build());
        var aclService = aclServiceProvider.getAclService(type.getType());
        var aces = aclService.listRootPermissions().entrySet().stream()
                .flatMap(entry -> convertToAce(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(AccessControlEntry::getSub, AclSubject.COMPARATOR))
                .toList();
        return resourceAccess
                .aces(aces)
                .build();
    }

    private Stream<AccessControlEntry> convertToAce(Sid sid, List<Permission> permissions) {
        var subject = AclSubject.of(sid);
        return permissions.stream()
                .map(permission -> AccessControlEntry.builder()
                        .sub(subject)
                        .role(AclRole.getRole(permission.getMask()))
                        .build());
    }

    @PostMapping
    @AdminPrivilege
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Overwrite ACL configuration for resources in bulk")
    public void overwriteAcl(@Valid @RequestBody BulkAclOverwriteRequest request) {
        bulkAclOverwriteService.process(request);
    }

}
