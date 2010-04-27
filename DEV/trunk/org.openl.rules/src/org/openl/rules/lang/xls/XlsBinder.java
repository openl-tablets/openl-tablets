/*
 * Created on Oct 2, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.openl.meta.IVocabulary;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.calc.SpreadsheetNodeBinder;
import org.openl.rules.cmatch.ColumnMatchNodeBinder;
import org.openl.rules.data.DataNodeBinder;
import org.openl.rules.datatype.binding.DatatypeNodeBinder;
import org.openl.rules.dt.DecisionTableNodeBinder;
import org.openl.rules.extension.bind.IExtensionBinder;
import org.openl.rules.extension.bind.NameConventionBinderFactory;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.OpenlSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.method.MethodTableNodeBinder;
import org.openl.rules.property.PropertyTableBinder;
import org.openl.rules.table.properties.PropertiesLoader;
import org.openl.rules.tbasic.AlgorithmNodeBinder;
import org.openl.rules.testmethod.TestMethodNodeBinder;
import org.openl.rules.validation.properties.dimentional.DispatcherTableBuilder;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.util.ASelector;
import org.openl.util.ISelector;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * Implements {@link IOpenBinder} abstraction for Excel files.
 * 
 * @author snshor
 * 
 */
public class XlsBinder implements IOpenBinder {

    private static final Log LOG = LogFactory.getLog(XlsBinder.class);
    private static Map<String, AXlsTableBinder> binderFactory;

    private static final String[][] binders = {
            { ITableNodeTypes.XLS_DATA, DataNodeBinder.class.getName() },
            { ITableNodeTypes.XLS_DATATYPE, DatatypeNodeBinder.class.getName() },
            { ITableNodeTypes.XLS_DT, DecisionTableNodeBinder.class.getName() },
            { ITableNodeTypes.XLS_SPREADSHEET, SpreadsheetNodeBinder.class.getName() },
            { ITableNodeTypes.XLS_METHOD, MethodTableNodeBinder.class.getName() },
            { ITableNodeTypes.XLS_TEST_METHOD, TestMethodNodeBinder.class.getName() },
            { ITableNodeTypes.XLS_RUN_METHOD, TestMethodNodeBinder.class.getName() },
            { ITableNodeTypes.XLS_TBASIC, AlgorithmNodeBinder.class.getName() },
            { ITableNodeTypes.XLS_COLUMN_MATCH, ColumnMatchNodeBinder.class.getName() },
            { ITableNodeTypes.XLS_PROPERTIES, PropertyTableBinder.class.getName() }, };

    public static synchronized Map<String, AXlsTableBinder> getBinderFactory() {

        if (binderFactory == null) {
            binderFactory = new HashMap<String, AXlsTableBinder>();

            for (int i = 0; i < binders.length; i++) {

                try {
                    binderFactory.put(binders[i][0], (AXlsTableBinder) Class.forName(binders[i][1]).newInstance());
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

            return BindHelper.makeInvalidCode(parsedCode, syntaxNode, new SyntaxNodeException[] { error });
        }

        IOpenBinder openlBinder = openl.getBinder();
        IBindingContext bindingContext = openlBinder.makeBindingContext();
        bindingContext = BindHelper.delegateContext(bindingContext, bindingContextDelegator);
        
        IBoundNode topNode = bind(moduleNode, openl, bindingContext);        
        
        return new BoundCode(parsedCode, topNode, bindingContext.getErrors(), 0);
    }
    
    /*
     * (non-Javadoc)
     * @see org.openl.IOpenBinder#bind(org.openl.syntax.IParsedCode)
     */
    public IBoundNode bind(XlsModuleSyntaxNode moduleNode, OpenL openl, IBindingContext bindingContext) {

        XlsModuleOpenClass module = new XlsModuleOpenClass(null,
            XlsSourceUtils.getModuleName(moduleNode),
            new XlsMetaInfo(moduleNode),
            openl);

        processExtensions(module, moduleNode, moduleNode.getExtensionNodes());
                
        RulesModuleBindingContext moduleContext = new RulesModuleBindingContext(bindingContext, module);
        
        IVocabulary vocabulary = makeVocabulary(moduleNode);

        if (vocabulary != null) {
            processVocabulary(vocabulary, bindingContext, moduleContext);
        }

        ASelector<ISyntaxNode> dataTypeSelector = new ASelector.StringValueSelector<ISyntaxNode>(ITableNodeTypes.XLS_DATATYPE,
            new SyntaxNodeConvertor());

        ASelector<ISyntaxNode> propertiesSelector = new ASelector.StringValueSelector<ISyntaxNode>(ITableNodeTypes.XLS_PROPERTIES,
            new SyntaxNodeConvertor());

        bindInternal(moduleNode, openl, moduleContext, module, propertiesSelector, null);
        bindInternal(moduleNode, openl, moduleContext, module, dataTypeSelector, null);

        ISelector<ISyntaxNode> notPropertiesSelector = propertiesSelector.not();
        ISelector<ISyntaxNode> notDataTypeSelector = dataTypeSelector.not();
        ISelector<ISyntaxNode> notProp_And_NotDatatypeSelectors = notDataTypeSelector.and(notPropertiesSelector);
        
        IBoundNode topNode = bindInternal(moduleNode,
                openl,
                moduleContext,
                module,
                notProp_And_NotDatatypeSelectors,
                new TableSyntaxNodeComparator());
                
        DispatcherTableBuilder dispTableBuilder = new DispatcherTableBuilder(openl, (XlsModuleOpenClass)topNode.getType(), moduleContext);
        dispTableBuilder.buildDispatcherTables();
        
        return topNode;
    }

    private void processVocabulary(IVocabulary vocabulary,
                                   IBindingContext bindingContext,
                                   RulesModuleBindingContext moduleContext) {

        IOpenClass[] types = null;

        try {
            types = vocabulary.getVocabularyTypes();
        } catch (SyntaxNodeException error) {
            BindHelper.processError(error, bindingContext);
        }

        if (types != null) {

            for (int i = 0; i < types.length; i++) {

                try {
                    moduleContext.addType(ISyntaxConstants.THIS_NAMESPACE, types[i]);
                } catch (Throwable t) {
                    BindHelper.processError(null, t, bindingContext);
                }
            }
        }
    }

    private void processExtensions(XlsModuleOpenClass module,
                                   XlsModuleSyntaxNode moduleNode,
                                   List<IdentifierNode> extensionNodes) {

        for (int i = 0; i < extensionNodes.size(); i++) {

            IdentifierNode identifierNode = extensionNodes.get(i);
            IExtensionBinder binder = NameConventionBinderFactory.INSTANCE.getNodeBinder(identifierNode);

            if (binder != null && binder.getNodeType().equals(identifierNode.getType())) {
                binder.bind(module, moduleNode, identifierNode);
            }
        }
    }

    private OpenL makeOpenL(XlsModuleSyntaxNode moduleNode) {

        String openlName = getOpenLName(moduleNode.getOpenlNode());
        List<String> allImports = moduleNode.getAllImports();

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

                    return (IVocabulary) vClass.newInstance();
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

        String type = syntaxNode.getType();
        AXlsTableBinder binder = getBinderFactory().get(type);

        if (binder == null) {
            String message = String.format("Unknown table type '%s'", type);
            LOG.debug(message);

            return null;
        }

        TableSyntaxNode tableSyntaxNode = (TableSyntaxNode) syntaxNode;

        PropertiesLoader propLoader = new PropertiesLoader(openl, bindingContext, moduleOpenClass);
        propLoader.loadProperties(tableSyntaxNode);

        return binder.preBind(tableSyntaxNode, openl, bindingContext, moduleOpenClass);
    }

    private String getOpenLName(OpenlSyntaxNode osn) {
        return osn == null ? "org.openl.rules.java" : osn.getOpenlName();
    }

    private TableSyntaxNode[] getChildTableSyntaxNodes(XlsModuleSyntaxNode moduleSyntaxNode,
                                                       ISelector<ISyntaxNode> childSelector,
                                                       Comparator<TableSyntaxNode> tableComparator) {

        ArrayList<ISyntaxNode> childSyntaxNodes = new ArrayList<ISyntaxNode>();

        for (TableSyntaxNode tsn : moduleSyntaxNode.getXlsTableSyntaxNodes()) {

            if (childSelector == null || childSelector.select(tsn)) {
                childSyntaxNodes.add(tsn);
            }
        }

        TableSyntaxNode[] tableSyntaxNodes = childSyntaxNodes.toArray(new TableSyntaxNode[childSyntaxNodes.size()]);

        if (tableComparator != null) {
            Arrays.sort(tableSyntaxNodes, tableComparator);
        }

        return tableSyntaxNodes;
    }

    /*
     * (non-Javadoc)
     * @see org.openl.IOpenBinder#bind(org.openl.syntax.IParsedCode)
     */
    private IBoundNode bindInternal(XlsModuleSyntaxNode moduleSyntaxNode,
                                    OpenL openl,
                                    RulesModuleBindingContext moduleContext,
                                    XlsModuleOpenClass module,
                                    ISelector<ISyntaxNode> childSelector,
                                    Comparator<TableSyntaxNode> tableComparator) {

        TableSyntaxNode[] tableSyntaxNodes = getChildTableSyntaxNodes(moduleSyntaxNode, childSelector, tableComparator);
        IMemberBoundNode[] children = new IMemberBoundNode[tableSyntaxNodes.length];

        for (int i = 0; i < tableSyntaxNodes.length; i++) {

            IMemberBoundNode child = beginBind(tableSyntaxNodes[i], module, openl, moduleContext);
            children[i] = child;

            if (child != null) {
                child.addTo(module);
            }
        }

        for (int i = 0; i < children.length; i++) {
			if (children[i] != null) {
				finilizeBind(children[i], tableSyntaxNodes[i], moduleContext);
			}
		}

        return new ModuleNode(moduleSyntaxNode, moduleContext.getModule());
    }

    private void finilizeBind(IMemberBoundNode memberBoundNode,
                              TableSyntaxNode tableSyntaxNode,
                              RulesModuleBindingContext moduleContext) {

        try {
            memberBoundNode.finalizeBind(moduleContext);

        } catch (SyntaxNodeException error) {
            processError(error, tableSyntaxNode, moduleContext);

        } catch (CompositeSyntaxNodeException ex) {

            for (SyntaxNodeException error : ex.getErrors()) {
                processError( error, tableSyntaxNode, moduleContext);
            }

        } catch (Throwable t) {

            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(t, tableSyntaxNode);
            processError(error, tableSyntaxNode, moduleContext);
        }
    }

    private IMemberBoundNode beginBind(TableSyntaxNode tableSyntaxNode,
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
                processError( error, tableSyntaxNode, moduleContext);
            }

            return null;

        } catch (Throwable t) {

            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(t, tableSyntaxNode);
            processError(error, tableSyntaxNode, moduleContext);

            return null;
        }
    }

    private void processError(SyntaxNodeException error,
                              TableSyntaxNode tableSyntaxNode,
                              RulesModuleBindingContext moduleContext) {

        error.setTopLevelSyntaxNode(tableSyntaxNode);

        tableSyntaxNode.addError(error);
        BindHelper.processError(error, moduleContext);
    }

}
