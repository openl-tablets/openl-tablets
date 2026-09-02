package org.openl.rules.rest.acl;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.MediaType;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.rest.acl.model.AclProjectModel;
import org.openl.rules.rest.acl.model.AclProjectModel.AclProjectSource;
import org.openl.rules.rest.acl.model.AclSubject;
import org.openl.rules.rest.acl.model.AclView;
import org.openl.rules.rest.acl.model.SetAclRoleModel;
import org.openl.rules.rest.acl.validation.SidExistsConstraint;
import org.openl.rules.webstudio.security.SecureDesignTimeRepository;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.util.StringUtils;

@Validated
@RestController
@RequestMapping(value = "/acls/projects", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "ACL Management: Projects", description = "ACL Management API for Projects")
public class AclProjectsController {
    private static final int DEFAULT_SUBJECT_PAGE_SIZE = 10;
    private static final int MAX_SUBJECT_PAGE_SIZE = 50;

    private final SecureDesignTimeRepository designTimeRepository;
    private final RepositoryAclServiceProvider aclServiceProvider;
    private final UserManagementService userManagementService;
    private final GroupManagementService groupManagementService;
    private final TransactionTemplate txTemplate;

    public AclProjectsController(SecureDesignTimeRepository designTimeRepository,
                                 RepositoryAclServiceProvider aclServiceProvider,
                                 UserManagementService userManagementService,
                                 GroupManagementService groupManagementService,
                                 PlatformTransactionManager txManager) {
        this.aclServiceProvider = aclServiceProvider;
        this.userManagementService = userManagementService;
        this.groupManagementService = groupManagementService;
        this.txTemplate = new TransactionTemplate(txManager);
        this.designTimeRepository = designTimeRepository;
    }

    @Operation(summary = "acls.get-project-rules.summary", description = "acls.get-project-rules.desc")
    @Parameters({
            @Parameter(name = "sid", description = "acls.param.sid.desc", in = ParameterIn.QUERY, required = true, schema = @Schema(implementation = String.class)),
            @Parameter(name = "principal", description = "acls.param.principal.desc", in = ParameterIn.QUERY, schema = @Schema(implementation = Boolean.class))
    })
    @GetMapping
    @JsonView(AclView.Project.class)
    public List<AclProjectModel> getAclProjectRules(@NotNull @SidExistsConstraint Sid sid) {
        return mapAclProjectModel(designTimeRepository.getManageableProjects(), sid)
                .collect(Collectors.toList());
    }

    @Operation(summary = "acls.get-project-rule.summary", description = "acls.get-project-rule.desc")
    @ProjectManagementPermission
    @GetMapping("/{project-id}")
    @JsonView(AclView.Sid.class)
    public List<AclProjectModel> getAclProjectRulesForSid(@ProjectIdPathParameter @PathVariable("project-id") AProject project,
                                                          @Parameter(description = "acls.get-project-rule.param.inherited.desc")
                                                          @RequestParam(value = "inherited",
                                                                  defaultValue = "false") boolean inherited) {
        return mapAclProjectModelForSid(project, inherited)
                .collect(Collectors.toList());
    }

    @Operation(summary = "acls.suggest-subjects.summary", description = "acls.suggest-subjects.desc")
    @ProjectManagementPermission
    @GetMapping("/{project-id}/subjects")
    public List<String> suggestAclSubjects(@ProjectIdPathParameter @PathVariable("project-id") AProject project,
                                           @Parameter(description = "acls.suggest-subjects.param.principal.desc")
                                           @RequestParam("principal") boolean principal,
                                           @Parameter(description = "acls.suggest-subjects.param.search.desc")
                                           @RequestParam("search") String searchTerm,
                                           @Parameter(description = "acls.suggest-subjects.param.page-size.desc")
                                           @RequestParam(value = "pageSize",
                                                   defaultValue = "" + DEFAULT_SUBJECT_PAGE_SIZE) int pageSize) {
        if (StringUtils.isBlank(searchTerm)) {
            return List.of();
        }
        var safePageSize = Math.clamp(pageSize, 1, MAX_SUBJECT_PAGE_SIZE);
        var trimmed = searchTerm.trim();
        return principal
                ? userManagementService.findUserNames(trimmed, safePageSize)
                : groupManagementService.findGroupNames(trimmed, safePageSize);
    }

    @Operation(summary = "acls.update-project-rule.summary", description = "acls.update-project-rule.desc")
    @Parameters({
            @Parameter(name = "sid", description = "acls.param.sid.desc", in = ParameterIn.QUERY, required = true, schema = @Schema(implementation = String.class)),
            @Parameter(name = "principal", description = "acls.param.principal.desc", in = ParameterIn.QUERY, schema = @Schema(implementation = Boolean.class))
    })
    @ProjectManagementPermission
    @PutMapping(value = "/{project-id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateAclProjectRulesForSid(@ProjectIdPathParameter @PathVariable("project-id") AProject project,
                                            @NotNull @SidExistsConstraint Sid sid,
                                            @Valid @RequestBody SetAclRoleModel requestBody) {
        var aclService = aclServiceProvider.getDesignRepoAclService();
        txTemplate.execute(status -> {
            aclService.removePermissions(project, sid);
            aclService.addPermissions(project, sid, requestBody.getRole().getCumulativePermission());
            return null;
        });
    }

    @Operation(summary = "acls.delete-project-rule.summary", description = "acls.delete-project-rule.desc")
    @Parameters({
            @Parameter(name = "sid", description = "acls.param.sid.desc", in = ParameterIn.QUERY, required = true, schema = @Schema(implementation = String.class)),
            @Parameter(name = "principal", description = "acls.param.principal.desc", in = ParameterIn.QUERY, schema = @Schema(implementation = Boolean.class))
    })
    @ProjectManagementPermission
    @DeleteMapping("/{project-id}")
    public void deleteAclProjectRulesForSid(@ProjectIdPathParameter @PathVariable("project-id") AProject project,
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

    private Stream<AclProjectModel> mapAclProjectModelForSid(AProject project, boolean inherited) {
        var directRules = mapProjectAcl(project, inherited ? AclProjectSource.PROJECT : null);
        if (!inherited) {
            return directRules;
        }
        var aclService = aclServiceProvider.getDesignRepoAclService();
        var repositoryId = project.getRepository().getId();
        if (!aclService.isGranted(repositoryId, null, List.of(BasePermission.ADMINISTRATION))) {
            return directRules;
        }
        var inheritedRules = aclService.listPermissions(repositoryId, null).entrySet().stream()
                .map(entry -> Pair.of(AclSubject.of(entry.getKey()), entry.getValue()))
                .flatMap(entry -> mapAclProjectModel(entry, AclProjectSource.REPOSITORY));
        return Stream.concat(directRules, inheritedRules)
                .sorted(Comparator.comparing(AclProjectModel::getSource)
                        .thenComparing(model -> model.getSid().getSid()));
    }

    private Stream<AclProjectModel> mapProjectAcl(AProject project, AclProjectSource source) {
        var aclService = aclServiceProvider.getDesignRepoAclService();
        return aclService.listPermissions(project).entrySet().stream()
                .map(entry -> Pair.of(AclSubject.of(entry.getKey()), entry.getValue()))
                .flatMap(entry -> mapAclProjectModel(entry, source));
    }

    private Stream<AclProjectModel> mapAclProjectModel(Pair<AclSubject, List<Permission>> entry,
                                                       AclProjectSource source) {
        return entry.getValue().stream()
                .map(permission -> AclProjectModel.builder()
                        .sid(entry.getKey())
                        .source(source)
                        .role(AclRole.getRole(permission.getMask()))
                        .build());
    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Parameter(description = "acls.param.project-id.desc", in = ParameterIn.PATH, required = true, schema = @Schema(implementation = String.class))
    public @interface ProjectIdPathParameter {

    }

}
