package org.openl.rules.ui;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openl.base.INamedThing;
import org.openl.main.OpenLWrapper;
import org.openl.meta.IMetaHolder;
import org.openl.meta.StringValue;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.search.IOpenLSearch;
import org.openl.rules.search.ISearchTableRow;
import org.openl.rules.search.OpenLAdvancedSearchResult;
import org.openl.rules.search.OpenLAdvancedSearchResultViewer;
import org.openl.rules.search.OpenLBussinessSearchResult;
import org.openl.rules.search.OpenLSavedSearch;
import org.openl.rules.search.OpenLAdvancedSearchResult.TableAndRows;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.ITable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.Table;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.ui.FilteredGrid;
import org.openl.rules.table.ui.RegionGridSelector;
import org.openl.rules.table.ui.filters.ColorGridFilter;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.table.ui.filters.XlsSimpleFilter;
import org.openl.rules.table.xls.XlsSheetGridImporter;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.model.ui.TableViewer;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.testmethod.TestResult;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.AllTestsRunResult.Test;
import org.openl.rules.ui.search.TableSearch;
import org.openl.rules.ui.tree.OpenMethodsGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.ProjectTreeNode;
import org.openl.rules.ui.tree.TreeBuilder;
import org.openl.rules.ui.tree.TreeCache;
import org.openl.rules.ui.tree.TreeNodeBuilder;
import org.openl.rules.webstudio.web.jsf.WebContext;
import org.openl.rules.webtools.WebTool;
import org.openl.rules.webtools.XlsUrlParser;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
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
import org.openl.vm.Tracer;

public class ProjectModel {

    private OpenLWrapper wrapper;

    private OpenLWrapperInfo wrapperInfo;

    private ProjectIndexer indexer;

    private WebStudio studio;

    private ColorFilterHolder filterHolder = new ColorFilterHolder();

    private OpenLSavedSearch[] savedSearches;

    private boolean readOnly;

    private ProjectTreeNode projectRoot = null;

    private TreeCache<String, ITreeElement<?>> idTreeCache = new TreeCache<String, ITreeElement<?>>();

    private TreeCache<String, ProjectTreeNode> uriTreeCache = new TreeCache<String, ProjectTreeNode>();

    public ProjectModel(WebStudio studio) {
        this.studio = studio;
    }

    public static TableModel buildModel(IGridTable gt, IGridFilter[] filters) {
        IGrid htmlGrid = gt.getGrid();
        if (!(htmlGrid instanceof FilteredGrid)) {
            int N = 1;
            IGridFilter[] f1 = new IGridFilter[filters == null ? N : filters.length + N];
            f1[0] = new XlsSimpleFilter();
            // f1[1] = new SimpleHtmlFilter();
            for (int i = N; i < f1.length; i++) {
                f1[i] = filters[i - N];
            }

            htmlGrid = new FilteredGrid(gt.getGrid(), f1);

        }

        return new TableViewer(htmlGrid, gt.getRegion()).buildModel(gt);
    }

    public static boolean intersects(XlsUrlParser p1, String url2) {
        XlsUrlParser p2 = new XlsUrlParser();
        p2.parse(url2);

        if (!p1.wbPath.equals(p2.wbPath) || !p1.wbName.equals(p2.wbName) || !p1.wsName.equals(p2.wsName)) {
            return false;
        }

        return IGridRegion.Tool.intersects(XlsSheetGridModel.makeRegion(p1.range), XlsSheetGridModel
                .makeRegion(p2.range));
    }

    private static boolean intersectsByLocation(XlsUrlParser parser, String url) {
        XlsUrlParser p2 = new XlsUrlParser();
        p2.parse(url);

        return parser.wbPath.equals(p2.wbPath) && parser.wbName.equals(p2.wbName);
    }

    public static Object wrapperNewInstance(Class<?> c) throws Exception {
        Constructor<?> ctr;
        try {
            ctr = c.getConstructor(new Class[] { boolean.class });
            return ctr.newInstance(new Object[] { Boolean.TRUE });
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Using older version of OpenL Wrapper, please run Generate ... Wrapper");
        }
    }

    public BenchmarkInfo benchmarkElement(String elementUri, final String testName, String testID,
            final String testDescr, int ms) throws Exception {

        BenchmarkUnit bu = null;

        if (testName == null) {
            IOpenMethod m = getMethod(elementUri);
            return benchmarkMethod(m, ms);
        }
        final AllTestsRunResult atr = getRunMethods(elementUri);

        final int tid = Integer.parseInt(testID);

        final IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        final Object target = wrapper.getOpenClass().newInstance(env);

        bu = new BenchmarkUnit() {

            @Override
            public String getName() {
                return testDescr;
            }

            @Override
            public void run() throws Exception {
                throw new RuntimeException();
            }

            @Override
            public void runNtimes(int times) throws Exception {
                try {
                    atr.run(testName, tid, target, env, times);
                } catch (Throwable t) {
                    Log.error("Error during Method run: ", t);
                    throw RuntimeExceptionWrapper.wrap(t);
                }
            }

            @Override
            public String[] unitName() {
                return new String[] { testName + ":" + tid };
            }

        };

        BenchmarkUnit[] buu = { bu };
        return new Benchmark(buu).runUnit(bu, ms, false);

    }

    public BenchmarkInfo benchmarkMethod(final IOpenMethod m, int ms) throws Exception {
        final IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        final Object target = wrapper.getOpenClass().newInstance(env);

        ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(wrapper.getClass().getClassLoader());

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

    private Object convertTestResult(Object res) {
        if (res == null) {
            return null;
        }
        Class<?> clazz = res.getClass();
        if (!(clazz == TestResult.class)) {
            return res;
        }
        TestResult tr = (TestResult) res;
        Object[] ores = new Object[tr.getNumberOfTests()];

        for (int i = 0; i < ores.length; i++) {
            ores[i] = tr.getResult(i);
        }

        return ores;
    }

    public TableSyntaxNode findAnyTableNodeByLocation(XlsUrlParser p1) {        
        TableSyntaxNode[] nodes = getTableSyntaxNodes();

        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i].getType().equals(ITableNodeTypes.XLS_DT)
                    && intersectsByLocation(p1, nodes[i].getTable().getGridTable().getUri())) {
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
            if (intersects(p1, nodes[i].getTable().getGridTable().getUri())) {
                return nodes[i];
            }
        }

        return null;
    }

    public AllTestsRunResult getAllTestMethods() {
        if (wrapper == null) {
            return null;
        }

        IOpenClass oc = wrapper.getOpenClass();
        List<IOpenMethod> methods = new ArrayList<IOpenMethod>();
        List<String> names = new ArrayList<String>();

        for (Iterator<?> iter = oc.methods(); iter.hasNext();) {
            IOpenMethod m = (IOpenMethod) iter.next();
            IMemberMetaInfo mi = m.getInfo();
            ISyntaxNode node = null;
            if (mi == null || ((node = mi.getSyntaxNode()) == null)) {
                continue;
            }
            if (node instanceof TableSyntaxNode) {
                TableSyntaxNode tnode = (TableSyntaxNode) node;

                if (tnode.getType().equals(ITableNodeTypes.XLS_TEST_METHOD)) {
                    methods.add(m);
                    names.add(TableSyntaxNodeUtils.getTableDisplayValue(tnode)[1]);
                }
            }
        }

        return new AllTestsRunResult((IOpenMethod[]) methods.toArray(new IOpenMethod[0]), (String[]) names
                .toArray(new String[0]));
    }

    public List<TableSyntaxNode> getAllValidatedNodes() {
        if (wrapper == null) {
            return Collections.emptyList();
        }
        
        TableSyntaxNode[] nodes = getTableSyntaxNodes();

        List<TableSyntaxNode> list = new ArrayList<TableSyntaxNode>();

        for (int i = 0; i < nodes.length; i++) {
            TableSyntaxNode tsn = nodes[i];

            if (tsn.getType() == ITableNodeTypes.XLS_DT) {
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

    public String getDisplayName(String elementUri) {
        ProjectTreeNode pte = getTreeNodeByUri(elementUri);
        if (pte == null) {
            return "";
        }

        String displayName = pte.getDisplayName(INamedThing.REGULAR);

        if (displayName == null) {
            return "NO_NAME";
        }

        if (displayName.length() > 30) {
            return displayName.substring(0, 29);
        }
        return displayName;

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

    public String getDisplayNameFull(String elementUri) {
        ProjectTreeNode pte = getTreeNodeByUri(elementUri);
        if (pte == null) {
            return "";
        }

        String displayName = pte.getDisplayName(INamedThing.REGULAR);

        if (displayName == null) {
            return "NO_NAME";
        }

        return displayName;
    }

    public SyntaxNodeException[] getErrors(String elementUri) {

        TableSyntaxNode tsn = getNode(elementUri);
        SyntaxNodeException[] se = null;

        if (tsn != null) {
            se = tsn.getErrors();
        }

        return se == null ? new SyntaxNodeException[0] : se;
    }
    
    public boolean hasErrors(String elementUri) {
        
        SyntaxNodeException[] se = getErrors(elementUri);
        
        return se.length > 0;
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

    public IOpenMethod getMethod(TableSyntaxNode tsn) {

        IOpenClass openClass = wrapper.getOpenClass();

        for (Iterator<IOpenMethod> iter = openClass.methods(); iter.hasNext();) {

            IOpenMethod method = iter.next();
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

    public AllTestsRunResult getRunMethods(String elementUri) {
        IOpenMethod m = getMethod(elementUri);

        if (m == null) {
            return null;
        }

        IOpenMethod[] runners = ProjectHelper.runners(m);
        String[] names = new String[runners.length];

        for (int i = 0; i < runners.length; i++) {
            IMemberMetaInfo mi = runners[i].getInfo();
            TableSyntaxNode tnode = (TableSyntaxNode) mi.getSyntaxNode();

            names[i] = TableSyntaxNodeUtils.getTableDisplayValue(tnode)[1];
        }

        return new AllTestsRunResult(runners, names);

    }

    public OpenLSavedSearch[] getSavedSearches() {
        if (savedSearches == null && isReady()) {
            TableSyntaxNode[] nodes = getTableSyntaxNodes();

            List<OpenLSavedSearch> savedSearches = new ArrayList<OpenLSavedSearch>();

            for (TableSyntaxNode node : nodes) {
                if (node.getType().equals(ITableNodeTypes.XLS_PERSISTENT)) {
                    String code = node.getHeader().getModule().getCode();
                    if ((IXlsTableNames.PERSISTENCE_TABLE + " " + OpenLSavedSearch.class.getName()).equals(code)) {
                        OpenLSavedSearch savedSearch = new OpenLSavedSearch().restore(new XlsSheetGridImporter(
                                (XlsSheetGridModel) node.getTable().getGridTable().getGrid(), node));
                        savedSearches.add(savedSearch);
                    }
                }
            }

            this.savedSearches = savedSearches.toArray(new OpenLSavedSearch[savedSearches.size()]);
        }
        return savedSearches;
    }

    public List<TableSearch> getAdvancedSearchResults(Object searchResult) {
        List<TableSearch> searchResults = new ArrayList<TableSearch>();

        if (searchResult instanceof OpenLAdvancedSearchResult) {
            TableAndRows[] tr = ((OpenLAdvancedSearchResult) searchResult).getFoundTableAndRows();
            OpenLAdvancedSearchResultViewer searchViewer = new OpenLAdvancedSearchResultViewer();
            for (int i = 0; i < tr.length; i++) {
                ISearchTableRow[] rows = tr[i].getRows();
                if (rows.length > 0) {
                    TableSyntaxNode tsn = tr[i].getTsn();
                    StringValue tableName = TableSyntaxNodeUtils.getTableSyntaxNodeName(tsn);
                    String tableUri = tsn.getUri();

                    CompositeGrid cg = searchViewer.makeGrid(rows);
                    IGridTable gridTable = cg != null ? cg.asGridTable() : null;

                    Table newTable = new Table();
                    newTable.setGridTable(gridTable);
                    newTable.setProperties(tsn.getTableProperties());

                    TableSearch tableSearch = new TableSearch();
                    tableSearch.setTableUri(tableUri);
                    tableSearch.setTable(newTable);
                    tableSearch.setXlsLink((getXlsOrDocUrlLink(tableName)));

                    searchResults.add(tableSearch);
                }
            }
        }

        return searchResults;
    }

    public List<TableSearch> getBussinessSearchResults(Object searchResult) {
        List<TableSearch> searchResults = new ArrayList<TableSearch>();

        if (searchResult instanceof OpenLBussinessSearchResult) {
            List<TableSyntaxNode> foundTables = ((OpenLBussinessSearchResult) searchResult).getFoundTables();
            for(TableSyntaxNode foundTable : foundTables) {
                TableSearch tableSearch = new TableSearch();
                tableSearch.setTableUri(foundTable.getUri());
                tableSearch.setTable(new TableSyntaxNodeAdapter(foundTable));
                tableSearch.setXlsLink((getXlsOrDocUrlLink(
                        TableSyntaxNodeUtils.getTableSyntaxNodeName(foundTable))));
                searchResults.add(tableSearch);
            }
        }

        return searchResults;
    }

    // TODO Move to UI
    public String getXlsOrDocUrlLink(IMetaHolder mh) {
        String display = String.valueOf(mh);
        StringBuffer buf = new StringBuffer();
        buf.append("<a ");

        String url = WebTool.makeXlsOrDocUrl(mh.getMetaInfo().getSourceUrl());
        buf.append("href='" + WebContext.getContextPath() + "/jsp/showLinks.jsp?").append(url).append("'");
        buf.append(" target='show_app_hidden'");

        buf.append(">");
        StringTool.encodeHTMLBody(display, buf);
        buf.append("</a>");

        return buf.toString();
    }

    public WebStudio getStudio() {
        return studio;
    }

    public ITable getTable(String elementUri) {
        TableSyntaxNode tsn = getNode(elementUri);
        if (tsn != null) {
            return new TableSyntaxNodeAdapter(tsn);
        }
        return null;
    }

    public IGridTable getGridTable(String elementUri) {
        TableSyntaxNode tsn = getNode(elementUri);
        return tsn == null ? null : tsn.getTable().getGridTable();
    }

    public TableInfo getTableInfo(String uri) {
        if (uri == null) {
            return null;
        }
        TableSyntaxNode node = findNode(uri);
        if (node == null) {
            return null;
        }

        return new TableInfo(node.getTable().getGridTable(), node.getDisplayName(), false, uri);
    }

    public String getTableView(String view) {
        return view == null ? studio.getMode().getTableMode() : view;
    }

    public IGridTable getTableWithMode(String elementUri) {
        return getTableWithMode(elementUri, null);
    }

    public IGridTable getTableWithMode(String elementUri, String mode) {

        TableSyntaxNode tsn = getNode(elementUri);
        if (tsn == null) {
            return null;
        }

        IGridTable gt = tsn.getTable().getGridTable();
        String type = getTableView(mode);

        if (type != null) {
            ILogicalTable gtx = tsn.getSubTables().get(type);
            if (gtx != null) {
                gt = gtx.getGridTable();
            }
        }
        return gt;

    }

    public AllTestsRunResult getTestMethods(String elementUri) {
        IOpenMethod[] testers = null;

        IOpenMethod m = getMethod(elementUri);
        if (m != null) {
            testers = ProjectHelper.testers(m);
        } else {
            testers = ProjectHelper.allTesters(wrapper.getOpenClass());
        }

        String[] names = new String[testers.length];

        for (int i = 0; i < testers.length; i++) {
            IMemberMetaInfo mi = testers[i].getInfo();
            TableSyntaxNode tnode = (TableSyntaxNode) mi.getSyntaxNode();

            names[i] = TableSyntaxNodeUtils.getTableDisplayValue(tnode)[1];
        }

        return new AllTestsRunResult(testers, names);

    }

    public String getUri(String elementUri) {
        IGridTable table = getGridTable(elementUri);

        if (table == null) {
            return "file://NO_FILE";
        }

        return table.getUri();
    }

    private XlsWorkbookSourceCodeModule getWorkbookSourceCodeModule() {
        if (wrapper != null) {
            TableSyntaxNode[] nodes = ((XlsMetaInfo) wrapper.getOpenClass().getMetaInfo()).getXlsModuleNode()
                    .getXlsTableSyntaxNodes();
            for (TableSyntaxNode node : nodes) {
                if (node.getType().equals(ITableNodeTypes.XLS_DT)) {
                    return ((XlsSheetSourceCodeModule) node.getModule()).getWorkbookSource();
                }
            }
        }

        return null;
    }

    public OpenLWrapper getWrapper() {
        return wrapper;
    }

    /**
     * @return Returns the wrapperInfo.
     */
    public OpenLWrapperInfo getWrapperInfo() {
        return wrapperInfo;
    }

    public XlsModuleSyntaxNode getXlsModuleNode() {
        XlsMetaInfo xmi = (XlsMetaInfo) wrapper.getOpenClass().getMetaInfo();
        XlsModuleSyntaxNode xsn = xmi.getXlsModuleNode();
        return xsn;
    }

    /**
     * Returns if current project is read only.
     * 
     * @return <code>true</code> if project is read only.
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isReady() {
        return wrapper != null;
    }

    public boolean isRunnable(String elementUri) {
        IOpenMethod m = getMethod(elementUri);
        if (m == null) {
            return false;
        }

        return ProjectHelper.isRunnable(m);
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
        if (wrapper == null) {
            return;
        }

        ProjectTreeNode root = makeProjectTreeRoot();

        TableSyntaxNode[] tableSyntaxNodes = getTableSyntaxNodes();
        
        OverloadedMethodsDictionary methodNodesDictionary = makeMethodNodesDictionary(tableSyntaxNodes);

        TreeBuilder<Object> treeBuilder = new TreeBuilder<Object>();

        TreeNodeBuilder<Object>[] treeSorters = studio.getMode().getBuilders();

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

        HashSet<TableSyntaxNode> nodesWithErrors = new HashSet<TableSyntaxNode>();

        boolean treeEnlarged = false;

        for (int i = 0; i < tableSyntaxNodes.length; i++) {                
                if (studio.getMode().select(tableSyntaxNodes[i])) {

                    treeBuilder.addToNode(root, tableSyntaxNodes[i], treeSorters);
                    treeEnlarged = true;

                } else if (tableSyntaxNodes[i].getErrors() != null) {

                    treeBuilder.addToNode(root, tableSyntaxNodes[i], treeSorters);
                    nodesWithErrors.add(tableSyntaxNodes[i]);
                }
        }

        if (!treeEnlarged) {
            // No selection have been made (usually in a business mode)
            for (int i = 0; i < tableSyntaxNodes.length; i++) {                    
                    if (!ITableNodeTypes.XLS_OTHER.equals(tableSyntaxNodes[i].getType())
                            && !nodesWithErrors.contains(tableSyntaxNodes[i])) {
                        
                        treeBuilder.addToNode(root, tableSyntaxNodes[i], treeSorters);
                    }
            }
        }

        projectRoot = root;
        uriTreeCache.clear();
        idTreeCache.clear();
        cacheTree(projectRoot);
    }

    private TableSyntaxNode[] getTableSyntaxNodes() {
        XlsModuleSyntaxNode moduleSyntaxNode = getXlsModuleNode();
        TableSyntaxNode[] tableSyntaxNodes = moduleSyntaxNode.getXlsTableSyntaxNodes();
        return tableSyntaxNodes;
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

        String name = studio.getMode().getDisplayName(wrapperInfo);

        return new ProjectTreeNode(new String[] { name, name, name }, "root", null, null, 0, null);
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

    public String makeXlsUrl(String elementUri) {
        if (elementUri == null) {
            return "problem: elementUri: " + elementUri;
        }
        return WebTool.makeXlsOrDocUrl(elementUri);
    }

    public void redraw() throws Exception {
        projectRoot = null;
    }
    
    public void reset(ReloadType reloadType) throws Exception {
        if (wrapperInfo != null) {
            wrapperInfo.reset();
            setWrapperInfo(wrapperInfo, reloadType);
        } else if (wrapper != null) {
            wrapper.reload();
        }

        savedSearches = null;
        projectRoot = null;
    }

    public AllTestsRunResult runAllTests(String elementUri) {

        // AllTestsRunResult atr = getAllTestMethods();
        AllTestsRunResult atr = getTestMethods(elementUri);

        Test[] ttm = atr.getTests();

        TestResult[] ttr = new TestResult[ttm.length];
        for (int i = 0; i < ttr.length; i++) {
            ttr[i] = (TestResult) runMethod(ttm[i].method);
        }

        atr.setResults(ttr);
        return atr;
    }

    public Object runElement(String elementUri, String testName, String testID) {
        if (testName == null) {
            IOpenMethod m = getMethod(elementUri);
            return convertTestResult(runMethod(m));
        }

        AllTestsRunResult atr = getRunMethods(elementUri);

        int tid = Integer.parseInt(testID);

        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        Object target = wrapper.getOpenClass().newInstance(env);
        try {
            Object res = atr.run(testName, tid, target, env, 1);
            return res;
        } catch (Throwable t) {
            Log.error("Error during Method run: ", t);
            return t;
        }

    }

    public Object runMethod(IOpenMethod m) {
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        Object target = wrapper.getOpenClass().newInstance(env);

        ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(wrapper.getClass().getClassLoader());

            Object res = null;

            try {
                res = m.invoke(target, new Object[] {}, env);

            } catch (Throwable t) {
                Log.error("Run Error:", t);
                return t;
            }
            return res;
        } finally {
            Thread.currentThread().setContextClassLoader(currentContextClassLoader);
        }
    }

    public Object runSearch(IOpenLSearch searchBean) {
        XlsModuleSyntaxNode xsn = getXlsModuleNode();

        return searchBean.search(xsn);
    }

    public void saveSearch(OpenLSavedSearch search) throws Exception {
        XlsWorkbookSourceCodeModule module = getWorkbookSourceCodeModule();
        if (module != null) {
            IExporter iExporter = IWritableGrid.Tool.createExporter(module);
            iExporter.persist(search);
            module.save();
            reset(ReloadType.RELOAD);
        }
    }

    public void setProjectTree(ProjectTreeNode projectRoot) {
        this.projectRoot = projectRoot;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void setWrapper(OpenLWrapper wrapper) {
        this.wrapper = wrapper;
    }

    public void setWrapperInfo(OpenLWrapperInfo wrapperInfo) throws Exception {
        setWrapperInfo(wrapperInfo, ReloadType.NO);
    }

    public void setWrapperInfo(OpenLWrapperInfo wrapperInfo, ReloadType reloadType) throws Exception {
        if (this.wrapperInfo == wrapperInfo && reloadType == ReloadType.NO) {
            return;
        }

        this.wrapperInfo = wrapperInfo;
        indexer = new ProjectIndexer(wrapperInfo.getProjectInfo().projectHome());
        Class<?> c = null;
        ClassLoader cl = null;
        wrapper = null;
        projectRoot = null;
        savedSearches = null;

        try {

            cl = wrapperInfo.getProjectInfo().getClassLoader(this.getClass().getClassLoader(), reloadType == ReloadType.FORCED);

            c = cl.loadClass(wrapperInfo.getWrapperClassName());
        } catch (Throwable t) {
            Log.error("Error instantiating wrapper", t);
            return;
        }

        Field f = c.getField("__userHome");

        if (Modifier.isStatic(f.getModifiers())) {
            f.set(null, wrapperInfo.getProjectInfo().projectHome());
        } else {
            throw new RuntimeException("Field " + f.getName() + " is not static in " + c.getName());
        }

        ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(cl);
            try {
                wrapper = (OpenLWrapper) wrapperNewInstance(c);
                if (reloadType != ReloadType.NO) {
                    wrapper.reload();
                }

            } catch (Throwable t) {
                Log.error("Problem Loading OpenLWrapper", t);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(threadClassLoader);
        }

    }

    public String showProperty(String elementUri, String propertyName) {
        TableSyntaxNode tsn = getNode(elementUri);
        if (tsn == null) {
            return "";
        }

        ITableProperties tp = tsn.getTableProperties();
        if (tp == null) {
            return "";
        }

        String p = tp.getPropertyValueAsString(propertyName);

        return p == null ? "" : p;

    }

    public String showTableWithSelection(String url, String view) {
        TableSyntaxNode tsn = findNode(url);
        if (tsn == null) {
            return "NOT FOUND";
        }

        XlsUrlParser p1 = new XlsUrlParser();
        p1.parse(url);

        IGridRegion region = XlsSheetGridModel.makeRegion(p1.range);

        if (view == null) {
            view = IXlsTableNames.VIEW_BUSINESS;
        }
        ILogicalTable gtx = tsn.getSubTables().get(view);
        IGridTable gt = tsn.getTable().getGridTable();
        if (gtx != null) {
            gt = gtx.getGridTable();
        }

        TableModel tableModel = buildModel(gt, new IGridFilter[] { new ColorGridFilter(new RegionGridSelector(region,
                true), filterHolder.makeFilter()) });
        // FIXME: should formulas be displayed?
        return new HTMLRenderer.TableRenderer(tableModel).render(false);
    }

    public Tracer traceElement(String elementUri) {
        IOpenMethod m = getMethod(elementUri);
        return traceMethod(m);
    }

    public Tracer traceMethod(IOpenMethod m) {
        Tracer t = new Tracer();
        Tracer.setTracer(t);

        ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(wrapper.getClass().getClassLoader());
            try {
                IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
                Object target = wrapper.getOpenClass().newInstance(env);

                m.invoke(target, new Object[] {}, env);
            } finally {
                Tracer.setTracer(null);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(currentContextClassLoader);
        }
        return t;
    }

    public List<Object> validateAll() {
        return null;
    }

    public TableEditorModel getTableEditorModel(String tableUri) {
        ITable table = getTable(tableUri);
        String tableView = getTableView(null);
        TableEditorModel tableModel = new TableEditorModel(table, tableView, false);
        return tableModel;
    }
    
    public static String showTable(IGridTable gt, boolean showgrid) {
        return showTable(gt, (IGridFilter[]) null, showgrid);
    }

    public static String showTable(IGridTable gt, IGridFilter[] filters, boolean showgrid) {
        TableModel model = buildModel(gt, filters);
        return TableViewer.showTable(model, showgrid);
    }

}
