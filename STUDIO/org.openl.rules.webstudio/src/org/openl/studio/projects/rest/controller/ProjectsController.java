package org.openl.studio.projects.rest.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.Reference;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import org.openl.rules.common.ProjectException;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.rest.model.UserInfoModel;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.testmethod.export.TestResultExport;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.GenericView;
import org.openl.studio.common.model.PageResponse;
import org.openl.studio.common.model.ResultNotReadyView;
import org.openl.studio.common.utils.WebTool;
import org.openl.studio.projects.messaging.SocketProjectAllTestsExecutionProgressListenerFactory;
import org.openl.studio.projects.model.BranchScope;
import org.openl.studio.projects.model.CreateBranchModel;
import org.openl.studio.projects.model.MigrationScope;
import org.openl.studio.projects.model.ModuleViewModel;
import org.openl.studio.projects.model.ProjectBranchInfo;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.ProjectInclude;
import org.openl.studio.projects.model.ProjectMigrationView;
import org.openl.studio.projects.model.ProjectStatusUpdateModel;
import org.openl.studio.projects.model.ProjectViewModel;
import org.openl.studio.projects.model.ProjectsPageResponse;
import org.openl.studio.projects.model.PropertyDefinitionView;
import org.openl.studio.projects.model.project.status.ProjectStatusViewModel;
import org.openl.studio.projects.model.tables.AppendTableView;
import org.openl.studio.projects.model.tables.CopyTableRequest;
import org.openl.studio.projects.model.tables.CreateNewTableRequest;
import org.openl.studio.projects.model.tables.EditableTableView;
import org.openl.studio.projects.model.tables.RawTableSourceAction;
import org.openl.studio.projects.model.tables.RawTableSourceActions;
import org.openl.studio.projects.model.tables.SummaryTableView;
import org.openl.studio.projects.model.tables.TableDetailsView;
import org.openl.studio.projects.model.tables.TableEditorsView;
import org.openl.studio.projects.model.tables.TableIdView;
import org.openl.studio.projects.model.tables.TableInputView;
import org.openl.studio.projects.model.tables.TableNodeView;
import org.openl.studio.projects.model.tables.TablePropertiesUpdate;
import org.openl.studio.projects.model.tables.TablePropertiesView;
import org.openl.studio.projects.model.tables.TableSearchScope;
import org.openl.studio.projects.model.tables.TableSort;
import org.openl.studio.projects.model.tables.TableTargetView;
import org.openl.studio.projects.model.tables.TableTestView;
import org.openl.studio.projects.model.tables.TableView;
import org.openl.studio.projects.model.tables.TestCaseView;
import org.openl.studio.projects.model.tests.TestCaseExecutionResult;
import org.openl.studio.projects.model.tests.TestExecutionSummaryQuery;
import org.openl.studio.projects.model.tests.TestUnitExecutionResult;
import org.openl.studio.projects.model.tests.TestsExecutionSummary;
import org.openl.studio.projects.model.tests.TestsExecutionSummaryResponseMapper;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.ProjectCriteriaQuery;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.ProjectMetadataService;
import org.openl.studio.projects.service.ProjectMigrationService;
import org.openl.studio.projects.service.ProjectObjectMapperService;
import org.openl.studio.projects.service.ProjectTableCriteriaQuery;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.merge.ProjectsMergeConflictsSessionHolder;
import org.openl.studio.projects.service.project.status.ProjectStatusMapper;
import org.openl.studio.projects.service.tables.TableInputService;
import org.openl.studio.projects.service.tables.graph.GraphDirection;
import org.openl.studio.projects.service.tables.graph.GraphLayer;
import org.openl.studio.projects.service.tables.graph.ProjectTablesGraphService;
import org.openl.studio.projects.service.tests.ExecutionTestsResultRegistry;
import org.openl.studio.projects.service.tests.RetainedTestUnit;
import org.openl.studio.projects.service.tests.TestExecutionStatus;
import org.openl.studio.projects.service.tests.TestsExecutorService;
import org.openl.studio.repositories.model.ProjectRevision;
import org.openl.studio.repositories.model.RepositoryConfigModel;
import org.openl.studio.repositories.service.ProjectRevisionService;
import org.openl.studio.repositories.service.RepositoryConfigService;
import org.openl.studio.rest.resolver.PaginationDefault;
import org.openl.util.StringUtils;

/**
 * Projects REST controller
 *
 * @author Vladyslav Pikus
 */
@RestController
@RequestMapping(value = "/projects", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects (BETA)", description = "Experimental projects API")
@Validated
@RequiredArgsConstructor
@Slf4j
public class ProjectsController {

    /** How many cases of a test table a page carries unless the client asks for another size. */
    private static final int TEST_CASE_PAGE_SIZE = 25;

    private static final String TAGS_PREFIX = "tags.";
    private static final String PROPERTIES_PREFIX = "properties.";
    private static final String APPLICATION_XLSX_MEDIATYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final WorkspaceProjectService projectService;
    private final TestsExecutorService testsExecutorService;
    private final ExecutionTestsResultRegistry executionTestsResultRegistry;
    private final SocketProjectAllTestsExecutionProgressListenerFactory socketProjectAllTestsExecutionProgressListenerFactory;
    private final ProjectObjectMapperService objectMapperService;
    private final ProjectsMergeConflictsSessionHolder conflictsSessionHolder;
    private final ProjectIdentifierMapper projectIdentifierMapper;
    private final ProjectStatusMapper projectStatusMapper;
    private final ProjectTablesGraphService graphService;
    private final RepositoryConfigService repositoryConfigService;
    private final ProjectMetadataService metadataService;
    private final ProjectMigrationService migrationService;
    private final ProjectRevisionService projectRevisionService;
    private final TableInputService tableInputService;

    @Lookup
    public WebStudio getWebStudio() {
        // Spring overrides this method with a lookup of the bean; the stub itself never runs.
        throw new UnsupportedOperationException("Overridden by the Spring @Lookup container");
    }

    /**
     * Marks the module a write changed to be compiled again, and nothing besides it.
     *
     * <p>Called where a write returned, never where it was refused: a write that did not happen changed no
     * workbook, and asking for the module to be built again on account of it throws away what the session has
     * compiled for nothing.
     */
    private void recompileWrittenModule() {
        recompileWrittenModule(true);
    }

    /**
     * The same, for a write that may have added a module to the project rather than written into one.
     *
     * @param intoAModuleOfItsOwn whether the write landed in a module the project already had
     */
    private void recompileWrittenModule(boolean intoAModuleOfItsOwn) {
        if (intoAModuleOfItsOwn) {
            getWebStudio().recompileCurrentModule();
        } else {
            getWebStudio().reset();
        }
    }

    @Lookup
    protected SchemaGenerator getSchemaGenerator(ObjectMapper objectMapper) {
        // Spring overrides this method with a lookup of the bean; the stub itself never runs.
        throw new UnsupportedOperationException("Overridden by the Spring @Lookup container");
    }

    /** The generator for the input a table takes. It also records the defaults a datatype declares. */
    @Lookup("inputSchemaGenerator")
    protected SchemaGenerator getInputSchemaGenerator(ObjectMapper objectMapper) {
        // Spring overrides this method with a lookup of the bean; the stub itself never runs.
        throw new UnsupportedOperationException("Overridden by the Spring @Lookup container");
    }

    @GetMapping
    @Operation(summary = "projects.list.summary")
    @Parameters({
            @Parameter(name = "status", description = "projects.list.param.status.desc", in = ParameterIn.QUERY, schema = @Schema(allowableValues = {
                    "LOCAL",
                    "DELETED",
                    "OPENED",
                    "VIEWING_VERSION",
                    "EDITING",
                    "CLOSED"})),
            @Parameter(name = "repository", description = "projects.list.param.repository.desc", in = ParameterIn.QUERY),
            @Parameter(name = "dependsOn", description = "projects.list.param.depends-on.desc", in = ParameterIn.QUERY),
            @Parameter(name = "name", description = "projects.list.param.name.desc", in = ParameterIn.QUERY),
            @Parameter(name = "author", description = "projects.list.param.author.desc", in = ParameterIn.QUERY),
            @Parameter(name = "branch", description = "projects.list.param.branch.desc", in = ParameterIn.QUERY),
            @Parameter(name = "sort", description = "projects.list.param.sort.desc", in = ParameterIn.QUERY, schema = @Schema(allowableValues = {"name", "status", "updated"})),
            @Parameter(
                    name = "include",
                    description = "projects.list.param.include.desc",
                    in = ParameterIn.QUERY,
                    style = ParameterStyle.FORM,
                    explode = Explode.TRUE,
                    array = @ArraySchema(schema = @Schema(implementation = ProjectInclude.class))),
            @Parameter(name = "tags", description = "projects.list.param.tags.desc", in = ParameterIn.QUERY, style = ParameterStyle.FORM, schema = @Schema(implementation = Object.class), explode = Explode.TRUE)
    })
    @JsonView(GenericView.Full.class)
    public ProjectsPageResponse getProjects(@Parameter(hidden = true) @RequestParam MultiValueMap<String, String> params,
                                                      @RequestParam(value = "status", required = false) List<ProjectStatus> statuses,
                                                      @RequestParam(value = "repository", required = false) List<String> repositories,
                                                      @RequestParam(value = "dependsOn", required = false) String dependsOn,
                                                      @RequestParam(value = "name", required = false) String name,
                                                      @RequestParam(value = "author", required = false) String author,
                                                      @RequestParam(value = "branch", required = false) String branch,
                                                      @RequestParam(value = "sort", required = false) @Nullable String sort,
                                                      @RequestParam(value = "include", required = false) List<ProjectInclude> includes,
                                                      @PaginationDefault Pageable page) {
        var queryBuilder = ProjectCriteriaQuery.builder()
                .repositoryIds(repositories)
                .statuses(statuses)
                .name(name)
                .author(author)
                .branch(branch)
                .sort(sort)
                .includes(ProjectInclude.normalize(includes));

        if (StringUtils.isNotEmpty(dependsOn)) {
            queryBuilder.dependsOn(ProjectIdModel.decode(dependsOn));
        }

        var tagValues = new LinkedHashMap<String, Set<String>>();
        params.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(TAGS_PREFIX))
                .forEach(entry -> {
                    var tag = entry.getKey().substring(TAGS_PREFIX.length());
                    var values = new LinkedHashSet<String>();
                    entry.getValue().stream()
                            .filter(StringUtils::isNotBlank)
                            .forEach(values::add);
                    if (!values.isEmpty()) {
                        tagValues.put(tag, values);
                    }
                });
        queryBuilder.tagValues(tagValues);
        return projectService.getProjects(queryBuilder.build(), page);
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "projects.get.summary")
    @JsonView(GenericView.Detailed.class)
    public ProjectViewModel getProject(@ProjectId @PathVariable("projectId") RulesProject project,
                                       @Parameter(description = "projects.get.param.include.desc",
                                               style = ParameterStyle.FORM,
                                               explode = Explode.TRUE,
                                               array = @ArraySchema(schema = @Schema(implementation = ProjectInclude.class)))
                                       @RequestParam(value = "include", required = false) List<ProjectInclude> includes) {
        return projectService.getProject(project, includes);
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "projects.delete.summary")
    public void deleteProject(@ProjectId @PathVariable("projectId") RulesProject project,
                              @Parameter(description = "projects.delete.param.comment.desc")
                              @RequestParam(value = "comment", required = false) @Nullable String comment) {
        projectService.delete(project, comment);
    }

    @PatchMapping("/{projectId}")
    @Operation(summary = "projects.status.update.summary")
    public void updateProjectStatus(@ProjectId @PathVariable("projectId") RulesProject project,
                                    @Valid @RequestBody ProjectStatusUpdateModel request) {
        var projectId = projectIdentifierMapper.map(project);
        if (conflictsSessionHolder.hasConflictInfo(projectId)) {
            throw new ConflictException("project.unresolved.merge.conflicts.message");
        }
        var normalized = normalize(request);
        try {
            projectService.updateProjectStatus(project, normalized);
            if (normalized.status() != null
                    || normalized.branch() != null
                    || Boolean.TRUE.equals(normalized.save())
                    || normalized.comment() != null
                    || normalized.revision() != null) {
                getWebStudio().reset();
            }
        } catch (ProjectException e) {
            // The answer carries a code only, so without this the failure leaves no trace anywhere.
            log.error("Failed to update the status of project '{}'.", project.getName(), e);
            throw new ConflictException("project.status.update.failed.message");
        }
    }

    @DeleteMapping("/{projectId}/lock")
    @Operation(summary = "projects.unlock.summary")
    public void unlockProject(@ProjectId @PathVariable("projectId") RulesProject project) {
        projectService.unlockProject(project);
        getWebStudio().reset();
    }

    @GetMapping("/{projectId}/status")
    @Operation(summary = "projects.status.get.summary", description = "projects.status.get.desc")
    @Deprecated(forRemoval = false)
    @JsonView(GenericView.Detailed.class)
    public ProjectStatusViewModel getStatus(@ProjectId @PathVariable("projectId") RulesProject project,
                                            @Parameter(description = "projects.status.param.branch.desc")
                                            @RequestParam(value = "branch", required = false) String branch) {
        // Read-only by design — the `branch` parameter is asserted against the project's
        // current branch; switching is exposed via PATCH /{projectId} with a
        // ProjectStatusUpdateModel that carries the target branch.
        if (StringUtils.isNotBlank(branch)) {
            if (!project.isSupportsBranches()) {
                throw new ConflictException("project.branch.unsupported.message");
            }
            if (!Objects.equals(branch, project.getBranch())) {
                throw new ConflictException("project.branch.mismatch.message");
            }
        }
        return projectStatusMapper.map(project);
    }

    @PostMapping("/{projectId}/branches")
    @Operation(summary = "projects.branch.create.summary")
    public void createBranch(@ProjectId @PathVariable("projectId") RulesProject project,
                             @Valid @RequestBody CreateBranchModel request) {
        try {
            projectService.createBranch(project, request);
            getWebStudio().reset();
        } catch (ProjectException e) {
            throw new ConflictException("project.branch.create.failed.message");
        }
    }

    @GetMapping("/{projectId}/repository-config")
    @Operation(summary = "projects.repository-config.get.summary", description = "projects.repository-config.get.desc")
    public RepositoryConfigModel getRepositoryConfig(@ProjectId @PathVariable("projectId") RulesProject project) {
        // Scoped to the project so that a user granted access to this project only — and not to the whole
        // repository — still gets the settings the project forms need. A project that lives only in the
        // workspace has no repository to be configured by.
        return project.isLocalOnly()
                ? RepositoryConfigModel.none()
                : repositoryConfigService.getConfig(project.getDesignRepository().getId());
    }

    @GetMapping("/{projectId}/history")
    @Operation(summary = "projects.history.list.summary", description = "projects.history.list.desc")
    @JsonView(UserInfoModel.View.Short.class)
    public PageResponse<ProjectRevision> getHistory(@ProjectId @PathVariable("projectId") RulesProject project,
                                                    @Parameter(description = "repo.param.branch-name.desc") @RequestParam(value = "branch", required = false) String branch,
                                                    @Parameter(description = "repo.param.search.desc") @RequestParam(value = "search", required = false) String search,
                                                    @Parameter(description = "repo.param.techRevs.desc") @RequestParam(value = "techRevs", required = false, defaultValue = "false") boolean techRevs,
                                                    @PaginationDefault Pageable page) throws IOException {
        return projectRevisionService.getProjectRevision(project, branch, search, techRevs, page);
    }

    @GetMapping("/{projectId}/branches")
    @Operation(summary = "projects.branch.list.summary")
    public List<ProjectBranchInfo> getBranches(@ProjectId @PathVariable("projectId") RulesProject project,
                                               @Parameter(description = "projects.branch.list.param.scope.desc")
                                               @RequestParam(value = "scope", required = false,
                                                       defaultValue = "project") BranchScope scope) {
        return projectService.getBranches(project, scope);
    }

    @DeleteMapping("/{projectId}/branches/{*branch}")
    @Operation(summary = "projects.branch.delete.summary", description = "projects.branch.delete.desc")
    public void deleteBranch(@ProjectId @PathVariable("projectId") RulesProject project,
                             @Parameter(description = "repo.param.branch-name.desc") @PathVariable("branch") String branch,
                             @Parameter(description = "projects.merge.param.force.desc")
                             @RequestParam(value = "force", required = false, defaultValue = "false") boolean force) {
        // Branch names may contain '/' (e.g. "project/user/date"), so the branch is captured as a trailing path
        // segment via {*branch}, which Spring exposes with a leading slash that must be removed.
        var branchName = branch.startsWith("/") ? branch.substring(1) : branch;
        if (StringUtils.isBlank(branchName)) {
            throw new BadRequestException("project.branch.name.empty.message");
        }
        projectService.deleteBranch(project, branchName, force);
        getWebStudio().reset();
    }

    @GetMapping("/{projectId}/tables")
    @Operation(summary = "projects.tables.list.summary")
    @Parameters({
            @Parameter(name = "kind", description = "projects.tables.list.param.kind.desc", in = ParameterIn.QUERY, schema = @Schema(implementation = String.class,
                    allowableValues = {
                            "Rules",
                            "Spreadsheet",
                            "Datatype",
                            "Data",
                            "Test",
                            "TBasic",
                            "Column Match",
                            "Method",
                            "Run",
                            "Constants",
                            "Conditions",
                            "Actions",
                            "Returns",
                            "Environment",
                            "Properties",
                            "Other"
                    })),
            @Parameter(name = "name", description = "projects.tables.list.param.name.desc", in = ParameterIn.QUERY),
            @Parameter(name = "module", description = "projects.tables.list.param.module.desc", in = ParameterIn.QUERY),
            @Parameter(name = "scope", description = "projects.tables.list.param.scope.desc", in = ParameterIn.QUERY,
                    schema = @Schema(implementation = TableSearchScope.class)),
            @Parameter(name = "header", description = "projects.tables.list.param.header.desc", in = ParameterIn.QUERY),
            @Parameter(name = "text", description = "projects.tables.list.param.text.desc", in = ParameterIn.QUERY),
            @Parameter(name = "properties", description = "projects.tables.list.param.properties.desc", in = ParameterIn.QUERY, style = ParameterStyle.FORM, schema = @Schema(implementation = Object.class), explode = Explode.TRUE)
    })
    public PageResponse<SummaryTableView> getTables(@ProjectId @PathVariable("projectId") RulesProject project,
                                                    @Parameter(hidden = true) @RequestParam Map<String, String> params,
                                                    @RequestParam(value = "kind", required = false) Set<String> kinds,
                                                    @RequestParam(value = "name", required = false) String name,
                                                    @RequestParam(value = "module", required = false) String module,
                                                    @RequestParam(value = "scope", required = false) TableSearchScope scope,
                                                    @RequestParam(value = "header", required = false) String header,
                                                    @RequestParam(value = "text", required = false) String text,
                                                    @RequestParam(value = "includeOther", defaultValue = "false") @Parameter(description = "projects.tables.list.param.include-other.desc") boolean includeOther,
                                                    @RequestParam(value = "sort", defaultValue = "name") @Parameter(description = "projects.tables.list.param.sort.desc") TableSort sort,
                                                    @PaginationDefault Pageable page) {

        var queryBuilder = ProjectTableCriteriaQuery.builder()
                .kinds(kinds)
                .name(name)
                .module(module)
                .scope(scope)
                .header(header)
                .text(text)
                .includeOther(includeOther)
                .sort(sort);
        params.entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith(PROPERTIES_PREFIX))
                .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
                .forEach(entry -> {
                    var tag = entry.getKey().substring(PROPERTIES_PREFIX.length());
                    queryBuilder.property(tag, entry.getValue());
                });

        return projectService.getTables(project, queryBuilder.build(), page);
    }

    @Operation(summary = "projects.tables.create.summary")
    @Parameter(name = "projectId", description = "projects.param.project-id.desc", in = ParameterIn.PATH, required = true, schema = @Schema(implementation = String.class))
    @PostMapping("/{projectId}/tables")
    @ResponseStatus(HttpStatus.CREATED)
    public SummaryTableView createNewTable(@ProjectId @PathVariable("projectId") RulesProject project,
                                           @Valid @RequestBody CreateNewTableRequest request) throws ProjectException {
        var tableId = projectService.createNewTable(project, request);
        // A table written into a module that did not exist before changes what the project is made of, not just
        // what one module holds, so the session is told to read the project again.
        recompileWrittenModule(tableId != null);
        var table = (TableView) request.table();
        return projectService.getCreatedTable(project, request.moduleName(), tableId, table.name);
    }

    @Operation(summary = "projects.tables.copy.summary", description = "projects.tables.copy.desc")
    @Parameter(name = "projectId", description = "projects.param.project-id.desc", in = ParameterIn.PATH, required = true, schema = @Schema(implementation = String.class))
    @PostMapping("/{projectId}/tables/{tableId}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    public SummaryTableView copyTable(@ProjectId @PathVariable("projectId") RulesProject project,
                                      @PathVariable("tableId") @Parameter(description = "project.table.id.desc") String tableId,
                                      @Valid @RequestBody CopyTableRequest request) throws ProjectException {
        var copyId = projectService.copyTable(project, tableId, request);
        recompileWrittenModule(copyId != null);
        // Read the copy back by its own id: a copy kept under the source's name cannot be told apart by name.
        return projectService.getCreatedTable(project, request.moduleName(), copyId, request.name());
    }

    @GetMapping("/{projectId}/modules")
    @Operation(summary = "projects.modules.list.summary")
    public List<ModuleViewModel> getModules(@ProjectId @PathVariable("projectId") RulesProject project) {
        return projectService.getModules(project);
    }

    @GetMapping("/{projectId}/migration")
    @Operation(summary = "projects.migration.get.summary", description = "projects.migration.get.desc")
    public ProjectMigrationView getMigration(@ProjectId @PathVariable("projectId") RulesProject project) {
        return migrationService.migrationInfo(project);
    }

    @PostMapping("/{projectId}/migrate")
    @Operation(summary = "projects.migration.migrate.summary", description = "projects.migration.migrate.desc")
    public void migrate(@ProjectId @PathVariable("projectId") RulesProject project,
            @Parameter(description = "projects.migration.migrate.param.scope.desc") @RequestParam("scope") MigrationScope scope) {
        migrationService.migrate(project, scope);
        getWebStudio().reset();
    }

    @PostMapping("/{projectId}/modules/{moduleName}/compile")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "projects.modules.compile.summary", description = "projects.modules.compile.desc")
    public void compileModule(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable("moduleName") @Parameter(description = "projects.modules.param.module-name.desc")
            String moduleName,
            @RequestParam(value = "reset", defaultValue = "false")
            @Parameter(description = "projects.modules.compile.param.reset.desc") boolean reset) {
        projectService.compileModule(project, moduleName, reset);
    }

    @DeleteMapping("/{projectId}/modules/{moduleName}/compile")
    @Operation(summary = "projects.modules.compile.cancel.summary", description = "projects.modules.compile.cancel.desc")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void cancelModuleCompilation(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable("moduleName") @Parameter(description = "projects.modules.param.module-name.desc")
            String moduleName) {
        projectService.cancelModuleCompilation(project, moduleName);
    }

    @GetMapping("/{projectId}/modules/{moduleName}/sheets")
    @Operation(summary = "projects.modules.sheets.summary")
    public List<String> getModuleSheets(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable("moduleName") @Parameter(description = "projects.modules.param.module-name.desc")
            String moduleName) {
        return projectService.getModuleSheets(project, moduleName);
    }

    @GetMapping("/{projectId}/properties")
    @Operation(summary = "projects.properties.list.summary")
    public List<PropertyDefinitionView> getProperties(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam(value = "tableType", required = false)
            @Parameter(description = "projects.properties.list.param.table-type.desc")
            @Nullable String tableType) {
        // The properties are the same for every project, but the path variable is still resolved: resolving it is
        // what checks that the caller may read this project.
        return metadataService.getProperties(tableType);
    }

    @GetMapping("/{projectId}/tables/{tableId}")
    @Operation(summary = "projects.table.get.summary")
    public EditableTableView getTable(@ProjectId @PathVariable("projectId") RulesProject project,
                                      @PathVariable("tableId") @Parameter(description = "project.table.id.desc") String tableId,
                                      @RequestParam(value = "raw", defaultValue = "false") @Parameter(description = "projects.table.get.param.raw.desc") boolean raw,
                                      @RequestParam(value = "startRow", required = false) @Min(0) @Parameter(description = "projects.table.get.param.start-row.desc") Integer startRow,
                                      @RequestParam(value = "maxRows", required = false) @Min(1) @Parameter(description = "projects.table.get.param.max-rows.desc") Integer maxRows,
                                      @RequestParam(value = "styles", defaultValue = "false") @Parameter(description = "projects.table.get.param.styles.desc") boolean styles,
                                      @RequestParam(value = "metaInfo", defaultValue = "false") @Parameter(description = "projects.table.get.param.meta-info.desc") boolean metaInfo,
                                      @RequestParam(value = "module", required = false) @Parameter(description = "projects.table.get.param.module.desc") String module,
                                      @RequestParam(value = "runState", defaultValue = "false") @Parameter(description = "projects.table.get.param.run-state.desc") boolean runState) {
        var read = raw
                ? projectService.getTableRaw(project, tableId, startRow, maxRows, styles, metaInfo, module)
                : (EditableTableView) projectService.getTable(project, tableId, module);
        if (runState && read instanceof TableView view) {
            view.runState = projectService.getTableRunState(project, tableId, module);
        }
        return read;
    }

    @GetMapping("/{projectId}/tables/{tableId}/tests")
    @Operation(summary = "projects.table.tests.summary", description = "projects.table.tests.desc")
    public List<TableTestView> getTableTests(@ProjectId @PathVariable("projectId") RulesProject project,
                                             @PathVariable("tableId") String tableId,
                                             @RequestParam(value = "module", required = false)
                                             @Parameter(description = "projects.table.get.param.module.desc")
                                             String module) {
        return projectService.getTableTests(project, tableId, module);
    }

    @GetMapping("/{projectId}/tables/{tableId}/targets")
    @Operation(summary = "projects.table.targets.summary", description = "projects.table.targets.desc")
    public List<TableTargetView> getTableTargets(@ProjectId @PathVariable("projectId") RulesProject project,
                                                 @PathVariable("tableId") @Parameter(description = "project.table.id.desc") String tableId,
                                                 @RequestParam(value = "module", required = false)
                                                 @Parameter(description = "projects.table.get.param.module.desc")
                                                 String module) {
        return projectService.getTableTargets(project, tableId, module);
    }

    @GetMapping("/{projectId}/tables/{tableId}/properties")
    @Operation(summary = "projects.table.properties.summary", description = "projects.table.properties.desc")
    public TablePropertiesView getTableProperties(@ProjectId @PathVariable("projectId") RulesProject project,
                                                  @PathVariable("tableId") @Parameter(description = "project.table.id.desc") String tableId) {
        return projectService.getTableProperties(project, tableId);
    }

    @GetMapping("/{projectId}/tables/{tableId}/details")
    @Operation(summary = "projects.table.details.summary", description = "projects.table.details.desc")
    public TableDetailsView getTableDetails(@ProjectId @PathVariable("projectId") RulesProject project,
                                            @PathVariable("tableId") @Parameter(description = "project.table.id.desc")
                                            String tableId,
                                            @RequestParam(value = "module", required = false)
                                            @Parameter(description = "projects.table.get.param.module.desc")
                                            String module) {
        return projectService.getTableDetails(project, tableId, module);
    }

    @GetMapping("/{projectId}/tables/{tableId}/editors")
    @Operation(summary = "projects.table.editors.summary", description = "projects.table.editors.desc")
    public TableEditorsView getTableEditors(@ProjectId @PathVariable("projectId") RulesProject project,
                                            @PathVariable("tableId") @Parameter(description = "project.table.id.desc")
                                            String tableId,
                                            @RequestParam(value = "startRow", required = false) @Min(0)
                                            @Parameter(description = "projects.table.get.param.start-row.desc")
                                            Integer startRow,
                                            @RequestParam(value = "maxRows", required = false) @Min(1)
                                            @Parameter(description = "projects.table.get.param.max-rows.desc")
                                            Integer maxRows,
                                            @RequestParam(value = "module", required = false)
                                            @Parameter(description = "projects.table.get.param.module.desc")
                                            String module) {
        return projectService.getTableEditors(project, tableId, startRow, maxRows, module);
    }

    @GetMapping(value = "/{projectId}/messages/{messageId}/stacktrace", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "projects.message.stacktrace.summary", description = "projects.message.stacktrace.desc")
    public String getMessageStacktrace(@ProjectId @PathVariable("projectId") RulesProject project,
                                       @PathVariable("messageId")
                                       @Parameter(description = "projects.message.stacktrace.param.id.desc")
                                       long messageId,
                                       @RequestParam(value = "module", required = false)
                                       @Parameter(description = "projects.table.get.param.module.desc")
                                       String module) {
        return projectService.getMessageStacktrace(project, messageId, module);
    }

    @GetMapping("/{projectId}/tables/{tableId}/input")
    @Operation(summary = "projects.table.input.summary", description = "projects.table.input.desc")
    public TableInputView getTableInput(@ProjectId @PathVariable("projectId") RulesProject project,
                                        @PathVariable("tableId") @Parameter(description = "project.table.id.desc") String tableId,
                                        @RequestParam(value = "fromModule", required = false) @Parameter(description = "projects.table.input.param.from-module.desc") String fromModule) {
        var moduleName = StringUtils.trimToNull(fromModule);
        var projectModel = projectService.openProject(project, moduleName).awaitCompiled();
        var objectMapper = objectMapperService.createObjectMapper();
        return tableInputService.describe(projectModel, requireTable(projectModel, tableId), moduleName != null,
                objectMapper, getInputSchemaGenerator(objectMapper));
    }

    @GetMapping("/{projectId}/tables/{tableId}/input/cases")
    @Operation(summary = "projects.table.input-cases.summary", description = "projects.table.input-cases.desc")
    public PageResponse<TestCaseView> getTableInputCases(@ProjectId @PathVariable("projectId") RulesProject project,
                                                         @PathVariable("tableId") @Parameter(description = "project.table.id.desc") String tableId,
                                                         @RequestParam(value = "fromModule", required = false) @Parameter(description = "projects.table.input.param.from-module.desc") String fromModule,
                                                         @PaginationDefault(size = TEST_CASE_PAGE_SIZE) Pageable page) {
        var moduleName = StringUtils.trimToNull(fromModule);
        var projectModel = projectService.openProject(project, moduleName).awaitCompiled();
        var objectMapper = objectMapperService.createObjectMapper();
        return tableInputService.listTestCases(projectModel, requireTable(projectModel, tableId), moduleName != null,
                page, objectMapper, getInputSchemaGenerator(objectMapper));
    }

    @GetMapping("/{projectId}/tables/{tableId}/input/cases/{caseId}")
    @Operation(summary = "projects.table.input-case.summary", description = "projects.table.input-case.desc")
    public TestCaseView getTableInputCase(@ProjectId @PathVariable("projectId") RulesProject project,
                                          @PathVariable("tableId") @Parameter(description = "project.table.id.desc") String tableId,
                                          @PathVariable("caseId") @Parameter(description = "projects.table.input-case.param.case-id.desc") String caseId,
                                          @RequestParam(value = "fromModule", required = false) @Parameter(description = "projects.table.input.param.from-module.desc") String fromModule) {
        var moduleName = StringUtils.trimToNull(fromModule);
        var projectModel = projectService.openProject(project, moduleName).awaitCompiled();
        var objectMapper = objectMapperService.createObjectMapper();
        return tableInputService.describeTestCase(projectModel, requireTable(projectModel, tableId), moduleName != null,
                caseId, objectMapper, getInputSchemaGenerator(objectMapper));
    }

    private static IOpenLTable requireTable(ProjectModel projectModel, String tableId) {
        var table = projectModel.getTableById(tableId);
        if (table == null) {
            throw new NotFoundException("table.message");
        }
        return table;
    }

    @GetMapping("/{projectId}/tables/graph")
    @Operation(summary = "project.tables.graph.summary", description = "project.tables.graph.desc")
    public List<TableNodeView> getTablesGraph(@ProjectId @PathVariable("projectId") RulesProject project,
                                              @RequestParam(value = "module", required = false) @Parameter(description = "project.tables.graph.module.desc") String module,
                                              @RequestParam(value = "layer", defaultValue = "all") @Parameter(description = "project.tables.graph.layer.desc") GraphLayer layer) {
        // a blank `?module=` means the whole project, not a module named "" (which would fail to resolve)
        var moduleName = StringUtils.trimToNull(module);
        var model = projectService.openProject(project, moduleName).awaitCompiled();
        return graphService.buildProjectGraph(model, moduleName != null, layer);
    }

    @GetMapping("/{projectId}/tables/{tableId}/graph")
    @Operation(summary = "project.table.graph.summary", description = "project.table.graph.desc")
    public List<TableNodeView> getTableGraph(@ProjectId @PathVariable("projectId") RulesProject project,
                                             @PathVariable("tableId") @Parameter(description = "project.table.id.desc") String tableId,
                                             @RequestParam(value = "direction", defaultValue = "BOTH") @Parameter(description = "project.table.graph.direction.desc") GraphDirection direction,
                                             @RequestParam(value = "depth", required = false) @Min(1) @Parameter(description = "project.table.graph.depth.desc") Integer depth) {
        var model = projectService.openProject(project).awaitCompiled();
        if (model.getTableById(tableId) == null) {
            throw new NotFoundException("table.message");
        }
        return graphService.buildTableGraph(model, tableId, direction, depth);
    }

    @Operation(summary = "project.table.update.summary", description = "project.table.update.desc")
    @ApiResponse(responseCode = "200", description = "project.table.update.200.desc", headers = @Header(name = HttpHeaders.LOCATION, description = "header.location.desc"))
    @ApiResponse(responseCode = "204", description = "project.table.update.204.desc")
    @PutMapping("/{projectId}/tables/{tableId}")
    public ResponseEntity<TableIdView> updateTable(@ProjectId @PathVariable("projectId") RulesProject project,
                                                   @PathVariable("tableId") @Parameter(description = "project.table.id.desc") String tableId,
                                                   @Valid @RequestBody EditableTableView editTable,
                                                   @RequestParam(value = "module", required = false) @Parameter(description = "projects.table.get.param.module.desc") String module) throws ProjectException {
        var newTableId = projectService.updateTable(project, tableId, editTable, module);
        recompileWrittenModule();
        return tableWriteResponse(tableId, newTableId);
    }

    @Operation(summary = "project.table.append.summary", description = "project.table.append.desc")
    @ApiResponse(responseCode = "200", description = "project.table.append.200.desc", headers = @Header(name = HttpHeaders.LOCATION, description = "header.location.desc"))
    @ApiResponse(responseCode = "204", description = "project.table.append.204.desc")
    @PostMapping("/{projectId}/tables/{tableId}/lines")
    public ResponseEntity<TableIdView> appendTable(@ProjectId @PathVariable("projectId") RulesProject project,
                                                   @PathVariable("tableId") @Parameter(description = "project.table.id.desc") String tableId,
                                                   @Valid @RequestBody AppendTableView editTable,
                                                   @RequestParam(value = "module", required = false) @Parameter(description = "projects.table.get.param.module.desc") String module) throws ProjectException {
        var newTableId = projectService.appendTableLines(project, tableId, editTable, module);
        recompileWrittenModule();
        return tableWriteResponse(tableId, newTableId);
    }

    @Operation(summary = "project.table.actions.summary", description = "project.table.actions.desc")
    @ApiResponse(responseCode = "200", description = "project.table.actions.200.desc", headers = @Header(name = HttpHeaders.LOCATION, description = "header.location.desc"))
    @ApiResponse(responseCode = "204", description = "project.table.actions.204.desc")
    @PostMapping("/{projectId}/tables/{tableId}/actions")
    public ResponseEntity<TableIdView> editTableSource(@ProjectId @PathVariable("projectId") RulesProject project,
                                                       @PathVariable("tableId") @Parameter(description = "project.table.id.desc") String tableId,
                                                       @Valid @RequestBody RawTableSourceAction action,
                                                       @RequestParam(value = "module", required = false) @Parameter(description = "projects.table.get.param.module.desc") String module) throws ProjectException {
        var newTableId = projectService.editTableSource(project, tableId, List.of(action), module);
        recompileWrittenModule();
        return tableWriteResponse(tableId, newTableId);
    }

    @Operation(summary = "project.table.actions.batch.summary", description = "project.table.actions.batch.desc")
    @ApiResponse(responseCode = "200", description = "project.table.actions.200.desc", headers = @Header(name = HttpHeaders.LOCATION, description = "header.location.desc"))
    @ApiResponse(responseCode = "204", description = "project.table.actions.204.desc")
    @PostMapping("/{projectId}/tables/{tableId}/actions/batch")
    public ResponseEntity<TableIdView> editTableSourceBatch(@ProjectId @PathVariable("projectId") RulesProject project,
                                                            @PathVariable("tableId") @Parameter(description = "project.table.id.desc") String tableId,
                                                            @Valid @RequestBody RawTableSourceActions actions,
                                                            @RequestParam(value = "module", required = false) @Parameter(description = "projects.table.get.param.module.desc") String module) throws ProjectException {
        var newTableId = projectService.editTableSource(project, tableId, actions.actions(), module);
        recompileWrittenModule();
        return tableWriteResponse(tableId, newTableId);
    }

    @Operation(summary = "project.table.properties.update.summary", description = "project.table.properties.update.desc")
    @ApiResponse(responseCode = "200", description = "project.table.properties.update.200.desc", headers = @Header(name = HttpHeaders.LOCATION, description = "header.location.desc"))
    @ApiResponse(responseCode = "204", description = "project.table.properties.update.204.desc")
    @PatchMapping("/{projectId}/tables/{tableId}/properties")
    public ResponseEntity<TableIdView> updateTableProperties(@ProjectId @PathVariable("projectId") RulesProject project,
                                                             @PathVariable("tableId") @Parameter(description = "project.table.id.desc") String tableId,
                                                             @Valid @RequestBody TablePropertiesUpdate update,
                                                             @RequestParam(value = "module", required = false) @Parameter(description = "projects.table.get.param.module.desc") String module) throws ProjectException {
        var newTableId = projectService.updateTableProperties(project, tableId, update.properties(), module);
        recompileWrittenModule();
        return tableWriteResponse(tableId, newTableId);
    }

    @Operation(summary = "project.table.delete.summary", description = "project.table.delete.desc")
    @ApiResponse(responseCode = "204", description = "project.table.delete.204.desc")
    @DeleteMapping("/{projectId}/tables/{tableId}")
    public ResponseEntity<Void> deleteTable(@ProjectId @PathVariable("projectId") RulesProject project,
                                            @PathVariable("tableId") @Parameter(description = "project.table.id.desc") String tableId,
                                            @RequestParam(value = "module", required = false) @Parameter(description = "projects.table.get.param.module.desc") String module) throws ProjectException {
        projectService.deleteTable(project, tableId, module);
        recompileWrittenModule();
        return ResponseEntity.noContent().build();
    }

    /**
     * Builds the response for a table write.
     * <p>
     * When the table ID is unchanged, returns 204 No Content with no headers. When the table was relocated and its ID
     * changed, returns 200 OK with the new ID in the body and a Location header pointing to the table resource under its
     * new ID.
     *
     * @param requestedTableId table ID from the request path
     * @param currentTableId   table ID after the write
     * @return 204 response when the ID is unchanged, otherwise a 200 response carrying the new ID
     */
    private static ResponseEntity<TableIdView> tableWriteResponse(String requestedTableId, String currentTableId) {
        if (currentTableId.equals(requestedTableId)) {
            return ResponseEntity.noContent().build();
        }
        var requestUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUriString();
        var tableUrl = requestUrl.substring(0, requestUrl.indexOf("/tables/")) + "/tables/" + currentTableId;
        return ResponseEntity.ok()
                .header(HttpHeaders.LOCATION, tableUrl)
                .body(new TableIdView(currentTableId));
    }

    @Operation(summary = "projects.tests.run.summary")
    @Parameter(name = "fromModule", description = "projects.tests.run.param.from-module.desc", in = ParameterIn.QUERY)
    @Parameter(name = "tableId", description = "projects.tests.run.param.table-id.desc", in = ParameterIn.QUERY)
    @Parameter(name = "testRanges", description = "projects.tests.run.param.test-ranges.desc", in = ParameterIn.QUERY)
    @PostMapping("/{projectId}/tests/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void runAllTests(@ProjectId @PathVariable("projectId") RulesProject project,
                            @RequestParam(value = "fromModule", required = false) String fromModule,
                            @RequestParam(value = "tableId", required = false) String tableId,
                            @RequestParam(value = "testRanges", required = false) String testRanges) {
        // A blank `?fromModule=` means the whole project, not a module named "", the way the benchmark reads it.
        var moduleName = StringUtils.trimToNull(fromModule);
        var projectModel = projectService.openProject(project, moduleName).awaitCompiled();
        var currentOpenedModule = moduleName != null;
        // Refused before any run is cancelled or announced: a request refused leaves the run before it going,
        // and announces no run whose end nobody would hear.
        var table = StringUtils.isBlank(tableId) ? null : requireTable(projectModel, tableId);
        var testSuite = table == null ? null : testSuiteOf(projectModel, table, currentOpenedModule);
        if (testSuite != null && StringUtils.isNotBlank(testRanges)) {
            TestCaseRanges.requireKnownCases(testSuite, testRanges);
        }
        executionTestsResultRegistry.cancelIfAny();
        var projectId = projectIdentifierMapper.map(project);
        var user = projectService.getUserWorkspace().getUser();
        CompletableFuture<List<TestUnitsResults>> testTask;
        var mapper = testsSummaryMapper(project);
        // A test table that has run is announced the way the screen reads the results, not written in full.
        Function<TestUnitsResults, TestCaseExecutionResult> announcement =
                testCase -> mapper.mapToTestCaseResult(testCase, TestExecutionSummaryQuery.lazy());
        if (table == null) {
            var listener = socketProjectAllTestsExecutionProgressListenerFactory.create(user, projectId, announcement);
            listener.onStatusChanged(TestExecutionStatus.PENDING);
            testTask = testsExecutorService.runAll(listener, projectModel, currentOpenedModule);
        } else {
            var listener = socketProjectAllTestsExecutionProgressListenerFactory.create(user,
                    projectId,
                    tableId,
                    announcement);
            listener.onStatusChanged(TestExecutionStatus.PENDING);
            // A test table, and a run table with it, is run as it stands; any other table is run through the
            // test tables that cover it.
            if (testSuite == null && StringUtils.isBlank(testRanges)) {
                testTask = testsExecutorService.runAllForTable(listener, projectModel, table, currentOpenedModule);
            } else {
                testTask = testsExecutorService.runSingle(listener, projectModel, table, testRanges, currentOpenedModule);
            }
        }
        executionTestsResultRegistry.setTask(projectId, testTask);
    }

    /** The test suite the table compiles to: a test table or a run table, or {@code null} for any other table. */
    private static @Nullable TestSuiteMethod testSuiteOf(ProjectModel projectModel,
                                                         IOpenLTable table,
                                                         boolean currentOpenedModule) {
        var uri = table.getUri();
        var method = currentOpenedModule ? projectModel.getOpenedModuleMethod(uri) : projectModel.getMethod(uri);
        return method instanceof TestSuiteMethod testSuiteMethod ? testSuiteMethod : null;
    }

    @Operation(summary = "projects.tests.summary.summary")
    @ApiResponse(responseCode = "404", description = "projects.tests.summary.404.desc")
    @ApiResponse(responseCode = "202", description = "projects.tests.summary.202.desc",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ResultNotReadyView.class)))
    @ApiResponse(responseCode = "406", description = "projects.tests.summary.406.desc")
    @ApiResponse(
            responseCode = "200",
            description = "projects.tests.summary.200.desc",
            headers = {
                    @Header(name = HttpHeaders.CONTENT_DISPOSITION, description = "header.content-disposition.desc")

            },
            content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TestsExecutionSummary.class)),
                    @Content(mediaType = APPLICATION_XLSX_MEDIATYPE, schema = @Schema(type = "string", format = "binary"))
            }
    )
    @GetMapping(value = "/{projectId}/tests/summary", produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_XLSX_MEDIATYPE})
    public ResponseEntity<?> getTestsSummary(@ProjectId @PathVariable("projectId") RulesProject project,
                                             @RequestParam(value = "failuresOnly", defaultValue = "false")
                                             @Parameter(description = "projects.tests.summary.param.failures-only.desc")
                                             boolean failuresOnly,
                                             @RequestParam(value = "failures", defaultValue = "5")
                                             @Parameter(description = "projects.tests.summary.param.failures.desc")
                                             @Min(1)
                                             int failures,
                                             @RequestParam(value = "allFailures", defaultValue = "false")
                                             @Parameter(description = "projects.tests.summary.param.all-failures.desc")
                                             boolean allFailures,
                                             @RequestParam(value = "compoundResult", defaultValue = "false")
                                             @Parameter(description = "projects.tests.summary.param.compound-result.desc")
                                             boolean compoundResult,
                                             @RequestParam(value = "lazyValues", defaultValue = "false")
                                             @Parameter(description = "projects.tests.summary.param.lazy-values.desc")
                                             boolean lazyValues,
                                             @PaginationDefault Pageable page,
                                             @Parameter(required = true, schema = @Schema(allowableValues = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_XLSX_MEDIATYPE}))
                                             @RequestHeader(name = HttpHeaders.ACCEPT)
                                             String acceptMediaType) throws IOException {
        var completed = completedTests(project);
        if (acceptMediaType.equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
            if (completed.isEmpty()) {
                return ResultNotReadyView.accepted();
            }
            var executionResults = completed.get();
            var mapper = testsSummaryMapper(project);
            var query = new TestExecutionSummaryQuery(failuresOnly,
                    allFailures ? TestUnitsResults.ALL_FAILURES : failures,
                    compoundResult,
                    lazyValues);
            return ResponseEntity.ok(mapper.mapExecutionSummary(executionResults, query, page));
        } else if (acceptMediaType.equalsIgnoreCase(APPLICATION_XLSX_MEDIATYPE)) {
            // A client that asked for a workbook is told by the status alone: it did not ask for JSON.
            if (completed.isEmpty()) {
                return ResponseEntity.accepted().build();
            }
            // A case that gave back to free memory a value the workbook writes runs again while its row is written.
            var output = new ByteArrayOutputStream();
            new TestResultExport().export(output, page.getPageSize(), completed.get().toArray(new TestUnitsResults[0]));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, WebTool.getContentDispositionValue("test-results.xlsx"))
                    .header(HttpHeaders.CONTENT_TYPE, APPLICATION_XLSX_MEDIATYPE)
                    .body(output.toByteArray());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
    }

    @Operation(summary = "projects.tests.case.summary", description = "projects.tests.case.desc")
    @ApiResponse(responseCode = "200", description = "projects.tests.case.200.desc",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TestUnitExecutionResult.class)))
    @ApiResponse(responseCode = "404", description = "projects.tests.case.404.desc")
    @ApiResponse(responseCode = "202", description = "projects.tests.summary.202.desc",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ResultNotReadyView.class)))
    @GetMapping("/{projectId}/tests/summary/{tableId}/cases/{caseId}")
    public ResponseEntity<?> getTestCaseResult(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable("tableId") @Parameter(description = "projects.tests.case.param.table-id.desc") String tableId,
            @PathVariable("caseId") @Parameter(description = "projects.tests.case.param.case-id.desc") String caseId) {

        var completed = completedTests(project);
        if (completed.isEmpty()) {
            return ResultNotReadyView.accepted();
        }
        var testCase = completed.get().stream()
                .filter(candidate -> tableId.equals(TableUtils.makeTableId(candidate.getTestSuite().getUri())))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("tests.execution.case.message", caseId));
        var testUnit = testCase.getTestUnits().stream()
                .filter(candidate -> caseId.equals(candidate.getTest().getId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("tests.execution.case.message", caseId));

        // What the case holds is held while it is written. A case that gave a value back to free memory runs again
        // for it, and what that returns is written and not kept.
        var held = RetainedTestUnit.heldValues(testUnit);
        var unit = testUnit instanceof RetainedTestUnit retained && held.stream().anyMatch(RetainedTestUnit::isReleased)
                ? retained.again()
                : testUnit;
        var answer = testsSummaryMapper(project)
                .mapToTestUnitResult(testCase, unit, TestExecutionSummaryQuery.inFull());
        Reference.reachabilityFence(held);
        return ResponseEntity.ok(answer);
    }

    /**
     * The mapper that reads a test run of the given project.
     *
     * <p>It is built per request: what it needs — how spreadsheet results are named, and which module holds
     * each table — belongs to the project as it stands now.
     */
    private TestsExecutionSummaryResponseMapper testsSummaryMapper(RulesProject project) {
        var objectMapper = objectMapperService.createObjectMapper();
        return new TestsExecutionSummaryResponseMapper(objectMapper, getSchemaGenerator(objectMapper),
                projectService.getSpreadsheetResultNamingStrategy(),
                projectService.getTableModules(project));
    }

    /**
     * The results of the test run that has ended, for the project of the request.
     *
     * <p>Empty while the tests are still running: the request is accepted, and there is nothing to report until
     * they have ended. Answered so rather than refused, so a screen asking after the result raises no error.
     *
     * @throws NotFoundException when no test run is remembered for the project
     */
    private Optional<List<TestUnitsResults>> completedTests(RulesProject project) {
        var projectId = projectIdentifierMapper.map(project);
        if (!executionTestsResultRegistry.hasTask(projectId)) {
            throw new NotFoundException("tests.execution.task.message");
        }
        if (!executionTestsResultRegistry.isDone(projectId)) {
            return Optional.empty();
        }
        var executionResults = executionTestsResultRegistry.getResultIfDone(projectId);
        if (executionResults == null) {
            throw new NotFoundException("tests.execution.task.message");
        }
        return Optional.of(executionResults);
    }

    /**
     * Trim incoming string fields and convert whitespace-only values to {@code null} so
     * downstream service logic can rely on null-vs-non-null checks instead of repeatedly
     * calling {@code isNotBlank} / {@code trimToNull}.
     */
    private static ProjectStatusUpdateModel normalize(ProjectStatusUpdateModel raw) {
        return ProjectStatusUpdateModel.builder()
                .status(raw.status())
                .branch(StringUtils.trimToNull(raw.branch()))
                .revision(StringUtils.trimToNull(raw.revision()))
                .comment(StringUtils.trimToNull(raw.comment()))
                .save(raw.save())
                .discardChanges(raw.discardChanges())
                .openDependencies(raw.openDependencies())
                .build();
    }

}
