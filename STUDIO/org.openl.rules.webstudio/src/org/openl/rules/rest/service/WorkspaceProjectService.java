/* Copyright © 2023 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package org.openl.rules.rest.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.openl.base.INamedThing;
import org.openl.binding.MethodUtil;
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
import org.openl.rules.rest.model.CreateBranchModel;
import org.openl.rules.rest.model.ProjectStatusUpdateModel;
import org.openl.rules.rest.model.TableInfo;
import org.openl.rules.rest.project.ProjectStateValidator;
import org.openl.rules.table.properties.ITableProperties;
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
import org.openl.types.impl.AMethod;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

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

    private static final BiMap<String, String> TABLE_TYPE_ITEMS;

    static {
        BiMap<String, String> tableTypeItems = HashBiMap.create();
        tableTypeItems.put(XlsNodeTypes.XLS_DT.toString(), "Decision");
        tableTypeItems.put(XlsNodeTypes.XLS_SPREADSHEET.toString(), "Spreadsheet");
        tableTypeItems.put(XlsNodeTypes.XLS_TBASIC.toString(), "TBasic");
        tableTypeItems.put(XlsNodeTypes.XLS_COLUMN_MATCH.toString(), "Column Match");
        tableTypeItems.put(XlsNodeTypes.XLS_DATATYPE.toString(), "Datatype");
        tableTypeItems.put(XlsNodeTypes.XLS_DATA.toString(), "Data");
        tableTypeItems.put(XlsNodeTypes.XLS_METHOD.toString(), "Method");
        tableTypeItems.put(XlsNodeTypes.XLS_TEST_METHOD.toString(), "Test");
        tableTypeItems.put(XlsNodeTypes.XLS_RUN_METHOD.toString(), "Run");
        tableTypeItems.put(XlsNodeTypes.XLS_CONSTANTS.toString(), "Constants");
        tableTypeItems.put(XlsNodeTypes.XLS_CONDITIONS.toString(), "Conditions");
        tableTypeItems.put(XlsNodeTypes.XLS_ACTIONS.toString(), "Actions");
        tableTypeItems.put(XlsNodeTypes.XLS_RETURNS.toString(), "Returns");
        tableTypeItems.put(XlsNodeTypes.XLS_ENVIRONMENT.toString(), "Environment");
        tableTypeItems.put(XlsNodeTypes.XLS_PROPERTIES.toString(), "Properties");
        tableTypeItems.put(XlsNodeTypes.XLS_OTHER.toString(), "Other");

        TABLE_TYPE_ITEMS = Maps.unmodifiableBiMap(tableTypeItems);
    }

    private final ProjectStateValidator projectStateValidator;
    private final ProjectDependencyResolver projectDependencyResolver;

    public WorkspaceProjectService(
            @Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService,
            OpenLProjectService projectService,
            ProjectStateValidator projectStateValidator,
            ProjectDependencyResolver projectDependencyResolver) {
        super(designRepositoryAclService, projectService);
        this.projectStateValidator = projectStateValidator;
        this.projectDependencyResolver = projectDependencyResolver;
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
    public Collection<TableInfo> getTables(RulesProject project,
            ProjectTableCriteriaQuery query) throws ProjectException {
        var moduleModel = getProjectModel(project);
        var result = new ArrayList<TableInfo>();

        Predicate<TableSyntaxNode> selectors = buildTableSelector(query);
        for (var table : moduleModel.search(selectors, SearchScope.CURRENT_PROJECT)) {
            var tableInfoBuilder = TableInfo.builder()
                .id(table.getId())
                .kind(TABLE_TYPE_ITEMS.get(table.getType()))
                .name(table.getDisplayName());

            var url = table.getUriParser();
            try {
                var file = new File("").getCanonicalFile()
                    .toPath()
                    .relativize(Path.of(url.getWbPath()))
                    .resolve(url.getWbName())
                    .toString();
                tableInfoBuilder.file(file);
            } catch (IOException e) {
                throw new ProjectException("Failed to resolve module location", e);
            }
            tableInfoBuilder.pos(url.getRange());

            Optional.ofNullable(table.getProperties())
                .map(ITableProperties::getTableProperties)
                .map(Map::copyOf)
                .ifPresent(tableInfoBuilder::properties);

            var tsn = table.getSyntaxNode();
            var header = tsn.getHeader();
            tableInfoBuilder.tableType(header.getHeaderToken().getIdentifier());
            var member = tsn.getMember();
            if (member instanceof AMethod) {
                var methodHeader = ((AMethod) member).getHeader();
                tableInfoBuilder.signature(MethodUtil.printSignature(methodHeader, INamedThing.REGULAR))
                    .returnType(MethodUtil.printType(methodHeader.getType()));
            }

            result.add(tableInfoBuilder.build());
        }
        return result;
    }

    private Predicate<TableSyntaxNode> buildTableSelector(ProjectTableCriteriaQuery query) {
        Predicate<TableSyntaxNode> selectors = tsn -> !XlsNodeTypes.XLS_OTHER.toString().equals(tsn.getType());

        var tableTypes = query.getKinds()
            .stream()
            .map(TABLE_TYPE_ITEMS.inverse()::get)
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
}
