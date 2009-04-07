package org.openl.rules.ui;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.openl.CompiledOpenClass;
import org.openl.base.INamedThing;
import org.openl.main.OpenLWrapper;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.binding.TableProperties;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.search.OpenLAdvancedSearch;
import org.openl.rules.search.OpenLSavedSearch;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.ui.ColorGridFilter;
import org.openl.rules.table.ui.FilteredGrid;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.table.ui.RegionGridSelector;
import org.openl.rules.table.xls.SimpleXlsFormatter;
import org.openl.rules.table.xls.XlsSheetGridImporter;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.testmethod.TestResult;
import org.openl.rules.ui.AllTestsRunResult.Test;
import org.openl.rules.ui.search.TableSearch;
import org.openl.rules.validator.dt.DTValidationResult;
import org.openl.rules.validator.dt.DTValidator;
import org.openl.rules.webtools.WebTool;
import org.openl.rules.webtools.XlsUrlParser;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.tableeditor.model.ui.TableViewer;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.SyntaxErrorException;
import org.openl.syntax.impl.SyntaxError;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.IBenchmarkableMethod;
import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.TreeSorter;
import org.openl.util.benchmark.Benchmark;
import org.openl.util.benchmark.BenchmarkInfo;
import org.openl.util.benchmark.BenchmarkUnit;
import org.openl.util.export.IExporter;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;
import org.openl.vm.Tracer;

public class ProjectModel implements IProjectTypes {

    OpenLWrapper wrapper;

    OpenLWrapperInfo wrapperInfo;

    Throwable projectProblem;

    ProjectIndexer indexer;

    WebStudio studio;

    ColorFilterHolder filterHolder = new ColorFilterHolder();

    private OpenLSavedSearch[] savedSearches;

    private boolean readOnly;

    ProjectTreeElement projectRoot = null;

    ProjectTreeRenderer ptr;

    List<Throwable> validationExceptions;

    public static TableModel buildModel(IGridTable gt, IGridFilter[] filters) {
        IGrid htmlGrid = gt.getGrid();
        if (!(htmlGrid instanceof FilteredGrid)) {
            int N = 1;
            IGridFilter[] f1 = new IGridFilter[filters == null ? N : filters.length + N];
            f1[0] = new SimpleXlsFormatter();
            // f1[1] = new SimpleHtmlFilter();
            for (int i = N; i < f1.length; i++) {
                f1[i] = filters[i - N];
            }

            htmlGrid = new FilteredGrid(gt.getGrid(), f1);

        }

        return new TableViewer(htmlGrid, gt.getRegion()).buildModel(gt);
    }

    static public boolean intersects(XlsUrlParser p1, String url2) {
        XlsUrlParser p2 = new XlsUrlParser();
        p2.parse(url2);

        if (!p1.wbPath.equals(p2.wbPath) || !p1.wbName.equals(p2.wbName) || !p1.wsName.equals(p2.wsName)) {
            return false;
        }

        return IGridRegion.Tool.intersects(XlsSheetGridModel.makeRegion(p1.range), XlsSheetGridModel
                .makeRegion(p2.range));
    }

    static private boolean intersectsByLocation(XlsUrlParser parser, String url) {
        XlsUrlParser p2 = new XlsUrlParser();
        p2.parse(url);

        return parser.wbPath.equals(p2.wbPath) && parser.wbName.equals(p2.wbName);
    }

    public static String showTable(IGridTable gt, boolean showgrid) {
        return showTable(gt, (IGridFilter[]) null, showgrid);
    }

    public static String showTable(IGridTable gt, IGridFilter filter, boolean showgrid) {
        return showTable(gt, new IGridFilter[] { filter }, showgrid);
    }

    public static String showTable(IGridTable gt, IGridFilter[] filters, boolean showgrid) {
        TableModel model = buildModel(gt, filters);
        return TableViewer.showTable(model, showgrid);
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

    public ProjectModel(WebStudio studio) {
        this.studio = studio;
    }

    private void addError(Throwable se, ProjectTreeElement errorFolder, int i) {
        String uri = null;
        /*
         * if (se instanceof SyntaxError) { uri = ((SyntaxError) se).getUri(); }
         */
        String name = se.getMessage();
        String[] names = { name, name, name };
        errorFolder.getElements().put(new ATableTreeSorter.Key(i, names),
                new ProjectTreeElement(names, PT_PROBLEM, uri, se, 0, null));
    }

    private void addErrors(CompiledOpenClass comp, ProjectTreeElement root) {
        String[] errName = { "All Errors", "All Errors", "All Errors" };

        ProjectTreeElement errorFolder = new ProjectTreeElement(errName, "folder", null, null, 0, null);
        root.getElements().put(new ATableTreeSorter.Key(0, errName), errorFolder);

        int pn = comp.getParsingErrors().length;
        for (int i = 0; i < pn; i++) {
            addError((SyntaxError) comp.getParsingErrors()[i], errorFolder, i);
        }

        for (int i = 0; i < comp.getBindingErrors().length; i++) {
            addError((SyntaxError) comp.getBindingErrors()[i], errorFolder, i + pn);
        }
        pn += comp.getBindingErrors().length;
        int k = pn;
        if (validationExceptions != null) {
            for (int i = 0; i < validationExceptions.size(); ++i) {
                Throwable t = validationExceptions.get(i);

                if (t instanceof SyntaxErrorException) {
                    SyntaxErrorException se = (SyntaxErrorException) t;
                    ISyntaxError[] errors = se.getSyntaxErrors();

                    for (int j = 0; j < errors.length; j++) {
                        addError((SyntaxError) errors[j], errorFolder, ++k);
                    }

                } else {
                    addError(t, errorFolder, ++k);
                }

            }
        }
    }

    // public ProjectModel(OpenLWrapper wrapper) {
    // this.wrapper = wrapper;
    // }

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

    /**
     * @param res
     * @return
     */
    private Object convertResult(Object res) {
        if (res == null) {
            return null;
        }
        Class clazz = res.getClass();
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

    public String displayResult(Object res) {
        return new ObjectViewer(this).displayResult(res);
    }

    public TableSyntaxNode findAnyTableNodeByLocation(XlsUrlParser p1) {
        XlsModuleSyntaxNode xsn = getXlsModuleNode();
        TableSyntaxNode[] nodes = xsn.getXlsTableSyntaxNodes();

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

        return findNode(parsedUrl);
    }

    public TableSyntaxNode findNode(XlsUrlParser p1) {
        XlsModuleSyntaxNode xsn = getXlsModuleNode();
        TableSyntaxNode[] nodes = xsn.getXlsTableSyntaxNodes();

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
        Vector mm = new Vector();
        Vector names = new Vector();

        for (Iterator iter = oc.methods(); iter.hasNext();) {
            IOpenMethod m = (IOpenMethod) iter.next();
            IMemberMetaInfo mi = m.getInfo();
            ISyntaxNode node = null;
            if (mi == null || ((node = mi.getSyntaxNode()) == null)) {
                continue;
            }
            if (node instanceof TableSyntaxNode) {
                TableSyntaxNode tnode = (TableSyntaxNode) node;

                if (tnode.getType().equals(ITableNodeTypes.XLS_TEST_METHOD)) {
                    mm.add(m);
                    names.add(TableInstanceSorter.getTableDisplayValue(tnode)[1]);
                }
            }
        }

        return new AllTestsRunResult((IOpenMethod[]) mm.toArray(new IOpenMethod[0]), (String[]) names
                .toArray(new String[0]));
    }

    public List<TableSyntaxNode> getAllValidatedNodes() {
        if (wrapper == null) {
            return Collections.EMPTY_LIST;
        }
        XlsMetaInfo xmi = (XlsMetaInfo) wrapper.getOpenClass().getMetaInfo();

        XlsModuleSyntaxNode xsn = xmi.getXlsModuleNode();

        TableSyntaxNode[] nodes = xsn.getXlsTableSyntaxNodes();

        List<TableSyntaxNode> list = new ArrayList<TableSyntaxNode>();

        for (int i = 0; i < nodes.length; i++) {
            TableSyntaxNode tsn = nodes[i];

            if (tsn.getType() != ITableNodeTypes.XLS_DT) {
                continue;
            }
            if (tsn.getErrors() != null) {
                continue;
            }

            if (!"on".equals(tsn.getProperty("validate"))) {
                continue;
            }

            list.add(tsn);

        }

        return list;

    }

    public String getDisplayName(String elementUri) {
        ProjectTreeElement pte = ptr.getElement(elementUri);
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

    public String getDisplayNameFull(String elementUri) {
        ProjectTreeElement pte = ptr.getElement(elementUri);
        if (pte == null) {
            return "";
        }

        String displayName = pte.getDisplayName(INamedThing.REGULAR);

        if (displayName == null) {
            return "NO_NAME";
        }

        return displayName;
    }

    public ISyntaxError[] getErrors(String elementUri) {
        TableSyntaxNode tsn = getNode(elementUri);
        ISyntaxError[] se = tsn.getErrors();
        return se == null ? ISyntaxError.EMPTY : se;
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
        IOpenClass oc = wrapper.getOpenClass();

        for (Iterator<IOpenMethod> iter = oc.methods(); iter.hasNext();) {
            IOpenMethod m = iter.next();
            IMemberMetaInfo mi = m.getInfo();
            if (mi != null && mi.getSyntaxNode() == tsn) {
                return m;
            }
        }

        return null;
    }

    public TableSyntaxNode getNode(String elementUri) {
        TableSyntaxNode tsn = null;
        if (elementUri != null) {
            ProjectTreeElement pte = ptr.getElement(elementUri);
            if (pte != null) {
                tsn = (TableSyntaxNode) pte.getObject();
            }
            if (tsn == null) {
                tsn = findNode(elementUri);
            }
        }
        return tsn;
    }

    public synchronized ProjectTreeElement getProjectRoot() {
        if (projectRoot == null) {
            projectRoot = makeProjectTree();
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

            names[i] = TableInstanceSorter.getTableDisplayValue(tnode)[1];
        }

        return new AllTestsRunResult(runners, names);

    }

    public OpenLSavedSearch[] getSavedSearches() {
        if (savedSearches == null && isReady()) {
            XlsModuleSyntaxNode xsn = getXlsModuleNode();
            TableSyntaxNode[] nodes = xsn.getXlsTableSyntaxNodes();

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

    public List<TableSearch> getSearchList(Object obj) {
        return new ObjectViewer(this).getSearchList(obj);
    }

    public WebStudio getStudio() {
        return studio;
    }

    public IGridTable getTable(String elementUri) {
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

            names[i] = TableInstanceSorter.getTableDisplayValue(tnode)[1];
        }

        return new AllTestsRunResult(testers, names);

    }

    public String getUri(String elementUri) {
        IGridTable table = getTable(elementUri);

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

    public boolean hasErrors(String elementUri) {
        TableSyntaxNode tsn = getNode(elementUri);
        ISyntaxError[] se = tsn.getErrors();
        return se != null || tsn.getValidationResult() != null;

    }

    public int indexForNode(TableSyntaxNode tsn) {
        for (Object obj : ptr.map.getValues()) {
            ProjectTreeElement pte = (ProjectTreeElement) obj;
            if (pte.getObject() == tsn) {
                return ptr.map.getID(obj);
            }
        }
        return -1;
    }

    public int indexForNodeByURI(String uri) {
        for (Object obj : ptr.map.getValues()) {
            ProjectTreeElement pte = (ProjectTreeElement) obj;
            if (pte.getObject() instanceof TableSyntaxNode) {
                TableSyntaxNode tableSyntaxNode = (TableSyntaxNode) pte.getObject();
                if (uri.equals(tableSyntaxNode.getUri())) {
                    return ptr.map.getID(obj);
                }

            }
        }

        return -1;
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

    public ProjectTreeElement makeProjectTree() {
        if (wrapper == null) {
            return null;
        }

        // String name =
        // StringTool.lastToken(wrapper.getClass()
        // .getName(), ".");

        String name = studio.getMode().getDisplayName(wrapperInfo);

        ProjectTreeElement root = new ProjectTreeElement(new String[] { name, name, name }, "root", null, null, 0, null);

        XlsMetaInfo xmi = (XlsMetaInfo) wrapper.getOpenClass().getMetaInfo();

        CompiledOpenClass comp = wrapper.getCompiledOpenClass();

        if (comp.hasErrors() || validationExceptions != null && validationExceptions.size() > 0) {
            addErrors(comp, root);
        }

        XlsModuleSyntaxNode xsn = xmi.getXlsModuleNode();

        TableSyntaxNode[] nodes = xsn.getXlsTableSyntaxNodes();

        ATableTreeSorter[][] sorters = studio.getMode().getSorters();
        String[][] folders = studio.getMode().getFolders();

        for (int k = 0; k < sorters.length; k++) {
            ProjectTreeElement folder = root;

            if (folders != null && folders[k] != null) {
                folder = new ProjectTreeElement(folders[k], "folder", null, null, 0, null);
                root.getElements().put(new ATableTreeSorter.Key(k + 1, folder.getDisplayName()), folder);
            }
            ATableTreeSorter[] ts = sorters[k];

            HashSet<TableSyntaxNode> added = new HashSet<TableSyntaxNode>();
            int cnt = 0;
            for (int i = 0; i < nodes.length; i++) {
                if (studio.getMode().select(nodes[i])) {
                    TreeSorter.addElement(folder, nodes[i], ts, 0);
                    ++cnt;
                } else if (nodes[i].getErrors() != null) {
                    TreeSorter.addElement(folder, nodes[i], ts, 0);
                    added.add(nodes[i]);
                }
            }

            if (cnt == 0) // no selection have been made (usually in a
            // business mode)
            {
                for (int i = 0; i < nodes.length; i++) {
                    if (!nodes[i].getType().equals(ITableNodeTypes.XLS_OTHER) && !added.contains(nodes[i])) {
                        TreeSorter.addElement(folder, nodes[i], ts, 0);
                    }
                }
            }

        }

        return root;
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

    public String renderTree(String targetJsp) {

        ProjectTreeElement tr = getProjectRoot();

        if (tr == null) {
            String errMsg = "";
            if (projectProblem != null) {
                Throwable t = projectProblem;
                try {
                    t = ExceptionUtils.getCause(projectProblem);
                    if (t == null) {
                        t = projectProblem;
                    }
                } catch (Exception e) {
                }

                errMsg = new ObjectViewer(this).displayResult(t);
            }

            if (errMsg == "" && wrapper == null) {
                return "document.all['msg'].innerHTML = 'No OpenL Projects in the Workspace'";
            }

            return "document.all['msg'].innerHTML = 'There was a problem opening OpenL Project."
                    + " Try to run <i>Generate Wrapper</i> procedure in Eclipse for this project."
                    + " You will have to refresh the <i>Eclipse project</i> and <a href=\"/webstudio/index.jsp?reload=true\" target=\"_top\">refresh the Web Studio</a> afterwards."
                    + " Check Eclipse Console for more details. <p/>" + errMsg + "'";

        }

        // StringBuffer buf = new StringBuffer(1000);

        // renderElement(null, tr, targetJsp, buf);
        ptr = new ProjectTreeRenderer(this, targetJsp, "mainFrame");
        return ptr.renderRoot(tr);
        // return buf.toString();
    }

    public void reset() throws Exception {
        if (wrapperInfo != null) {
            wrapperInfo.reset();
            setWrapperInfo(wrapperInfo, true);
        } else if (wrapper != null) {
            wrapper.reload();
        }

        validationExceptions = null;
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
            return convertResult(runMethod(m));
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

    public Object runSearch(OpenLAdvancedSearch searchBean) {
        XlsModuleSyntaxNode xsn = getXlsModuleNode();

        return searchBean.search(xsn);
    }

    public void saveSearch(OpenLSavedSearch search) throws Exception {
        XlsWorkbookSourceCodeModule module = getWorkbookSourceCodeModule();
        if (module != null) {
            IExporter iExporter = IWritableGrid.Tool.createExporter(module);
            iExporter.persist(search);
            module.save();
            reset();
        }
    }

    public void setProjectRoot(ProjectTreeElement projectRoot) {
        this.projectRoot = projectRoot;
    }

    // /**
    // * @return Returns the folders.
    // */
    // public String[] getFolders() {
    // return folders;
    // }
    //
    // /**
    // * @param folders
    // * The folders to set.
    // */
    // public void setFolders(String[] folders) {
    // this.folders = folders;
    // }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void setWrapper(OpenLWrapper wrapper) {
        this.wrapper = wrapper;
    }

    // String errorMsg = null;

    public void setWrapperInfo(OpenLWrapperInfo wrapperInfo) throws Exception {
        setWrapperInfo(wrapperInfo, false);
    }

    public void setWrapperInfo(OpenLWrapperInfo wrapperInfo, boolean reload) throws Exception {
        if (this.wrapperInfo == wrapperInfo && !reload) {
            return;
        }

        this.wrapperInfo = wrapperInfo;
        indexer = new ProjectIndexer(wrapperInfo.getProjectInfo().projectHome());
        Class<?> c = null;
        ClassLoader cl = null;
        validationExceptions = null;
        wrapper = null;
        projectRoot = null;
        projectProblem = null;
        savedSearches = null;

        try {

            cl = wrapperInfo.getProjectInfo().getClassLoader(this.getClass().getClassLoader(), reload);

            c = cl.loadClass(wrapperInfo.getWrapperClassName());
        } catch (Throwable t) {
            Log.error("Error instantiating wrapper", t);
            projectProblem = t;
            return;
            // errorMsg = "Most probably ";
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
                if (reload) {
                    wrapper.reload();
                }

            } catch (Throwable t) {
                Log.error("Problem Loading OpenLWrapper", t);
                projectProblem = t;
            }
        } finally {
            Thread.currentThread().setContextClassLoader(threadClassLoader);
        }

    }

    public Object showError(int elementId) {
        ProjectTreeElement pte = ptr.getElement(elementId);
        if (pte == null) {
            return null;
        }

        Object error = pte.getProblem();

        return new ObjectViewer(this).displayResult(error);
    }

    public String showErrors(String elementUri) {
        TableSyntaxNode tsn = getNode(elementUri);
        ISyntaxError[] se = tsn.getErrors();
        if (se != null) {
            return new ObjectViewer(this).displayResult(se);
        }
        if (tsn.getValidationResult() != null) {
            return new ObjectViewer(this).displayResult(tsn.getValidationResult());
        }
        return "";
    }

    public String showProperty(String elementUri, String propertyName) {
        TableSyntaxNode tsn = getNode(elementUri);
        if (tsn == null) {
            return "";
        }

        TableProperties tp = tsn.getTableProperties();
        if (tp == null) {
            return "";
        }

        String p = tp.getPropertyValue(propertyName);

        return p == null ? "" : p;

    }

    public String showTable(String elementUri, String view) {
        TableSyntaxNode tsn = getNode(elementUri);
        if (tsn == null) {
            return "No Table have been selected yet";
        }

        IGridTable gt = tsn.getTable().getGridTable();
        if (view == null) {
            view = studio.getMode().getTableMode();
        }

        boolean showGrid = studio.getMode().showTableGrid();

        if (view != null) {
            ILogicalTable gtx = tsn.getSubTables().get(view);
            if (gtx != null) {
                gt = gtx.getGridTable();
            }
        }

        // return new TableViewer().showTable(gt, new
        // ICellFilter[]{cellFilter});
        return showTable(gt, showGrid);
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
        return new HTMLRenderer.TableRenderer(tableModel).renderWithMenu(null);
    }

    public Tracer traceElement(String elementUri) throws Exception {
        IOpenMethod m = getMethod(elementUri);
        return traceMethod(m);
    }

    public Tracer traceMethod(IOpenMethod m) throws Exception {
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

        List<Object> ve = new ArrayList<Object>();
        if (wrapper == null) {
            return ve;
        }

        List<TableSyntaxNode> nodes = getAllValidatedNodes();

        for (int i = 0; i < nodes.size(); i++) {
            validateNode(nodes.get(i), ve);
        }

        // validationExceptions = ve;
        return ve;
    }

    public void validateNode(TableSyntaxNode tsn, List<Object> ve) {

        DecisionTable dt = (DecisionTable) tsn.getMember();

        try {
            DTValidationResult dtr = DTValidator.validateDT(dt, null, wrapper.getOpenClass());

            if (dtr.hasProblems()) {
                tsn.setValidationResult(dtr);
                ve.add(dtr);
            } else {
                tsn.setValidationResult(null);
            }
        } catch (Throwable t) {
            tsn.setValidationResult(t);
            ve.add(t);
        }
    }
}
