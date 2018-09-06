package org.openl.rules.ui;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.Privileges.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.conf.ClassLoaderFactory;
import org.openl.conf.OpenLConfiguration;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.engine.OpenLSystemProperties;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.extension.ExtensionWrapperGrid;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessages;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.dependency.graph.DependencyRulesGraph;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.XlsWorkbookListener;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule.ModificationChecker;
import org.openl.rules.lang.xls.XlsWorkbookSourceHistoryListener;
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
import org.openl.rules.project.instantiation.*;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.table.xls.XlsUrlUtils;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.testmethod.*;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.benchmark.Benchmark;
import org.openl.rules.ui.benchmark.BenchmarkInfo;
import org.openl.rules.ui.benchmark.BenchmarkUnit;
import org.openl.rules.ui.tree.OpenMethodsGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.ProjectTreeNode;
import org.openl.rules.ui.tree.TreeBuilder;
import org.openl.rules.ui.tree.TreeNodeBuilder;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.rules.webstudio.dependencies.WebStudioWorkspaceDependencyManagerFactory;
import org.openl.rules.webstudio.dependencies.WebStudioWorkspaceRelatedDependencyManager;
import org.openl.rules.webstudio.web.trace.node.CachingArgumentsCloner;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.source.SourceHistoryManager;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.NullOpenClass;
import org.openl.util.FileUtils;
import org.openl.util.ISelector;
import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.tree.ITreeElement;
import org.openl.vm.IRuntimeEnv;

public class ProjectModel {

    /**
     * Compiled rules with errors. Representation of wrapper.
     */
    private CompiledOpenClass compiledOpenClass;

    private XlsModuleSyntaxNode xlsModuleSyntaxNode;
    private Collection<XlsModuleSyntaxNode> allXlsModuleSyntaxNodes = new HashSet<XlsModuleSyntaxNode>();

    private Module moduleInfo;

    private boolean openedInSingleModuleMode;

    private WebStudioWorkspaceDependencyManagerFactory webStudioWorkspaceDependencyManagerFactory;
    private WebStudioWorkspaceRelatedDependencyManager webStudioWorkspaceDependencyManager;

    private WebStudio studio;

    private ColorFilterHolder filterHolder = new ColorFilterHolder();

    private ProjectTreeNode projectRoot = null;

    // TODO Fix performance
    private Map<String, TableSyntaxNode> uriTableCache = new HashMap<String, TableSyntaxNode>();
    private Map<String, TableSyntaxNode> idTableCache = new HashMap<String, TableSyntaxNode>();

    private DependencyRulesGraph dependencyGraph;

    private SourceHistoryManager<File> historyManager;

    private RecentlyVisitedTables recentlyVisitedTables = new RecentlyVisitedTables();
    private final TestSuiteExecutor testSuiteExecutor;
    private final TestRunner testRunner = new TestRunner(BaseTestUnit.Builder.getInstance());

    /**
     * For tests only
     */
    ProjectModel(WebStudio studio) {
        this(studio, null);
    }

    public ProjectModel(WebStudio studio, TestSuiteExecutor testSuiteExecutor) {
        this.studio = studio;
        this.openedInSingleModuleMode = studio.isSingleModuleModeByDefault();
        this.webStudioWorkspaceDependencyManagerFactory = new WebStudioWorkspaceDependencyManagerFactory(studio);
        this.testSuiteExecutor = testSuiteExecutor;
    }

    public RulesProject getProject() {
        return studio.getCurrentProject();
    }

    public BenchmarkInfo benchmarkTestsSuite(final TestSuite testSuite, int ms) throws Exception {
        final IOpenClass openClass = compiledOpenClass.getOpenClassWithErrors();

        ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(compiledOpenClass.getClassLoader());

            // Object res = null;
            BenchmarkUnit bu = null;

            try {

                bu = new BenchmarkUnit() {
                    @Override
                    public String getName() {
                        return testSuite.getName();
                    }

                    @Override
                    public int nUnitRuns() {
                        return testSuite.getNumberOfTests();
                    }

                    @Override
                    protected void run() throws Exception {
                        throw new RuntimeException();
                    }

                    @Override
                    public void runNtimes(long times) throws Exception {
                        testSuite.invokeSequentially(openClass, times);
                    }
                };
                return new Benchmark().runUnit(bu, ms);

            } catch (Throwable t) {
                Log.error("Run Error:", t);
                return new BenchmarkInfo(t, bu, testSuite.getName());
            }
        } finally {
            Thread.currentThread().setContextClassLoader(currentContextClassLoader);
        }
    }

    public BenchmarkInfo benchmarkSingleTest(final TestSuite testSuite, final int testIndex, int ms) throws Exception {

        BenchmarkUnit bu;

        final IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        final Object target = compiledOpenClass.getOpenClassWithErrors().newInstance(env);

        bu = new BenchmarkUnit() {

            @Override
            public String getName() {
                return testSuite.getName() + ":" + testIndex;
            }

            @Override
            public void run() throws Exception {
                throw new RuntimeException();
            }

            @Override
            public void runNtimes(long times) throws Exception {
                try {
                    TestDescription test = testSuite.getTest(testIndex);
                    testRunner.runTest(test, target, env, times);
                } catch (Throwable t) {
                    Log.error("Error during Method run: ", t);
                    throw RuntimeExceptionWrapper.wrap(t);
                }
            }
        };

        return new Benchmark().runUnit(bu, ms);

    }

    public TableSyntaxNode findNode(String url) {
        XlsUrlParser parsedUrl = new XlsUrlParser();
        parsedUrl.parse(url);

        if (parsedUrl.getRange() == null) {
            return null;
        }

        return findNode(parsedUrl);
    }

    public String findTableUri(String partialUri) {
        TableSyntaxNode tableSyntaxNode = findNode(partialUri);

        if (tableSyntaxNode != null) {
            return tableSyntaxNode.getUri();
        }

        return null;
    }

    private boolean findInCompositeGrid(CompositeGrid compositeGrid, XlsUrlParser p1) {
        for (IGridTable gridTable : compositeGrid.getGridTables()) {
            if (gridTable.getGrid() instanceof CompositeGrid) {
                if (findInCompositeGrid((CompositeGrid) gridTable.getGrid(), p1)) {
                    return true;
                }
            } else {
                if (XlsUrlUtils.intersects(p1, gridTable.getUriParser())) {
                    return true;
                }
            }
        }
        return false;
    }

    private TableSyntaxNode findNode(XlsUrlParser p1) {
//        TableSyntaxNode[] nodes = getTableSyntaxNodes();
        TableSyntaxNode[] nodes = getAllTableSyntaxNodes();

        for (int i = 0; i < nodes.length; i++) {
            if (XlsUrlUtils.intersects(p1, nodes[i].getGridTable().getUriParser())) {
                TableSyntaxNode tsn = nodes[i];
                if (XlsNodeTypes.XLS_TABLEPART.equals(tsn.getNodeType())) {
                    for (int j = 0; j < nodes.length; j++) {
                        IGridTable table = nodes[j].getGridTable();
                        if (table.getGrid() instanceof CompositeGrid) {
                            CompositeGrid compositeGrid = (CompositeGrid) table.getGrid();
                            if (findInCompositeGrid(compositeGrid, p1)) {
                                return nodes[j];
                            }
                        }
                    }
                }
                return tsn;
            }
        }

        return null;
    }

    // TODO Cache it
    public int getErrorNodesNumber() {
        int count = 0;
        if (compiledOpenClass != null) {
            TableSyntaxNode[] nodes = getTableSyntaxNodes();

            for (int i = 0; i < nodes.length; i++) {
                TableSyntaxNode tsn = nodes[i];

                if (tsn.getErrors() != null) {
                    count++;
                }
            }
        }
        return count;
    }

    public Map<String, TableSyntaxNode> getAllTableNodes() {
        return uriTableCache;
    }

    public TableSyntaxNode getTableByUri(String uri) {
        return uriTableCache.get(uri);
    }

    public TableSyntaxNode getNodeById(String id) {
        return idTableCache.get(id);
    }

    public ColorFilterHolder getFilterHolder() {
        return filterHolder;
    }

    public IOpenMethod getMethod(String tableUri) {
        TableSyntaxNode tsn = getNode(tableUri);
        if (tsn == null) {
            return null;
        }

        return getMethod(tsn);
    }

    public List<IOpenMethod> getTargetMethods(String testOrRunUri) {
        List<IOpenMethod> targetMethods = new ArrayList<IOpenMethod>();
        IOpenMethod testMethod = getMethod(testOrRunUri);

        if (testMethod instanceof TestSuiteMethod) {
            IOpenMethod targetMethod = ((TestSuiteMethod) testMethod).getTestedMethod();

            // Overloaded methods
            if (targetMethod instanceof OpenMethodDispatcher) {
                List<IOpenMethod> overloadedMethods = ((OpenMethodDispatcher) targetMethod).getCandidates();
                targetMethods.addAll(overloadedMethods);
            } else {
                targetMethods.add(targetMethod);
            }
        }

        return targetMethods;
    }

    public List<IOpenLTable> getTargetTables(String testOrRunUri) {
        List<IOpenLTable> targetTables = new ArrayList<IOpenLTable>();
        List<IOpenMethod> targetMethods = getTargetMethods(testOrRunUri);

        for (IOpenMethod targetMethod : targetMethods) {
            if (targetMethod != null) {
                IMemberMetaInfo methodInfo = targetMethod.getInfo();
                if (methodInfo != null) {
                    TableSyntaxNode tsn = (TableSyntaxNode) methodInfo.getSyntaxNode();
                    IOpenLTable targetTable = new TableSyntaxNodeAdapter(tsn);
                    targetTables.add(targetTable);
                }
            }
        }

        return targetTables;
    }

    public IOpenMethod getMethod(TableSyntaxNode tsn) {

        if (!isProjectCompiledSuccessfully()) {
            return null;
        }

        IOpenClass openClass = compiledOpenClass.getOpenClassWithErrors();

        for (IOpenMethod method : openClass.getMethods()) {
            IOpenMethod resolvedMethod = null;

            if (method instanceof OpenMethodDispatcher) {
                resolvedMethod = resolveMethodDispatcher((OpenMethodDispatcher) method, tsn);
            } else {
                resolvedMethod = resolveMethod(method, tsn);
            }

            if (resolvedMethod != null) {
                return resolvedMethod;
            }
        }

        // for methods that exist in module but not included in
        // CompiledOpenClass
        // e.g. elder inactive versions of methods
        if (tsn.getMember() instanceof IOpenMethod) {
            return (IOpenMethod) tsn.getMember();
        }

        return null;
    }

    private IOpenMethod resolveMethodDispatcher(OpenMethodDispatcher method, TableSyntaxNode syntaxNode) {
        List<IOpenMethod> candidates = method.getCandidates();

        for (IOpenMethod candidate : candidates) {
            IOpenMethod resolvedMethod = resolveMethod(candidate, syntaxNode);

            if (resolvedMethod != null) {
                return resolvedMethod;
            }
        }

        return null;
    }

    private IOpenMethod getMethodFromDispatcher(OpenMethodDispatcher method, TableSyntaxNode syntaxNode) {
        List<IOpenMethod> candidates = method.getCandidates();

        for (IOpenMethod candidate : candidates) {
            IOpenMethod resolvedMethod = resolveMethod(candidate, syntaxNode);

            if (resolvedMethod != null) {
                return resolvedMethod;
            }
        }

        return null;
    }

    private IOpenMethod resolveMethod(IOpenMethod method, TableSyntaxNode syntaxNode) {

        if (isInstanceOfTable(method, syntaxNode)) {
            return method;
        }

        return null;
    }

    /**
     * Checks that {@link IOpenMethod} object is instance that represents the
     * given {@link TableSyntaxNode} object. Actually,
     * {@link IOpenMethod} object must have the same syntax node as given one.
     * If given method is instance of {@link OpenMethodDispatcher}
     * <code>false</code> value will be returned.
     *
     * @param method     method to check
     * @param syntaxNode syntax node
     * @return <code>true</code> if {@link IOpenMethod} object represents the
     * given table syntax node; <code>false</code> - otherwise
     */
    private boolean isInstanceOfTable(IOpenMethod method, TableSyntaxNode syntaxNode) {

        IMemberMetaInfo metaInfo = method.getInfo();

        return (metaInfo != null && metaInfo.getSyntaxNode() == syntaxNode);
    }

    public TableSyntaxNode getNode(String tableUri) {
        TableSyntaxNode tsn = null;
        if (tableUri != null) {
            tsn = getTableByUri(tableUri);
            if (tsn == null) {
                tsn = findNode(tableUri);
            }
        }
        return tsn;
    }

    public synchronized ITreeElement<?> getProjectTree() {
        if (projectRoot == null) {
            buildProjectTree();
        }
        return projectRoot;
    }

    public IOpenLTable getTable(String tableUri) {
        TableSyntaxNode tsn = getNode(tableUri);
        if (tsn != null) {
            return new TableSyntaxNodeAdapter(tsn);
        }
        updateCacheTree();
        tsn = getNode(tableUri);
        if (tsn != null) {
            return new TableSyntaxNodeAdapter(tsn);
        }
        return null;

    }

    public IOpenLTable getTableById(String id) {
        if (projectRoot == null) {
            buildProjectTree();
        }
        TableSyntaxNode tsn = getNodeById(id);
        if (tsn != null) {
            return new TableSyntaxNodeAdapter(tsn);
        }
        updateCacheTree();
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
     * @return all test methods, including tests with test cases, runs with
     * filled runs, tests without cases(empty), runs without any
     * parameters and tests without cases and runs.
     */
    public IOpenMethod[] getTestAndRunMethods(String tableUri) {
        IOpenMethod method = getMethod(tableUri);
        if (method != null) {
            List<IOpenMethod> res = new ArrayList<IOpenMethod>();
            Collection<IOpenMethod> methods = compiledOpenClass.getOpenClassWithErrors().getMethods();

            for (IOpenMethod tester : methods) {
                if (ProjectHelper.isTestForMethod(tester, method)) {
                    res.add(tester);
                }
            }
            return res.toArray(new IOpenMethod[0]);
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

    public boolean isSourceModified() {
        WorkbookSyntaxNode[] workbookNodes = getWorkbookNodes();
        if (workbookNodes != null) {
            for (WorkbookSyntaxNode node : workbookNodes) {
                XlsWorkbookSourceCodeModule workbookSourceCodeModule = node.getWorkbookSourceCodeModule();
                if (workbookSourceCodeModule.isModified()) {
                    getLocalRepository().getProjectState(workbookSourceCodeModule.getSourceFile().getPath()).notifyModified();
                    return true;
                }
            }
        }
        return false;
    }

    public void resetSourceModified() {
        WorkbookSyntaxNode[] workbookNodes = getWorkbookNodes();
        if (workbookNodes != null) {
            for (WorkbookSyntaxNode node : workbookNodes) {
                node.getWorkbookSourceCodeModule().resetModified();
            }
        }
    }

    public CompiledOpenClass getCompiledOpenClass() {
        return compiledOpenClass;
    }

    public List<OpenLMessage> getModuleMessages() {
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

    private XlsModuleSyntaxNode findXlsModuleSyntaxNode(IDependencyManager dependencyManager) {
        if (isSingleModuleMode()) {
            XlsMetaInfo xmi = (XlsMetaInfo) compiledOpenClass.getOpenClassWithErrors().getMetaInfo();
            return xmi.getXlsModuleNode();
        } else {
            try {
                Dependency dependency = new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, moduleInfo.getName(), null));

                XlsMetaInfo xmi = (XlsMetaInfo) dependencyManager.loadDependency(dependency)
                        .getCompiledOpenClass().getOpenClassWithErrors().getMetaInfo();
                return xmi == null ? null : xmi.getXlsModuleNode();
            } catch (OpenLCompilationException e) {
                throw new OpenlNotCheckedException(e);
            }
        }
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
        return !isTablePart(uri) && !isExtensionGrid(uri) && isEditable();
    }

    /**
     * Check is the table is partial
     */
    public boolean isTablePart(String uri) {
        IGridTable grid = this.getGridTable(uri);

        if (grid != null && grid.getGrid() instanceof CompositeGrid) {
            return true;
        }

        return false;
    }

    public boolean isExtensionGrid(String uri) {
        IGridTable grid = getGridTable(uri);
        return grid != null && grid.getGrid() instanceof ExtensionWrapperGrid;
    }

    public boolean isCurrentModuleLoadedByExtension() {
        Module moduleInfo = getModuleInfo();
        return moduleInfo != null && moduleInfo.getExtension() != null;
    }

    public boolean isCanCreateTable() {
        return isEditable() && isGranted(CREATE_TABLES) && !isCurrentModuleLoadedByExtension();
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

    public boolean isTestable(TableSyntaxNode tsn) {
        IOpenMethod m = getMethod(tsn);
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

        TreeBuilder<Object> treeBuilder = new TreeBuilder<Object>();

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

        for (int i = 0; i < tableSyntaxNodes.length; i++) {
            treeBuilder.addToNode(root, tableSyntaxNodes[i], treeSorters);
        }

        projectRoot = root;
        uriTableCache.clear();
        idTableCache.clear();
        cacheTree(projectRoot);

        dependencyGraph = null;

        historyManager = null;
        initProjectHistory();
    }

    private void initProjectHistory() {
        WorkbookSyntaxNode[] workbookNodes = getWorkbookNodes();
        if (workbookNodes != null) {
            LocalRepository repository = getLocalRepository();

            for (WorkbookSyntaxNode workbookSyntaxNode : workbookNodes) {
                XlsWorkbookSourceCodeModule sourceCodeModule = workbookSyntaxNode.getWorkbookSourceCodeModule();

                Collection<XlsWorkbookListener> listeners = sourceCodeModule.getListeners();
                for (XlsWorkbookListener listener : listeners) {
                    if (listener instanceof XlsWorkbookSourceHistoryListener) {
                        return;
                    }
                }

                XlsWorkbookListener historyListener = new XlsWorkbookSourceHistoryListener(getHistoryManager());
                sourceCodeModule.addListener(historyListener);
                sourceCodeModule.addListener(new XlsModificationListener(repository));
            }
        }
    }

    private LocalRepository getLocalRepository() {
        UserWorkspace userWorkspace = WebStudioUtils.getUserWorkspace(FacesUtils.getSession());
        return userWorkspace.getLocalWorkspace().getRepository();
    }

    public TableSyntaxNode[] getTableSyntaxNodes() {
        if (isProjectCompiledSuccessfully()) {
            XlsModuleSyntaxNode moduleSyntaxNode = getXlsModuleNode();
            return moduleSyntaxNode.getXlsTableSyntaxNodes();
        }

        return new TableSyntaxNode[0];
    }

    public TableSyntaxNode[] getAllTableSyntaxNodes() {
        List<TableSyntaxNode> nodes = new ArrayList<TableSyntaxNode>();

        if (isProjectCompiledSuccessfully()) {
            for (XlsModuleSyntaxNode node : allXlsModuleSyntaxNodes) {
                if (node != null) {
                    nodes.addAll(Arrays.asList(node.getXlsTableSyntaxNodes()));
                }
            }
        }

        return nodes.toArray(new TableSyntaxNode[nodes.size()]);
    }

    public int getNumberOfTables() {
        int count = 0;
        TableSyntaxNode[] tables = getTableSyntaxNodes();

        for (TableSyntaxNode table : tables) {
            if (!XlsNodeTypes.XLS_OTHER.toString().equals(table.getType())) {
                count++;
            }
        }
        return count;
    }

    private void cacheTree(ProjectTreeNode treeNode) {
        Iterable<? extends ITreeElement<Object>> children = treeNode.getChildren();
        for (ITreeElement<Object> item : children) {
            // TODO: Remove class casting
            ProjectTreeNode child = (ProjectTreeNode) item;
            if (child.getType().startsWith(IProjectTypes.PT_TABLE + ".")) {
                TableSyntaxNode tsn = child.getTableSyntaxNode();
                uriTableCache.put(child.getUri(), tsn);
                idTableCache.put(tsn.getId(), tsn);
            }
            cacheTree(child);
        }
    }
    
    private void updateCacheTree() {
        TableSyntaxNode[] tableSyntaxNodes = getTableSyntaxNodes();
        for (TableSyntaxNode tsn : tableSyntaxNodes) {
            if (tsn.getType().startsWith(XlsNodeTypes.XLS_DT.toString())) {
                if (!uriTableCache.containsKey(tsn.getUri())){
                    uriTableCache.put(tsn.getUri(), tsn);
                }
                if (!idTableCache.containsKey(tsn.getId())){
                    idTableCache.put(tsn.getId(), tsn);
                }
            }
        }
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
        return new ProjectTreeNode(new String[]{null, null, null}, "root", null, null, 0, null);
    }

    private List<TableSyntaxNode> getAllExecutableTables(TableSyntaxNode[] nodes) {
        List<TableSyntaxNode> executableNodes = new ArrayList<TableSyntaxNode>();
        for (TableSyntaxNode node : nodes) {
            if (node.getMember() instanceof IOpenMethod) {
                executableNodes.add(node);
            }
        }
        return executableNodes;
    }

    public void redraw() throws Exception {
        projectRoot = null;
    }

    public void reset(ReloadType reloadType) throws Exception {
        reset(reloadType, moduleInfo);
    }

    public void reset(ReloadType reloadType, Module moduleToOpen) throws Exception {
        switch (reloadType) {
            case FORCED:
                OpenL.reset();
                OpenLConfiguration.reset();
                ClassLoaderFactory.reset();
                moduleToOpen = studio.getCurrentModule();
                // falls through
            case RELOAD:
                if (webStudioWorkspaceDependencyManager != null){
                    webStudioWorkspaceDependencyManager.resetAll();
                }
                webStudioWorkspaceDependencyManager = null;
                recentlyVisitedTables.clear();
                break;
            case SINGLE:
                webStudioWorkspaceDependencyManager.reset(new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, moduleToOpen.getName(), null)));
                break;
        }
        setModuleInfo(moduleToOpen, reloadType);
        projectRoot = null;
    }

    public TestUnitsResults runTest(TestSuite test) {
        boolean isParallel = OpenLSystemProperties.isRunTestsInParallel(studio.getSystemConfigManager().getProperties());
        return runTest(test, isParallel);
    }

    private TestUnitsResults runTest(TestSuite test, boolean isParallel) {
        IOpenClass openClass = compiledOpenClass.getOpenClassWithErrors();
        if (!isParallel) {
            return test.invokeSequentially(openClass, 1);
        } else {
            return test.invokeParallel(testSuiteExecutor, openClass, 1);
        }
    }

    public List<IOpenLTable> search(ISelector<TableSyntaxNode> selectors) {
        XlsModuleSyntaxNode xsn = getXlsModuleNode();
        List<IOpenLTable> searchResults = new ArrayList<IOpenLTable>();

        TableSyntaxNode[] tables = xsn.getXlsTableSyntaxNodes();
        for (TableSyntaxNode table : tables) {
            if (!XlsNodeTypes.XLS_TABLEPART.toString().equals(table.getType()) // Exclude
                                                                               // TablePart
                                                                               // tables
                    && selectors.select(table)) {
                searchResults.add(new TableSyntaxNodeAdapter(table));
            }
        }

        return searchResults;
    }

    public void setProjectTree(ProjectTreeNode projectRoot) {
        this.projectRoot = projectRoot;
    }

    public void clearModuleInfo() {
        this.moduleInfo = null;

        // Clear project messages (errors, warnings)
        clearOpenLMessages();

        clearModuleResources(); // prevent memory leak

        compiledOpenClass = null;
        if (webStudioWorkspaceDependencyManager != null){
            webStudioWorkspaceDependencyManager.resetAll();
        }
        webStudioWorkspaceDependencyManager = null;
        xlsModuleSyntaxNode = null;
        allXlsModuleSyntaxNodes.clear();
        projectRoot = null;
    }

    public void setModuleInfo(Module moduleInfo) throws Exception {
        setModuleInfo(moduleInfo, ReloadType.NO);
    }

    // TODO Remove "throws Exception"
    public void setModuleInfo(Module moduleInfo, ReloadType reloadType) throws Exception {
        setModuleInfo(moduleInfo, reloadType, shouldOpenInSingleMode(moduleInfo));
    }
    
    public synchronized void setModuleInfo(Module moduleInfo, ReloadType reloadType, boolean singleModuleMode) throws Exception {
        if (moduleInfo == null || (this.moduleInfo == moduleInfo && reloadType == ReloadType.NO)) {
            return;
        }

        boolean moduleInfoWasChanged = false;
        
        if (moduleInfo != this.moduleInfo) {
            moduleInfoWasChanged = true;
            // Current module changed - mark the previous one as read only
            XlsModuleSyntaxNode moduleSyntaxNode = xlsModuleSyntaxNode;
            if (moduleSyntaxNode != null) {
                for (WorkbookSyntaxNode workbookSyntaxNode : moduleSyntaxNode.getWorkbookSyntaxNodes()) {
                    workbookSyntaxNode.getWorkbookSourceCodeModule().getWorkbookLoader().setCanUnload(true);
                }
            }
        }

        if (reloadType != ReloadType.NO) {
            uriTableCache.clear();
            idTableCache.clear();
        }

        File projectFolder = moduleInfo.getProject().getProjectFolder();
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
        

        // Clear project messages (errors, warnings)
        clearOpenLMessages();

        clearModuleResources(); // prevent memory leak

        compiledOpenClass = null;
        projectRoot = null;
        xlsModuleSyntaxNode = null;
        allXlsModuleSyntaxNodes.clear();
        
        prepareWebstudioWorkspaceDependencyManager(singleModuleMode, moduleInfoWasChanged);
        
        Map<String, Object> externalParameters;
        RulesInstantiationStrategy instantiationStrategy;
        
        //Create instantiation strategy for opened module
        if (singleModuleMode) {
            ClassLoader classLoader = webStudioWorkspaceDependencyManager.getClassLoader(this.moduleInfo.getProject());
            instantiationStrategy = RulesInstantiationStrategyFactory.getStrategy(this.moduleInfo, false, webStudioWorkspaceDependencyManager, classLoader);
            externalParameters = studio.getSystemConfigManager().getProperties();
        } else {
            List<Module> modules = this.moduleInfo.getProject().getModules();
            instantiationStrategy = new SimpleMultiModuleInstantiationStrategy(modules, webStudioWorkspaceDependencyManager, false);

            externalParameters = ProjectExternalDependenciesHelper.getExternalParamsWithProjectDependencies(studio.getSystemConfigManager()
                .getProperties(),
                modules);

        }
        instantiationStrategy.setExternalParameters(externalParameters);
        instantiationStrategy.setServiceClass(SimpleProjectDependencyLoader.EmptyInterface.class);
        
        LazyWorkbookLoaderFactory factory = new LazyWorkbookLoaderFactory();

        try {
            if (reloadType == ReloadType.FORCED) {
                // FIXME Why we create classloader in singleModuleMode and immediately clear it by invoking forcedReset() ?
                instantiationStrategy.forcedReset();
            } else if (reloadType != ReloadType.NO) {
                instantiationStrategy.reset();
            }

            WorkbookLoaders.setCurrentFactory(factory);
            factory.disallowUnload();

            // Find all dependent XlsModuleSyntaxNode-s
            final String moduleName = moduleInfo.getName();

            compiledOpenClass = instantiationStrategy.compile();

            if (reloadType == ReloadType.FORCED) {
                // EPBDS-6193: After instantiationStrategy.forcedReset() classloader is cleared (not closed). But after
                // instantiationStrategy.compile() the new one is created. We need to save the latter inside dependency
                // manager, otherwise newly created classloader will not be closed when the project is deleted.
                // TODO Classloader must be created, handled and cleared by dependency manager only, not in the ProjectModel
                webStudioWorkspaceDependencyManager.replaceClassLoader(moduleInfo.getProject(), instantiationStrategy.getClassLoader());
            }

            for (CompiledDependency dependency : webStudioWorkspaceDependencyManager.getCompiledDependencies()) {
                if (!dependency.getDependencyName().equals(moduleName)) {
                    XlsMetaInfo metaInfo = (XlsMetaInfo) dependency.getCompiledOpenClass().getOpenClassWithErrors().getMetaInfo();

                    if (metaInfo != null) {
                        allXlsModuleSyntaxNodes.add(metaInfo.getXlsModuleNode());
                    }
                }
            }

            xlsModuleSyntaxNode = findXlsModuleSyntaxNode(webStudioWorkspaceDependencyManager);
            allXlsModuleSyntaxNodes.add(xlsModuleSyntaxNode);
            if (!isSingleModuleMode()) {
                // EPBDS-7629: In multimodule mode xlsModuleSyntaxNode doesn't contain Virtual Module with dispatcher table syntax nodes.
                // Such dispatcher syntax nodes are needed to show dispatcher tables in Trace.
                // That's why we should add virtual module to allXlsModuleSyntaxNodes.
                XlsMetaInfo xmi = (XlsMetaInfo) compiledOpenClass.getOpenClassWithErrors().getMetaInfo();
                allXlsModuleSyntaxNodes.add(xmi.getXlsModuleNode());
            }

            factory.allowUnload();
            WorkbookLoaders.resetCurrentFactory();

            // Edit current module, others should be read only
            // TODO Set edit mode only when actually editing: cell edit, table creating wizards etc
            WorkbookSyntaxNode[] workbookNodes = getWorkbookNodes();
            if (workbookNodes != null) {
                for (WorkbookSyntaxNode workbookSyntaxNode : workbookNodes) {
                    XlsWorkbookSourceCodeModule module = workbookSyntaxNode.getWorkbookSourceCodeModule();
                    boolean currentModule = this.moduleInfo.getRulesRootPath() == null ||
                            module.getSourceFile().getName().equals(FileUtils.getName(this.moduleInfo.getRulesRootPath().getPath()));
                    module.getWorkbookLoader().setCanUnload(!currentModule);
                }
            }
        } catch (Throwable t) {
            Log.error("Problem Loading OpenLWrapper", t);
            List<OpenLMessage> messages = new ArrayList<OpenLMessage>();
            for (OpenLMessage openLMessage : OpenLMessagesUtils.newMessages(t)) {
                String message = String.format("Can't load the module: %s", openLMessage.getSummary());
                messages.add(new OpenLMessage(message, Severity.ERROR));
            }

            compiledOpenClass = new CompiledOpenClass(NullOpenClass.the, messages, new SyntaxNodeException[0],
                    new SyntaxNodeException[0]);

            factory.allowUnload();
            WorkbookLoaders.resetCurrentFactory();
        }

    }

    private void prepareWebstudioWorkspaceDependencyManager(boolean singleModuleMode, boolean moduleInfoWasChanged) {
        if (webStudioWorkspaceDependencyManager == null) {
            webStudioWorkspaceDependencyManager = webStudioWorkspaceDependencyManagerFactory
                .getDependencyManager(this.moduleInfo, singleModuleMode);
            openedInSingleModuleMode = singleModuleMode;
        } else {
            if (openedInSingleModuleMode == singleModuleMode) {
                boolean found = false;
                for (ProjectDescriptor projectDescriptor : webStudioWorkspaceDependencyManager
                    .getProjectDescriptors()) {
                    if (this.moduleInfo.getProject().getName().equals(projectDescriptor.getName())) {
                        found = true;
                        break;
                    }
                }
                if (!found || moduleInfoWasChanged && singleModuleMode) {
                    webStudioWorkspaceDependencyManager.resetAll();
                    webStudioWorkspaceDependencyManager = webStudioWorkspaceDependencyManagerFactory
                        .getDependencyManager(this.moduleInfo, singleModuleMode);
                    openedInSingleModuleMode = singleModuleMode;
                }
            } else {
                webStudioWorkspaceDependencyManager.resetAll();
                webStudioWorkspaceDependencyManager = webStudioWorkspaceDependencyManagerFactory
                    .getDependencyManager(this.moduleInfo, singleModuleMode);
                openedInSingleModuleMode = singleModuleMode;
            }
        }
    }

    /**
     * To prevent memory leaks. OpenLMessages instance is ThreadLocal and we
     * have to clear previous OpenLMessages instance if it was created from
     * another thread(due to running as web application).
     */
    private OpenLMessages previousUsedMessages;

    private void clearOpenLMessages() {
        if (previousUsedMessages != null) {
            previousUsedMessages.clear();
        }
        previousUsedMessages = OpenLMessages.getCurrentInstance();
    }

    public void traceElement(TestSuite testSuite) {
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

    public TableEditorModel getTableEditorModel(String tableUri) {
        IOpenLTable table = getTable(tableUri);
        return getTableEditorModel(table);
    }

    public TableEditorModel getTableEditorModel(IOpenLTable table) {
        String tableView = studio.getTableView();
        return new TableEditorModel(table, tableView, false);
    }

    public boolean isProjectCompiledSuccessfully() {
        return compiledOpenClass != null && compiledOpenClass.getOpenClassWithErrors() != null
                && !(compiledOpenClass.getOpenClassWithErrors() instanceof NullOpenClass)
                && xlsModuleSyntaxNode != null;
    }

    public DependencyRulesGraph getDependencyGraph() {
        if (dependencyGraph == null) {
            Collection<IOpenMethod> rulesMethods = compiledOpenClass.getOpenClassWithErrors().getMethods();
            dependencyGraph = DependencyRulesGraph.filterAndCreateGraph(rulesMethods);
        }
        return dependencyGraph;
    }

    public List<File> getSources() {
        List<File> sourceFiles = new ArrayList<File>();

        WorkbookSyntaxNode[] workbookNodes = getWorkbookNodes();
        if (workbookNodes != null) {
            for (WorkbookSyntaxNode workbookSyntaxNode : workbookNodes) {
                File sourceFile = workbookSyntaxNode.getWorkbookSourceCodeModule().getSourceFile();
                sourceFiles.add(sourceFile);
            }
        }

        // TODO: Consider the case when there is compilation error. In this case sourceFiles will be empty, it can break history manager.

        return sourceFiles;
    }

    public String[] getModuleSourceNames() {
        List<File> moduleSources = getSources();
        String[] moduleSourceNames = new String[moduleSources.size()];
        int i = 0;
        for (File source : moduleSources) {
            moduleSourceNames[i] = source.getName();
            i++;
        }
        return moduleSourceNames;
    }

    public File getSourceByName(String fileName) {
        List<File> sourceFiles = getSources();

        for (File file : sourceFiles) {
            if (file.getName().equals(fileName)) {
                return file;
            }
        }

        return null;
    }

    public SourceHistoryManager<File> getHistoryManager() {
        if (historyManager == null) {
            String projecthistoryHome = studio.getSystemConfigManager().getStringProperty("project.history.home");
            Integer maxFilesInStorage = studio.getSystemConfigManager().getIntegerProperty("project.history.count");
            boolean unlimitedStorage = studio.getSystemConfigManager().getBooleanProperty("project.history.unlimited");
            String storagePath = projecthistoryHome + File.separator + getProject().getName();
            historyManager = new FileBasedProjectHistoryManager(this, storagePath, maxFilesInStorage, unlimitedStorage);
        }
        return historyManager;
    }

    public RecentlyVisitedTables getRecentlyVisitedTables() {
        return recentlyVisitedTables;
    }

    public void openWorkbookForEdit(String workBookName) {
        for (WorkbookSyntaxNode workbookSyntaxNode : getWorkbookNodes()) {
            XlsWorkbookSourceCodeModule module = workbookSyntaxNode.getWorkbookSourceCodeModule();

            if (module.getSourceFile().getName().equals(workBookName)) {
                module.setModificationChecker(new EditXlsModificationChecker(module));
                break;
            }
        }

    }

    public void afterOpenWorkbookForEdit(String workBookName) {
        for (WorkbookSyntaxNode workbookSyntaxNode : getWorkbookNodes()) {
            XlsWorkbookSourceCodeModule module = workbookSyntaxNode.getWorkbookSourceCodeModule();
            if (module.getSourceFile().getName().equals(workBookName)) {
                ModificationChecker checker = module.getModificationChecker();

                if (checker instanceof EditXlsModificationChecker) {
                    ((EditXlsModificationChecker) checker).afterXlsOpened();
                }

                break;
            }
        }

    }

    public XlsWorkbookSourceCodeModule getCurrentModuleWorkbook() {
        PathEntry rulesRootPath = studio.getCurrentModule().getRulesRootPath();

        WorkbookSyntaxNode[] workbookNodes = getWorkbookNodes();
        if (workbookNodes == null) {
            return null;
        }

        for (WorkbookSyntaxNode workbookSyntaxNode : workbookNodes) {
            XlsWorkbookSourceCodeModule module = workbookSyntaxNode.getWorkbookSourceCodeModule();
            if (rulesRootPath != null &&
                    module.getSourceFile().getName().equals(FileUtils.getName(rulesRootPath.getPath()))) {
                return module;
            }
        }
        return null;
    }

    public boolean isSingleModuleMode() {
        if (!isProjectCompiledSuccessfully()) {
            return shouldOpenInSingleMode(moduleInfo);
        }
        return !isVirtualWorkbook();
    }

    public void useSingleModuleMode() throws Exception {
        setModuleInfo(moduleInfo, ReloadType.SINGLE, true);
    }

    public void useMultiModuleMode() throws Exception {
        setModuleInfo(moduleInfo, ReloadType.SINGLE, false);
    }

    /**
     * Returns true if both are true:
     * 1) Old project version is opened
     * and
     * 2) project isn't modified yet.
     *
     * Otherwise return false
     */
    public boolean isConfirmOverwriteNewerRevision() {
        RulesProject project = getProject();
        return project != null && project.isOpenedOtherVersion() && !project.isModified();
    }

    private static class EditXlsModificationChecker implements ModificationChecker {
        private final XlsWorkbookSourceCodeModule module;
        private final File sourceFile;

        private final long beforeOpenFileSize;
        private final long beforeOpenModifiedTime;
        private long afterOpenModifiedTime;

        private boolean initializing = true;

        public EditXlsModificationChecker(XlsWorkbookSourceCodeModule module) {
            this.module = module;
            this.sourceFile = module.getSourceFile();
            this.beforeOpenFileSize = sourceFile.length();
            this.beforeOpenModifiedTime = sourceFile.lastModified();
        }

        public void afterXlsOpened() {
            if (module.DEFAULT_MODIDFICATION_CHECKER.isModified() && sourceFile.length() == beforeOpenFileSize) {
                // workaround for xls
                afterOpenModifiedTime = sourceFile.lastModified();
                initializing = false;
            } else {
                // not xls or file is changed. There is no need for a workaround
                module.setModificationChecker(module.DEFAULT_MODIDFICATION_CHECKER);
            }
        }

        @Override
        public boolean isModified() {
            if (initializing) {
                // assume that during opening file for edit it is not changed
                return false;
            }

            if (sourceFile.lastModified() == afterOpenModifiedTime && sourceFile.length() == beforeOpenFileSize) {
                return false;
            }

            // file is modified or closed (modification time is reverted to
            // original state)
            module.setModificationChecker(module.DEFAULT_MODIDFICATION_CHECKER);
            return !(sourceFile.lastModified() == beforeOpenModifiedTime && sourceFile.length() == beforeOpenFileSize);
        }
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
                    if (listener instanceof XlsWorkbookSourceHistoryListener) {
                        iterator.remove();
                        break;
                    }
                }
            }
        }
    }

    public IOpenMethod getCurrentDispatcherMethod(IOpenMethod method, String uri) {
        TableSyntaxNode tsn = getNode(uri);
        return getMethodFromDispatcher((OpenMethodDispatcher) method, tsn);
    }

    private boolean isVirtualWorkbook() {
        XlsMetaInfo xmi = (XlsMetaInfo) compiledOpenClass.getOpenClassWithErrors().getMetaInfo();
        return xmi.getXlsModuleNode().getModule() instanceof VirtualSourceCodeModule;
    }

    /**
     * Determine if we should open in single module mode or multi module mode
     *
     * @param module opening module
     * @return if true - single module mode, if false - multi module mode
     */
    private boolean shouldOpenInSingleMode(Module module) {
        if (module != null) {
            if (moduleInfo != null) {
                ProjectDescriptor project = moduleInfo.getProject();
                ProjectDescriptor newProject = module.getProject();
                if (project.getName().equals(newProject.getName())) {
                    return openedInSingleModuleMode;
                }
            }
        }
        return studio.isSingleModuleModeByDefault();
    }

    private static class XlsModificationListener implements XlsWorkbookListener {

        private final LocalRepository repository;

        private XlsModificationListener(LocalRepository repository) {
            this.repository = repository;
        }

        @Override
        public void beforeSave(XlsWorkbookSourceCodeModule workbookSourceCodeModule) {

        }

        @Override
        public void afterSave(XlsWorkbookSourceCodeModule workbookSourceCodeModule) {
            repository.getProjectState(workbookSourceCodeModule.getSourceFile().getPath()).notifyModified();
        }
    }
}
