package org.openl.rules.ui;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.DefaultPrivileges.PRIVILEGE_CREATE_TABLES;
import static org.openl.rules.security.DefaultPrivileges.PRIVILEGE_EDIT_PROJECTS;
import static org.openl.rules.security.DefaultPrivileges.PRIVILEGE_EDIT_TABLES;

import java.io.File;
import java.util.*;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.conf.ClassLoaderFactory;
import org.openl.conf.OpenLConfiguration;
import org.openl.dependency.IDependencyManager;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessages;
import org.openl.message.Severity;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.dependency.graph.DependencyRulesGraph;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.XlsWorkbookListener;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule.ModificationChecker;
import org.openl.rules.lang.xls.XlsWorkbookSourceHistoryListener;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.project.ModulesCache;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.rules.search.IOpenLSearch;
import org.openl.rules.search.ISearchTableRow;
import org.openl.rules.search.OpenLAdvancedSearchResult;
import org.openl.rules.search.OpenLAdvancedSearchResult.TableAndRows;
import org.openl.rules.search.OpenLAdvancedSearchResultViewer;
import org.openl.rules.search.OpenLBussinessSearchResult;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.OpenLTable;
import org.openl.rules.table.search.SearchResult;
import org.openl.rules.table.search.TableSearcher;
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
import org.openl.rules.ui.tree.TreeNodeBuilder;
import org.openl.rules.webstudio.dependencies.WebStudioDependencyManagerFactory;
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
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ISelector;
import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;
import org.openl.util.benchmark.Benchmark;
import org.openl.util.benchmark.BenchmarkInfo;
import org.openl.util.benchmark.BenchmarkUnit;
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

    private ProjectTreeNode projectRoot = null;

    // TODO Fix performance
    private Map<String, TableSyntaxNode> uriTableCache = new HashMap<String, TableSyntaxNode>();

    private DependencyRulesGraph dependencyGraph;

    private SourceHistoryManager<File> historyManager;

    private RecentlyVisitedTables recentlyVisitedTables = new RecentlyVisitedTables();

    // FIXME last test suite should have temporary location(such as Flash scope)
    // but now it placed to session bean due to WebStudio navigation specific
    // TODO move this object to the correct place
    private Stack<TestSuite> testSuitesToRun = new Stack<TestSuite>();

    private WebStudioDependencyManagerFactory dependencyManagerFactory;

    public boolean hasTestSuitesToRun() {
        return testSuitesToRun.size() > 0;
    }

    public TestSuite popLastTest() {
        return testSuitesToRun.pop();
    }

    public void addTestSuiteToRun(TestSuite singleTestSuite) {
        this.testSuitesToRun.push(singleTestSuite);
    }

    public void addTestSuitesToRun(Collection<TestSuite> testSuites) {
        testSuitesToRun.addAll(testSuites);
    }

    public ProjectModel(WebStudio studio) {
        this.studio = studio;
        this.dependencyManagerFactory = new WebStudioDependencyManagerFactory(studio);
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
                    public void runNtimes(long times) throws Exception {
                        testSuite.invoke(target, env, times);
                    }

                    @Override
                    public String[] unitName() {
                        // FIXME
                        return testSuite.getTestSuiteMethod().unitName();
                    }

                };
                BenchmarkUnit[] buu = { bu };
                return new Benchmark(buu).runUnit(bu, ms, false);

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
            public void runNtimes(long times) throws Exception {
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
                        public void runNtimes(long times) throws Exception {
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
                return new BenchmarkInfo(t, bu, bu != null ? bu.getName() : null);
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

    private boolean findInCompositeGrid(CompositeGrid compositeGrid, XlsUrlParser p1) {
        for (IGridTable gridTable : compositeGrid.getGridTables()) {
            if (gridTable.getGrid() instanceof CompositeGrid) {
                if (findInCompositeGrid((CompositeGrid) gridTable.getGrid(), p1)) {
                    return true;
                }
            } else {
                if (XlsUrlUtils.intersects(p1, gridTable.getUri())) {
                    return true;
                }
            }
        }
        return false;
    }

    public TableSyntaxNode findNode(XlsUrlParser p1) {
        TableSyntaxNode[] nodes = getTableSyntaxNodes();

        for (int i = 0; i < nodes.length; i++) {
            if (XlsUrlUtils.intersects(p1, nodes[i].getGridTable().getUri())) {
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

    public ColorFilterHolder getFilterHolder() {
        return filterHolder;
    }

    /**
     * @return Returns the indexer.
     */
    public ProjectIndexer getIndexer() {
        return indexer;
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
                return method;
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

    public List<IOpenLTable> getSearchResults(Object searchResult) {
        List<IOpenLTable> searchResults = new ArrayList<IOpenLTable>();

        if (searchResult instanceof SearchResult) {
            List<TableSyntaxNode> foundTables = ((SearchResult) searchResult).getFoundTables();
            for (TableSyntaxNode foundTable : foundTables) {
                searchResults.add(new TableSyntaxNodeAdapter(foundTable));
            }

        } else if (searchResult instanceof OpenLAdvancedSearchResult) {
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

    @Deprecated
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

    public IOpenLTable getTable(String tableUri) {
        TableSyntaxNode tsn = getNode(tableUri);
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
     * @param forTable
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
     * @param tableUri
     * @return all test methods, including tests with test cases, runs with
     *         filled runs, tests without cases(empty), runs without any
     *         parameters and tests without cases and runs.
     */
    public IOpenMethod[] getTestAndRunMethods(String tableUri) {
        IOpenMethod method = getMethod(tableUri);
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
            return (project.isOpenedForEditing() || project.isLocalOnly()) && isGranted(PRIVILEGE_EDIT_PROJECTS);
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

        if (grid != null && grid.getGrid() instanceof CompositeGrid) {
            return true;
        }

        return false;
    }

    public boolean isCanStartEditing() {
        RulesProject project = getProject();
        return project != null && (project.isLocalOnly() || !project.isLocked()) && isGranted(PRIVILEGE_EDIT_PROJECTS);
    }

    public boolean isCanCreateTable() {
        return isEditable() && isGranted(PRIVILEGE_CREATE_TABLES);
    }

    public boolean isCanEditTable(String uri) {
        return isEditableTable(uri) && isGranted(PRIVILEGE_EDIT_TABLES);
    }

    public boolean isCanEditProject() {
        return isEditable() && isGranted(PRIVILEGE_EDIT_TABLES);
    }

    public boolean isReady() {
        return compiledOpenClass != null;
    }

    public boolean isMethodHasParams(String tableUri) {
        IOpenMethod m = getMethod(tableUri);
        if (m == null) {
            return false;
        }
        return ProjectHelper.isMethodHasParams(m);
    }

    public boolean isTestable(String tableUri) {
        IOpenMethod m = getMethod(tableUri);
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

    public TableSyntaxNode[] getTableSyntaxNodes() {
        if (isProjectCompiledSuccessfully()) {
            XlsModuleSyntaxNode moduleSyntaxNode = getXlsModuleNode();
            TableSyntaxNode[] tableSyntaxNodes = moduleSyntaxNode.getXlsTableSyntaxNodes();
            return tableSyntaxNodes;
        }

        return new TableSyntaxNode[0];
    }

    private void cacheTree(ProjectTreeNode treeNode) {
        for (Iterator<?> iterator = treeNode.getChildren(); iterator.hasNext();) {
            ProjectTreeNode child = (ProjectTreeNode) iterator.next();
            if (child.getType().startsWith(IProjectTypes.PT_TABLE + ".")) {
                uriTableCache.put(child.getUri(), child.getTableSyntaxNode());
            }
            cacheTree(child);
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
                /* falls through */
            case RELOAD:
                modulesCache.reset();
                /* falls through */
            case SINGLE:
                if (moduleInfo != null) {
                    // Clear the cache of dependency manager, as the project has
                    // been modified
                    getDependencyManager().reset(
                            new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, moduleInfo.getName(),
                                    null)));
                }
                break;
        }
        setModuleInfo(moduleInfo, reloadType);
        projectRoot = null;
    }

    public TestUnitsResults[] runAllTests() {
        TestSuiteMethod[] tests = getAllTestMethods();
        TestUnitsResults[] results = new TestUnitsResults[tests.length];
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        Object target = compiledOpenClass.getOpenClassWithErrors().newInstance(env);
        for (int i = 0; i < tests.length; i++) {
            results[i] = new TestSuite(tests[i]).invoke(target, env, 1);
        }
        return results;
    }

    public TestUnitsResults[] runAllTests(String forTable) {
        IOpenMethod[] tests = getTestMethods(forTable);
        if (tests != null) {
            TestUnitsResults[] results = new TestUnitsResults[tests.length];
            for (int i = 0; i < tests.length; i++) {
                results[i] = runTest(new TestSuite((TestSuiteMethod) tests[i]));
            }
            return results;
        }
        return new TestUnitsResults[0];
    }

    public TestUnitsResults runTest(String testUri) {
        TestSuiteMethod testMethod = (TestSuiteMethod) getMethod(testUri);
        return runTest(new TestSuite(testMethod));
    }

    public TestUnitsResults runTest(String testUri, int... caseNumbers) {
        IOpenMethod testMethod = getMethod(testUri);
        TestSuite test;

        if (testMethod instanceof TestSuiteMethod) {
            if (caseNumbers == null) {
                test = new TestSuite((TestSuiteMethod) testMethod);
            } else {
                test = new TestSuite((TestSuiteMethod) testMethod, caseNumbers);
            }
        } else { // Method without cases
            test = new TestSuite(new TestDescription(testMethod, new Object[] {}));
        }

        return runTest(test);
    }

    public TestUnitsResults runTest(TestSuite test) {
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        Object target = compiledOpenClass.getOpenClassWithErrors().newInstance(env);
        return test.invoke(target, env, 1);
    }

    public TestUnit runTestCase(String testUri, String caseNumber) {
        return runTestCase(testUri, Integer.valueOf(caseNumber));
    }

    public TestUnit runTestCase(String testUri, int caseNumber) {
        TestSuiteMethod testMethod = (TestSuiteMethod) getMethod(testUri);
        return runTestCase(testMethod.getTest(caseNumber));
    }

    public TestUnit runTestCase(TestDescription testCase) {
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        Object target = compiledOpenClass.getOpenClassWithErrors().newInstance(env);
        return testCase.runTest(target, env, 1);
    }

    @Deprecated
    public Object runSearch(IOpenLSearch searcher) {
        XlsModuleSyntaxNode xsn = getXlsModuleNode();
        return searcher.search(xsn);
    }

    public List<IOpenLTable> search(ISelector<TableSyntaxNode> selectors) {
        XlsModuleSyntaxNode xsn = getXlsModuleNode();
        return getSearchResults(new TableSearcher().search(xsn, selectors));
    }

    public void setProjectTree(ProjectTreeNode projectRoot) {
        this.projectRoot = projectRoot;
    }

    public void clearModuleInfo() {
        this.moduleInfo = null;

        // Clear project messages (errors, warnings)
        clearOpenLMessages();

        destroy(); // prevent memory leak

        compiledOpenClass = null;
        projectRoot = null;
    }

    public void setModuleInfo(Module moduleInfo) throws Exception {
        setModuleInfo(moduleInfo, ReloadType.NO);
    }

    // TODO Remove "throws Exception"
    public void setModuleInfo(Module moduleInfo, ReloadType reloadType) throws Exception {
        if (moduleInfo == null || (this.moduleInfo == moduleInfo && reloadType == ReloadType.NO)) {
            return;
        }

        if (reloadType != ReloadType.NO) {
            modulesCache.removeCachedModule(moduleInfo);
            uriTableCache.clear();
        }

        File projectFolder = moduleInfo.getProject().getProjectFolder();
        if (reloadType == ReloadType.FORCED) {
            RulesProjectResolver projectResolver = studio.getProjectResolver();
            ResolvingStrategy resolvingStrategy = projectResolver.isRulesProject(projectFolder);
            ProjectDescriptor projectDescriptor = resolvingStrategy.resolveProject(projectFolder);
            this.moduleInfo = projectDescriptor.getModuleByClassName(moduleInfo.getClassname());
            // When moduleInfo cannot be found by class name, it is searched by
            // module name
            if (this.moduleInfo == null) {
                for (Module module : projectDescriptor.getModules()) {
                    if (moduleInfo.getName().equals(module.getName())) {
                        this.moduleInfo = module;
                        break;
                    }
                }
            }
        } else {
            this.moduleInfo = moduleInfo;
        }

        indexer = new ProjectIndexer(projectFolder.getAbsolutePath());

        // Clear project messages (errors, warnings)
        clearOpenLMessages();

        destroy(); // prevent memory leak

        compiledOpenClass = null;
        projectRoot = null;

        RulesInstantiationStrategy instantiationStrategy = modulesCache.getInstantiationStrategy(this.moduleInfo,
                getDependencyManager());
        Map<String, Object> externalParameters = ProjectExternalDependenciesHelper.getExternalParamsWithProjectDependencies(
                studio.getSystemConfigManager().getProperties(),
                Arrays.asList(this.moduleInfo)
        );
        instantiationStrategy.setExternalParameters(externalParameters);

        try {
            if (reloadType == ReloadType.FORCED) {
                instantiationStrategy.forcedReset();
            } else if (reloadType != ReloadType.NO) {
                instantiationStrategy.reset();
            }
            compiledOpenClass = instantiationStrategy.compile();
        } catch (Throwable t) {
            Log.error("Problem Loading OpenLWrapper", t);

            String message = "Cannot load the module: " + t.getMessage();
            List<OpenLMessage> messages = new ArrayList<OpenLMessage>();
            messages.add(new OpenLMessage(message, StringUtils.EMPTY, Severity.ERROR));

            compiledOpenClass = new CompiledOpenClass(NullOpenClass.the, messages, new SyntaxNodeException[0],
                    new SyntaxNodeException[0]);
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

    public Tracer traceElement(TestSuite testSuite) {
        Tracer t = new Tracer();
        Tracer.setTracer(t);

        ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(compiledOpenClass.getClassLoader());
            try {
                runTest(testSuite);
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
        return getTableEditorModel(table);
    }

    public TableEditorModel getTableEditorModel(IOpenLTable table) {
        String tableView = studio.getTableView();
        TableEditorModel tableModel = new TableEditorModel(table, tableView, false);
        return tableModel;
    }

    public boolean isProjectCompiledSuccessfully() {
        return compiledOpenClass != null && compiledOpenClass.getOpenClassWithErrors() != null
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
        for (WorkbookSyntaxNode workbookSyntaxNode : getWorkbookNodes()) {
            XlsWorkbookSourceCodeModule module = workbookSyntaxNode.getWorkbookSourceCodeModule();
            if (module.getSourceFile().getName().equals(
                    FilenameUtils.getName(studio.getCurrentModule().getRulesRootPath().getPath()))) {
                return module;
            }
        }
        return null;
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
        removeListeners();

        if (compiledOpenClass != null) {
            releaseClassLoader(compiledOpenClass.getClassLoader());
        }
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

    /**
     * Release classLoader that will not be used anymore TODO Must be moved to a
     * separate class.
     * 
     * @param classLoader class loader
     */
    private void releaseClassLoader(ClassLoader classLoader) {
        if (classLoader != null) {
            JavaOpenClass.resetClassloader(classLoader);
            String2DataConvertorFactory.unregisterClassLoader(classLoader);
        }
    }

    private IDependencyManager getDependencyManager() {
        IDependencyManager dependencyManager = dependencyManagerFactory.getDependencyManager(moduleInfo, true);
        return modulesCache.wrapToCollectDependencies(dependencyManager, moduleInfo);
    }

    public IOpenMethod getCurrentDispatcherMethod(IOpenMethod method, String uri) {
        TableSyntaxNode tsn = getNode(uri);
        return getMethodFromDispatcher((OpenMethodDispatcher) method, tsn);
    }
}
