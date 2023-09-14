package org.openl.rules.rest.service;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.openl.rules.common.ProjectException;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.git.MergeConflictException;
import org.openl.rules.rest.ProjectHistoryService;
import org.openl.rules.rest.exception.BadRequestException;
import org.openl.rules.rest.exception.ConflictException;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.rest.model.CreateBranchModel;
import org.openl.rules.rest.model.ProjectStatusUpdateModel;
import org.openl.rules.rest.model.tables.DatatypeView;
import org.openl.rules.rest.model.tables.EditableTableView;
import org.openl.rules.rest.model.tables.SimpleRulesView;
import org.openl.rules.rest.model.tables.SimpleSpreadsheetView;
import org.openl.rules.rest.model.tables.SpreadsheetView;
import org.openl.rules.rest.model.tables.SummaryTableView;
import org.openl.rules.rest.model.tables.TableView;
import org.openl.rules.rest.model.tables.VocabularyView;
import org.openl.rules.rest.project.ProjectStateValidator;
import org.openl.rules.rest.service.tables.OpenLTableUtils;
import org.openl.rules.rest.service.tables.read.EditableTableReader;
import org.openl.rules.rest.service.tables.read.SummaryTableReader;
import org.openl.rules.rest.service.tables.write.DatatypeTableWriter;
import org.openl.rules.rest.service.tables.write.SimpleRulesWriter;
import org.openl.rules.rest.service.tables.write.SimpleSpreadsheetTableWriter;
import org.openl.rules.rest.service.tables.write.SpreadsheetTableWriter;
import org.openl.rules.rest.service.tables.write.VocabularyTableWriter;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.service.OpenLProjectService;
import org.openl.rules.webstudio.web.SearchScope;
import org.openl.rules.webstudio.web.TablePropertiesSelector;
import org.openl.rules.webstudio.web.repository.CommentValidator;
import org.openl.rules.webstudio.web.repository.merge.ConflictUtils;
import org.openl.rules.webstudio.web.repository.merge.MergeConflictInfo;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Implementation of project service for workspace projects.
 *
 * @author Vladyslav Pikus
 */
@Component
@ParametersAreNonnullByDefault
public class WorkspaceProjectService extends AbstractProjectService<RulesProject> {

    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceProjectService.class);

    private static final Set<ProjectStatus> ALLOWED_STATUSES = EnumSet.of(ProjectStatus.CLOSED, ProjectStatus.VIEWING);

    private final ProjectStateValidator projectStateValidator;
    private final ProjectDependencyResolver projectDependencyResolver;
    private final SummaryTableReader summaryTableReader;
    private final List<EditableTableReader<? extends EditableTableView, ? extends TableView.Builder<?>>> readers;

    public WorkspaceProjectService(
            @Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService,
            OpenLProjectService projectService,
            ProjectStateValidator projectStateValidator,
            ProjectDependencyResolver projectDependencyResolver,
            SummaryTableReader summaryTableReader,
            List<EditableTableReader<? extends EditableTableView, ? extends TableView.Builder<?>>> readers) {
        super(designRepositoryAclService, projectService);
        this.projectStateValidator = projectStateValidator;
        this.projectDependencyResolver = projectDependencyResolver;
        this.summaryTableReader = summaryTableReader;
        this.readers = readers;
    }

    @Lookup
    public UserWorkspace getUserWorkspace() {
        return null;
    }

    @Lookup
    public WebStudio getWebStudio() {
        return null;
    }

    @Override
    protected Stream<RulesProject> getProjects0(ProjectCriteriaQuery query) {
        var workspace = getUserWorkspace();
        Collection<RulesProject> projects;
        if (query.getRepositoryId().isPresent()) {
            var repositoryId = query.getRepositoryId().get();
            projects = workspace.getProjects(repositoryId);
        } else {
            projects = workspace.getProjects();
        }
        return projects.stream();
    }

    @Override
    @Nonnull
    protected Predicate<AProject> buildFilterCriteria(ProjectCriteriaQuery query) {
        Predicate<AProject> filter = super.buildFilterCriteria(query);

        if (query.getStatus().isPresent()) {
            filter = filter.and(project -> {
                var workspaceProject = (UserWorkspaceProject) project;
                return workspaceProject.getStatus() == query.getStatus().get();
            });
        }

        return filter;
    }

    @Override
    public void updateProjectStatus(RulesProject project, ProjectStatusUpdateModel model) throws ProjectException {
        if (model.getStatus() != null && !ALLOWED_STATUSES.contains(model.getStatus())) {
            throw new BadRequestException("invalid.project.status.message");
        }
        if (project.isModified() && model.getComment().isPresent()) {
            save(project, model);
        }
        if (model.getStatus() == ProjectStatus.VIEWING) {
            if (!project.isOpened() || model.getBranch().isPresent() || model.getRevision().isPresent()) {
                open(project, false, model);
            }
        } else {
            if (model.getStatus() == ProjectStatus.CLOSED && project.getStatus() != ProjectStatus.CLOSED) {
                close(project);
            }
            if (model.getBranch().isPresent()) {
                switchToBranch(project, model.getBranch().get());
            }
        }
    }

    public void save(RulesProject project, ProjectStatusUpdateModel model) throws ProjectException {
        if (!project.isModified()) {
            return;
        }
        var comment = model.getComment().map(String::trim).orElse(null);
        try {
            CommentValidator.forRepo(project.getRepository().getId()).validate(comment);
        } catch (Exception e) {
            throw new BadRequestException("repo.invalid.comment.message", new Object[] { e.getMessage() });
        }
        project.getFileData().setComment(comment);
        try {
            ConflictUtils.removeMergeConflict();
            getWebStudio().saveProject(project);
        } catch (ProjectException e) {
            var cause = e.getCause();
            if (cause instanceof MergeConflictException) {
                var info = new MergeConflictInfo((MergeConflictException) cause, project);
                ConflictUtils.saveMergeConflict(info);
            }
            throw e;
        }
    }

    @Override
    public void close(RulesProject project) throws ProjectException {
        var webStudio = getWebStudio();
        if (!designRepositoryAclService.isGranted(project, List.of(AclPermission.VIEW))) {
            throw new SecurityException();
        }
        if (project.isDeleted()) {
            throw new ConflictException("project.close.deleted.message", project.getBusinessName());
        } else if (!projectStateValidator.canClose(project)) {
            throw new ConflictException("project.close.conflict.message");
        }
        try {
            ProjectHistoryService.deleteHistory(project.getBusinessName());
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(e.getMessage(), e);
            }
            throw new ProjectException("Failed to delete project history", e);
        }
        // We must release module info because it can hold jars.
        // We cannot rely on studio.getProject() to determine if closing project is compiled inside
        // studio.getModel()
        // because project could be changed or cleared before (See studio.reset() usages). Also that project can be
        // a dependency of other. That's why we must always clear moduleInfo when closing a project.
        webStudio.getModel().clearModuleInfo();
        project.close();
    }

    @Override
    public void open(RulesProject project, boolean openDependencies) throws ProjectException {
        open(project, openDependencies, new ProjectStatusUpdateModel());
    }

    private void open(RulesProject project,
            boolean openDependencies,
            ProjectStatusUpdateModel model) throws ProjectException {
        if (!designRepositoryAclService.isGranted(project, List.of(AclPermission.VIEW))) {
            throw new SecurityException();
        }
        var workspace = getUserWorkspace();
        if (project.isDeleted()) {
            throw new ConflictException("project.open.deleted.message", project.getBusinessName());
        } else if (!projectStateValidator.canOpen(project)) {
            throw new ConflictException("project.open.conflict.message");
        }

        if (model.getRevision().isPresent()) {
            AProject historic = new AProject(project.getDesignRepository(),
                project.getDesignFolderName(),
                model.getRevision().get());
            if (workspace.isOpenedOtherProject(historic)) {
                throw new ConflictException("open.duplicated.project");
            }
        }
        // Do we really need to check this if we have a version? Copy-paste from
        // RepositoryTreeController#openProjectVersion
        if (workspace.isOpenedOtherProject(project)) {
            throw new ConflictException("open.duplicated.project");
        }

        var wasOpened = project.isOpened();
        var webStudio = getWebStudio();
        if (wasOpened && (model.getBranch().isPresent() || model.getRevision().isPresent())) {
            // We must clear module info and release project lock
            // because project was already opened and we are going to open it in another branch or revision
            webStudio.getModel().clearModuleInfo();
            project.releaseMyLock();
        }

        if (model.getBranch().isPresent()) {
            switchToBranch(project, model.getBranch().get());
        }

        if (model.getRevision().isPresent()) {
            project.openVersion(model.getRevision().get());
        } else {
            if (model.getBranch().isPresent() || !wasOpened) {
                project.open();
            } else {
                throw new ConflictException("project.open.conflict.message");
            }
        }

        if (openDependencies) {
            openAllDependencies(project);
        }
    }

    private void switchToBranch(RulesProject project, String branchName) throws ProjectException {
        if (!project.isSupportsBranches()) {
            throw new ConflictException("project.branch.unsupported.message");
        }
        var previousBranch = project.getBranch();
        if (Objects.equals(previousBranch, branchName)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Project '{}' is already opened in branch '{}'", project.getBusinessName(), branchName);
            }
            return;
        }

        var wasOpened = project.isOpened();
        if (wasOpened) {
            var webStudio = getWebStudio();
            // We must clear module info and release project lock
            // because project was already opened and we are going to open it in another branch or revision
            webStudio.getModel().clearModuleInfo();
            project.releaseMyLock();
        }

        var previousBusinessName = project.getBusinessName();
        project.setBranch(branchName);
        if (project.getLastHistoryVersion() == null) {
            project.setBranch(previousBranch);
            throw new ConflictException("project.switch.branch.failed.message", branchName);
        }
        if (wasOpened) {
            if (project.isDeleted()) {
                project.close();
            } else {
                // Update files
                try {
                    ProjectHistoryService.deleteHistory(previousBusinessName);
                } catch (IOException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(e.getMessage(), e);
                    }
                    throw new ProjectException("Failed to delete project history", e);
                }
                var workspace = getUserWorkspace();
                if (workspace.isOpenedOtherProject(project)) {
                    throw new ConflictException("open.duplicated.project");
                }
            }
        }
    }

    private void openAllDependencies(RulesProject project) throws ProjectException {
        for (RulesProject rulesProject : projectDependencyResolver.getProjectDependencies(project)) {
            rulesProject.open();
        }
    }

    @Override
    public void createBranch(RulesProject project, CreateBranchModel model) throws ProjectException {
        if (!project.isSupportsBranches()) {
            throw new ConflictException("project.branch.unsupported.message");
        }
        if (!hasCreateBranchPermissions(project)) {
            throw new SecurityException();
        }
        var repository = (BranchRepository) project.getDesignRepository();
        try {
            repository.createBranch(project.getFolderPath(), model.getBranch(), model.getRevision());
        } catch (IOException e) {
            throw new ProjectException("Failed to create branch", e);
        }
    }

    public boolean hasCreateBranchPermissions(RulesProject project) {
        if (project.isSupportsBranches()) {
            for (AProjectArtefact artefact : project.getArtefacts()) {
                if (designRepositoryAclService.isGranted(artefact,
                    List.of(AclPermission.EDIT, AclPermission.DELETE, AclPermission.ADD))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Collection<SummaryTableView> getTables(RulesProject project,
            ProjectTableCriteriaQuery query) throws ProjectException {
        var moduleModel = getProjectModel(project);

        var selectors = buildTableSelector(query);
        return moduleModel.search(selectors, SearchScope.CURRENT_PROJECT)
            .stream()
            .map(summaryTableReader::read)
            .collect(Collectors.toList());
    }

    private Predicate<TableSyntaxNode> buildTableSelector(ProjectTableCriteriaQuery query) {
        Predicate<TableSyntaxNode> selectors = tsn -> !XlsNodeTypes.XLS_OTHER.toString().equals(tsn.getType());

        var tableTypes = query.getKinds()
            .stream()
            .map(OpenLTableUtils.getTableTypeItems().inverse()::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (CollectionUtils.isNotEmpty(tableTypes)) {
            selectors = selectors.and(tsn -> tableTypes.contains(tsn.getType()));
        }

        if (query.getName().isPresent()) {
            var name = query.getName().get();
            selectors = selectors.and(tsn -> StringUtils.containsIgnoreCase(tsn.getDisplayName(), name));
        }

        if (CollectionUtils.isNotEmpty(query.getProperties())) {
            selectors = selectors.and(new TablePropertiesSelector(query.getProperties()));
        }

        return selectors;
    }

    private ProjectModel getProjectModel(RulesProject project) throws ProjectException {
        var webstudio = getWebStudio();

        var projectDescriptor = webstudio.getProjectByName(project.getRepository().getId(), project.getName());
        var module = projectDescriptor.getModules().stream().findFirst().orElse(null);
        if (module != null) {
            webstudio.init(project.getRepository().getId(), project.getBranch(), project.getName(), module.getName());
            var moduleModel = webstudio.getModel();
            while (!moduleModel.isProjectCompilationCompleted()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ProjectException("Project compilation interrupted", e);
                }
            }
            return moduleModel;
        } else {
            throw new ProjectException("Failed to find module for project " + project.getBusinessName());
        }
    }

    @Override
    public EditableTableView getTable(RulesProject project, String tableId) throws ProjectException {
        var moduleModel = getProjectModel(project);
        var table = moduleModel.getTableById(tableId);
        if (table == null) {
            // TODO
            throw new NotFoundException("TODO");
        }
        var reader = readers.stream()
                .filter(r -> r.supports(table))
                .findFirst().orElseThrow();
        return reader.read(table);
    }

    @Override
    public void updateTable(RulesProject project, String tableId, EditableTableView tableView) throws ProjectException {
        var moduleModel = getProjectModel(project);
        var table = moduleModel.getTableById(tableId);
        if (table == null) {
            // TODO
            throw new NotFoundException("TODO");
        }
        boolean found = false;
        if (Objects.equals(XlsNodeTypes.XLS_DATATYPE.toString(), table.getType())) {
            if (tableView instanceof VocabularyView) {
                found = true;
                var writer = new VocabularyTableWriter(table);
                writer.write((VocabularyView) tableView);
            } else if (tableView instanceof DatatypeView) {
                found = true;
                var writer = new DatatypeTableWriter(table);
                writer.write((DatatypeView) tableView);
            }
        } else if (Objects.equals(XlsNodeTypes.XLS_SPREADSHEET.toString(), table.getType())) {
            if (tableView instanceof SimpleSpreadsheetView) {
                found = true;
                var writer = new SimpleSpreadsheetTableWriter(table);
                writer.write((SimpleSpreadsheetView) tableView);
            } else if (tableView instanceof SpreadsheetView) {
                found = true;
                var writer = new SpreadsheetTableWriter(table);
                writer.write((SpreadsheetView) tableView);
            }
        } else if (Objects.equals(XlsNodeTypes.XLS_DT.toString(), table.getType())) {
            if (tableView instanceof SimpleRulesView) {
                found = true;
                var writer = new SimpleRulesWriter(table);
                writer.write((SimpleRulesView) tableView);
            }
        }
        if (!found) {
            // TODO
            throw new UnsupportedOperationException("Table type doesn't match writer type");
        }
    }
}
