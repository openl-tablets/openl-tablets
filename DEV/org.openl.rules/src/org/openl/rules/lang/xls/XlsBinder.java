/*
 * Created on Oct 2, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls;

import java.util.*;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.binding.*;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.BindingContext;
import org.openl.binding.impl.BoundCode;
import org.openl.binding.impl.ErrorBoundNode;
import org.openl.binding.impl.module.ModuleNode;
import org.openl.conf.IUserContext;
import org.openl.conf.OpenLBuilderImpl;
import org.openl.conf.OpenLConfigurationException;
import org.openl.dependency.CompiledDependency;
import org.openl.engine.OpenLManager;
import org.openl.engine.OpenLSystemProperties;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.binding.RecursiveOpenMethodPreBinder;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetNodeBinder;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.cmatch.ColumnMatchNodeBinder;
import org.openl.rules.constants.ConstantsTableBinder;
import org.openl.rules.data.DataBase;
import org.openl.rules.data.DataNodeBinder;
import org.openl.rules.data.IDataBase;
import org.openl.rules.datatype.binding.DatatypeNodeBinder;
import org.openl.rules.datatype.binding.DatatypesSorter;
import org.openl.rules.dt.ActionsTableBinder;
import org.openl.rules.dt.ConditionsTableBinder;
import org.openl.rules.dt.ReturnsTableBinder;
import org.openl.rules.fuzzy.OpenLFuzzyUtils;
import org.openl.rules.lang.xls.binding.AExecutableNodeBinder;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.OpenlSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeHelper;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.method.table.MethodTableNodeBinder;
import org.openl.rules.property.PropertyTableBinder;
import org.openl.rules.table.properties.PropertiesLoader;
import org.openl.rules.tbasic.AlgorithmNodeBinder;
import org.openl.rules.testmethod.TestMethodNodeBinder;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ASelector;
import org.openl.util.ASelector.StringValueSelector;
import org.openl.util.ISelector;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements {@link IOpenBinder} abstraction for Excel files.
 *
 * @author snshor
 */
public class XlsBinder implements IOpenBinder {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private final Logger log = LoggerFactory.getLogger(XlsBinder.class);

    private static class BinderFactoryHolder {
        private static final Map<String, AXlsTableBinder> INSTANCE;

        private static final String[][] BINDERS = {
                { XlsNodeTypes.XLS_DATA.toString(), DataNodeBinder.class.getName() },
                { XlsNodeTypes.XLS_DATATYPE.toString(), DatatypeNodeBinder.class.getName() },
                { XlsNodeTypes.XLS_DT.toString(), org.openl.rules.dt.DecisionTableNodeBinder.class.getName() },
                { XlsNodeTypes.XLS_SPREADSHEET.toString(), SpreadsheetNodeBinder.class.getName() },
                { XlsNodeTypes.XLS_METHOD.toString(), MethodTableNodeBinder.class.getName() },
                { XlsNodeTypes.XLS_TEST_METHOD.toString(), TestMethodNodeBinder.class.getName() },
                { XlsNodeTypes.XLS_RUN_METHOD.toString(), TestMethodNodeBinder.class.getName() },
                { XlsNodeTypes.XLS_TBASIC.toString(), AlgorithmNodeBinder.class.getName() },
                { XlsNodeTypes.XLS_COLUMN_MATCH.toString(), ColumnMatchNodeBinder.class.getName() },
                { XlsNodeTypes.XLS_PROPERTIES.toString(), PropertyTableBinder.class.getName() },
                { XlsNodeTypes.XLS_CONDITIONS.toString(), ConditionsTableBinder.class.getName() },
                { XlsNodeTypes.XLS_ACTIONS.toString(), ActionsTableBinder.class.getName() },
                { XlsNodeTypes.XLS_RETURNS.toString(), ReturnsTableBinder.class.getName() },
                { XlsNodeTypes.XLS_CONSTANTS.toString(), ConstantsTableBinder.class.getName() } };

        static {
            Map<String, AXlsTableBinder> binderFactory = new HashMap<>();
            for (String[] binder : BINDERS) {
                try {
                    binderFactory.put(binder[0], (AXlsTableBinder) Class.forName(binder[1]).newInstance());
                } catch (Exception ex) {
                    throw RuntimeExceptionWrapper.wrap(ex);
                }
            }
            INSTANCE = Collections.unmodifiableMap(binderFactory);
        }
    }

    public Map<String, AXlsTableBinder> getBinderFactories() {
        return BinderFactoryHolder.INSTANCE;
    }

    private IUserContext userContext;

    public XlsBinder(IUserContext userContext) {
        this.userContext = userContext;
    }

    @Override
    public ICastFactory getCastFactory() {
        return null;
    }

    @Override
    public INameSpacedMethodFactory getMethodFactory() {
        return null;
    }

    @Override
    public INodeBinderFactory getNodeBinderFactory() {
        return null;
    }

    @Override
    public INameSpacedTypeFactory getTypeFactory() {
        return null;
    }

    @Override
    public INameSpacedVarFactory getVarFactory() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.IOpenBinder#makeBindingContext()
     */
    @Override
    public IBindingContext makeBindingContext() {
        return new BindingContext(null, JavaOpenClass.VOID, null);
    }

    @Override
    public IBoundCode bind(IParsedCode parsedCode) {
        return bind(parsedCode, null);
    }

    @Override
    public IBoundCode bind(IParsedCode parsedCode, IBindingContext bindingContext) {

        XlsModuleSyntaxNode moduleNode = (XlsModuleSyntaxNode) parsedCode.getTopNode();

        OpenL openl = null;
        List<SyntaxNodeException> exceptions = new ArrayList<>();
        try {
            openl = makeOpenL(moduleNode, exceptions);
        } catch (OpenLConfigurationException ex) {
            OpenlSyntaxNode syntaxNode = moduleNode.getOpenlNode();
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError("Error Creating OpenL", ex, syntaxNode);

            ErrorBoundNode boundNode = new ErrorBoundNode(syntaxNode);

            return new BoundCode(parsedCode, boundNode, new SyntaxNodeException[] { error }, null);
        }

        if (bindingContext == null) {
            IOpenBinder openlBinder = openl.getBinder();
            bindingContext = openlBinder.makeBindingContext();
        } else {
            if (bindingContext instanceof BindingContext) {
                BindingContext bc = (BindingContext) bindingContext;
                if (bc.getOpenL() == null || bc.getBinder() == null) { // Workaround
                    bc.setOpenl(openl);
                    bc.setBinder(openl.getBinder());
                }
            }
        }
        // add collected exceptions
        exceptions.forEach(bindingContext::addError);

        if (parsedCode.getExternalParams() != null) {
            bindingContext.setExternalParams(parsedCode.getExternalParams());
        }

        IBoundNode topNode;

        Set<CompiledDependency> compiledDependencies = parsedCode.getCompiledDependencies();
        compiledDependencies = compiledDependencies.isEmpty() ? null : compiledDependencies; // !!! empty to null
        XlsModuleOpenClass moduleOpenClass = createModuleOpenClass(moduleNode,
            openl,
            getModuleDatabase(),
            compiledDependencies,
            bindingContext);
        RulesModuleBindingContext rulesModuleBindingContext = moduleOpenClass.getRulesModuleBindingContext();

        if (compiledDependencies != null) {
            /*
             * Bind module with processing dependent modules, previously compiled.<br> Creates {@link
             * XlsModuleOpenClass} with dependencies and<br> populates {@link RulesModuleBindingContext} for current
             * module with types<br> from dependent modules.
             */
            try {
                for (IOpenClass type : moduleOpenClass.getTypes()) {
                    rulesModuleBindingContext.addType(ISyntaxConstants.THIS_NAMESPACE, type);
                }
            } catch (Exception ex) {
                SyntaxNodeException error = SyntaxNodeExceptionUtils
                    .createError("Cannot add datatype from dependency", ex, moduleNode);
                bindingContext.addError(error);
            }
        }

        topNode = processBinding(moduleNode, openl, rulesModuleBindingContext, moduleOpenClass, bindingContext);

        return new BoundCode(parsedCode, topNode, bindingContext.getErrors(), bindingContext.getMessages());
    }

    protected IDataBase getModuleDatabase() {
        return new DataBase();
    }

    /**
     * Common binding cycle.
     *
     * @param moduleNode
     * @param openl
     * @param rulesModuleBindingContext
     * @param moduleOpenClass
     * @param bindingContext
     * @return
     */
    private IBoundNode processBinding(XlsModuleSyntaxNode moduleNode,
            OpenL openl,
            RulesModuleBindingContext rulesModuleBindingContext,
            XlsModuleOpenClass moduleOpenClass,
            IBindingContext bindingContext) {
        try {
            //
            // Selectors
            //
            ASelector<ISyntaxNode> propertiesSelector = getSelector(XlsNodeTypes.XLS_PROPERTIES);
            ASelector<ISyntaxNode> constantsSelector = getSelector(XlsNodeTypes.XLS_CONSTANTS);
            ASelector<ISyntaxNode> dataTypeSelector = getSelector(XlsNodeTypes.XLS_DATATYPE);
            ASelector<ISyntaxNode> conditionsSelector = getSelector(XlsNodeTypes.XLS_CONDITIONS);
            ASelector<ISyntaxNode> actionsSelector = getSelector(XlsNodeTypes.XLS_ACTIONS);
            ASelector<ISyntaxNode> returnsSelector = getSelector(XlsNodeTypes.XLS_RETURNS);

            ISelector<ISyntaxNode> dtDefinitionSelector = conditionsSelector.or(returnsSelector).or(actionsSelector);

            ISelector<ISyntaxNode> notPropertiesAndNotDatatypeAndNotConstantsSelector = propertiesSelector.not()
                .and(dataTypeSelector.not())
                .and(constantsSelector.not());

            ISelector<ISyntaxNode> spreadsheetSelector = getSelector(XlsNodeTypes.XLS_SPREADSHEET);
            ISelector<ISyntaxNode> dtSelector = getSelector(XlsNodeTypes.XLS_DT);
            ISelector<ISyntaxNode> testMethodSelector = getSelector(XlsNodeTypes.XLS_TEST_METHOD);
            ISelector<ISyntaxNode> runMethodSelector = getSelector(XlsNodeTypes.XLS_RUN_METHOD);

            ISelector<ISyntaxNode> commonTablesSelector = notPropertiesAndNotDatatypeAndNotConstantsSelector
                .and(spreadsheetSelector.not()
                    .and(testMethodSelector.not()
                        .and(runMethodSelector.not().and(dtSelector.not().and(dtDefinitionSelector.not())))));

            // Bind property node at first.
            //
            TableSyntaxNode[] propertiesNodes = selectNodes(moduleNode, propertiesSelector);
            bindInternal(moduleNode, moduleOpenClass, propertiesNodes, openl, rulesModuleBindingContext);

            bindPropertiesForAllTables(moduleNode, moduleOpenClass, openl, rulesModuleBindingContext);

            IBoundNode topNode = null;

            // Bind constants
            TableSyntaxNode[] constantNodes = selectNodes(moduleNode, constantsSelector);
            bindInternal(moduleNode, moduleOpenClass, constantNodes, openl, rulesModuleBindingContext);

            // Bind datatype nodes.
            TableSyntaxNode[] datatypeNodes = selectNodes(moduleNode, dataTypeSelector);

            /*
             * Processes datatype table nodes before the bind operation. Checks type declarations and finds invalid
             * using of inheritance feature at this step.
             */
            TableSyntaxNode[] processedDatatypeNodes = DatatypesSorter.sort(datatypeNodes, rulesModuleBindingContext); // Rewrite
            // this
            // sorter
            // with
            // TableSyntaxNodeRelationsUtils

            bindInternal(moduleNode, moduleOpenClass, processedDatatypeNodes, openl, rulesModuleBindingContext);

            // Conditions && Returns && Actions
            TableSyntaxNode[] dtHeaderDefinitionsNodes = selectNodes(moduleNode, dtDefinitionSelector);

            // Select nodes excluding Properties, Datatype, Spreadsheet, Test,
            // RunMethod tables
            TableSyntaxNode[] commonTables = selectNodes(moduleNode, commonTablesSelector);

            // Select and sort Spreadsheet tables
            TableSyntaxNode[] spreadsheets = selectTableSyntaxNodes(moduleNode, spreadsheetSelector);
            if (OpenLSystemProperties.isCustomSpreadsheetType(bindingContext.getExternalParams())) {
                try {
                    spreadsheets = TableSyntaxNodeRelationsUtils.sort(spreadsheets,
                        new SpreadsheetTableSyntaxNodeRelationsDeterminer());
                } catch (TableSyntaxNodeCircularDependencyException e) {
                    for (TableSyntaxNode tsn : e.getTableSyntaxNodes()) {
                        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(e, tsn);
                        processError(error, tsn, rulesModuleBindingContext);
                    }
                }
            }

            TableSyntaxNode[] dts = selectTableSyntaxNodes(moduleNode, dtSelector);

            TableSyntaxNode[] commonAndSpreadsheetTables = ArrayUtils.addAll(
                ArrayUtils.addAll(ArrayUtils.addAll(dtHeaderDefinitionsNodes, dts), spreadsheets),
                commonTables);
            bindInternal(moduleNode, moduleOpenClass, commonAndSpreadsheetTables, openl, rulesModuleBindingContext);

            // Select Test and RunMethod tables
            TableSyntaxNode[] runTables = selectNodes(moduleNode, runMethodSelector);
            bindInternal(moduleNode, moduleOpenClass, runTables, openl, rulesModuleBindingContext);

            TableSyntaxNode[] testTables = selectNodes(moduleNode, testMethodSelector);
            topNode = bindInternal(moduleNode, moduleOpenClass, testTables, openl, rulesModuleBindingContext);

            if (moduleOpenClass.isUseDescisionTableDispatcher()) {
                DispatcherTablesBuilder dispTableBuilder = new DispatcherTablesBuilder(
                    (XlsModuleOpenClass) topNode.getType(),
                    rulesModuleBindingContext);
                dispTableBuilder.build();
            }

            ((XlsModuleOpenClass) topNode.getType()).completeOpenClassBuilding();

            processErrors(moduleOpenClass.getErrors(), bindingContext);

            return topNode;
        } finally {
            OpenLFuzzyUtils.clearCaches();
        }
    }

    private StringValueSelector<ISyntaxNode> getSelector(XlsNodeTypes selectorValue) {
        return getSelector(selectorValue.toString());
    }

    private StringValueSelector<ISyntaxNode> getSelector(String selectorValue) {
        return new ASelector.StringValueSelector<>(selectorValue, new SyntaxNodeConvertor());
    }

    /**
     * Creates {@link XlsModuleOpenClass}
     *
     * @param moduleDependencies set of dependent modules for creating module.
     */
    protected XlsModuleOpenClass createModuleOpenClass(XlsModuleSyntaxNode moduleNode,
            OpenL openl,
            IDataBase dbase,
            Set<CompiledDependency> moduleDependencies,
            IBindingContext bindingContext) {

        return new XlsModuleOpenClass(XlsHelper.getModuleName(moduleNode),
            new XlsMetaInfo(moduleNode),
            openl,
            dbase,
            moduleDependencies,
            Thread.currentThread().getContextClassLoader(),
            bindingContext);
    }

    private void bindPropertiesForAllTables(XlsModuleSyntaxNode moduleNode,
            XlsModuleOpenClass module,
            OpenL openl,
            RulesModuleBindingContext bindingContext) {
        ASelector<ISyntaxNode> propertiesSelector = getSelector(XlsNodeTypes.XLS_PROPERTIES);
        ASelector<ISyntaxNode> otherNodesSelector = getSelector(XlsNodeTypes.XLS_OTHER);
        ISelector<ISyntaxNode> notPropertiesAndNotOtherSelector = propertiesSelector.not()
            .and(otherNodesSelector.not());

        TableSyntaxNode[] tableSyntaxNodes = selectNodes(moduleNode, notPropertiesAndNotOtherSelector);

        PropertiesLoader propLoader = new PropertiesLoader(openl, bindingContext, module);
        for (TableSyntaxNode tsn : tableSyntaxNodes) {
            try {
                propLoader.loadProperties(tsn);
            } catch (SyntaxNodeException error) {
                processError(error, tsn, bindingContext);
            } catch (CompositeSyntaxNodeException ex) {
                for (SyntaxNodeException error : ex.getErrors()) {
                    processError(error, tsn, bindingContext);
                }
            } catch (Exception | LinkageError t) {
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(t, tsn);
                processError(error, tsn, bindingContext);
            }
        }
    }

    private void addImports(XlsModuleSyntaxNode moduleNode,
            OpenLBuilderImpl builder,
            Collection<String> imports,
            List<SyntaxNodeException> exceptions) {
        Collection<String> packageNames = new LinkedHashSet<>();
        Collection<String> classNames = new LinkedHashSet<>();
        Collection<String> libraries = new LinkedHashSet<>();
        for (String singleImport : imports) {
            if (singleImport.endsWith(".*")) {
                String libraryClassName = singleImport.substring(0, singleImport.length() - 2);
                try {
                    userContext.getUserClassLoader().loadClass(libraryClassName); // try
                    // load
                    // class
                    libraries.add(libraryClassName);
                } catch (Exception e) {
                    packageNames.add(libraryClassName);
                } catch (LinkageError e) {
                    exceptions.add(SyntaxNodeExceptionUtils.createError(e, moduleNode.getOpenlNode()));
                }
            } else {
                try {
                    userContext.getUserClassLoader().loadClass(singleImport); // try
                    // load
                    // class
                    classNames.add(singleImport);
                } catch (Exception e) {
                    packageNames.add(singleImport);
                } catch (LinkageError e) {
                    exceptions.add(SyntaxNodeExceptionUtils.createError(e, moduleNode.getOpenlNode()));
                }
            }
        }
        builder.setPackageImports(packageNames.toArray(EMPTY_STRING_ARRAY));
        builder.setClassImports(classNames.toArray(EMPTY_STRING_ARRAY));
        builder.setLibraries(libraries.toArray(EMPTY_STRING_ARRAY));
    }

    private OpenL makeOpenL(XlsModuleSyntaxNode moduleNode, List<SyntaxNodeException> exceptions) {

        String openlName = getOpenLName(moduleNode.getOpenlNode());
        Collection<String> imports = moduleNode.getImports();

        if (imports == null) {
            return OpenL.getInstance(openlName, userContext);
        }

        OpenLBuilderImpl builder = new OpenLBuilderImpl();

        builder.setExtendsCategory(openlName);

        String category = openlName + "::" + moduleNode.getModule().getUri();
        builder.setCategory(category);

        addImports(moduleNode, builder, imports, exceptions);

        builder.setContexts(null, userContext);

        return OpenL.getInstance(category, userContext, builder);
    }

    private IMemberBoundNode preBindXlsNode(ISyntaxNode syntaxNode,
            OpenL openl,
            RulesModuleBindingContext bindingContext,
            XlsModuleOpenClass moduleOpenClass) throws Exception {

        String tableSyntaxNodeType = syntaxNode.getType();
        AXlsTableBinder binder = findBinder(tableSyntaxNodeType);

        if (binder == null) {
            log.debug("Unknown table type '{}'", tableSyntaxNodeType);
            return null;
        }

        TableSyntaxNode tableSyntaxNode = (TableSyntaxNode) syntaxNode;
        return binder.preBind(tableSyntaxNode, openl, bindingContext, moduleOpenClass);
    }

    protected AXlsTableBinder findBinder(String tableSyntaxNodeType) {
        return getBinderFactories().get(tableSyntaxNodeType);
    }

    protected String getDefaultOpenLName() {
        return OpenL.OPENL_JAVA_NAME;
    }

    private String getOpenLName(OpenlSyntaxNode osn) {
        return osn == null ? getDefaultOpenLName() : osn.getOpenlName();
    }

    private TableSyntaxNode[] selectNodes(XlsModuleSyntaxNode moduleSyntaxNode, ISelector<ISyntaxNode> childSelector) {
        return selectAndSortNodes(moduleSyntaxNode, childSelector, null);
    }

    private TableSyntaxNode[] selectAndSortNodes(XlsModuleSyntaxNode moduleSyntaxNode,
            ISelector<ISyntaxNode> childSelector,
            Comparator<TableSyntaxNode> nodesComparator) {

        ArrayList<TableSyntaxNode> childSyntaxNodes = new ArrayList<>();

        for (TableSyntaxNode tsn : moduleSyntaxNode.getXlsTableSyntaxNodes()) {

            if (childSelector == null || childSelector.select(tsn)) {
                childSyntaxNodes.add(tsn);
            }
        }

        TableSyntaxNode[] tableSyntaxNodes = childSyntaxNodes.toArray(new TableSyntaxNode[childSyntaxNodes.size()]);

        if (nodesComparator != null) {
            try {
                Arrays.sort(tableSyntaxNodes, nodesComparator);
            } catch (Exception e) {
                // ignore sort exceptions.
            }
        }
        return tableSyntaxNodes;
    }

    private TableSyntaxNode[] selectTableSyntaxNodes(XlsModuleSyntaxNode moduleSyntaxNode,
            ISelector<ISyntaxNode> childSelector) {

        ArrayList<TableSyntaxNode> childSyntaxNodes = new ArrayList<>();

        for (TableSyntaxNode tsn : moduleSyntaxNode.getXlsTableSyntaxNodes()) {
            if (childSelector == null || childSelector.select(tsn)) {
                childSyntaxNodes.add(tsn);
            }
        }

        return childSyntaxNodes.toArray(new TableSyntaxNode[childSyntaxNodes.size()]);
    }

    private boolean isExecutableTableSyntaxNode(TableSyntaxNode tableSyntaxNode) {
        return XlsNodeTypes.XLS_DT.equals(tableSyntaxNode.getNodeType()) || XlsNodeTypes.XLS_TBASIC
            .equals(tableSyntaxNode.getNodeType()) || XlsNodeTypes.XLS_METHOD
                .equals(tableSyntaxNode.getNodeType()) || XlsNodeTypes.XLS_COLUMN_MATCH.equals(tableSyntaxNode
                    .getNodeType()) || XlsNodeTypes.XLS_SPREADSHEET.equals(tableSyntaxNode.getNodeType());
    }

    private boolean isCustomSpreadsheetResultTableSyntaxNode(TableSyntaxNode tableSyntaxNode) {
        if (XlsNodeTypes.XLS_SPREADSHEET.equals(tableSyntaxNode.getNodeType())) {
            String returnTypeToken = TableSyntaxNodeHelper.getTableReturnType(tableSyntaxNode);
            if (returnTypeToken != null && (SpreadsheetResult.class.getSimpleName()
                .equals(returnTypeToken) || SpreadsheetResult.class.getName().equals(returnTypeToken) || returnTypeToken
                    .equals(TableSyntaxNodeHelper.getTableName(tableSyntaxNode)))) {
                return true;
            }
        }
        return false;
    }

    private Collection<CustomSpreadsheetResultOpenClass> registerCustomSpreadsheetResultTypes(
            TableSyntaxNode[] tableSyntaxNodes,
            RulesModuleBindingContext rulesModuleBindingContext) {
        if (OpenLSystemProperties.isCustomSpreadsheetType(rulesModuleBindingContext.getExternalParams())) {
            Collection<CustomSpreadsheetResultOpenClass> customSpreadsheetResultOpenClasses = new ArrayList<>();
            for (int i = 0; i < tableSyntaxNodes.length; i++) {
                if (isCustomSpreadsheetResultTableSyntaxNode(tableSyntaxNodes[i])) {
                    final String sprResTypeName = Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + TableSyntaxNodeHelper
                        .getTableName(tableSyntaxNodes[i]);
                    if (rulesModuleBindingContext.getModule().findType(sprResTypeName) == null) {
                        CustomSpreadsheetResultOpenClass csroc = new CustomSpreadsheetResultOpenClass(sprResTypeName,
                            rulesModuleBindingContext.getModule());
                        rulesModuleBindingContext.getModule().addType(csroc);
                        customSpreadsheetResultOpenClasses.add(csroc);
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    protected IBoundNode bindInternal(XlsModuleSyntaxNode moduleSyntaxNode,
            XlsModuleOpenClass module,
            TableSyntaxNode[] tableSyntaxNodes,
            OpenL openl,
            RulesModuleBindingContext rulesModuleBindingContext) {

        IMemberBoundNode[] children = new IMemberBoundNode[tableSyntaxNodes.length];
        OpenMethodHeader[] openMethodHeaders = new OpenMethodHeader[tableSyntaxNodes.length];

        registerCustomSpreadsheetResultTypes(tableSyntaxNodes, rulesModuleBindingContext);

        SyntaxNodeExceptionHolder syntaxNodeExceptionHolder = new SyntaxNodeExceptionHolder();
        try {
            rulesModuleBindingContext.setIgnoreCustomSpreadsheetResultCompilation(true);
            for (int i = 0; i < tableSyntaxNodes.length; i++) { // Add methods that should be compiled recursively
                if (isExecutableTableSyntaxNode(tableSyntaxNodes[i])) {
                    openMethodHeaders[i] = addMethodHeaderToContext(module,
                        tableSyntaxNodes[i],
                        openl,
                        rulesModuleBindingContext,
                        syntaxNodeExceptionHolder,
                        children,
                        i);
                }
            }
        } finally {
            rulesModuleBindingContext.setIgnoreCustomSpreadsheetResultCompilation(false);
        }

        for (int i = 0; i < tableSyntaxNodes.length; i++) {
            if (!isExecutableTableSyntaxNode(tableSyntaxNodes[i])) {
                IMemberBoundNode child = beginBind(tableSyntaxNodes[i], module, openl, rulesModuleBindingContext);
                children[i] = child;
                if (child != null) {
                    try {
                        child.addTo(module);
                    } catch (OpenlNotCheckedException e) {
                        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(e, tableSyntaxNodes[i]);
                        processError(error, tableSyntaxNodes[i], rulesModuleBindingContext);
                    }
                }
            }
        }

        for (int i = 0; i < children.length; i++) {
            if (isExecutableTableSyntaxNode(tableSyntaxNodes[i])) {
                rulesModuleBindingContext.preBindMethod(openMethodHeaders[i]);
            }
        }

        for (int i = 0; i < children.length; i++) {
            if (children[i] != null) {
                finilizeBind(children[i], tableSyntaxNodes[i], rulesModuleBindingContext);
            }
        }

        syntaxNodeExceptionHolder.processBindingContextErrors(rulesModuleBindingContext);

        if (rulesModuleBindingContext.isExecutionMode()) {
            removeDebugInformation(children, tableSyntaxNodes, rulesModuleBindingContext);
        }

        return new ModuleNode(moduleSyntaxNode, rulesModuleBindingContext.getModule());
    }

    private OpenMethodHeader addMethodHeaderToContext(XlsModuleOpenClass module,
            TableSyntaxNode tableSyntaxNode,
            OpenL openl,
            RulesModuleBindingContext rulesModuleBindingContext,
            SyntaxNodeExceptionHolder syntaxNodeExceptionHolder,
            IMemberBoundNode[] children,
            int index) {
        try {
            AExecutableNodeBinder aExecutableNodeBinder = (AExecutableNodeBinder) getBinderFactories()
                .get(tableSyntaxNode.getType());
            IOpenSourceCodeModule source = aExecutableNodeBinder.createHeaderSource(tableSyntaxNode,
                rulesModuleBindingContext);

            OpenMethodHeader openMethodHeader = (OpenMethodHeader) OpenLManager
                .makeMethodHeader(openl, source, rulesModuleBindingContext);
            XlsBinderExecutableMethodBind xlsBinderExecutableMethodBind = new XlsBinderExecutableMethodBind(module,
                openl,
                tableSyntaxNode,
                children,
                index,
                openMethodHeader,
                rulesModuleBindingContext,
                syntaxNodeExceptionHolder);
            rulesModuleBindingContext.addBinderMethod(openMethodHeader, xlsBinderExecutableMethodBind);
            return openMethodHeader;
        } catch (Exception | LinkageError e) {
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(e, tableSyntaxNode);
            processError(error, tableSyntaxNode, rulesModuleBindingContext);
        }
        return null;
    }

    protected void finilizeBind(IMemberBoundNode memberBoundNode,
            TableSyntaxNode tableSyntaxNode,
            RulesModuleBindingContext rulesModuleBindingContext) {

        try {
            memberBoundNode.finalizeBind(rulesModuleBindingContext);
        } catch (SyntaxNodeException error) {
            processError(error, tableSyntaxNode, rulesModuleBindingContext);
        } catch (CompositeSyntaxNodeException ex) {
            if (ex.getErrors() != null) {
                for (SyntaxNodeException error : ex.getErrors()) {
                    processError(error, tableSyntaxNode, rulesModuleBindingContext);
                }
            }
        } catch (Exception | LinkageError t) {
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(t, tableSyntaxNode);
            processError(error, tableSyntaxNode, rulesModuleBindingContext);
        }
    }

    protected void removeDebugInformation(IMemberBoundNode[] boundNodes,
            TableSyntaxNode[] tableSyntaxNodes,
            RulesModuleBindingContext ruleModuleBindingContext) {
        for (int i = 0; i < boundNodes.length; i++) {
            if (boundNodes[i] != null) {
                try {
                    boundNodes[i].removeDebugInformation(ruleModuleBindingContext);

                } catch (SyntaxNodeException error) {
                    processError(error, tableSyntaxNodes[i], ruleModuleBindingContext);

                } catch (CompositeSyntaxNodeException ex) {

                    for (SyntaxNodeException error : ex.getErrors()) {
                        processError(error, tableSyntaxNodes[i], ruleModuleBindingContext);
                    }

                } catch (Exception | LinkageError t) {
                    SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(t, tableSyntaxNodes[i]);
                    processError(error, tableSyntaxNodes[i], ruleModuleBindingContext);
                }
            }
        }
    }

    protected IMemberBoundNode beginBind(TableSyntaxNode tableSyntaxNode,
            XlsModuleOpenClass module,
            OpenL openl,
            RulesModuleBindingContext rulesModuleBindingContext) {
        try {
            return preBindXlsNode(tableSyntaxNode, openl, rulesModuleBindingContext, module);
        } catch (SyntaxNodeException error) {
            processError(error, tableSyntaxNode, rulesModuleBindingContext);
        } catch (CompositeSyntaxNodeException ex) {
            for (SyntaxNodeException error : ex.getErrors()) {
                processError(error, tableSyntaxNode, rulesModuleBindingContext);
            }
        } catch (Exception | LinkageError t) {
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(t, tableSyntaxNode);
            processError(error, tableSyntaxNode, rulesModuleBindingContext);
        }
        return null;
    }

    protected void processError(SyntaxNodeException error,
            TableSyntaxNode tableSyntaxNode,
            RulesModuleBindingContext rulesModuleBindingContext) {
        tableSyntaxNode.addError(error);
        rulesModuleBindingContext.addError(error);
    }

    protected void processErrors(List<Exception> errors, IBindingContext bindingContext) {
        if (errors != null) {
            for (Exception error : errors) {
                if (error instanceof SyntaxNodeException) {
                    bindingContext.addError((SyntaxNodeException) error);
                } else if (error instanceof CompositeSyntaxNodeException) {
                    BindHelper.processError((CompositeSyntaxNodeException) error, bindingContext);
                } else {
                    BindHelper.processError(error, null, bindingContext);
                }
            }
        }
    }

    class XlsBinderExecutableMethodBind implements RecursiveOpenMethodPreBinder {
        TableSyntaxNode tableSyntaxNode;
        RulesModuleBindingContext rulesModuleBindingContext;
        OpenL openl;
        XlsModuleOpenClass module;
        IMemberBoundNode[] childrens;
        int index;
        OpenMethodHeader openMethodHeader;
        boolean preBinding = false;
        SyntaxNodeExceptionHolder syntaxNodeExceptionHolder;
        boolean completed = false;

        public XlsBinderExecutableMethodBind(XlsModuleOpenClass module,
                OpenL openl,
                TableSyntaxNode tableSyntaxNode,
                IMemberBoundNode[] childrens,
                int index,
                OpenMethodHeader openMethodHeader,
                RulesModuleBindingContext rulesModuleBindingContext,
                SyntaxNodeExceptionHolder syntaxNodeExceptionHolder) {
            this.tableSyntaxNode = tableSyntaxNode;
            this.rulesModuleBindingContext = rulesModuleBindingContext;
            this.module = module;
            this.openl = openl;
            this.childrens = childrens;
            this.index = index;
            this.openMethodHeader = openMethodHeader;
            this.syntaxNodeExceptionHolder = syntaxNodeExceptionHolder;
        }

        @Override
        public TableSyntaxNode getTableSyntaxNode() {
            return tableSyntaxNode;
        }

        @Override
        public boolean isReturnsCustomSpreadsheetResult() {
            if (XlsNodeTypes.XLS_SPREADSHEET.equals(this.tableSyntaxNode.getNodeType()) && openMethodHeader.getType()
                .getInstanceClass() != null && SpreadsheetResult.class
                    .isAssignableFrom(openMethodHeader.getType().getInstanceClass())) {
                if (openMethodHeader.getType() instanceof CustomSpreadsheetResultOpenClass) {
                    return Objects.equals(openMethodHeader.getType().getName(),
                        Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + openMethodHeader.getName());
                }
                return true;
            }
            return false;
        }

        @Override
        public OpenMethodHeader getHeader() {
            return openMethodHeader;
        }

        @Override
        public String getDisplayName(int mode) {
            return openMethodHeader.getDisplayName(mode);
        }

        @Override
        public IOpenClass getType() {
            return openMethodHeader.getType();
        }

        @Override
        public IOpenMethod getMethod() {
            return this;
        }

        @Override
        public IMethodSignature getSignature() {
            return openMethodHeader.getSignature();
        }

        @Override
        public String getName() {
            return openMethodHeader.getName();
        }

        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IMemberMetaInfo getInfo() {
            return openMethodHeader.getInfo();
        }

        @Override
        public boolean isStatic() {
            return openMethodHeader.isStatic();
        }

        @Override
        public boolean isConstructor() {
            return false;
        }

        @Override
        public IOpenClass getDeclaringClass() {
            return module;
        }

        @Override
        public void startPreBind() {
            if (completed) {
                throw new IllegalStateException(String.format("Method '%s' is already pre-compiled.",
                    MethodUtil.printMethod(getHeader().getName(), getHeader().getSignature().getParameterTypes())));
            }
            preBinding = true;
        }

        @Override
        public void finishPreBind() {
            if (!completed && preBinding) {
                throw new IllegalStateException(String.format("Method '%s' is not pre-compiled.",
                    MethodUtil.printMethod(getHeader().getName(), getHeader().getSignature().getParameterTypes())));
            }
            if (!preBinding) {
                throw new IllegalStateException(String.format("Pre-compilation is not started for method '%s'.",
                    MethodUtil.printMethod(getHeader().getName(), getHeader().getSignature().getParameterTypes())));
            }
            preBinding = false;
        }

        @Override
        public void preBind() {
            try {
                if (!completed) {
                    if (!preBinding) {
                        throw new IllegalStateException(String.format("Pre-compilation is not started for method '%s'.",
                            MethodUtil.printMethod(getHeader().getName(),
                                getHeader().getSignature().getParameterTypes())));
                    }
                    try {
                        rulesModuleBindingContext.pushErrors();
                        IMemberBoundNode memberBoundNode = XlsBinder.this
                            .beginBind(tableSyntaxNode, module, openl, rulesModuleBindingContext);
                        childrens[index] = memberBoundNode;
                        if (memberBoundNode != null) {
                            try {
                                memberBoundNode.addTo(module);
                            } catch (Exception | LinkageError e) {
                                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(e, tableSyntaxNode);
                                processError(error, tableSyntaxNode, rulesModuleBindingContext);
                            }
                        }
                    } finally {
                        rulesModuleBindingContext.popErrors()
                            .stream()
                            .forEach(syntaxNodeExceptionHolder::addBindingContextError);
                    }
                }
            } finally {
                completed = true;
            }
        }

        @Override
        public boolean isPreBindStarted() {
            return preBinding;
        }

        @Override
        public boolean isCompleted() {
            return completed;
        }
    }

    private static class SyntaxNodeExceptionHolder {

        private List<SyntaxNodeException> syntaxNodeExceptions = new ArrayList<>();

        private void addBindingContextError(SyntaxNodeException e) {
            syntaxNodeExceptions.add(e);
        }

        private void processBindingContextErrors(IBindingContext bindingContext) {
            for (SyntaxNodeException e : syntaxNodeExceptions) {
                bindingContext.addError(e);
            }
            syntaxNodeExceptions.clear();
        }
    }
}
