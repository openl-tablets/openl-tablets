package org.openl.rules.rest.acl;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
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

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.rest.acl.model.AclProjectModel;
import org.openl.rules.rest.acl.model.AclSubject;
import org.openl.rules.rest.acl.model.AclView;
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
public class AclProjectsController {

    private final SecureDesignTimeRepository designTimeRepository;
    private final RepositoryAclServiceProvider aclServiceProvider;
    private final TransactionTemplate txTemplate;

    public AclProjectsController(SecureDesignTimeRepository designTimeRepository,
                                 RepositoryAclServiceProvider aclServiceProvider,
                                 PlatformTransactionManager txManager) {
        this.aclServiceProvider = aclServiceProvider;
        this.txTemplate = new TransactionTemplate(txManager);
        this.designTimeRepository = designTimeRepository;
    }

    @Operation(summary = "Get a list of ACL rules for all projects by criteria")
    @Parameters({
            @Parameter(name = "sid", in = ParameterIn.QUERY, required = true, schema = @Schema(implementation = String.class)),
            @Parameter(name = "principal", in = ParameterIn.QUERY, schema = @Schema(implementation = Boolean.class))
    })
    @GetMapping
    @JsonView(AclView.Project.class)
    public List<AclProjectModel> getAclProjectRules(@NotNull @SidExistsConstraint Sid sid) {
        return mapAclProjectModel(designTimeRepository.getManageableProjects(), sid)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Get a list of ACL rules for a single project")
    @ProjectManagementPermission
    @GetMapping("/{project-id}")
    @JsonView(AclView.Sid.class)
    public List<AclProjectModel> getAclProjectRulesForSid(@ProjectIdPathParameter @PathVariable("project-id") AProject project) {
        return mapAclProjectModelForSid(project)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Update existing ACL rule for a single project")
    @Parameters({
            @Parameter(name = "sid", in = ParameterIn.QUERY, required = true, schema = @Schema(implementation = String.class)),
            @Parameter(name = "principal", in = ParameterIn.QUERY, schema = @Schema(implementation = Boolean.class))
    })
    @ProjectManagementPermission
    @ResponseStatus(HttpStatus.NO_CONTENT)
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

    @Operation(summary = "Delete an ACL rule for the project by the requested criteria")
    @Parameters({
            @Parameter(name = "sid", in = ParameterIn.QUERY, required = true, schema = @Schema(implementation = String.class)),
            @Parameter(name = "principal", in = ParameterIn.QUERY, schema = @Schema(implementation = Boolean.class))
    })
    @ProjectManagementPermission
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(value = "/{project-id}")
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

    private Stream<AclProjectModel> mapAclProjectModelForSid(AProject project) {
        var aclService = aclServiceProvider.getDesignRepoAclService();
        return aclService.listPermissions(project).entrySet().stream()
                .map(entry -> Pair.of(AclSubject.of(entry.getKey()), entry.getValue()))
                .flatMap(entry -> entry.getValue().stream()
                        .map(permission -> AclProjectModel.builder()
                                .sid(entry.getKey())
                                .role(AclRole.getRole(permission.getMask()))
                                .build()));
    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Parameter(description = "Project ID", in = ParameterIn.PATH, required = true, schema = @Schema(implementation = String.class))
    public @interface ProjectIdPathParameter {

    }

}
