package org.openl.rules.ui;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.Privileges.CREATE_TABLES;
import static org.openl.rules.security.Privileges.EDIT_PROJECTS;
import static org.openl.rules.security.Privileges.EDIT_TABLES;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.openl.CompiledOpenClass;
import org.openl.OpenClassUtil;
import org.openl.base.INamedThing;
import org.openl.dependency.CompiledDependency;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.meta.IMetaInfo;
import org.openl.rules.dependency.graph.DependencyRulesGraph;
import org.openl.rules.lang.xls.OverloadedMethodsDictionary;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.XlsWorkbookListener;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.binding.wrapper.WrapperLogic;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.SimpleDependencyLoader;
import org.openl.rules.project.instantiation.SimpleMultiModuleInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.validation.openapi.OpenApiProjectValidator;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.ProjectHistoryService;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteExecutor;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.tree.OpenMethodsGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.ProjectTreeNode;
import org.openl.rules.ui.tree.TreeNodeBuilder;
import org.openl.rules.ui.tree.richfaces.TreeNode;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.rules.webstudio.dependencies.WebStudioWorkspaceDependencyManagerFactory;
import org.openl.rules.webstudio.dependencies.WebStudioWorkspaceRelatedDependencyManager;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.SearchScope;
import org.openl.rules.webstudio.web.admin.AdministrationSettings;
import org.openl.rules.webstudio.web.trace.node.CachingArgumentsCloner;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.NullOpenClass;
import org.openl.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectModel {

    private final Logger log = LoggerFactory.getLogger(ProjectModel.class);

    private static final Comparator<TableSyntaxNode> DEFAULT_NODE_CMP = Comparator.comparing(
        node -> Optional.ofNullable(node.getMember()).map(INamedThing::getName).orElse(null),
        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    /**
     * Compiled rules with errors. Representation of wrapper.
     */
    private volatile CompiledOpenClass compiledOpenClass;
    private volatile CompiledOpenClass openedModuleCompiledOpenClass;
    private volatile boolean compilationInProgress;
    private volatile String projectCompilationCompleted;

    private XlsModuleSyntaxNode xlsModuleSyntaxNode;
    private final Map<String, Set<XlsModuleSyntaxNode>> xlsModuleSyntaxNodesPerProject = new ConcurrentHashMap<>();
    private final Collection<WorkbookSyntaxNode> workbookSyntaxNodes = ConcurrentHashMap.newKeySet();

    private Module moduleInfo;
    private long moduleLastModified;

    private final WebStudioWorkspaceDependencyManagerFactory webStudioWorkspaceDependencyManagerFactory;
    private WebStudioWorkspaceRelatedDependencyManager webStudioWorkspaceDependencyManager;

    private final WebStudio studio;

    private final ColorFilterHolder filterHolder = new ColorFilterHolder();

    private TreeNode projectRoot = null;

    private DependencyRulesGraph dependencyGraph;
    private String historyStoragePath;

    private final RecentlyVisitedTables recentlyVisitedTables = new RecentlyVisitedTables();
    private final TestSuiteExecutor testSuiteExecutor;

    /**
     * For tests only
     */
    ProjectModel(WebStudio studio) {
        this(studio, null);
    }

    public ProjectModel(WebStudio studio, TestSuiteExecutor testSuiteExecutor) {
        this.studio = studio;
        this.webStudioWorkspaceDependencyManagerFactory = new WebStudioWorkspaceDependencyManagerFactory(studio);
        this.testSuiteExecutor = testSuiteExecutor;
    }

    public synchronized RulesProject getProject() {
        return studio.getCurrentProject();
    }

    public synchronized TableSyntaxNode findNode(String url) {
        XlsUrlParser parsedUrl = new XlsUrlParser(url);

        return findNode(parsedUrl);
    }

    private boolean findInCompositeGrid(CompositeGrid compositeGrid, XlsUrlParser p1) {
        for (IGridTable gridTable : compositeGrid.getGridTables()) {
            if (gridTable.getGrid() instanceof CompositeGrid) {
                if (findInCompositeGrid((CompositeGrid) gridTable.getGrid(), p1)) {
                    return true;
                }
            } else {
                if (p1.intersects(gridTable.getUriParser())) {
                    return true;
                }
            }
        }
        return false;
    }

    private TableSyntaxNode findNode(XlsUrlParser p1) {
        Collection<TableSyntaxNode> nodes = getAllTableSyntaxNodes();
        for (TableSyntaxNode node : nodes) {
            if (p1.intersects(node.getGridTable().getUriParser())) {
                if (XlsNodeTypes.XLS_TABLEPART.equals(node.getNodeType())) {
                    for (TableSyntaxNode tableSyntaxNode : nodes) {
                        IGridTable table = tableSyntaxNode.getGridTable();
                        if (table.getGrid() instanceof CompositeGrid) {
                            CompositeGrid compositeGrid = (CompositeGrid) table.getGrid();
                            if (findInCompositeGrid(compositeGrid, p1)) {
                                return tableSyntaxNode;
                            }
                        }
                    }
                }
                return node;
            }
        }

        return null;
    }

    public synchronized int getErrorNodesNumber() {
        int count = 0;
        Collection<Pair<OpenLMessage, XlsUrlParser>> messages = getModuleMessages().stream()
            .map(e -> Pair.of(e, e.getSourceLocation() != null ? new XlsUrlParser(e.getSourceLocation()) : null))
            .collect(Collectors.toList());
        for (TableSyntaxNode tsn : getTableSyntaxNodes()) {
            for (Pair<OpenLMessage, XlsUrlParser> pair : messages) {
                if (pair.getRight() != null && pair.getLeft().getSeverity() == Severity.ERROR) {
                    if (pair.getRight().intersects(tsn.getUriParser())) {
                        count++;
                        break;
                    }
                }
            }
        }
        return count;
    }

    public synchronized TableSyntaxNode getTableByUri(String uri) {
        for (TableSyntaxNode tableSyntaxNode : getTableSyntaxNodes()) {
            if (Objects.equals(uri, tableSyntaxNode.getUri())) {
                return tableSyntaxNode;
            }
        }
        for (TableSyntaxNode tableSyntaxNode : getAllTableSyntaxNodes()) {
            if (Objects.equals(uri, tableSyntaxNode.getUri())) {
                return tableSyntaxNode;
            }
        }
        return null;
    }

    public synchronized TableSyntaxNode getNodeById(String id) {
        for (TableSyntaxNode tableSyntaxNode : getTableSyntaxNodes()) {
            if (Objects.equals(id, tableSyntaxNode.getId())) {
                return tableSyntaxNode;
            }
        }
        for (TableSyntaxNode tableSyntaxNode : getAllTableSyntaxNodes()) {
            if (Objects.equals(id, tableSyntaxNode.getId())) {
                return tableSyntaxNode;
            }
        }
        return null;
    }

    public synchronized List<OpenLMessage> getWarnsByUri(String uri) {
        return getMessagesByTsn(uri, Severity.WARN);
    }

    private List<OpenLMessage> getMessagesByTsn(TableSyntaxNode tableSyntaxNode,
            Severity severity,
            Collection<OpenLMessage> openLMessages) {
        List<OpenLMessage> messages = new ArrayList<>();
        for (OpenLMessage openLMessage : openLMessages) {
            if (openLMessage.getSourceLocation() != null && openLMessage.getSeverity() == severity) {
                XlsUrlParser xlsUrlParser = new XlsUrlParser(openLMessage.getSourceLocation());
                if (tableSyntaxNode.getUriParser().intersects(xlsUrlParser)) {
                    messages.add(openLMessage);
                }
            }
        }
        return messages;
    }

    public List<OpenLMessage> getMessagesByTsn(String uri, Severity severity) {
        TableSyntaxNode tableSyntaxNode = getTableByUri(uri);
        return getMessagesByTsn(tableSyntaxNode, severity, getModuleMessages());
    }

    public List<OpenLMessage> getOpenedModuleMessagesByTsn(String uri, Severity severity) {
        TableSyntaxNode tableSyntaxNode = getTableByUri(uri);
        return getMessagesByTsn(tableSyntaxNode, severity, getOpenedModuleMessages());
    }

    public synchronized List<OpenLMessage> getErrorsByUri(String uri) {
        return getMessagesByTsn(uri, Severity.ERROR);
    }

    public synchronized ColorFilterHolder getFilterHolder() {
        return filterHolder;
    }

    public synchronized IOpenMethod getMethod(String tableUri) {
        if (!isCompiledSuccessfully()) {
            return null;
        }
        IOpenClass openClass = compiledOpenClass.getOpenClassWithErrors();
        return getOpenClassMethod(tableUri, openClass);
    }

    public synchronized IOpenMethod getOpenedModuleMethod(String tableUri) {
        if (!isOpenedModuleCompiledSuccessfully()) {
            return null;
        }
        IOpenClass openClass = openedModuleCompiledOpenClass.getOpenClassWithErrors();
        return getOpenClassMethod(tableUri, openClass);
    }

    public synchronized IOpenMethod getOpenClassMethod(String tableUri, IOpenClass openClass) {
        for (IOpenMethod method : openClass.getMethods()) {
            IOpenMethod resolvedMethod;

            if (method instanceof OpenMethodDispatcher) {
                resolvedMethod = resolveMethodDispatcher((OpenMethodDispatcher) method, tableUri);
                if (resolvedMethod != null) {
                    return method;
                }
            } else {
                resolvedMethod = resolveMethod(method, tableUri);
            }

            if (resolvedMethod != null) {
                return resolvedMethod;
            }
        }

        // for methods that exist in module but not included in
        // CompiledOpenClass
        // e.g. elder inactive versions of methods
        TableSyntaxNode tsn = getNode(tableUri);
        if (tsn != null && tsn.getMember() instanceof IOpenMethod) {
            return (IOpenMethod) tsn.getMember();
        }

        return null;
    }

    private IOpenMethod resolveMethodDispatcher(OpenMethodDispatcher method, String uri) {
        List<IOpenMethod> candidates = method.getCandidates();

        for (IOpenMethod candidate : candidates) {
            IOpenMethod resolvedMethod = resolveMethod(candidate, uri);

            if (resolvedMethod != null) {
                return resolvedMethod;
            }
        }

        return null;
    }

    private IOpenMethod getMethodFromDispatcher(OpenMethodDispatcher method, String uri) {
        List<IOpenMethod> candidates = method.getCandidates();

        for (IOpenMethod candidate : candidates) {
            IOpenMethod resolvedMethod = resolveMethod(candidate, uri);
            if (resolvedMethod != null) {
                return WrapperLogic.wrapOpenMethod(resolvedMethod, (XlsModuleOpenClass) method.getDeclaringClass());
            }
        }

        return null;
    }

    private IOpenMethod resolveMethod(IOpenMethod method, String uri) {

        if (isInstanceOfTable(method, uri)) {
            return method;
        }

        return null;
    }

    /**
     * Checks that {@link IOpenMethod} object is instance that represents the given {@link TableSyntaxNode} object.
     * Actually, {@link IOpenMethod} object must have the same syntax node as given one. If given method is instance of
     * {@link OpenMethodDispatcher} <code>false</code> value will be returned.
     *
     * @param method method to check
     * @return <code>true</code> if {@link IOpenMethod} object represents the given table syntax node;
     *         <code>false</code> - otherwise
     */
    private boolean isInstanceOfTable(IOpenMethod method, String uri) {

        IMemberMetaInfo metaInfo = method.getInfo();

        return metaInfo != null && uri.equals(metaInfo.getSourceUrl());
    }

    public synchronized TableSyntaxNode getNode(String tableUri) {
        TableSyntaxNode tsn = null;
        if (tableUri != null) {
            tsn = getTableByUri(tableUri);
            if (tsn == null) {
                tsn = findNode(tableUri);
            }
        }
        return tsn;
    }

    public synchronized TreeNode getProjectTree() {
        if (projectRoot == null) {
            buildProjectTree();
        }
        return projectRoot;
    }

    public synchronized IOpenLTable getTable(String tableUri) {
        TableSyntaxNode tsn = getNode(tableUri);
        if (tsn != null) {
            return new TableSyntaxNodeAdapter(tsn);
        }
        tsn = getNode(tableUri);
        if (tsn != null) {
            return new TableSyntaxNodeAdapter(tsn);
        }
        return null;

    }

    public synchronized IOpenLTable getTableById(String id) {
        if (projectRoot == null) {
            buildProjectTree();
        }
        TableSyntaxNode tsn = getNodeById(id);
        if (tsn != null) {
            return new TableSyntaxNodeAdapter(tsn);
        }
        tsn = getNodeById(id);
        if (tsn != null) {
            return new TableSyntaxNodeAdapter(tsn);
        }
        return null;
    }

    public IGridTable getGridTable(String tableUri) {
        TableSyntaxNode tsn = getNode(tableUri);
        return tsn == null ? null : tsn.getGridTable();
    }

    /**
     * Gets test methods for method by uri.
     *
     * @param forTable uri for method table
     * @return test methods
     */
    public IOpenMethod[] getTestMethods(String forTable, boolean currentOpenedModule) {
        IOpenMethod method = currentOpenedModule ? getOpenedModuleMethod(forTable) : getMethod(forTable);
        if (method != null) {
            return ProjectHelper.testers(method,
                currentOpenedModule ? openedModuleCompiledOpenClass : compiledOpenClass);
        }
        return null;
    }

    /**
     * Gets all test methods for method by uri.
     *
     * @param tableUri uri for method table
     * @return all test methods, including tests with test cases, runs with filled runs, tests without cases(empty),
     *         runs without any parameters and tests without cases and runs.
     */
    public IOpenMethod[] getTestAndRunMethods(String tableUri, boolean currentOpenedModule) {
        IOpenMethod method = getMethod(tableUri);
        if (method != null) {
            List<IOpenMethod> res = new ArrayList<>();
            Collection<IOpenMethod> methods;
            if (currentOpenedModule) {
                methods = openedModuleCompiledOpenClass.getOpenClassWithErrors().getMethods();
            } else {
                methods = compiledOpenClass.getOpenClassWithErrors().getMethods();
            }
            for (IOpenMethod tester : methods) {
                if (ProjectHelper.isTestForMethod(tester, method)) {
                    res.add(tester);
                }
            }
            return res.toArray(IOpenMethod.EMPTY_ARRAY);
        }
        return null;
    }

    public TestSuiteMethod[] getAllTestMethods() {
        if (isCompiledSuccessfully()) {
            return ProjectHelper.allTesters(compiledOpenClass.getOpenClassWithErrors());
        }
        return null;
    }

    public TestSuiteMethod[] getOpenedModuleTestMethods() {
        if (isOpenedModuleCompiledSuccessfully()) {
            return ProjectHelper.allTesters(openedModuleCompiledOpenClass.getOpenClassWithErrors());
        }
        return null;
    }

    public WorkbookSyntaxNode[] getWorkbookNodes() {
        if (!isCompiledSuccessfully()) {
            return null;
        }

        return getXlsModuleNode().getWorkbookSyntaxNodes();
    }

    /**
     * Get all workbooks of all modules
     * 
     * @return all workbooks
     */
    public Collection<WorkbookSyntaxNode> getAllWorkbookNodes() {
        if (!isCompiledSuccessfully()) {
            return null;
        }

        return workbookSyntaxNodes;
    }

    public boolean isSourceModified() {
        RulesProject project = getProject();
        if (project == null || !project.isOpened()) {
            return false;
        }
        if (studio.isProjectFrozen(project.getName())) {
            log.debug("Project is saving currently. Ignore it's intermediate state.");
            return false;
        }

        if (isModified()) {
            getLocalRepository().getProjectState(moduleInfo.getRulesPath().toString()).notifyModified();
            return true;
        }
        return false;
    }

    public void resetSourceModified() {
        isModified();
    }

    public CompiledOpenClass getCompiledOpenClass() {
        return compiledOpenClass;
    }

    public CompiledOpenClass getOpenedModuleCompiledOpenClass() {
        return openedModuleCompiledOpenClass;
    }

    public synchronized ProjectCompilationStatus getCompilationStatus() {
        ProjectCompilationStatus.Builder compilationStatus = ProjectCompilationStatus.newBuilder();
        if (moduleInfo != null && moduleInfo.getWebstudioConfiguration() != null && moduleInfo
            .getWebstudioConfiguration()
            .isCompileThisModuleOnly()) {
            compilationStatus.addMessages(compiledOpenClass.getAllMessages());
            compilationStatus.setModulesCompiled(1);
            compilationStatus.addModulesCount(1);
        } else {
            Consumer<ProjectDescriptor> projectDescriptorConsumer = (projectDescriptor) -> {
                projectDescriptor.getModules()
                    .stream()
                    .map(Module::getName)
                    .map(webStudioWorkspaceDependencyManager::findDependencyLoadersByName)
                    .map(Collection::size)
                    .forEach(compilationStatus::addModulesCount);

                if (isProjectCompilationCompleted()) {
                    compilationStatus.clearMessages()
                        .addMessages(compiledOpenClass.getAllMessages())
                        .setModulesCompiled(compilationStatus.build().getModulesCount());
                } else {
                    if (!Objects.equals(projectDescriptor.getName(), moduleInfo.getProject().getName())) {
                        String dependencyName = ProjectExternalDependenciesHelper
                            .buildDependencyNameForProject(projectDescriptor.getName());
                        webStudioWorkspaceDependencyManager.findDependencyLoadersByName(dependencyName)
                            .stream()
                            .map(IDependencyLoader::getRefToCompiledDependency)
                            .filter(Objects::nonNull)
                            .map(CompiledDependency::getCompiledOpenClass)
                            .map(CompiledOpenClass::getMessages)
                            .forEach(compilationStatus::addMessages);
                    }

                    projectDescriptor.getModules().forEach(module -> {
                        if (Objects.equals(module.getName(), moduleInfo.getName()) && Objects
                            .equals(module.getProject().getName(), moduleInfo.getProject().getName())) {
                            compilationStatus.addMessages(openedModuleCompiledOpenClass.getAllMessages())
                                .addModulesCompiled(1);
                        } else {
                            webStudioWorkspaceDependencyManager.findDependencyLoadersByName(module.getName())
                                .stream()
                                .map(IDependencyLoader::getRefToCompiledDependency)
                                .filter(Objects::nonNull)
                                .forEach(compiledDependency -> compilationStatus
                                    .addMessages(compiledDependency.getCompiledOpenClass().getMessages())
                                    .addModulesCompiled(1));
                        }
                    });
                }
            };

            forEachProjectDependency(projectDescriptorConsumer);
        }
        return compilationStatus.build();
    }

    private void forEachProjectDependency(Consumer<ProjectDescriptor> projectDescriptorConsumer) {
        if (webStudioWorkspaceDependencyManager != null) {
            Deque<ProjectDescriptor> queue = new ArrayDeque<>();
            queue.add(moduleInfo.getProject());
            Set<ProjectDescriptor> projectDescriptors = new HashSet<>();
            while (!queue.isEmpty()) {
                ProjectDescriptor projectDescriptor = queue.poll();
                projectDescriptorConsumer.accept(projectDescriptor);
                if (projectDescriptor.getDependencies() != null) {
                    projectDescriptor.getDependencies()
                        .stream()
                        .map(ProjectDependencyDescriptor::getName)
                        .map(ProjectExternalDependenciesHelper::buildDependencyNameForProject)
                        .map(webStudioWorkspaceDependencyManager::findDependencyLoadersByName)
                        .filter(Objects::nonNull)
                        .forEach(dependencyLoaders -> dependencyLoaders.stream()
                            .filter(Objects::nonNull)
                            .filter(IDependencyLoader::isProjectLoader)
                            .findFirst()
                            .map(IDependencyLoader::getProject)
                            .filter(e->!projectDescriptors.contains(e))
                            .ifPresent(e -> {
                                queue.add(e);
                                projectDescriptors.add(e);
                            }));
                }
            }
        }
    }


    public Collection<OpenLMessage> getModuleMessages() {
        CompiledOpenClass compiledOpenClass = getCompiledOpenClass();
        if (compiledOpenClass != null) {
            return compiledOpenClass.getAllMessages();
        }
        return Collections.emptyList();
    }

    public Collection<OpenLMessage> getOpenedModuleMessages() {
        CompiledOpenClass openedCompiledOpenClass = getOpenedModuleCompiledOpenClass();
        if (openedCompiledOpenClass != null) {
            return openedCompiledOpenClass.getAllMessages();
        }
        return Collections.emptyList();
    }

    /**
     * @return Returns the wrapperInfo.
     */
    public Module getModuleInfo() {
        return moduleInfo;
    }

    public XlsModuleSyntaxNode getXlsModuleNode() {

        if (!isCompiledSuccessfully()) {
            return null;
        }

        return xlsModuleSyntaxNode;
    }

    private XlsModuleSyntaxNode findXlsModuleSyntaxNode(CompiledOpenClass compiledOpenClass) {
        XlsMetaInfo xmi = (XlsMetaInfo) compiledOpenClass.getOpenClassWithErrors().getMetaInfo();
        return xmi == null ? null : xmi.getXlsModuleNode();
    }

    /**
     * Returns if current project is read only.
     *
     * @return <code>true</code> if project is read only.
     */
    public boolean isEditable() {
        if (isGranted(EDIT_PROJECTS) && !isCurrentBranchProtected()) {
            RulesProject project = getProject();

            if (project != null) {
                return project.isLocalOnly() || !project.isLocked() || project.isOpenedForEditing();
            }
        }
        return false;
    }

    public boolean isEditableTable(String uri) {
        return !isTablePart(uri) && isEditable();
    }

    /**
     * Check is the table is partial
     */
    public boolean isTablePart(String uri) {
        IGridTable grid = this.getGridTable(uri);
        return grid != null && grid.getGrid() instanceof CompositeGrid;
    }

    public boolean isCanCreateTable() {
        return isEditable() && isGranted(CREATE_TABLES);
    }

    public boolean isCanEditTable(String uri) {
        return isEditableTable(uri) && isGranted(EDIT_TABLES) && !isCurrentBranchProtected();
    }

    public boolean isCanEditProject() {
        return isEditable() && isGranted(EDIT_TABLES);
    }

    private boolean isCurrentBranchProtected() {
        RulesProject project = getProject();
        if (project != null && !project.isLocalOnly()) {
            Repository repo = project.getDesignRepository();
            return repo.supports().branches() && ((BranchRepository) repo).isBranchProtected(project.getBranch());
        }
        return false;
    }

    public boolean isReady() {
        return compiledOpenClass != null;
    }

    public boolean isTestable(String uri) {
        IOpenMethod m = getMethod(uri);
        if (m == null) {
            return false;
        }

        return ProjectHelper.testers(m, compiledOpenClass).length > 0;
    }

    public synchronized void buildProjectTree() {
        if (compiledOpenClass == null || studio.getCurrentModule() == null) {
            return;
        }

        ProjectTreeNode root = makeProjectTreeRoot();

        TableSyntaxNode[] tableSyntaxNodes = getTableSyntaxNodes();

        OverloadedMethodsDictionary methodNodesDictionary = makeMethodNodesDictionary(tableSyntaxNodes);

        TreeNodeBuilder<Object>[] treeSorters = studio.getTreeView().getBuilders();

        // Find all group sorters defined for current subtree.
        // Group sorter should have additional information for grouping
        // nodes by method signature.
        // author: Alexey Gamanovich
        //
        for (TreeNodeBuilder<?> treeSorter : treeSorters) {

            if (treeSorter instanceof OpenMethodsGroupTreeNodeBuilder) {
                // Set to sorter information about open methods.
                // author: Alexey Gamanovich
                //
                OpenMethodsGroupTreeNodeBuilder tableTreeNodeBuilder = (OpenMethodsGroupTreeNodeBuilder) treeSorter;
                tableTreeNodeBuilder.setOpenMethodGroupsDictionary(methodNodesDictionary);
            }
        }

        for (TableSyntaxNode tableSyntaxNode : tableSyntaxNodes) {
            ProjectTreeNode element = root;
            for (TreeNodeBuilder treeSorter : treeSorters) {
                element = addToNode(element, tableSyntaxNode, treeSorter);
            }
        }
        dependencyGraph = null;

        projectRoot = build(root);

        initProjectHistory();
    }

    /**
     * Adds new object to target tree node.
     *
     * The algorithm of adding new object to tree is following: the new object is passed to each tree node builder using
     * order in which they are appear in builders array. Tree node builder makes appropriate tree node or nothing if it
     * is not necessary (e.g. builder that makes folder nodes). The new node is added to tree.
     *
     * @param targetNode target node to which will be added new object
     * @param object object to add
     */
    private ProjectTreeNode addToNode(ProjectTreeNode targetNode, Object object, TreeNodeBuilder treeNodeBuilder) {

        // Create key for adding object. It used to check that the same node
        // exists.
        //
        Comparable<?> key = treeNodeBuilder.makeKey(object);

        ProjectTreeNode element = null;

        // If key is null the rest of building node process should be skipped.
        //
        if (treeNodeBuilder.isBuilderApplicableForObject(object) && key != null) {

            // Try to find child node with the same object.
            //
            element = targetNode.getChild(key);

            // If element is null the node with same object is absent.
            //
            if (element == null) {

                // Build new node for the object.
                //
                element = treeNodeBuilder.makeNode(object, 0);

                // If element is null then builder has not created the new
                // element
                // and this builder should be skipped.
                // author: Alexey Gamanovich
                //
                if (element != null) {
                    targetNode.addChild(key, element);
                } else {
                    element = targetNode;
                }
            }

            // ///////
            // ???????????????????
            // //////
            else if (treeNodeBuilder.isUnique(object)) {

                for (int i = 2; i < 100; ++i) {

                    Comparable<?> key2 = treeNodeBuilder.makeKey(object, i);
                    element = targetNode.getChild(key2);

                    if (element == null) {

                        element = treeNodeBuilder.makeNode(object, i);

                        // If element is null then sorter has not created the
                        // new
                        // element and this sorter should be skipped.
                        // author: Alexey Gamanovich
                        //
                        if (element != null) {
                            targetNode.addChild(key2, element);
                        } else {
                            element = targetNode;
                        }

                        break;
                    }
                }
            }
        }

        // If node is null skip the current builder: set the targetNode to
        // current element.
        //
        if (element == null) {
            element = targetNode;
        }

        return element;
    }

    private boolean isGapOverlap(ProjectTreeNode tableNode) {
        if (tableNode.getTableSyntaxNode() != null) {
            String tableType = tableNode.getTableSyntaxNode().getType();
            if (XlsNodeTypes.XLS_DT.toString().equals(tableType)) {
                return DispatcherTablesBuilder.isDispatcherTable(tableNode.getTableSyntaxNode());
            }
        }
        return false;
    }

    private TreeNode build(ProjectTreeNode root) {
        TreeNode node = createNode(root);
        Iterable<ProjectTreeNode> children = root.getChildren();
        int errors = 0;
        for (ProjectTreeNode child : children) {
            // Always hide dispatcher tables
            if (isGapOverlap(child)) {
                continue;
            }
            TreeNode rfChild = build(child);
            if (IProjectTypes.PT_WORKSHEET.equals(rfChild.getType()) || IProjectTypes.PT_WORKBOOK
                .equals(rfChild.getType())) {
                // skip workbook or worksheet node if it has no children nodes
                if (!rfChild.getChildrenKeysIterator().hasNext()) {
                    continue;
                }
            }
            errors += rfChild.getNumErrors();
            node.addChild(rfChild, rfChild);
        }
        node.setNumErrors(node.getNumErrors() + errors);
        return node;
    }

    private TreeNode createNode(ProjectTreeNode element) {

        boolean leaf = element.getChildren().isEmpty();
        String name = element.getDisplayName(INamedThing.SHORT);
        String title = element.getDisplayName(INamedThing.REGULAR);

        String type = element.getType();
        String url = null;
        int state = 0;
        int numErrors = 0;
        boolean active = true;

        if (type.startsWith(IProjectTypes.PT_TABLE + ".")) {
            TableSyntaxNode tsn = element.getTableSyntaxNode();
            url = studio.url("table?" + Constants.REQUEST_PARAM_ID + "=" + tsn.getId());
            if (studio.getModel().isTestable(element.getTableSyntaxNode().getUri())) {
                state = 2; // has tests
            }

            if (leaf) {
                numErrors = getErrorsByUri(tsn.getUri()).size();
            }
            ITableProperties tableProperties = tsn.getTableProperties();
            if (tableProperties != null) {
                Boolean act = tableProperties.getActive();
                if (act != null) {
                    active = act;
                }
            }
        }
        TreeNode node = new TreeNode(leaf);
        node.setName(name);
        node.setTitle(title);
        node.setType(type);
        node.setUrl(url);
        node.setState(state);
        node.setNumErrors(numErrors);
        node.setActive(active);

        return node;
    }

    private void initProjectHistory() {
        WorkbookSyntaxNode[] workbookNodes = getWorkbookNodes();
        if (workbookNodes != null) {
            LocalRepository repository = getLocalRepository();

            for (WorkbookSyntaxNode workbookSyntaxNode : workbookNodes) {
                XlsWorkbookSourceCodeModule sourceCodeModule = workbookSyntaxNode.getWorkbookSourceCodeModule();

                Collection<XlsWorkbookListener> listeners = sourceCodeModule.getListeners();
                for (XlsWorkbookListener listener : listeners) {
                    if (listener instanceof XlsModificationListener) {
                        return;
                    }
                }

                sourceCodeModule.addListener(new XlsModificationListener(repository, getHistoryStoragePath()));
            }
        }
    }

    private LocalRepository getLocalRepository() {
        UserWorkspace userWorkspace = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession());
        return userWorkspace.getLocalWorkspace().getRepository(studio.getCurrentRepositoryId());
    }

    public synchronized TableSyntaxNode[] getTableSyntaxNodes() {
        if (isCompiledSuccessfully()) {
            XlsModuleSyntaxNode moduleSyntaxNode = getXlsModuleNode();
            return moduleSyntaxNode.getXlsTableSyntaxNodes();
        }

        return TableSyntaxNode.EMPTY_ARRAY;
    }

    public synchronized Set<TableSyntaxNode> getAllTableSyntaxNodes() {
        Set<TableSyntaxNode> result = ConcurrentHashMap.newKeySet();
        forEachProjectDependency(
            (projectDescriptor) -> getModuleSyntaxNodesByProject(projectDescriptor.getName()).stream()
                .map(XlsModuleSyntaxNode::getXlsTableSyntaxNodes)
                .filter(Objects::nonNull)
                .map(Arrays::asList)
                .forEach(result::addAll));
        return result;
    }

    private synchronized Set<TableSyntaxNode> getCurrentProjectTableSyntaxNodes() {
        return Optional.ofNullable(studio.getCurrentProject())
            .map(AProjectFolder::getName)
            .map(this::getModuleSyntaxNodesByProject)
            .map(nodes -> nodes.stream()
                .filter(Objects::nonNull)
                .map(XlsModuleSyntaxNode::getXlsTableSyntaxNodes)
                .map(Arrays::asList)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet()))
            .orElse(Collections.emptySet());
    }

    public synchronized int getNumberOfTables() {
        int count = 0;
        TableSyntaxNode[] tables = getTableSyntaxNodes();

        for (TableSyntaxNode table : tables) {
            if (!XlsNodeTypes.XLS_OTHER.toString().equals(table.getType())) {
                count++;
            }
        }
        return count;
    }

    private OverloadedMethodsDictionary makeMethodNodesDictionary(TableSyntaxNode[] tableSyntaxNodes) {

        // Create open methods dictionary that organizes
        // open methods in groups using their meta info.
        // Dictionary contains required information what will be used to create
        // groups of methods in tree.
        // author: Alexey Gamanovich
        //
        List<TableSyntaxNode> executableNodes = getAllExecutableTables(tableSyntaxNodes);
        OverloadedMethodsDictionary methodNodesDictionary = new OverloadedMethodsDictionary();
        methodNodesDictionary.addAll(executableNodes);

        return methodNodesDictionary;
    }

    private ProjectTreeNode makeProjectTreeRoot() {
        return new ProjectTreeNode(new String[] { null, null, null }, "root", null);
    }

    private List<TableSyntaxNode> getAllExecutableTables(TableSyntaxNode[] nodes) {
        List<TableSyntaxNode> executableNodes = new ArrayList<>();
        for (TableSyntaxNode node : nodes) {
            if (node.getMember() instanceof IOpenMethod) {
                executableNodes.add(node);
            }
        }
        return executableNodes;
    }

    public synchronized void redraw() {
        projectRoot = null;
    }

    public void reset(ReloadType reloadType) throws Exception {
        reset(reloadType, moduleInfo);
    }

    public synchronized void reset(ReloadType reloadType, Module moduleToOpen) throws Exception {
        switch (reloadType) {
            case FORCED:
                moduleToOpen = studio.getCurrentModule();
                // falls through
            case RELOAD:
                if (webStudioWorkspaceDependencyManager != null) {
                    webStudioWorkspaceDependencyManager.shutdown();
                    xlsModuleSyntaxNodesPerProject.clear();
                    workbookSyntaxNodes.clear();
                }
                webStudioWorkspaceDependencyManager = null;
                recentlyVisitedTables.clear();

                break;
            case SINGLE:
                webStudioWorkspaceDependencyManager.reset(new Dependency(DependencyType.MODULE,
                    new IdentifierNode(null, null, moduleToOpen.getName(), null)));
                break;
        }
        setModuleInfo(moduleToOpen, reloadType);
        projectRoot = null;
    }

    public synchronized TestUnitsResults runTest(TestSuite test, boolean currentOpenedModule) {
        Integer threads = Props.integer(AdministrationSettings.TEST_RUN_THREAD_COUNT_PROPERTY);
        boolean isParallel = threads != null && threads > 1;
        return runTest(test,
            isParallel,
            currentOpenedModule ? openedModuleCompiledOpenClass.getOpenClassWithErrors()
                                : compiledOpenClass.getOpenClassWithErrors());
    }

    private TestUnitsResults runTest(TestSuite test, boolean isParallel, IOpenClass openClass) {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(compiledOpenClass.getClassLoader());
            if (!isParallel) {
                return test.invokeSequentially(openClass, 1);
            } else {
                return test.invokeParallel(testSuiteExecutor, openClass, 1);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    public synchronized List<IOpenLTable> search(Predicate<TableSyntaxNode> selectors, SearchScope searchScope) {
        return getSearchScopeData(searchScope).stream()
            .filter(tableSyntaxNode -> !XlsNodeTypes.XLS_TABLEPART.toString().equals(tableSyntaxNode.getType()))
            .filter(selectors)
            .map(TableSyntaxNodeAdapter::new)
            .collect(Collectors.toList());
    }

    // Logic in this block of code implemented with a recursion to achieve sorting of each dataset by comparator
    // and place this sets in the proper order.
    private Set<TableSyntaxNode> getSearchScopeData(SearchScope searchScope) {
        if (searchScope == SearchScope.ALL) {
            Set<TableSyntaxNode> nodes = getSearchScopeData(SearchScope.CURRENT_PROJECT);
            getAllTableSyntaxNodes().stream().sorted(DEFAULT_NODE_CMP).forEach(nodes::add);
            return nodes;
        } else if (searchScope == SearchScope.CURRENT_PROJECT) {
            Set<TableSyntaxNode> nodes = getSearchScopeData(SearchScope.CURRENT_MODULE);
            getCurrentProjectTableSyntaxNodes().stream().sorted(DEFAULT_NODE_CMP).forEach(nodes::add);
            return nodes;
        } else if (searchScope == SearchScope.CURRENT_MODULE) {
            return Arrays.stream(getXlsModuleNode().getXlsTableSyntaxNodes())
                .sorted(DEFAULT_NODE_CMP)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            throw new IllegalStateException();
        }
    }

    public synchronized void clearModuleInfo() {
        this.moduleInfo = null;
        historyStoragePath = null;

        clearModuleResources(); // prevent memory leak

        OpenClassUtil.release(compiledOpenClass);
        compiledOpenClass = null;

        if (webStudioWorkspaceDependencyManager != null) {
            webStudioWorkspaceDependencyManager.shutdown();
            xlsModuleSyntaxNodesPerProject.clear();
            workbookSyntaxNodes.clear();
        }
        webStudioWorkspaceDependencyManager = null;
        xlsModuleSyntaxNode = null;
        projectRoot = null;
    }

    public void setModuleInfo(Module moduleInfo) throws Exception {
        setModuleInfo(moduleInfo, ReloadType.NO);
    }

    private void addCompiledDependency(IDependencyLoader dependencyLoader, CompiledDependency compiledDependency) {
        IMetaInfo metaInfo = compiledDependency.getCompiledOpenClass().getOpenClassWithErrors().getMetaInfo();
        if (metaInfo instanceof XlsMetaInfo) {
            XlsMetaInfo xlsMetaInfo = (XlsMetaInfo) metaInfo;
            XlsModuleSyntaxNode xlsModuleSyntaxNode = xlsMetaInfo.getXlsModuleNode();
            if (xlsModuleSyntaxNode != null) {
                getModuleSyntaxNodesByProject(dependencyLoader.getProject().getName()).add(xlsModuleSyntaxNode);
                if (!(xlsModuleSyntaxNode.getModule() instanceof VirtualSourceCodeModule)) {
                    workbookSyntaxNodes.addAll(Arrays.asList(xlsModuleSyntaxNode.getWorkbookSyntaxNodes()));
                }
            }
        }
    }

    private void removeCompiledDependency(IDependencyLoader dependencyLoader, CompiledDependency compiledDependency) {
        IMetaInfo metaInfo = compiledDependency.getCompiledOpenClass().getOpenClassWithErrors().getMetaInfo();
        if (metaInfo instanceof XlsMetaInfo) {
            XlsMetaInfo xlsMetaInfo = (XlsMetaInfo) metaInfo;
            XlsModuleSyntaxNode xlsModuleSyntaxNode = xlsMetaInfo.getXlsModuleNode();
            if (xlsModuleSyntaxNode != null) {
                getModuleSyntaxNodesByProject(dependencyLoader.getProject().getName()).remove(xlsModuleSyntaxNode);
                if (!(xlsModuleSyntaxNode.getModule() instanceof VirtualSourceCodeModule)) {
                    workbookSyntaxNodes.removeAll(Arrays.asList(xlsModuleSyntaxNode.getWorkbookSyntaxNodes()));
                }
            }
        }
    }

    private Set<XlsModuleSyntaxNode> getModuleSyntaxNodesByProject(String projectName) {
        return xlsModuleSyntaxNodesPerProject.computeIfAbsent(projectName, e -> ConcurrentHashMap.newKeySet());
    }

    public synchronized void setModuleInfo(Module moduleInfo, ReloadType reloadType) throws Exception {
        if (moduleInfo == null || this.moduleInfo == moduleInfo && reloadType == ReloadType.NO) {
            return;
        }

        File projectFolder = moduleInfo.getProject().getProjectFolder().toFile();
        if (reloadType == ReloadType.FORCED) {
            ProjectResolver projectResolver = studio.getProjectResolver();
            ProjectDescriptor projectDescriptor = projectResolver.resolve(projectFolder);
            Module reloadedModule = null;
            for (Module module : projectDescriptor.getModules()) {
                if (moduleInfo.getName().equals(module.getName())) {
                    reloadedModule = module;
                    break;
                }
            }
            this.moduleInfo = reloadedModule;
        } else {
            this.moduleInfo = moduleInfo;
        }

        initHistoryStoragePath();
        isModified();
        clearModuleResources(); // prevent memory leak
        projectRoot = null;
        xlsModuleSyntaxNode = null;

        prepareWorkspaceDependencyManager();

        try {
            CompiledOpenClass thisModuleCompiledOpenClass = webStudioWorkspaceDependencyManager
                .loadDependency(new Dependency(DependencyType.MODULE,
                    new IdentifierNode(DependencyType.MODULE.name(), null, moduleInfo.getName(), null)))
                .getCompiledOpenClass();

            xlsModuleSyntaxNode = findXlsModuleSyntaxNode(thisModuleCompiledOpenClass);
            openedModuleCompiledOpenClass = thisModuleCompiledOpenClass;
            if (compiledOpenClass == null || !isProjectCompilationCompleted() || !ReloadType.NO.equals(reloadType)) {
                compiledOpenClass = thisModuleCompiledOpenClass;
            }
            if (!moduleInfo.getWebstudioConfiguration().isCompileThisModuleOnly()) {
                String projectDependencyName = SimpleDependencyLoader.buildDependencyName(moduleInfo.getProject(),
                    null);
                if (!ReloadType.NO.equals(reloadType) || !Objects.equals(projectDependencyName,
                    projectCompilationCompleted)) {
                    this.compilationInProgress = true;
                    projectCompilationCompleted = null;
                    webStudioWorkspaceDependencyManager.loadDependencyAsync(
                        new Dependency(DependencyType.MODULE,
                            new IdentifierNode(DependencyType.MODULE.name(), null, projectDependencyName, null)),
                        (compiledDependency) -> {
                            try {
                                this.compiledOpenClass = this.validate();
                                XlsMetaInfo metaInfo1 = (XlsMetaInfo) this.compiledOpenClass.getOpenClassWithErrors()
                                    .getMetaInfo();
                                getModuleSyntaxNodesByProject(moduleInfo.getProject().getName())
                                    .add(metaInfo1.getXlsModuleNode());
                                redraw();
                            } catch (Exception | LinkageError e) {
                                onCompilationFailed(e);
                            }
                            this.projectCompilationCompleted = compiledDependency.getDependencyName();
                            this.compilationInProgress = false;
                        });
                }
            } else {
                this.compilationInProgress = false;
            }
        } catch (Exception | LinkageError e) {
            onCompilationFailed(e);
        }
    }

    private void onCompilationFailed(Throwable t) {
        compilationInProgress = false;
        log.error("Failed to load.", t);
        Collection<OpenLMessage> messages = new LinkedHashSet<>();
        for (OpenLMessage openLMessage : OpenLMessagesUtils.newErrorMessages(t)) {
            String message = String.format("Cannot load the module: %s", openLMessage.getSummary());
            messages.add(new OpenLMessage(message, Severity.ERROR));
        }
        compiledOpenClass = new CompiledOpenClass(NullOpenClass.the, messages);
        openedModuleCompiledOpenClass = new CompiledOpenClass(NullOpenClass.the, messages);
    }

    private boolean isModified() {
        if (moduleInfo == null) {
            return false;
        }
        long modificationTime = moduleLastModified;
        moduleLastModified = moduleInfo.getRulesPath().toFile().lastModified();
        return modificationTime != moduleLastModified;
    }

    private CompiledOpenClass validate() throws RulesInstantiationException {
        List<Module> modules = moduleInfo.getProject().getModules();
        RulesInstantiationStrategy instantiationStrategy = new SimpleMultiModuleInstantiationStrategy(modules,
            webStudioWorkspaceDependencyManager,
            false);
        Map<String, Object> externalParameters = ProjectExternalDependenciesHelper
            .buildExternalParamsWithProjectDependencies(studio.getExternalProperties(), modules);
        instantiationStrategy.setExternalParameters(externalParameters);
        OpenApiProjectValidator openApiProjectValidator = new OpenApiProjectValidator();
        return openApiProjectValidator.validate(moduleInfo.getProject(), instantiationStrategy);
    }

    private void prepareWorkspaceDependencyManager() {
        if (webStudioWorkspaceDependencyManager == null) {
            webStudioWorkspaceDependencyManager = webStudioWorkspaceDependencyManagerFactory
                .buildDependencyManager(this.moduleInfo.getProject());
            webStudioWorkspaceDependencyManager.registerOnCompilationCompleteListener(this::addCompiledDependency);
            webStudioWorkspaceDependencyManager.registerOnResetCompleteListener(this::removeCompiledDependency);
            projectCompilationCompleted = null;
        } else {
            Set<ProjectDescriptor> projectsInWorkspace = webStudioWorkspaceDependencyManagerFactory
                .resolveWorkspace(this.moduleInfo.getProject());
            Set<String> projectNamesInWorkspace = projectsInWorkspace.stream()
                .map(ProjectDescriptor::getName)
                .collect(Collectors.toSet());
            boolean foundOpenedProject = false;
            boolean allProjectCanBeReused = true;
            for (ProjectDescriptor projectDescriptor : webStudioWorkspaceDependencyManager.getProjectDescriptors()) {
                if (this.moduleInfo.getProject().getName().equals(projectDescriptor.getName())) {
                    foundOpenedProject = true;
                }
                if (!projectNamesInWorkspace.contains(projectDescriptor.getName())) {
                    allProjectCanBeReused = false;
                }
            }
            if (!foundOpenedProject) {
                if (!allProjectCanBeReused) {
                    webStudioWorkspaceDependencyManager.shutdown();
                    xlsModuleSyntaxNodesPerProject.clear();
                    workbookSyntaxNodes.clear();
                    webStudioWorkspaceDependencyManager = webStudioWorkspaceDependencyManagerFactory
                        .buildDependencyManager(this.moduleInfo.getProject());
                    webStudioWorkspaceDependencyManager
                        .registerOnCompilationCompleteListener(this::addCompiledDependency);
                    webStudioWorkspaceDependencyManager.registerOnResetCompleteListener(this::removeCompiledDependency);
                    projectCompilationCompleted = null;
                } else {
                    // If loaded projects are a part of the new opened project, then we can reuse dependency manager
                    webStudioWorkspaceDependencyManager.expand(
                        webStudioWorkspaceDependencyManagerFactory.resolveWorkspace(this.moduleInfo.getProject()));
                }
            }
        }
    }

    public synchronized void traceElement(TestSuite testSuite) {
        ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
        boolean currentOpenedModule = Boolean
            .parseBoolean(WebStudioUtils.getRequestParameter(Constants.REQUEST_PARAM_CURRENT_OPENED_MODULE));
        try {
            if (currentOpenedModule) {
                Thread.currentThread().setContextClassLoader(openedModuleCompiledOpenClass.getClassLoader());
                CachingArgumentsCloner.initInstance();
                runTest(testSuite, false, openedModuleCompiledOpenClass.getOpenClassWithErrors());
            } else {
                Thread.currentThread().setContextClassLoader(compiledOpenClass.getClassLoader());
                CachingArgumentsCloner.initInstance();
                runTest(testSuite, false, compiledOpenClass.getOpenClassWithErrors());
            }
        } finally {
            Thread.currentThread().setContextClassLoader(currentContextClassLoader);
            CachingArgumentsCloner.removeInstance();
        }
    }

    public synchronized TableEditorModel getTableEditorModel(String tableUri) {
        IOpenLTable table = getTable(tableUri);
        return getTableEditorModel(table);
    }

    public WebStudioWorkspaceRelatedDependencyManager getWebStudioWorkspaceDependencyManager() {
        return webStudioWorkspaceDependencyManager;
    }

    public synchronized TableEditorModel getTableEditorModel(IOpenLTable table) {
        String tableView = studio.getTableView();
        return new TableEditorModel(table, tableView, false);
    }

    public synchronized boolean isCompiledSuccessfully() {
        return compiledOpenClass != null && compiledOpenClass.getOpenClassWithErrors() != null && !(compiledOpenClass
            .getOpenClassWithErrors() instanceof NullOpenClass) && xlsModuleSyntaxNode != null;
    }

    public synchronized boolean isOpenedModuleCompiledSuccessfully() {
        return openedModuleCompiledOpenClass != null && openedModuleCompiledOpenClass
            .getOpenClassWithErrors() != null && !(openedModuleCompiledOpenClass
                .getOpenClassWithErrors() instanceof NullOpenClass) && xlsModuleSyntaxNode != null;
    }

    public synchronized DependencyRulesGraph getDependencyGraph() {
        if (dependencyGraph == null) {
            Collection<IOpenMethod> rulesMethods = compiledOpenClass.getOpenClassWithErrors().getMethods();
            dependencyGraph = DependencyRulesGraph.filterAndCreateGraph(rulesMethods);
        }
        return dependencyGraph;
    }

    public synchronized List<File> getSources() {
        List<File> sourceFiles = new ArrayList<>();

        WorkbookSyntaxNode[] workbookNodes = getWorkbookNodes();
        if (workbookNodes != null) {
            for (WorkbookSyntaxNode workbookSyntaxNode : workbookNodes) {
                File sourceFile = workbookSyntaxNode.getWorkbookSourceCodeModule().getSourceFile();
                sourceFiles.add(sourceFile);
            }
        }

        // TODO: Consider the case when there is compilation error. In this case sourceFiles will be empty, it can break
        // history manager.

        return sourceFiles;
    }

    public synchronized String[] getModuleSourceNames() {
        List<File> moduleSources = getSources();
        String[] moduleSourceNames = new String[moduleSources.size()];
        int i = 0;
        for (File source : moduleSources) {
            moduleSourceNames[i] = source.getName();
            i++;
        }
        return moduleSourceNames;
    }

    public synchronized File getSourceByName(String fileName) {
        List<File> sourceFiles = getSources();

        for (File file : sourceFiles) {
            if (file.getName().equals(fileName)) {
                return file;
            }
        }

        return null;
    }

    public String getHistoryStoragePath() {
        return historyStoragePath;
    }

    private void initHistoryStoragePath() {
        if (WebStudioUtils.getSession() != null) {
            File location = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession())
                .getLocalWorkspace()
                .getLocation();
            this.historyStoragePath = Paths
                .get(location.getPath(), FolderHelper.resolveHistoryFolder(getProject(), moduleInfo))
                .toString();
        }
    }

    public synchronized RecentlyVisitedTables getRecentlyVisitedTables() {
        return recentlyVisitedTables;
    }

    public synchronized XlsWorkbookSourceCodeModule getCurrentModuleWorkbook() {
        Module currentModule = studio.getCurrentModule();
        if (currentModule == null) {
            return null;
        }

        PathEntry rulesRootPath = currentModule.getRulesRootPath();

        WorkbookSyntaxNode[] workbookNodes = getWorkbookNodes();
        if (workbookNodes == null) {
            return null;
        }

        for (WorkbookSyntaxNode workbookSyntaxNode : workbookNodes) {
            XlsWorkbookSourceCodeModule module = workbookSyntaxNode.getWorkbookSourceCodeModule();
            if (rulesRootPath != null && module.getSourceFile()
                .getName()
                .equals(FileUtils.getName(rulesRootPath.getPath()))) {
                return module;
            }
        }
        return null;
    }

    /**
     * Returns true if both are true: 1) Old project version is opened and 2) project is not modified yet.
     *
     * Otherwise return false
     */
    public synchronized boolean isConfirmOverwriteNewerRevision() {
        RulesProject project = getProject();
        return project != null && project.isOpenedOtherVersion() && !project.isModified();
    }

    public void destroy() {
        clearModuleInfo();
    }

    private void clearModuleResources() {
        removeListeners();
    }

    /**
     * Remove listeners added in {@link #initProjectHistory()}
     */
    private void removeListeners() {
        WorkbookSyntaxNode[] workbookNodes = getWorkbookNodes();
        if (workbookNodes != null) {
            for (WorkbookSyntaxNode workbookSyntaxNode : workbookNodes) {
                XlsWorkbookSourceCodeModule sourceCodeModule = workbookSyntaxNode.getWorkbookSourceCodeModule();

                Iterator<XlsWorkbookListener> iterator = sourceCodeModule.getListeners().iterator();
                while (iterator.hasNext()) {
                    XlsWorkbookListener listener = iterator.next();
                    if (listener instanceof XlsModificationListener) {
                        iterator.remove();
                        break;
                    }
                }
            }
        }
    }

    public boolean isCompilationInProgress() {
        return compilationInProgress;
    }

    public boolean isProjectCompilationCompleted() {
        if (moduleInfo != null) {
            String projectDependencyName = SimpleDependencyLoader.buildDependencyName(moduleInfo.getProject(), null);
            return Objects.equals(projectCompilationCompleted, projectDependencyName);
        }
        return false;
    }

    public synchronized IOpenMethod getCurrentDispatcherMethod(IOpenMethod method, String uri) {
        return getMethodFromDispatcher((OpenMethodDispatcher) method, uri);
    }

    public synchronized String getMessageNodeId(String sourceLocation) {
        XlsUrlParser xlsUrlParser = sourceLocation != null ? new XlsUrlParser(sourceLocation) : null;
        for (TableSyntaxNode tsn : getAllTableSyntaxNodes()) { // for all modules
            if (xlsUrlParser != null && xlsUrlParser.intersects(tsn.getUriParser())) {
                return tsn.getId();
            }
        }
        return null;
    }

    private class XlsModificationListener implements XlsWorkbookListener {

        private final LocalRepository repository;
        private final String historyStoragePath;

        private XlsModificationListener(LocalRepository repository, String historyStoragePath) {
            this.repository = repository;
            this.historyStoragePath = historyStoragePath;
        }

        @Override
        public void beforeSave(XlsWorkbookSourceCodeModule workbookSourceCodeModule) {
            File sourceFile = workbookSourceCodeModule.getSourceFile();
            ProjectHistoryService.init(historyStoragePath, sourceFile);
        }

        @Override
        public void afterSave(XlsWorkbookSourceCodeModule workbookSourceCodeModule) {
            isModified();
            File sourceFile = workbookSourceCodeModule.getSourceFile();
            repository.getProjectState(sourceFile.getPath()).notifyModified();
            ProjectHistoryService.save(historyStoragePath, sourceFile);
        }
    }
}
