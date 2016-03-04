/*
 * Created on Oct 2, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.CompiledOpenClass;
import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundCode;
import org.openl.binding.IBoundNode;
import org.openl.binding.ICastFactory;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.INameSpacedMethodFactory;
import org.openl.binding.INameSpacedTypeFactory;
import org.openl.binding.INameSpacedVarFactory;
import org.openl.binding.INodeBinderFactory;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.BoundCode;
import org.openl.binding.impl.module.ModuleNode;
import org.openl.conf.IExecutable;
import org.openl.conf.IUserContext;
import org.openl.conf.OpenConfigurationException;
import org.openl.conf.OpenLBuilderImpl;
import org.openl.dependency.CompiledDependency;
import org.openl.engine.OpenLSystemProperties;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.meta.IVocabulary;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.calc.SpreadsheetNodeBinder;
import org.openl.rules.cmatch.ColumnMatchNodeBinder;
import org.openl.rules.data.DataBase;
import org.openl.rules.data.DataNodeBinder;
import org.openl.rules.data.IDataBase;
import org.openl.rules.datatype.binding.DatatypeNodeBinder;
import org.openl.rules.datatype.binding.DatatypesSorter;
import org.openl.rules.lang.xls.binding.AExecutableNodeBinder;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.OpenlSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.method.table.MethodTableNodeBinder;
import org.openl.rules.property.PropertyTableBinder;
import org.openl.rules.table.properties.PropertiesLoader;
import org.openl.rules.tbasic.AlgorithmNodeBinder;
import org.openl.rules.testmethod.TestMethodNodeBinder;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.util.ASelector;
import org.openl.util.ASelector.StringValueSelector;
import org.openl.util.ISelector;
import org.openl.util.RuntimeExceptionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements {@link IOpenBinder} abstraction for Excel files.
 *
 * @author snshor
 */
public class XlsBinder implements IOpenBinder {

    private final Logger log = LoggerFactory.getLogger(XlsBinder.class);
    private static Map<String, AXlsTableBinder> binderFactory;
    
    public static final String DEFAULT_OPENL_NAME = "org.openl.rules.java";

    private static final String[][] BINDERS = {{XlsNodeTypes.XLS_DATA.toString(), DataNodeBinder.class.getName()},
            {XlsNodeTypes.XLS_DATATYPE.toString(), DatatypeNodeBinder.class.getName()},
            {XlsNodeTypes.XLS_DT.toString(), org.openl.rules.dt.DecisionTableNodeBinder.class.getName()},
            {XlsNodeTypes.XLS_SPREADSHEET.toString(), SpreadsheetNodeBinder.class.getName()},
            {XlsNodeTypes.XLS_METHOD.toString(), MethodTableNodeBinder.class.getName()},
            {XlsNodeTypes.XLS_TEST_METHOD.toString(), TestMethodNodeBinder.class.getName()},
            {XlsNodeTypes.XLS_RUN_METHOD.toString(), TestMethodNodeBinder.class.getName()},
            {XlsNodeTypes.XLS_TBASIC.toString(), AlgorithmNodeBinder.class.getName()},
            {XlsNodeTypes.XLS_COLUMN_MATCH.toString(), ColumnMatchNodeBinder.class.getName()},
            {XlsNodeTypes.XLS_PROPERTIES.toString(), PropertyTableBinder.class.getName()},};

    public static synchronized Map<String, AXlsTableBinder> getBinderFactory() {

        if (binderFactory == null) {
            binderFactory = new HashMap<String, AXlsTableBinder>();

            for (int i = 0; i < BINDERS.length; i++) {

                try {
                    binderFactory.put(BINDERS[i][0], (AXlsTableBinder) Class.forName(BINDERS[i][1]).newInstance());
                } catch (Exception ex) {
                    throw RuntimeExceptionWrapper.wrap(ex);
                }
            }
        }

        return binderFactory;
    }

    private IUserContext userContext;

    public XlsBinder(IUserContext userContext) {
        this.userContext = userContext;
    }

    public ICastFactory getCastFactory() {
        return null;
    }

    public INameSpacedMethodFactory getMethodFactory() {
        return null;
    }

    public INodeBinderFactory getNodeBinderFactory() {
        return null;
    }

    public INameSpacedTypeFactory getTypeFactory() {
        return null;
    }

    public INameSpacedVarFactory getVarFactory() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.IOpenBinder#makeBindingContext()
     */
    public IBindingContext makeBindingContext() {
        throw new UnsupportedOperationException("XlsBinder is top level Binder");
    }

    public IBoundCode bind(IParsedCode parsedCode) {
        return bind(parsedCode, null);
    }

    public IBoundCode bind(IParsedCode parsedCode, IBindingContextDelegator bindingContextDelegator) {

        XlsModuleSyntaxNode moduleNode = (XlsModuleSyntaxNode) parsedCode.getTopNode();

        OpenL openl = null;

        try {
            openl = makeOpenL(moduleNode);
        } catch (OpenConfigurationException ex) {

            OpenlSyntaxNode syntaxNode = moduleNode.getOpenlNode();

            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError("Error Creating OpenL", ex, syntaxNode);
            BindHelper.processError(error);

            return BindHelper.makeInvalidCode(parsedCode, syntaxNode, new SyntaxNodeException[]{error});
        }

        IOpenBinder openlBinder = openl.getBinder();
        IBindingContext bindingContext = openlBinder.makeBindingContext();
        bindingContext = BindHelper.delegateContext(bindingContext, bindingContextDelegator);

        if (parsedCode.getExternalParams() != null) {
            bindingContext.setExternalParams(parsedCode.getExternalParams());
        }

        IBoundNode topNode = null;

        if (!parsedCode.getCompiledDependencies().isEmpty()) {
            topNode = bindWithDependencies(moduleNode, openl, bindingContext, parsedCode.getCompiledDependencies());
        } else {
            topNode = bind(moduleNode, openl, bindingContext);
        }

        return new BoundCode(parsedCode, topNode, bindingContext.getErrors(), 0);
    }


    /**
     * Bind module with processing dependent modules, previously compiled.<br>
     * Creates {@link XlsModuleOpenClass} with dependencies and<br>
     * populates {@link RulesModuleBindingContext} for current module with types<br>
     * from dependent modules.
     *
     * @param moduleNode
     * @param openl
     * @param bindingContext
     * @param moduleDependencies
     * @return
     */
    private IBoundNode bindWithDependencies(XlsModuleSyntaxNode moduleNode, OpenL openl, IBindingContext bindingContext,
                                            Set<CompiledDependency> moduleDependencies) {
        XlsModuleOpenClass moduleOpenClass = createModuleOpenClass(moduleNode, openl, getModuleDatabase(), moduleDependencies, bindingContext);

        RulesModuleBindingContext moduleContext = populateBindingContextWithDependencies(moduleNode,
                bindingContext, moduleDependencies, moduleOpenClass);
        return processBinding(moduleNode, openl, moduleContext, moduleOpenClass, bindingContext);
    }

    protected IDataBase getModuleDatabase() {
        return new DataBase();
    }

    /**
     * Creates {@link RulesModuleBindingContext} and populates it with types from dependent modules.
     *
     * @param moduleNode         just for processing error
     * @param bindingContext
     * @param moduleDependencies
     * @param moduleOpenClass
     * @return {@link RulesModuleBindingContext} created with bindingContext and moduleOpenClass.
     */
    private RulesModuleBindingContext populateBindingContextWithDependencies(XlsModuleSyntaxNode moduleNode,
                                                                             IBindingContext bindingContext, Set<CompiledDependency> moduleDependencies,
                                                                             XlsModuleOpenClass moduleOpenClass) {
        RulesModuleBindingContext moduleContext = createRulesBindingContext(bindingContext, moduleOpenClass);
        try {
            moduleContext.addTypes(filterDependencyTypes(moduleOpenClass.getTypes(), moduleContext.getInternalTypes()));
        } catch (Exception ex) {
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError("Can`t add datatype from dependency", ex, moduleNode);
            BindHelper.processError(error);
        }
        return moduleContext;
    }

    /**
     * Filter the datatypes from dependency, remove those that already presents in datatypes from context and are equal.
     * Handles the case when for example 'main' module includes 'dependency1'(with A, B datatypes) and 'dependency2' (with C datatype, and
     * 'dependency2' includes 'dependency1' itself). So to prevent adding datatypes A, B from 'dependency1' and 'dependency2' we handle this case.
     *
     * @param dependencyDatatypes datatypes from dependency module
     * @param contextTypes        datatypes already presented in the context
     * @return filtered dependency datatypes
     * @author DLiauchuk
     */
    private Map<String, IOpenClass> filterDependencyTypes(Map<String, IOpenClass> dependencyDatatypes, Map<String, IOpenClass> contextTypes) {
        Map<String, IOpenClass> filteredDependencyDatatypes = new HashMap<String, IOpenClass>();
        for (String key : dependencyDatatypes.keySet()) {
            IOpenClass dependencyDatatype = dependencyDatatypes.get(key);
            IOpenClass contextDatatype = contextTypes.get(key);
            if (!dependencyDatatype.equals(contextDatatype)) {
                filteredDependencyDatatypes.put(key, dependencyDatatype);
            }
        }
        return filteredDependencyDatatypes;
    }

    public IBoundNode bind(XlsModuleSyntaxNode moduleNode, OpenL openl, IBindingContext bindingContext) {

        XlsModuleOpenClass moduleOpenClass = createModuleOpenClass(moduleNode, openl, getModuleDatabase(), null, bindingContext);

        RulesModuleBindingContext moduleContext = createRulesBindingContext(bindingContext, moduleOpenClass);

        return processBinding(moduleNode, openl, moduleContext, moduleOpenClass, bindingContext);
    }

    /**
     * Common binding cycle.
     *
     * @param moduleNode
     * @param openl
     * @param moduleContext
     * @param moduleOpenClass
     * @param bindingContext
     * @return
     */
    private IBoundNode processBinding(XlsModuleSyntaxNode moduleNode,
            OpenL openl,
            RulesModuleBindingContext moduleContext,
            XlsModuleOpenClass moduleOpenClass, IBindingContext bindingContext) {

        IVocabulary vocabulary = makeVocabulary(moduleNode);

        if (vocabulary != null) {
            processVocabulary(vocabulary, moduleContext);
        }

        ASelector<ISyntaxNode> propertiesSelector = getSelector(XlsNodeTypes.XLS_PROPERTIES);

        // Bind property node at first.
        //
        TableSyntaxNode[] propertiesNodes = selectNodes(moduleNode, propertiesSelector);
        bindInternal(moduleNode, moduleOpenClass, propertiesNodes, openl, moduleContext);

        bindPropertiesForAllTables(moduleNode, moduleOpenClass, openl, moduleContext);

        // Bind datatype nodes.
        //
        ASelector<ISyntaxNode> dataTypeSelector = getSelector(XlsNodeTypes.XLS_DATATYPE);
        TableSyntaxNode[] datatypeNodes = selectNodes(moduleNode, dataTypeSelector);

        /*
         * Processes datatype table nodes before the bind operation. Checks type
         * declarations and finds invalid using of inheritance feature at this
         * step.
         */
        TableSyntaxNode[] processedDatatypeNodes = new DatatypesSorter().sort(datatypeNodes, moduleContext);

        bindInternal(moduleNode, moduleOpenClass, processedDatatypeNodes, openl, moduleContext);

        // Selector for other nodes
        //
        ISelector<ISyntaxNode> notPropertiesAndNotDatatypeSelector = propertiesSelector.not().and(dataTypeSelector.not());

        IBoundNode topNode = null;
        if (OpenLSystemProperties.isCustomSpreadsheetType(moduleContext.getExternalParams())) {
            topNode = bindInternalWithCustomSpreadsheetTypes(moduleNode, openl, moduleContext, moduleOpenClass,
                    notPropertiesAndNotDatatypeSelector);
        } else {
            TableSyntaxNode[] otherNodes = selectAndSortNodes(moduleNode, notPropertiesAndNotDatatypeSelector,
                    new TestAndMethodTableNodeComparator());
            topNode = bindInternal(moduleNode, moduleOpenClass, otherNodes, openl, moduleContext);
        }
        DispatcherTablesBuilder dispTableBuilder = new DispatcherTablesBuilder((XlsModuleOpenClass) topNode.getType(),
                moduleContext);
        dispTableBuilder.build();

        processErrors(moduleOpenClass.getErrors(), bindingContext);

        return topNode;
    }

    /**
     * Bind the tables in next order:
     * 1) Get tables, excluding Properties, Datatype, Spreadsheet, Test, RunMethod tables
     * 2) Get Spreadsheets, sort them (see {@link SpreadsheetNodeSorter}) and bind together with 1st point tables
     * 3) Bind Test and RunMethod tables
     *
     * @param moduleNode                          Syntax node for the whole module
     * @param openl                               OpenL object
     * @param moduleContext                       Context for the module
     * @param moduleOpenClass                     Open Class for the Module
     * @param notPropertiesAndNotDatatypeSelector selector excluding Properties tables and Datatype tables
     * @return binded top node
     */
    private IBoundNode bindInternalWithCustomSpreadsheetTypes(XlsModuleSyntaxNode moduleNode, OpenL openl,
                                                              RulesModuleBindingContext moduleContext, XlsModuleOpenClass moduleOpenClass,
                                                              ISelector<ISyntaxNode> notPropertiesAndNotDatatypeSelector) {
        ISelector<ISyntaxNode> spreadsheetSelector = getSelector(XlsNodeTypes.XLS_SPREADSHEET);
        ISelector<ISyntaxNode> testMethodSelector = getSelector(XlsNodeTypes.XLS_TEST_METHOD);
        ISelector<ISyntaxNode> runMethodSelector = getSelector(XlsNodeTypes.XLS_RUN_METHOD);

        ISelector<ISyntaxNode> commonTablesSelector = notPropertiesAndNotDatatypeSelector
                .and(spreadsheetSelector.not().and(testMethodSelector.not().and(runMethodSelector.not())));

        // Select nodes excluding Properties, Datatype, Spreadsheet, Test, RunMethod tables
        TableSyntaxNode[] commonTables = selectNodes(moduleNode, commonTablesSelector);

        // Select and sort Spreadsheet tables
        TableSyntaxNode[] spreadsheets = selectAndSortSpreadsheetNodes(moduleNode, spreadsheetSelector);

        TableSyntaxNode[] commonAndSpreadsheetTables = ArrayUtils.addAll(commonTables, spreadsheets);
        bindInternal(moduleNode, moduleOpenClass, commonAndSpreadsheetTables, openl, moduleContext);

        // Select Test and RunMethod tables
        TableSyntaxNode[] testsAndRunTables = selectNodes(moduleNode, testMethodSelector.or(runMethodSelector));
        return bindInternal(moduleNode, moduleOpenClass, testsAndRunTables, openl, moduleContext);
    }

    private StringValueSelector<ISyntaxNode> getSelector(XlsNodeTypes selectorValue) {
        return getSelector(selectorValue.toString());
    }

    private StringValueSelector<ISyntaxNode> getSelector(String selectorValue) {
        return new ASelector.StringValueSelector<ISyntaxNode>(selectorValue, new SyntaxNodeConvertor());
    }

    private RulesModuleBindingContext createRulesBindingContext(IBindingContext bindingContext,
                                                                XlsModuleOpenClass moduleOpenClass) {
        return new RulesModuleBindingContext(bindingContext, moduleOpenClass);
    }

    /**
     * Creates {@link XlsModuleOpenClass}
     *
     * @param moduleDependencies set of dependent modules for creating module.
     */
    protected XlsModuleOpenClass createModuleOpenClass(XlsModuleSyntaxNode moduleNode, OpenL openl,
                                                       IDataBase dbase,
                                                       Set<CompiledDependency> moduleDependencies, IBindingContext bindingContext) {

        return new XlsModuleOpenClass(XlsHelper.getModuleName(moduleNode), new XlsMetaInfo(moduleNode),
                openl, dbase, moduleDependencies, Thread.currentThread().getContextClassLoader(), OpenLSystemProperties.isDTDispatchingMode(bindingContext.getExternalParams()), OpenLSystemProperties.isDispatchingValidationEnabled(bindingContext.getExternalParams()));
    }

    private void bindPropertiesForAllTables(XlsModuleSyntaxNode moduleNode, XlsModuleOpenClass module, OpenL openl, RulesModuleBindingContext bindingContext) {
        ASelector<ISyntaxNode> propertiesSelector = getSelector(XlsNodeTypes.XLS_PROPERTIES);
        ASelector<ISyntaxNode> otherNodesSelector = getSelector(XlsNodeTypes.XLS_OTHER);
        ISelector<ISyntaxNode> notPropertiesAndNotOtherSelector = propertiesSelector.not().and(otherNodesSelector.not());

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
            } catch (Throwable t) {
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(t, tsn);
                processError(error, tsn, bindingContext);
            }
        }
    }

    private void processVocabulary(IVocabulary vocabulary,
                                   RulesModuleBindingContext moduleContext) {
        IOpenClass[] types = null;

        try {
            types = vocabulary.getVocabularyTypes();
        } catch (SyntaxNodeException error) {
            BindHelper.processError(error, moduleContext);
        }

        if (types != null) {

            for (int i = 0; i < types.length; i++) {

                try {
                    moduleContext.addType(ISyntaxConstants.THIS_NAMESPACE, types[i]);
                } catch (Throwable t) {
                    BindHelper.processError(null, t, moduleContext);
                }
            }
        }
    }

    private OpenL makeOpenL(XlsModuleSyntaxNode moduleNode) {

        String openlName = getOpenLName(moduleNode.getOpenlNode());
        Collection<String> allImports = moduleNode.getImports();

        if (allImports == null) {
            return OpenL.getInstance(openlName, userContext);
        }

        OpenLBuilderImpl builder = new OpenLBuilderImpl();

        builder.setExtendsCategory(openlName);

        String category = openlName + "::" + moduleNode.getModule().getUri(0);
        builder.setCategory(category);
        builder.setImports(allImports);
        builder.setContexts(null, userContext);
        builder.setInheritExtendedConfigurationLoader(true);

        return OpenL.getInstance(category, userContext, builder);
    }

    private IVocabulary makeVocabulary(XlsModuleSyntaxNode moduleNode) {

        final IdentifierNode vocabularyNode = moduleNode.getVocabularyNode();

        if (vocabularyNode == null) {
            return null;
        }

        final ClassLoader userClassLoader = userContext.getUserClassLoader();
        Thread.currentThread().setContextClassLoader(userClassLoader);

        IVocabulary vocabulary = (IVocabulary) userContext.execute(new IExecutable() {

            public Object execute() {

                String vocabularyClassName = vocabularyNode.getIdentifier();

                try {
                    Class<?> vClass = userClassLoader.loadClass(vocabularyClassName);

                    return vClass.newInstance();
                } catch (Throwable t) {
                    String message = String.format("Vocabulary type '%s' cannot be loaded", vocabularyClassName);
                    BindHelper.processError(message, vocabularyNode, t);

                    return null;
                }
            }
        });
        return vocabulary;
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
        return getBinderFactory().get(tableSyntaxNodeType);
    }

    protected String getDefaultOpenLName() {
        return DEFAULT_OPENL_NAME;
    }

    private String getOpenLName(OpenlSyntaxNode osn) {
        return osn == null ? getDefaultOpenLName() : osn.getOpenlName();
    }

    private TableSyntaxNode[] selectNodes(XlsModuleSyntaxNode moduleSyntaxNode,
                                          ISelector<ISyntaxNode> childSelector) {
        return selectAndSortNodes(moduleSyntaxNode, childSelector, null);
    }

    private TableSyntaxNode[] selectAndSortNodes(XlsModuleSyntaxNode moduleSyntaxNode,
                                                 ISelector<ISyntaxNode> childSelector,
                                                 Comparator<TableSyntaxNode> nodesComparator) {

        ArrayList<TableSyntaxNode> childSyntaxNodes = new ArrayList<TableSyntaxNode>();

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

    private TableSyntaxNode[] selectAndSortSpreadsheetNodes(XlsModuleSyntaxNode moduleSyntaxNode,
            ISelector<ISyntaxNode> childSelector) {

        ArrayList<TableSyntaxNode> childSyntaxNodes = new ArrayList<TableSyntaxNode>();

        for (TableSyntaxNode tsn : moduleSyntaxNode.getXlsTableSyntaxNodes()) {

            if (childSelector == null || childSelector.select(tsn)) {
                childSyntaxNodes.add(tsn);
            }
        }

        TableSyntaxNode[] tableSyntaxNodes = childSyntaxNodes.toArray(new TableSyntaxNode[childSyntaxNodes.size()]);

        return SpreadsheetNodeSorter.sort(tableSyntaxNodes);
    }

    private boolean isExecutableTableSyntaxNode(TableSyntaxNode tableSyntaxNode){
        return tableSyntaxNode.getNodeType() == XlsNodeTypes.XLS_DT ||
                tableSyntaxNode.getNodeType() == XlsNodeTypes.XLS_TBASIC ||
                tableSyntaxNode.getNodeType() == XlsNodeTypes.XLS_METHOD ||
                tableSyntaxNode.getNodeType() == XlsNodeTypes.XLS_COLUMN_MATCH ||
                tableSyntaxNode.getNodeType() == XlsNodeTypes.XLS_SPREADSHEET;
    }
    
    protected IBoundNode bindInternal(XlsModuleSyntaxNode moduleSyntaxNode,
                                      XlsModuleOpenClass module,
                                      TableSyntaxNode[] tableSyntaxNodes,
                                      OpenL openl,
                                      RulesModuleBindingContext moduleContext) {

        IMemberBoundNode[] children = new IMemberBoundNode[tableSyntaxNodes.length]; 

        for (int i = 0; i < tableSyntaxNodes.length; i++) {
            if (isExecutableTableSyntaxNode(tableSyntaxNodes[i])){
                AExecutableNodeBinder aExecutableNodeBinder = (AExecutableNodeBinder) getBinderFactory().get(tableSyntaxNodes[i].getType());
                try{
                    OpenMethodHeader openMethodHeader = aExecutableNodeBinder.createHeader(tableSyntaxNodes[i], openl, moduleContext);
                    if (moduleContext.isExecutionMode()){
                        moduleContext.addPrebindMethod(new PrebindOpenMethod(openMethodHeader, null));
                    }else{
                        moduleContext.addPrebindMethod(new PrebindOpenMethod(openMethodHeader, tableSyntaxNodes[i]));
                    }
                }catch(Exception e){
                    //skip
                }
            }else{
                IMemberBoundNode child = beginBind(tableSyntaxNodes[i], module, openl, moduleContext);
                children[i] = child;
                if (child != null) {
                    try {
                        child.addTo(module);
                    } catch (OpenlNotCheckedException e) {
                        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(e, tableSyntaxNodes[i]);
                        processError(error, tableSyntaxNodes[i], moduleContext);
                    }
                }
            }
        }
        
        for (int i = 0; i < children.length; i++) {
            if (isExecutableTableSyntaxNode(tableSyntaxNodes[i])) {
                IMemberBoundNode child = beginBind(tableSyntaxNodes[i], module, openl, moduleContext);
                children[i] = child;
                if (child != null) {
                    try {
                        child.addTo(module);
                    } catch (OpenlNotCheckedException e) {
                        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(e, tableSyntaxNodes[i]);
                        processError(error, tableSyntaxNodes[i], moduleContext);
                    }
                }
            }
        }

        for (int i = 0; i < children.length; i++) {
            if (children[i] != null) {
                finilizeBind(children[i], tableSyntaxNodes[i], moduleContext);
            }
        }

        if (moduleContext.isExecutionMode()) {
            removeDebugInformation(children, tableSyntaxNodes, moduleContext);
        }
        
        moduleContext.clearPrebindMethods();

        return new ModuleNode(moduleSyntaxNode, moduleContext.getModule());
    }

    protected void finilizeBind(IMemberBoundNode memberBoundNode,
                                TableSyntaxNode tableSyntaxNode,
                                RulesModuleBindingContext moduleContext) {

        try {
            memberBoundNode.finalizeBind(moduleContext);

        } catch (SyntaxNodeException error) {
            processError(error, tableSyntaxNode, moduleContext);

        } catch (CompositeSyntaxNodeException ex) {
            if (ex.getErrors() != null) {
                for (SyntaxNodeException error : ex.getErrors()) {
                    processError(error, tableSyntaxNode, moduleContext);
                }
            }
        } catch (Throwable t) {

            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(t, tableSyntaxNode);
            processError(error, tableSyntaxNode, moduleContext);
        }
    }

    protected void removeDebugInformation(IMemberBoundNode[] boundNodes, TableSyntaxNode[] tableSyntaxNodes,
                                          RulesModuleBindingContext moduleContext) {
        for (int i = 0; i < boundNodes.length; i++) {
            if (boundNodes[i] != null) {
                try {
                    boundNodes[i].removeDebugInformation(moduleContext);

                } catch (SyntaxNodeException error) {
                    processError(error, tableSyntaxNodes[i], moduleContext);

                } catch (CompositeSyntaxNodeException ex) {

                    for (SyntaxNodeException error : ex.getErrors()) {
                        processError(error, tableSyntaxNodes[i], moduleContext);
                    }

                } catch (Throwable t) {

                    SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(t, tableSyntaxNodes[i]);
                    processError(error, tableSyntaxNodes[i], moduleContext);
                }
            }
        }
    }

    protected IMemberBoundNode beginBind(TableSyntaxNode tableSyntaxNode,
                                         XlsModuleOpenClass module,
                                         OpenL openl,
                                         RulesModuleBindingContext moduleContext) {

        try {
            return preBindXlsNode(tableSyntaxNode, openl, moduleContext, module);

        } catch (SyntaxNodeException error) {
            processError(error, tableSyntaxNode, moduleContext);

            return null;

        } catch (CompositeSyntaxNodeException ex) {

            for (SyntaxNodeException error : ex.getErrors()) {
                processError(error, tableSyntaxNode, moduleContext);
            }

            return null;

        } catch (Throwable t) {

            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(t, tableSyntaxNode);
            processError(error, tableSyntaxNode, moduleContext);

            return null;
        }
    }

    protected void processError(SyntaxNodeException error,
                                TableSyntaxNode tableSyntaxNode,
                                RulesModuleBindingContext moduleContext) {

        tableSyntaxNode.addError(error);
        BindHelper.processError(error, moduleContext);
    }

    protected void processErrors(List<Throwable> errors, IBindingContext bindingContext) {
        if (errors != null) {
            for (Throwable error : errors) {
                if (error instanceof SyntaxNodeException) {
                    BindHelper.processError((SyntaxNodeException) error, bindingContext);
                } else if (error instanceof CompositeSyntaxNodeException) {
                    BindHelper.processError((CompositeSyntaxNodeException) error, bindingContext);
                } else {
                    BindHelper.processError(error, null, bindingContext);
                }
            }
        }
    }
}
