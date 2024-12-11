package org.openl.rules.rest.acl;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.model.Sid;
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

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.rest.acl.model.AclProjectModel;
import org.openl.rules.rest.acl.model.AclSidModel;
import org.openl.rules.rest.acl.model.SetAclRoleModel;
import org.openl.rules.rest.acl.validation.SidExistsConstraint;
import org.openl.rules.rest.model.ProjectIdModel;
import org.openl.rules.webstudio.security.SecureDesignTimeRepository;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;

@Validated
@RestController
@RequestMapping(value = "/acls/projects", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "ACL Management: Projects", description = "ACL Management API for Projects")
@Hidden
public class AclProjectsController {

    private final SecureDesignTimeRepository designTimeRepository;
    private final RepositoryAclServiceProvider aclServiceProvider;
    private final TransactionTemplate txTemplate;

    public AclProjectsController(SecureDesignTimeRepository designTimeRepository,
                                 RepositoryAclServiceProvider aclServiceProvider,
                                 TransactionTemplate txTemplate) {
        this.aclServiceProvider = aclServiceProvider;
        this.txTemplate = txTemplate;
        this.designTimeRepository = designTimeRepository;
    }

    @GetMapping
    public List<AclProjectModel> getAclProjectRules(@NotNull @SidExistsConstraint Sid sid) {
        return mapAclProjectModel(designTimeRepository.getManageableProjects(), sid)
                .collect(Collectors.toList());
    }

    @GetMapping("/{project-id}")
    @PreAuthorize("hasAuthority(T(org.openl.rules.security.Privileges).ADMIN.getAuthority()) or @aclProjectsHelper.hasPermission(#project, T(org.openl.security.acl.permission.AclPermission).ADMINISTRATION)")
    public List<AclProjectModel> getAclProjectRulesForSid(@PathVariable("project-id") AProject project) {
        return mapAclProjectModelForSid(project)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAuthority(T(org.openl.rules.security.Privileges).ADMIN.getAuthority()) or @aclProjectsHelper.hasPermission(#project, T(org.openl.security.acl.permission.AclPermission).ADMINISTRATION)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping(value = "/{project-id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateAclProjectRulesForSid(@PathVariable("project-id") AProject project,
                                            @NotNull @SidExistsConstraint Sid sid,
                                            @Valid @RequestBody SetAclRoleModel requestBody) {
        var aclService = aclServiceProvider.getDesignRepoAclService();
        txTemplate.execute(status -> {
            aclService.removePermissions(project, sid);
            aclService.addPermissions(project, sid, requestBody.getRole().getCumulativePermission());
            return null;
        });
    }

    @PreAuthorize("hasAuthority(T(org.openl.rules.security.Privileges).ADMIN.getAuthority()) or @aclProjectsHelper.hasPermission(#project, T(org.openl.security.acl.permission.AclPermission).ADMINISTRATION)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(value = "/{project-id}")
    public void deleteAclProjectRulesForSid(@PathVariable("project-id") AProject project,
                                            @NotNull @SidExistsConstraint Sid sid) {
        var aclService = aclServiceProvider.getDesignRepoAclService();
        aclService.removePermissions(project, sid);
    }

    private Stream<AclProjectModel> mapAclProjectModel(List<AProject> projects, Sid sid) {
        var aclService = aclServiceProvider.getDesignRepoAclService();
        return projects.stream()
                .map(project -> Pair.of(project, AclProjectModel.builder()
                        .id(ProjectIdModel.builder()
                                .repository(project.getRepository().getId())
                                .projectName(project.getName())
                                .build())
                        .name(project.getName()))
                )
                .flatMap(entry -> aclService.listPermissions(entry.getKey(), sid).stream()
                        .map(permission -> entry.getValue()
                                .role(AclRole.getRole(permission.getMask()))
                                .build()));
    }

    private Stream<AclProjectModel> mapAclProjectModelForSid(AProject project) {
        var projectId = ProjectIdModel.builder()
                .repository(project.getRepository().getId())
                .projectName(project.getName())
                .build();
        var aclService = aclServiceProvider.getDesignRepoAclService();
        return aclService.listPermissions(project).entrySet().stream()
                .map(entry -> Pair.of(AclSidModel.of(entry.getKey()), entry.getValue()))
                .flatMap(entry -> entry.getValue().stream()
                        .map(permission -> AclProjectModel.builder()
                                .id(projectId)
                                .name(project.getName())
                                .sid(entry.getKey())
                                .role(AclRole.getRole(permission.getMask()))
                                .build()));
    }

}
