package org.openl.rules.ui;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.Privileges.CREATE_TABLES;
import static org.openl.rules.security.Privileges.EDIT_PROJECTS;
import static org.openl.rules.security.Privileges.EDIT_TABLES;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
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
import org.openl.rules.dependency.graph.DependencyRulesGraph;
import org.openl.rules.lang.xls.OverloadedMethodsDictionary;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.XlsWorkbookListener;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.load.LazyWorkbookLoaderFactory;
import org.openl.rules.lang.xls.load.WorkbookLoaders;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
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
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.validation.openapi.OpenApiProjectValidator;
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
import org.openl.rules.webstudio.dependencies.WebStudioWorkspaceDependencyManagerFactory;
import org.openl.rules.webstudio.dependencies.WebStudioWorkspaceRelatedDependencyManager;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.admin.AdministrationSettings;
import org.openl.rules.webstudio.web.trace.node.CachingArgumentsCloner;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
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

    /**
     * Compiled rules with errors. Representation of wrapper.
     */
    private CompiledOpenClass compiledOpenClass;

    private XlsModuleSyntaxNode xlsModuleSyntaxNode;
    private final Collection<XlsModuleSyntaxNode> allXlsModuleSyntaxNodes = new HashSet<>();
    private WorkbookSyntaxNode[] workbookSyntaxNodes;

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

    private List<OpenLMessage> getMessagesByTsn(TableSyntaxNode tableSyntaxNode, Severity severity) {
        List<OpenLMessage> messages = new ArrayList<>();
        for (OpenLMessage openLMessage : getModuleMessages()) {
            if (openLMessage.getSourceLocation() != null && openLMessage.getSeverity() == severity) {
                XlsUrlParser xlsUrlParser = new XlsUrlParser(openLMessage.getSourceLocation());
                if (tableSyntaxNode.getUriParser().intersects(xlsUrlParser)) {
                    messages.add(openLMessage);
                }
            }
        }
        return messages;
    }

    private List<OpenLMessage> getMessagesByTsn(String uri, Severity severity) {
        TableSyntaxNode tableSyntaxNode = getTableByUri(uri);
        return getMessagesByTsn(tableSyntaxNode, severity);
    }

    public synchronized List<OpenLMessage> getErrorsByUri(String uri) {
        return getMessagesByTsn(uri, Severity.ERROR);
    }

    public synchronized ColorFilterHolder getFilterHolder() {
        return filterHolder;
    }

    public synchronized IOpenMethod getMethod(String tableUri) {
        if (!isProjectCompiledSuccessfully()) {
            return null;
        }

        IOpenClass openClass = compiledOpenClass.getOpenClassWithErrors();

        for (IOpenMethod method : openClass.getMethods()) {
            IOpenMethod resolvedMethod;

            if (method instanceof OpenMethodDispatcher) {
                resolvedMethod = resolveMethodDispatcher((OpenMethodDispatcher) method, tableUri);
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
                return resolvedMethod;
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
    public IOpenMethod[] getTestMethods(String forTable) {
        IOpenMethod method = getMethod(forTable);
        if (method != null) {
            return ProjectHelper.testers(method);
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
    public IOpenMethod[] getTestAndRunMethods(String tableUri) {
        IOpenMethod method = getMethod(tableUri);
        if (method != null) {
            List<IOpenMethod> res = new ArrayList<>();
            Collection<IOpenMethod> methods = compiledOpenClass.getOpenClassWithErrors().getMethods();

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
        if (isProjectCompiledSuccessfully()) {
            return ProjectHelper.allTesters(compiledOpenClass.getOpenClassWithErrors());
        }
        return null;
    }

    public WorkbookSyntaxNode[] getWorkbookNodes() {
        if (!isProjectCompiledSuccessfully()) {
            return null;
        }

        return getXlsModuleNode().getWorkbookSyntaxNodes();
    }

    /**
     * Get all workbooks of all modules
     * 
     * @return all workbooks
     */
    public WorkbookSyntaxNode[] getAllWorkbookNodes() {
        if (!isProjectCompiledSuccessfully()) {
            return null;
        }

        return workbookSyntaxNodes;
    }

    public boolean isSourceModified() {
        RulesProject project = getProject();
        if (project != null && studio.isProjectFrozen(project.getName())) {
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

    public Collection<OpenLMessage> getModuleMessages() {
        CompiledOpenClass compiledOpenClass = getCompiledOpenClass();
        if (compiledOpenClass != null) {
            return compiledOpenClass.getMessages();
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

        if (!isProjectCompiledSuccessfully()) {
            return null;
        }

        return xlsModuleSyntaxNode;
    }

    private XlsModuleSyntaxNode findXlsModuleSyntaxNode(CompiledOpenClass compiledOpenClass) {
        XlsMetaInfo xmi = (XlsMetaInfo) compiledOpenClass
                .getOpenClassWithErrors()
                .getMetaInfo();
        return xmi == null ? null : xmi.getXlsModuleNode();
    }

    /**
     * Returns if current project is read only.
     *
     * @return <code>true</code> if project is read only.
     */
    public boolean isEditable() {
        if (isGranted(EDIT_PROJECTS)) {
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
        return isEditableTable(uri) && isGranted(EDIT_TABLES);
    }

    public boolean isCanEditProject() {
        return isEditable() && isGranted(EDIT_TABLES);
    }

    public boolean isReady() {
        return compiledOpenClass != null;
    }

    public boolean isTestable(String uri) {
        IOpenMethod m = getMethod(uri);
        if (m == null) {
            return false;
        }

        return ProjectHelper.testers(m).length > 0;
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

    private TreeNode build(ProjectTreeNode root) {
        TreeNode node = createNode(root);
        Iterable<ProjectTreeNode> children = root.getChildren();
        int errors = 0;
        for (ProjectTreeNode child : children) {
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
            url = WebStudioUtils.getWebStudio().url("table?" + Constants.REQUEST_PARAM_ID + "=" + tsn.getId());
            if (WebStudioUtils.getProjectModel().isTestable(element.getTableSyntaxNode().getUri())) {
                state = 2; // has tests
            }

            String uri = tsn.getUri();
            numErrors = getErrorsByUri(uri).size();
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
        if (isProjectCompiledSuccessfully()) {
            XlsModuleSyntaxNode moduleSyntaxNode = getXlsModuleNode();
            return moduleSyntaxNode.getXlsTableSyntaxNodes();
        }

        return TableSyntaxNode.EMPTY_ARRAY;
    }

    public synchronized Collection<TableSyntaxNode> getAllTableSyntaxNodes() {
        List<TableSyntaxNode> tableSyntaxNodes = new ArrayList<>();
        if (isProjectCompiledSuccessfully()) {
            for (XlsModuleSyntaxNode node : allXlsModuleSyntaxNodes) {
                if (node != null) {
                    tableSyntaxNodes.addAll(Arrays.asList(node.getXlsTableSyntaxNodes()));
                }
            }
        }
        return tableSyntaxNodes;
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
                    webStudioWorkspaceDependencyManager.resetAll();
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

    public synchronized TestUnitsResults runTest(TestSuite test) {
        Integer threads = Props.integer(AdministrationSettings.TEST_RUN_THREAD_COUNT_PROPERTY);
        boolean isParallel = threads != null && threads > 1;
        return runTest(test, isParallel);
    }

    private TestUnitsResults runTest(TestSuite test, boolean isParallel) {
        IOpenClass openClass = compiledOpenClass.getOpenClassWithErrors();
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

    public synchronized List<IOpenLTable> search(Predicate<TableSyntaxNode> selectors) {
        XlsModuleSyntaxNode xsn = getXlsModuleNode();
        List<IOpenLTable> searchResults = new ArrayList<>();

        TableSyntaxNode[] tables = xsn.getXlsTableSyntaxNodes();
        for (TableSyntaxNode table : tables) {
            if (!XlsNodeTypes.XLS_TABLEPART.toString().equals(table.getType()) // Exclude
                    // TablePart
                    // tables
                    && selectors.test(table)) {
                searchResults.add(new TableSyntaxNodeAdapter(table));
            }
        }

        return searchResults;
    }

    public synchronized void clearModuleInfo() {
        this.moduleInfo = null;

        clearModuleResources(); // prevent memory leak

        OpenClassUtil.release(compiledOpenClass);
        compiledOpenClass = null;

        if (webStudioWorkspaceDependencyManager != null) {
            webStudioWorkspaceDependencyManager.shutdown();
            webStudioWorkspaceDependencyManager.resetAll();
        }
        webStudioWorkspaceDependencyManager = null;
        xlsModuleSyntaxNode = null;
        allXlsModuleSyntaxNodes.clear();
        projectRoot = null;
        workbookSyntaxNodes = null;
    }

    public void setModuleInfo(Module moduleInfo) throws Exception {
        setModuleInfo(moduleInfo, ReloadType.NO);
    }

    private void addAllSyntaxNodes() {
        for (IDependencyLoader dl : webStudioWorkspaceDependencyManager.getAllDependencyLoaders()) {
            CompiledDependency compiledDependency = dl.getRefToCompiledDependency();
            if (compiledDependency != null) {
                XlsMetaInfo metaInfo = (XlsMetaInfo) compiledDependency.getCompiledOpenClass()
                    .getOpenClassWithErrors()
                    .getMetaInfo();
                if (metaInfo != null) {
                    allXlsModuleSyntaxNodes.add(metaInfo.getXlsModuleNode());
                }
            }
        }
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

        isModified();
        clearModuleResources(); // prevent memory leak
        compiledOpenClass = null;
        projectRoot = null;
        xlsModuleSyntaxNode = null;
        allXlsModuleSyntaxNodes.clear();
        workbookSyntaxNodes = null;

        prepareWorkspaceDependencyManager();

        // If autoCompile is false we cannot unload workbook during editing because we must show to a user latest edited
        // data (not parsed and compiled data).
        boolean canUnload = studio.isAutoCompile();
        LazyWorkbookLoaderFactory factory = new LazyWorkbookLoaderFactory(canUnload);

        try {
            WorkbookLoaders.setCurrentFactory(factory);

            CompiledOpenClass thisModuleCompiledOpenClass = webStudioWorkspaceDependencyManager
                    .loadDependency(new Dependency(DependencyType.MODULE,
                            new IdentifierNode(DependencyType.MODULE.name(), null, moduleInfo.getName(), null)))
                    .getCompiledOpenClass();

            // Find all dependent XlsModuleSyntaxNode-s
            addAllSyntaxNodes();

            xlsModuleSyntaxNode = findXlsModuleSyntaxNode(thisModuleCompiledOpenClass);
            compiledOpenClass = thisModuleCompiledOpenClass;
            allXlsModuleSyntaxNodes.add(xlsModuleSyntaxNode);

            List<WorkbookSyntaxNode> workbookSyntaxNodes = new ArrayList<>();
            for (XlsModuleSyntaxNode xlsSyntaxNode : allXlsModuleSyntaxNodes) {
                if (!(xlsSyntaxNode.getModule() instanceof VirtualSourceCodeModule)) {
                    workbookSyntaxNodes.addAll(Arrays.asList(xlsSyntaxNode.getWorkbookSyntaxNodes()));
                }
            }
            this.workbookSyntaxNodes = workbookSyntaxNodes.toArray(new WorkbookSyntaxNode[0]);
            // EPBDS-7629: In multimodule mode xlsModuleSyntaxNode does not contain Virtual Module with dispatcher
            // table syntax nodes.
            // Such dispatcher syntax nodes are needed to show dispatcher tables in Trace.
            // That's why we should add virtual module to allXlsModuleSyntaxNodes.
            XlsMetaInfo xmi = (XlsMetaInfo) thisModuleCompiledOpenClass.getOpenClassWithErrors().getMetaInfo();
            allXlsModuleSyntaxNodes.add(xmi.getXlsModuleNode());

            WorkbookLoaders.resetCurrentFactory();

            webStudioWorkspaceDependencyManager
                .loadDependencyAsync(
                    new Dependency(DependencyType.MODULE,
                        new IdentifierNode(DependencyType.MODULE.name(),
                            null,
                            SimpleDependencyLoader.buildDependencyName(moduleInfo.getProject(), null),
                            null)),
                    (e) -> {
                        List<Module> modules = this.moduleInfo.getProject().getModules();
                        RulesInstantiationStrategy instantiationStrategy = new SimpleMultiModuleInstantiationStrategy(
                            modules,
                            webStudioWorkspaceDependencyManager,
                            false);
                        Map<String, Object> externalParameters = ProjectExternalDependenciesHelper
                            .buildExternalParamsWithProjectDependencies(studio.getExternalProperties(), modules);
                        instantiationStrategy.setExternalParameters(externalParameters);
                        try {
                            this.compiledOpenClass = this.validate(instantiationStrategy);
                            XlsMetaInfo metaInfo1 = (XlsMetaInfo) this.compiledOpenClass.getOpenClassWithErrors()
                                .getMetaInfo();
                            allXlsModuleSyntaxNodes.add(metaInfo1.getXlsModuleNode());
                        } catch (RulesInstantiationException ignored) {
                        } finally {
                            addAllSyntaxNodes();
                        }
                    });
        } catch (Throwable t) {
            log.error("Failed to load.", t);
            Collection<OpenLMessage> messages = new LinkedHashSet<>();
            for (OpenLMessage openLMessage : OpenLMessagesUtils.newErrorMessages(t)) {
                String message = String.format("Cannot load the module: %s", openLMessage.getSummary());
                messages.add(new OpenLMessage(message, Severity.ERROR));
            }

            compiledOpenClass = new CompiledOpenClass(NullOpenClass.the, messages);

            WorkbookLoaders.resetCurrentFactory();
        }
    }

    private boolean isModified() {
        if (moduleInfo == null) {
            return false;
        }
        long modificationTime = moduleLastModified;
        moduleLastModified = moduleInfo.getRulesPath().toFile().lastModified();
        return modificationTime != moduleLastModified;
    }

    private CompiledOpenClass validate(
            RulesInstantiationStrategy rulesInstantiationStrategy) throws RulesInstantiationException {
        OpenApiProjectValidator openApiProjectValidator = new OpenApiProjectValidator();
        return openApiProjectValidator.validate(moduleInfo.getProject(), rulesInstantiationStrategy);
    }

    private void prepareWorkspaceDependencyManager() {
        if (webStudioWorkspaceDependencyManager == null) {
            webStudioWorkspaceDependencyManager = webStudioWorkspaceDependencyManagerFactory
                .getDependencyManager(this.moduleInfo);
        } else {
            boolean found = false;
            for (ProjectDescriptor projectDescriptor : webStudioWorkspaceDependencyManager.getProjectDescriptors()) {
                if (this.moduleInfo.getProject().getName().equals(projectDescriptor.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                webStudioWorkspaceDependencyManager.shutdown();
                webStudioWorkspaceDependencyManager.resetAll();
                webStudioWorkspaceDependencyManager = webStudioWorkspaceDependencyManagerFactory
                    .getDependencyManager(this.moduleInfo);
            }
        }
    }

    public synchronized void traceElement(TestSuite testSuite) {
        ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(compiledOpenClass.getClassLoader());
            CachingArgumentsCloner.initInstance();
            runTest(testSuite, false);
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

    public synchronized boolean isProjectCompiledSuccessfully() {
        return compiledOpenClass != null && compiledOpenClass.getOpenClassWithErrors() != null && !(compiledOpenClass
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

    public synchronized String getHistoryStoragePath() {
        if (historyStoragePath == null) {
            File location = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession())
                .getLocalWorkspace()
                .getLocation();
            return Paths.get(location.getPath(), getProject().getName(), ".history", getModuleInfo().getName())
                .toString();
        }
        return historyStoragePath;
    }

    public synchronized RecentlyVisitedTables getRecentlyVisitedTables() {
        return recentlyVisitedTables;
    }

    public synchronized XlsWorkbookSourceCodeModule getCurrentModuleWorkbook() {
        PathEntry rulesRootPath = studio.getCurrentModule().getRulesRootPath();

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

    public synchronized IOpenMethod getCurrentDispatcherMethod(IOpenMethod method, String uri) {
        return getMethodFromDispatcher((OpenMethodDispatcher) method, uri);
    }

    public synchronized String getMessageNodeId(OpenLMessage message) {
        XlsUrlParser xlsUrlParser = message.getSourceLocation() != null ? new XlsUrlParser(message.getSourceLocation())
                                                                        : null;
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
