package org.openl.rules.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.conf.ClassLoaderFactory;
import org.openl.conf.OpenLConfiguration;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessages;
import org.openl.message.Severity;
import org.openl.rules.dependency.graph.DependencyRulesGraph;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.XlsWorkbookListener;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceHistoryListener;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.project.ModulesCache;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.rules.search.IOpenLSearch;
import org.openl.rules.search.ISearchTableRow;
import org.openl.rules.search.OpenLAdvancedSearchResult;
import org.openl.rules.search.OpenLAdvancedSearchResultViewer;
import org.openl.rules.search.OpenLBussinessSearchResult;
import org.openl.rules.search.OpenLSavedSearch;
import org.openl.rules.search.OpenLAdvancedSearchResult.TableAndRows;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.OpenLTable;
import org.openl.rules.table.xls.XlsCellStyle2;
import org.openl.rules.table.xls.XlsSheetGridHelper;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.table.xls.XlsUrlUtils;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnit;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.tree.OpenMethodsGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.ProjectTreeNode;
import org.openl.rules.ui.tree.TreeBuilder;
import org.openl.rules.ui.tree.TreeCache;
import org.openl.rules.ui.tree.TreeNodeBuilder;
import org.openl.rules.ui.RecentlyVisitedTables;
import org.openl.source.SourceHistoryManager;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.IBenchmarkableMethod;
import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;
import org.openl.util.benchmark.Benchmark;
import org.openl.util.benchmark.BenchmarkInfo;
import org.openl.util.benchmark.BenchmarkUnit;
import org.openl.util.export.IExporter;
import org.openl.util.tree.ITreeElement;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;
import org.openl.vm.trace.Tracer;

public class ProjectModel {

    /**
     * Compiled rules with errors. Representation of wrapper.
     */
    private CompiledOpenClass compiledOpenClass;

    private Module moduleInfo;

    private ModulesCache modulesCache = new ModulesCache();

    private ProjectIndexer indexer;

    private WebStudio studio;

    private ColorFilterHolder filterHolder = new ColorFilterHolder();

    private OpenLSavedSearch[] savedSearches = new OpenLSavedSearch[0];

    private ProjectTreeNode projectRoot = null;

    private TreeCache<String, ITreeElement<?>> idTreeCache = new TreeCache<String, ITreeElement<?>>();

    private TreeCache<String, ProjectTreeNode> uriTreeCache = new TreeCache<String, ProjectTreeNode>();

    private DependencyRulesGraph dependencyGraph;

    private SourceHistoryManager<File> historyManager;

    private RecentlyVisitedTables recentlyVisitedTables = new RecentlyVisitedTables();

    // FIXME last test suite should have temporary location(such as Flash scope)
    // but now it placed to session bean due to WebStudio navigation specific
    // TODO move this object to the correctplace
    private TestSuite lastTest;

    public boolean hasLastTest() {
        return lastTest != null;
    }

    public TestSuite popLastTest() {
        TestSuite result = lastTest;
        lastTest = null;
        return result;
    }

    public void setLastTest(TestSuite lastTest) {
        this.lastTest = lastTest;
    }

    public ProjectModel(WebStudio studio) {
        this.studio = studio;
    }    

    public RulesProject getProject() {
        return studio.getCurrentProject();
    }

    public BenchmarkInfo benchmarkTestsSuite(final TestSuite testSuite, int ms) throws Exception {
        final IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        final Object target = compiledOpenClass.getOpenClassWithErrors().newInstance(env);

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
                        public void runNtimes(int times) throws Exception {
                            testSuite.invoke(target, env, times);
                        }

                        @Override
                        public String[] unitName() {
                            //FIXME
                            return testSuite.getTestSuiteMethod().unitName();
                        }

                    };
                BenchmarkUnit[] buu = { bu };
                return new Benchmark(buu).runUnit(bu, ms, false);

            } catch (Throwable t) {
                Log.error("Run Error:", t);
                return new BenchmarkInfo(t, bu, bu.getName());
            }
        } finally {
            Thread.currentThread().setContextClassLoader(currentContextClassLoader);
        }
    }
    
    public BenchmarkInfo benchmarkSingleTest(final TestSuite testSuite, final int testIndex, int ms) throws Exception {

        BenchmarkUnit bu = null;

        final IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
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
            public void runNtimes(int times) throws Exception {
                try {
                    testSuite.getTest(testIndex).runTest(target, env, times);
                } catch (Throwable t) {
                    Log.error("Error during Method run: ", t);
                    throw RuntimeExceptionWrapper.wrap(t);
                }
            }

            @Override
            public String[] unitName() {
                return new String[] { testSuite.getName() + ":" + testIndex };
            }

        };

        BenchmarkUnit[] buu = { bu };
        return new Benchmark(buu).runUnit(bu, ms, false);

    }

    public BenchmarkInfo benchmarkMethod(final IOpenMethod m, int ms) throws Exception {
        final IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        final Object target = compiledOpenClass.getOpenClassWithErrors().newInstance(env);

        ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(compiledOpenClass.getClassLoader());

            final Object[] params = {};

            // Object res = null;
            BenchmarkUnit bu = null;

            try {

                if (m instanceof IBenchmarkableMethod) {
                    final IBenchmarkableMethod bm = (IBenchmarkableMethod) m;
                    bu = new BenchmarkUnit() {
                        @Override
                        public String getName() {
                            return bm.getBenchmarkName();
                        }

                        @Override
                        public int nUnitRuns() {
                            return bm.nUnitRuns();
                        }

                        @Override
                        protected void run() throws Exception {
                            throw new RuntimeException();
                        }

                        @Override
                        public void runNtimes(int times) throws Exception {
                            bm.invokeBenchmark(target, params, env, times);
                        }

                        @Override
                        public String[] unitName() {
                            return bm.unitName();
                        }

                    };

                } else {
                    bu = new BenchmarkUnit() {

                        @Override
                        public String getName() {
                            return m.getName();
                        }

                        @Override
                        protected void run() throws Exception {
                            m.invoke(target, params, env);
                        }

                    };

                }

                BenchmarkUnit[] buu = { bu };
                return new Benchmark(buu).runUnit(bu, ms, false);

            } catch (Throwable t) {
                Log.error("Run Error:", t);
                return new BenchmarkInfo(t, bu, bu.getName());
            }
        } finally {
            Thread.currentThread().setContextClassLoader(currentContextClassLoader);
        }

    }

    public TableSyntaxNode findAnyTableNodeByLocation(XlsUrlParser p1) {        
        TableSyntaxNode[] nodes = getTableSyntaxNodes();

        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i].getType().equals(XlsNodeTypes.XLS_DT.toString())
                    && XlsUrlUtils.intersectsByLocation(p1, nodes[i].getGridTable().getUri())) {
                return nodes[i];
            }
        }

        return null;
    }

    public TableSyntaxNode findNode(String url) {
        XlsUrlParser parsedUrl = new XlsUrlParser();
        parsedUrl.parse(url);

        if (parsedUrl.range == null) {
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

    public TableSyntaxNode findNode(XlsUrlParser p1) {        
        TableSyntaxNode[] nodes = getTableSyntaxNodes();

        for (int i = 0; i < nodes.length; i++) {
            if (XlsUrlUtils.intersects(p1, nodes[i].getGridTable().getUri())) {
                return nodes[i];
            }
        }

        return null;
    }

    public List<TableSyntaxNode> getAllValidatedNodes() {
        if (compiledOpenClass == null) {
            return Collections.emptyList();
        }
        
        TableSyntaxNode[] nodes = getTableSyntaxNodes();

        List<TableSyntaxNode> list = new ArrayList<TableSyntaxNode>();

        for (int i = 0; i < nodes.length; i++) {
            TableSyntaxNode tsn = nodes[i];

            if (tsn.getType() == XlsNodeTypes.XLS_DT.toString()) {
                if (tsn.getErrors() == null) {
                    if (tsn.getTableProperties() != null) {
                        if ("on".equals(tsn.getTableProperties().getValidateDT())) {
                            list.add(tsn);
                        }
                    }
                }
            }
        }
        return list;
    }

    public String getTreeNodeId(ITreeElement<?> treeNode) {
        return idTreeCache.getKey(treeNode);
    }

    public String getTreeNodeId(String uri) {
        ProjectTreeNode node = uriTreeCache.getNode(uri);
        String nodeId = idTreeCache.getKey(node);
        return nodeId;
    }

    public ProjectTreeNode getTreeNodeById(String id) {
        return (ProjectTreeNode) idTreeCache.getNode(id);
    }

    public ProjectTreeNode getTreeNodeByUri(String uri) {
        return uriTreeCache.getNode(uri);
    }

    public ColorFilterHolder getFilterHolder() {
        return filterHolder;
    }

    /**
     * @return Returns the indexer.
     */
    public ProjectIndexer getIndexer() {
        return indexer;
    }

    public IOpenMethod getMethod(String elementUri) {
        TableSyntaxNode tsn = getNode(elementUri);
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

        if(!isProjectCompiledSuccessfully()) {
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

        return null;
    }

    private IOpenMethod resolveMethodDispatcher(OpenMethodDispatcher method, TableSyntaxNode syntaxNode) {

        List<IOpenMethod> candidates = method.getCandidates();

        for (IOpenMethod candidate : candidates) {

            IOpenMethod resolvedMethod = resolveMethod(candidate, syntaxNode);

            if (resolvedMethod != null) {
                return method;
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
     * given {@TableSyntaxNode} object. Actually,
     * {@link IOpenMethod} object must have the same syntax node as given one.
     * If given method is instance of {@link OpenMethodDispatcher}
     * <code>false</code> value will be returned.
     * 
     * @param method method to check
     * @param syntaxNode syntax node
     * @return <code>true</code> if {@link IOpenMethod} object represents the
     *         given table syntax node; <code>false</code> - otherwise
     */
    private boolean isInstanceOfTable(IOpenMethod method, TableSyntaxNode syntaxNode) {

        IMemberMetaInfo metaInfo = method.getInfo();

        return (metaInfo != null && metaInfo.getSyntaxNode() == syntaxNode);
    }

    public TableSyntaxNode getNode(String elementUri) {
        TableSyntaxNode tsn = null;
        if (elementUri != null) {
            ProjectTreeNode pte = getTreeNodeByUri(elementUri);
            if (pte != null) {
                tsn = (TableSyntaxNode) pte.getObject();
            }
            if (tsn == null) {
                tsn = findNode(elementUri);
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

    // TODO Implement with User Settings
    public OpenLSavedSearch[] getSavedSearches() {
        return savedSearches;
    }

    public List<IOpenLTable> getAdvancedSearchResults(Object searchResult) {
        List<IOpenLTable> searchResults = new ArrayList<IOpenLTable>();

        if (searchResult instanceof OpenLAdvancedSearchResult) {
            TableAndRows[] tr = ((OpenLAdvancedSearchResult) searchResult).getFoundTableAndRows();
            OpenLAdvancedSearchResultViewer searchViewer = new OpenLAdvancedSearchResultViewer();
            for (int i = 0; i < tr.length; i++) {
                ISearchTableRow[] rows = tr[i].getRows();
                if (rows.length > 0) {
                    TableSyntaxNode tsn = tr[i].getTsn();
                    String tableUri = tsn.getUri();

                    CompositeGrid cg = searchViewer.makeGrid(rows);
                    IGridTable gridTable = cg != null ? cg.asGridTable() : null;

                    OpenLTable newTable = new OpenLTable();
                    newTable.setGridTable(gridTable);
                    newTable.setUri(tableUri);
                    newTable.setProperties(tsn.getTableProperties());

                    searchResults.add(newTable);
                }
            }
        }

        return searchResults;
    }

    public List<IOpenLTable> getBussinessSearchResults(Object searchResult) {
        List<IOpenLTable> searchResults = new ArrayList<IOpenLTable>();

        if (searchResult instanceof OpenLBussinessSearchResult) {
            List<TableSyntaxNode> foundTables = ((OpenLBussinessSearchResult) searchResult).getFoundTables();
            for (TableSyntaxNode foundTable : foundTables) {
                searchResults.add(new TableSyntaxNodeAdapter(foundTable));
            }
        }

        return searchResults;
    }

    public WebStudio getStudio() {
        return studio;
    }

    public IOpenLTable getTable(String elementUri) {
        TableSyntaxNode tsn = getNode(elementUri);
        if (tsn != null) {
            return new TableSyntaxNodeAdapter(tsn);
        }
        return null;
    }

    public IGridTable getGridTable(String elementUri) {
        TableSyntaxNode tsn = getNode(elementUri);
        return tsn == null ? null : tsn.getGridTable();
    }

    public String getTableView(String view) {
        return view == null ? studio.getTableView() : view;
    }

    /**
     * Gets all test methods for method by uri.
     * 
     * @param elementUri
     * @return all test methods, including tests with test cases, runs with filled runs, tests without cases(empty),
     * runs without any parameters and tests without cases and runs.
     */
    public IOpenMethod[] getAllTestMethods(String elementUri) {
        IOpenMethod method = getMethod(elementUri);
        if (method != null) {
            return ProjectHelper.allTesters(method);
        }
        return null;
    }

    public TestSuiteMethod[] getAllTestMethods() {
        if (isProjectCompiledSuccessfully()) {
            return ProjectHelper.allTesters(compiledOpenClass.getOpenClassWithErrors());
        }
        return null;
    }

    // TODO Refactor
    private WorkbookSyntaxNode[] getWorkbookNodes() {
        if (!isProjectCompiledSuccessfully()) {
            return null;
        }
        
        if (compiledOpenClass != null) {
            XlsModuleSyntaxNode xlsModuleNode = ((XlsMetaInfo) compiledOpenClass.getOpenClassWithErrors().getMetaInfo())
                .getXlsModuleNode();
            WorkbookSyntaxNode[] workbookNodes = xlsModuleNode.getWorkbookSyntaxNodes();
            return workbookNodes;
        }

        return null;
    }

    public boolean isSourceModified() {
        WorkbookSyntaxNode[] workbookNodes = getWorkbookNodes();
        if (workbookNodes != null) {
            for (WorkbookSyntaxNode node : workbookNodes) {
                if (node.getWorkbookSourceCodeModule().isModified()) {
                    return true;
                }
            }
        }
        return false;
    }

    public CompiledOpenClass getCompiledOpenClass() {
        return compiledOpenClass;
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
        
        XlsMetaInfo xmi = (XlsMetaInfo) compiledOpenClass.getOpenClassWithErrors().getMetaInfo();
        XlsModuleSyntaxNode xsn = xmi.getXlsModuleNode();
        return xsn;
    }

    /**
     * Returns if current project is read only.
     * 
     * @return <code>true</code> if project is read only.
     */
    public boolean isEditable() {
        RulesProject project = getProject();

        if (project != null) {
            return project.isCheckedOut() || project.isLocalOnly();
        }

        return false;
    }

    public boolean isReady() {
        return compiledOpenClass != null;
    }

    public boolean isMethodHasParams(String elementUri) {
        IOpenMethod m = getMethod(elementUri);
        if (m == null) {
            return false;
        }
        return ProjectHelper.isMethodHasParams(m);
    }

    public boolean isTestable(String elementUri) {
        IOpenMethod m = getMethod(elementUri);
        if (m == null) {
            return false;
        }

        return ProjectHelper.isTestable(m);
    }

    public boolean isTestable(TableSyntaxNode tsn) {
        IOpenMethod m = getMethod(tsn);
        if (m == null) {
            return false;
        }

        return ProjectHelper.isTestable(m);
    }

    public synchronized void buildProjectTree() {
        if (compiledOpenClass == null) {
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
        uriTreeCache.clear();
        idTreeCache.clear();
        cacheTree(projectRoot);

        dependencyGraph = null;

        historyManager = null;
        initProjectHistory();
    }

    private void initProjectHistory() {
        WorkbookSyntaxNode[] workbookNodes = getWorkbookNodes();
        if (workbookNodes != null) {
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
            }
        }
    }

    private TableSyntaxNode[] getTableSyntaxNodes() {
        if (isProjectCompiledSuccessfully()) {
            XlsModuleSyntaxNode moduleSyntaxNode = getXlsModuleNode();
            TableSyntaxNode[] tableSyntaxNodes = moduleSyntaxNode.getXlsTableSyntaxNodes();
            return tableSyntaxNodes;
        }

        return new TableSyntaxNode[0];
    }

    private void cacheTree(String key, ProjectTreeNode treeNode) {
        int childNumber = 0;
        for (Iterator<?> iterator = treeNode.getChildren(); iterator.hasNext();) {
            ProjectTreeNode child = (ProjectTreeNode) iterator.next();
            if (child.getType().startsWith(IProjectTypes.PT_TABLE + ".")) {
                ProjectTreeNode ptr = (ProjectTreeNode) child;
                uriTreeCache.put(ptr.getUri(), ptr);
            }
            String childKey = (StringUtils.isNotBlank(key) ? key + ":" : "") + (childNumber + 1);
            idTreeCache.put(childKey, child);
            childNumber++;
            cacheTree(childKey, child);
        }
    }

    private void cacheTree(ProjectTreeNode treeNode) {
        cacheTree(null, treeNode);
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
        String moduleName = getModuleDisplayName(moduleInfo);
        return new ProjectTreeNode(new String[] { moduleName, moduleName, moduleName }, "root", null, null, 0, null);
    }

    /**
     * Gets module display name.
     * 
     * @param module OpenL project module
     * @return display name
     */
    public String getModuleDisplayName(Module module) {
        String displayName = module.getName();

        if (displayName.equals(module.getClassname())) {
            displayName = StringTool.lastToken(displayName, ".");
        }
        return displayName + " (" + module.getClassname() + ")";
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
        switch (reloadType) {
            case FORCED:
                OpenL.reset();
                OpenLConfiguration.reset();
                ClassLoaderFactory.reset();
            case RELOAD:
                modulesCache.reset();
            case SINGLE:
                // Clear the cache of dependency manager, as the project has been modified.
                studio.getDependencyManager()
                    .reset(new Dependency(
                            DependencyType.MODULE, new IdentifierNode(null, null, moduleInfo.getName(), null)));
                break;
        }
        setModuleInfo(moduleInfo, reloadType);
        savedSearches = null;
        projectRoot = null;
    }

    public TestUnitsResults[] runAllTests() {
        TestSuiteMethod[] tests = getAllTestMethods();
        TestUnitsResults[] results = new TestUnitsResults[tests.length];
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        Object target = compiledOpenClass.getOpenClassWithErrors().newInstance(env);
        for(int i = 0 ; i< tests.length; i++){
            results[i] = new TestSuite(tests[i]).invoke(target, env, 1);
        }
        return results;
    }
    
    public TestUnitsResults runTestSuite(String tableUri){
        TestSuiteMethod testSuiteMethod = (TestSuiteMethod) getMethod(tableUri);
        return runTestSuite(new TestSuite(testSuiteMethod));
    }

    public TestUnitsResults runTestSuite(TestSuite testSuite) {
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        Object target = compiledOpenClass.getOpenClassWithErrors().newInstance(env);
        return testSuite.invoke(target, env, 1);
    }

    public TestUnit runSingleTestUnit(String tableUri, String testNumber){
        return runSingleTestUnit(tableUri, Integer.valueOf(testNumber));
    }

    public TestUnit runSingleTestUnit(String tableUri, int testNumber){
        TestSuiteMethod testSuiteMethod = (TestSuiteMethod) getMethod(tableUri);
        return runSingleTestUnit(testSuiteMethod.getTest(testNumber));
    }
    public TestUnit runSingleTestUnit(TestDescription test){
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        Object target = compiledOpenClass.getOpenClassWithErrors().newInstance(env);
        return test.runTest(target, env, 1);
    }

    public Object runSearch(IOpenLSearch searchBean) {
        XlsModuleSyntaxNode xsn = getXlsModuleNode();

        return searchBean.search(xsn);
    }

    public void saveSearch(OpenLSavedSearch search) throws Exception {
        XlsWorkbookSourceCodeModule module = getWorkbookNodes()[0].getWorkbookSourceCodeModule();
        if (module != null) {
            IExporter iExporter = XlsSheetGridHelper.createExporter(module);
            iExporter.persist(search);
            module.save();
            reset(ReloadType.SINGLE);
        }
    }

    public void setProjectTree(ProjectTreeNode projectRoot) {
        this.projectRoot = projectRoot;
    }

    public void setModuleInfo(Module moduleInfo) throws Exception {
        setModuleInfo(moduleInfo, ReloadType.NO);
    }

    public void setModuleInfo(Module moduleInfo, ReloadType reloadType) throws Exception {
        if (this.moduleInfo == moduleInfo && reloadType == ReloadType.NO) {
            return;
        }
        
        File projectFolder = moduleInfo.getProject().getProjectFolder();
        if (reloadType == ReloadType.FORCED) {
            RulesProjectResolver projectResolver = studio.getProjectResolver();
            ResolvingStrategy resolvingStrategy = projectResolver.isRulesProject(projectFolder);
            this.moduleInfo = resolvingStrategy.resolveProject(projectFolder)
                .getModuleByClassName(moduleInfo.getClassname());
        } else {
            this.moduleInfo = moduleInfo;
        }

        indexer = new ProjectIndexer(projectFolder.getAbsolutePath());

        // Clear project messages (errors, warnings)
        clearOpenLMessages();

        compiledOpenClass = null;
        projectRoot = null;
        savedSearches = null;
        

        if (reloadType != ReloadType.NO) {
            XlsCellStyle2.cleareThemesCache(); // Clear cache due to new loaded workbooks
        }
        
        RulesInstantiationStrategy instantiationStrategy = modulesCache.getInstantiationStrategy(moduleInfo, 
            studio.getDependencyManager());
        
        try {
            compiledOpenClass = instantiationStrategy.compile(reloadType);
        } catch (Throwable t) {
            Log.error("Problem Loading OpenLWrapper", t);

            String message = "Cannot load the module: " + t.getMessage();
            List<OpenLMessage> messages = new ArrayList<OpenLMessage>();
            messages.add(new OpenLMessage(message, StringUtils.EMPTY, Severity.ERROR));

            compiledOpenClass = new CompiledOpenClass(
                    NullOpenClass.the, messages, new SyntaxNodeException[0], new SyntaxNodeException[0]);
        }

    }

    /**
     * To prevent memory leaks. OpenLMessages instance is ThreadLocal and we
     * have to clear previous OpenLMessages instance if it was created from
     * another thread(due to running as web application).
     */
    private OpenLMessages previousUsedMessages;
    private void clearOpenLMessages(){
        if (previousUsedMessages != null) {
            previousUsedMessages.clear();
        }
        previousUsedMessages = OpenLMessages.getCurrentInstance();
    }

    public Tracer traceElement(TestSuite testSuite) {
        Tracer t = new Tracer();
        Tracer.setTracer(t);

        ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(compiledOpenClass.getClassLoader());
            try {
                runTestSuite(testSuite);
            } finally {
                Tracer.setTracer(null);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(currentContextClassLoader);
        }
        return t;
    }

    public TableEditorModel getTableEditorModel(String tableUri) {
        IOpenLTable table = getTable(tableUri);
        String tableView = getTableView(null);
        TableEditorModel tableModel = new TableEditorModel(table, tableView, false);
        return tableModel;
    }

    public boolean isProjectCompiledSuccessfully() {
        return compiledOpenClass != null 
            && compiledOpenClass.getOpenClassWithErrors() != null 
            && !(compiledOpenClass.getOpenClassWithErrors() instanceof NullOpenClass);
    }

    public DependencyRulesGraph getDependencyGraph() {
        if (dependencyGraph == null) {
            List<IOpenMethod> rulesMethods = compiledOpenClass.getOpenClassWithErrors().getMethods();
            dependencyGraph = DependencyRulesGraph.filterAndCreateGraph(rulesMethods);
        }
        return dependencyGraph;
    }

    public boolean tableBelongsToCurrentModule(String tableUri) {
        return getTable(tableUri) != null;
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
            String storagePath = projecthistoryHome + File.separator + getProject().getName();
            historyManager = new FileBasedProjectHistoryManager(this, storagePath);
        }
        return historyManager;
    }

    public RecentlyVisitedTables getRecentlyVisitedTables() {
        return recentlyVisitedTables;
    }

}
