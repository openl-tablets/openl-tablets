package org.openl.rules.rest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.rest.exception.ConflictException;
import org.openl.rules.rest.model.CreateBranchModel;
import org.openl.rules.rest.model.ProjectStatusUpdateModel;
import org.openl.rules.rest.model.ProjectViewModel;
import org.openl.rules.rest.model.tables.AppendTableView;
import org.openl.rules.rest.model.tables.EditableTableView;
import org.openl.rules.rest.model.tables.SummaryTableView;
import org.openl.rules.rest.service.ProjectCriteriaQuery;
import org.openl.rules.rest.service.ProjectTableCriteriaQuery;
import org.openl.rules.rest.service.WorkspaceProjectService;
import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.rest.validation.NewBranchValidator;
import org.openl.rules.ui.WebStudio;
import org.openl.util.StringUtils;

/**
 * Projects REST controller
 *
 * @author Vladyslav Pikus
 */
@RestController
@RequestMapping(value = "/projects", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects (BETA)", description = "Experimental projects API")
public class ProjectsController {

    private static final String TAGS_PREFIX = "tags.";
    private static final String PROPERTIES_PREFIX = "properties.";

    private final WorkspaceProjectService projectService;
    private final Function<BranchRepository, NewBranchValidator> newBranchValidatorFactory;
    private final BeanValidationProvider validationProvider;

    public ProjectsController(WorkspaceProjectService projectService,
                              Function<BranchRepository, NewBranchValidator> newBranchValidatorFactory,
                              BeanValidationProvider validationProvider) {
        this.projectService = projectService;
        this.newBranchValidatorFactory = newBranchValidatorFactory;
        this.validationProvider = validationProvider;
    }

    @Lookup
    public WebStudio getWebStudio() {
        return null;
    }

    @GetMapping
    @Operation(summary = "Get projects (BETA)")
    @Parameters({
            @Parameter(name = "status", description = "Project status", in = ParameterIn.QUERY, schema = @Schema(allowableValues = {
                    "LOCAL",
                    "ARCHIVED",
                    "OPENED",
                    "VIEWING_VERSION",
                    "EDITING",
                    "CLOSED"})),
            @Parameter(name = "repository", description = "Repository ID", in = ParameterIn.QUERY),
            @Parameter(name = "tags", description = "Project tags. Must start with `tags.` ", in = ParameterIn.QUERY, style = ParameterStyle.FORM, schema = @Schema(implementation = Object.class), explode = Explode.TRUE)})
    public List<ProjectViewModel> getProjects(@Parameter(hidden = true) @RequestParam Map<String, String> params,
                                              @RequestParam(value = "status", required = false) ProjectStatus status,
                                              @RequestParam(value = "repository", required = false) String repository) {

        var queryBuilder = ProjectCriteriaQuery.builder().repositoryId(repository).status(status);

        params.entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith(TAGS_PREFIX))
                .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
                .forEach(entry -> {
                    var tag = entry.getKey().substring(TAGS_PREFIX.length());
                    queryBuilder.tag(tag, entry.getValue());
                });

        return projectService.getProjects(queryBuilder.build());
    }

    @Hidden
    @GetMapping("/{projectId}")
    public ProjectViewModel getProject(@ProjectId @PathVariable("projectId") RulesProject project) {
        return projectService.getProject(project);
    }

    @PatchMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Update project status (BETA)")
    public void updateProjectStatus(@ProjectId @PathVariable("projectId") RulesProject project,
                                    @RequestBody ProjectStatusUpdateModel request) {
        try {
            projectService.updateProjectStatus(project, request);
            if (request.getStatus() != null
                    || request.getBranch().isPresent()
                    || request.getComment().isPresent()
                    || request.getRevision().isPresent()) {
                getWebStudio().reset();
            }
        } catch (ProjectException e) {
            throw new ConflictException("project.status.update.failed.message");
        }
    }

    @PostMapping("/{projectId}/branches")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Create branch (BETA)")
    public void createBranch(@ProjectId @PathVariable("projectId") RulesProject project,
                             @RequestBody CreateBranchModel request) {
        var repository = project.getDesignRepository();
        if (!project.isSupportsBranches()) {
            throw new ConflictException("project.branch.unsupported.message");
        }
        var validator = newBranchValidatorFactory.apply((BranchRepository) repository);
        validationProvider.validate(request.getBranch(), validator);
        try {
            projectService.createBranch(project, request);
            getWebStudio().reset();
        } catch (ProjectException e) {
            throw new ConflictException("project.branch.create.failed.message");
        }
    }

    @GetMapping("/{projectId}/tables")
    @Operation(summary = "Get project tables (BETA)")
    @Parameters({@Parameter(name = "kind", description = "Table kinds", in = ParameterIn.QUERY),
            @Parameter(name = "name", description = "Table name fragment", in = ParameterIn.QUERY),
            @Parameter(name = "properties", description = "Project properties. Must start with `properties.` ", in = ParameterIn.QUERY, style = ParameterStyle.FORM, schema = @Schema(implementation = Object.class), explode = Explode.TRUE)})
    public Collection<SummaryTableView> getTables(@ProjectId @PathVariable("projectId") RulesProject project,
                                                  @Parameter(hidden = true) @RequestParam Map<String, String> params,
                                                  @RequestParam(value = "kind", required = false) Set<String> kinds,
                                                  @RequestParam(value = "name", required = false) String name) {

        var queryBuilder = ProjectTableCriteriaQuery.builder().kinds(kinds).name(name);

        params.entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith(PROPERTIES_PREFIX))
                .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
                .forEach(entry -> {
                    var tag = entry.getKey().substring(PROPERTIES_PREFIX.length());
                    queryBuilder.property(tag, entry.getValue());
                });

        return projectService.getTables(project, queryBuilder.build());
    }

    @GetMapping("/{projectId}/tables/{tableId}")
    @Operation(summary = "Get project table (BETA)")
    public EditableTableView getTable(@ProjectId @PathVariable("projectId") RulesProject project,
                                      @PathVariable("tableId") String tableId) {
        return (EditableTableView) projectService.getTable(project, tableId);
    }

    @Operation(summary = "Update project table (BETA)")
    @PutMapping("/{projectId}/tables/{tableId}")
    public void updateTable(@ProjectId @PathVariable("projectId") RulesProject project,
                            @PathVariable("tableId") String tableId,
                            @RequestBody EditableTableView editTable) throws ProjectException {
        try {
            projectService.updateTable(project, tableId, editTable);
        } finally {
            getWebStudio().reset();
        }
    }

    @Operation(summary = "Append project table (BETA)")
    @PostMapping("/{projectId}/tables/{tableId}/lines")
    public void appendTable(@ProjectId @PathVariable("projectId") RulesProject project,
                            @PathVariable("tableId") String tableId,
                            @RequestBody AppendTableView editTable) throws ProjectException {
        try {
            projectService.appendTableLines(project, tableId, editTable);
        } finally {
            getWebStudio().reset();
        }
    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Parameter(description = "Project ID", in = ParameterIn.PATH, required = true, schema = @Schema(implementation = String.class))
    public @interface ProjectId {

    }

}
