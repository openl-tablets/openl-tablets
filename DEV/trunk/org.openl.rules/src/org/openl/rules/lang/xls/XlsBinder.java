/*
 * Created on Oct 2, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.IOpenBinder;
import org.openl.OpenConfigurationException;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundCode;
import org.openl.binding.IBoundError;
import org.openl.binding.IBoundNode;
import org.openl.binding.ICastFactory;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.INameSpacedMethodFactory;
import org.openl.binding.INameSpacedTypeFactory;
import org.openl.binding.INameSpacedVarFactory;
import org.openl.binding.INodeBinderFactory;
import org.openl.binding.impl.BoundCode;
import org.openl.binding.impl.BoundError;
import org.openl.binding.impl.TooManyErrorsError;
import org.openl.binding.impl.module.ModuleNode;
import org.openl.conf.IExecutable;
import org.openl.conf.IUserContext;
import org.openl.conf.OpenLBuilderImpl;
import org.openl.message.OpenLMessagesUtils;
import org.openl.meta.IVocabulary;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.calc.SSheetNodeBinder;
import org.openl.rules.cmatch.ColumnMatchNodeBinder;
import org.openl.rules.data.binding.DataNodeBinder;
import org.openl.rules.datatype.binding.DatatypeNodeBinder;
import org.openl.rules.dt.binding.DTNodeBinder;
import org.openl.rules.extension.bind.IExtensionBinder;
import org.openl.rules.extension.bind.NameConventionBinderFactory;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.OpenlSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.method.binding.MethodTableNodeBinder;
import org.openl.rules.property.binding.PropertyTableBinder;
import org.openl.rules.table.properties.PropertiesLoader;
import org.openl.rules.tbasic.AlgorithmNodeBinder;
import org.openl.rules.testmethod.binding.TestMethodNodeBinder;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.SyntaxErrorException;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.util.ASelector;
import org.openl.util.AStringConvertor;
import org.openl.util.ISelector;
import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;

/**
 * Implements {@link IOpenBinder} abstraction for Excel files.
 * 
 * @author snshor
 * 
 */
public class XlsBinder implements IOpenBinder {

    private static class SyntaxConvertor extends AStringConvertor<ISyntaxNode> {

        @Override
        public String getStringValue(ISyntaxNode test) {

            return test.getType();
        }
    }

    private static Comparator<TableSyntaxNode> tableComparator = new Comparator<TableSyntaxNode>() {

        public int compare(TableSyntaxNode ts1, TableSyntaxNode ts2) {

            String s1 = ts1.getType();
            String s2 = ts2.getType();

            int i1 = ITableNodeTypes.XLS_TEST_METHOD.equals(s1) || ITableNodeTypes.XLS_RUN_METHOD.equals(s1) ? 1 : 0;
            int i2 = ITableNodeTypes.XLS_TEST_METHOD.equals(s2) || ITableNodeTypes.XLS_RUN_METHOD.equals(s2) ? 1 : 0;

            return i1 - i2;
        }
    };

    private static Map<String, AXlsTableBinder> binderFactory;

    private static final String[][] binders = {
            { ITableNodeTypes.XLS_DATA, DataNodeBinder.class.getName() },
            { ITableNodeTypes.XLS_DATATYPE, DatatypeNodeBinder.class.getName() },
            { ITableNodeTypes.XLS_DT, DTNodeBinder.class.getName() },
            { ITableNodeTypes.XLS_SPREADSHEET, SSheetNodeBinder.class.getName() },
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

    public static String getModuleName(XlsModuleSyntaxNode node) {

        String uri = node.getModule().getUri(0);

        try {
            URL url = new URL(uri);
            String file = url.getFile();
            int index = file.lastIndexOf('/');

            file = index < 0 ? file : file.substring(index + 1);

            index = file.lastIndexOf('.');

            if (index > 0) {
                file = file.substring(0, index);
            }

            return StringTool.makeJavaIdentifier(file);

        } catch (MalformedURLException e) {

            Log.error("Error URI to name conversion", e);
            OpenLMessagesUtils.addError(e.getMessage());

            return "UndefinedXlsType";
        }
    }

    public XlsBinder(IUserContext ucxt) {

        this.userContext = ucxt;
    }

    public IBoundCode bind(IParsedCode parsedCode) {

        return bind(parsedCode, null);
    }

    public IBoundCode bind(IParsedCode parsedCode, IBindingContextDelegator delegator) {

        XlsModuleSyntaxNode moduleNode = (XlsModuleSyntaxNode) parsedCode.getTopNode();

        OpenL openl = null;

        try {
            openl = makeOpenL(moduleNode);
        } catch (OpenConfigurationException e) {

            BoundError error = new BoundError(moduleNode.getOpenlNode(), "Error Creating OpenL", e);
            OpenLMessagesUtils.addError(error.getMessage());

            IBoundError[] errors = new IBoundError[] { error };

            return new BoundCode(parsedCode, null, errors, 0);
        }

        IOpenBinder openlBinder = openl.getBinder();

        IBindingContext cxt = openlBinder.makeBindingContext();

        if (delegator != null) {
            delegator.setTopDelegate(cxt);
            cxt = delegator;
        }

        IBoundNode topNode = null;

        try {
            topNode = bind(moduleNode, openl, cxt);
        } catch (TooManyErrorsError err) {
            Log.error("Too many errors");

            for (ISyntaxError error : cxt.getError()) {
                OpenLMessagesUtils.addError(error.getMessage());
            }
        }

        return new BoundCode(parsedCode, topNode, cxt.getError(), 0);
    }

    /*
     * (non-Javadoc)
     * @see org.openl.IOpenBinder#bind(org.openl.syntax.IParsedCode)
     */
    public IBoundNode bind(XlsModuleSyntaxNode moduleNode, OpenL openl, IBindingContext cxt) {

        // TODO fix schema, name
        XlsModuleOpenClass module = new XlsModuleOpenClass(null,
            getModuleName(moduleNode),
            new XlsMetaInfo(moduleNode),
            openl);

        // int nchildren = moduleNode.getNumberOfChildren();

        proccessExtensions(module, moduleNode, moduleNode.getExtensionNodes());

        // IMemberBoundNode[] children = new IMemberBoundNode[nchildren];
        RulesModuleBindingContext moduleContext = new RulesModuleBindingContext(cxt, module);

        IVocabulary vocabulary = null;

        try {
            vocabulary = makeVocabulary(moduleNode);
        } catch (BoundError error) {
            OpenLMessagesUtils.addError(error.getMessage());
            cxt.addError(error);
        }

        if (vocabulary != null) {

            IOpenClass[] types = null;

            try {
                types = vocabulary.getVocabularyTypes();
            } catch (BoundError error) {
                OpenLMessagesUtils.addError(error.getMessage());
                cxt.addError(error);
            }

            if (types != null) {
                for (int i = 0; i < types.length; i++) {

                    try {
                        moduleContext.addType(ISyntaxConstants.THIS_NAMESPACE, types[i]);
                    } catch (Throwable t) {
                        IBoundError error = new BoundError(t, null);
                        OpenLMessagesUtils.addError(error.getMessage());
                        cxt.addError(error);
                    }
                }
            }
        }

        ASelector<ISyntaxNode> dataTypeSelector = new ASelector.StringValueSelector<ISyntaxNode>(ITableNodeTypes.XLS_DATATYPE,
            new SyntaxConvertor());

        ASelector<ISyntaxNode> propertiesSelector = new ASelector.StringValueSelector<ISyntaxNode>(ITableNodeTypes.XLS_PROPERTIES,
            new SyntaxConvertor());

        bindInternal(moduleNode, openl, moduleContext, module, propertiesSelector, null);
        bindInternal(moduleNode, openl, moduleContext, module, dataTypeSelector, null);

        ISelector<ISyntaxNode> notPropertiesSelector = propertiesSelector.not();
        ISelector<ISyntaxNode> notDataTypeSelector = dataTypeSelector.not();
        ISelector<ISyntaxNode> notPropAnd_NotDatatypeSelectors = notDataTypeSelector.and(notPropertiesSelector);

        return bindInternal(moduleNode, openl, moduleContext, module, notPropAnd_NotDatatypeSelectors, tableComparator);
    }

    private void proccessExtensions(XlsModuleOpenClass module,
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

    /*
     * (non-Javadoc)
     * @see org.openl.IOpenBinder#bind(org.openl.syntax.IParsedCode)
     */
    protected IBoundNode bindInternal(ISyntaxNode moduleNode,
                                      OpenL openl,
                                      RulesModuleBindingContext moduleContext,
                                      XlsModuleOpenClass module,
                                      ISelector<ISyntaxNode> childSelector,
                                      Comparator<TableSyntaxNode> tableComparator) {

        XlsModuleSyntaxNode xmsn = (XlsModuleSyntaxNode) moduleNode;

        ArrayList<ISyntaxNode> childSyntaxNodes = new ArrayList<ISyntaxNode>();

        for (TableSyntaxNode tsn : xmsn.getXlsTableSyntaxNodes()) {

            if (childSelector == null || childSelector.select(tsn)) {
                childSyntaxNodes.add(tsn);
            }
        }

        TableSyntaxNode[] chNodes = childSyntaxNodes.toArray(new TableSyntaxNode[0]);

        if (tableComparator != null) {
            Arrays.sort(chNodes, tableComparator);
        }

        IMemberBoundNode[] children = new IMemberBoundNode[chNodes.length];

        for (int i = 0; i < chNodes.length; i++) {

            try {

                // ISyntaxNode childNode = moduleNode.getChild(i);
                // if (childSelector.select(childNode))
                children[i] = preBindXlsNode(chNodes[i], openl, moduleContext, module);

                if (children[i] != null) {
                    children[i].addTo(module);
                }
            } catch (BoundError error) {
                moduleContext.addError(error);
                error.setTopLevelSyntaxNode(chNodes[i]);
                chNodes[i].addError(error);
                OpenLMessagesUtils.addError(error.getMessage());
            } catch (SyntaxErrorException se) {

                ISyntaxError[] ee = se.getSyntaxErrors();

                for (int j = 0; j < ee.length; j++) {
                    ee[j].setTopLevelSyntaxNode(chNodes[i]);
                    chNodes[i].addError(ee[j]);
                    moduleContext.addError(ee[j]);

                    OpenLMessagesUtils.addError(ee[j].getMessage());
                }

            } catch (Throwable e) {
                BoundError error = new BoundError(chNodes[i], null, e);
                error.setTopLevelSyntaxNode(chNodes[i]);
                chNodes[i].addError(error);
                moduleContext.addError(error);

                OpenLMessagesUtils.addError(error.getMessage());
            }
        }

        // if (moduleContext.getNumberOfErrors() == 0)
        for (int i = 0; i < children.length; i++) {

            if (children[i] != null) {

                try {
                    children[i].finalizeBind(moduleContext);
                } catch (BoundError error) {
                    error.setTopLevelSyntaxNode(chNodes[i]);
                    chNodes[i].addError(error);
                    moduleContext.addError(error);
                    OpenLMessagesUtils.addError(error.getMessage());
                } catch (SyntaxErrorException se) {
                    ISyntaxError[] ee = se.getSyntaxErrors();

                    for (int j = 0; j < ee.length; j++) {
                        ee[j].setTopLevelSyntaxNode(chNodes[i]);
                        chNodes[i].addError(ee[j]);
                        moduleContext.addError(ee[j]);

                        OpenLMessagesUtils.addError(ee[j].getMessage());
                    }

                } catch (Throwable e) {
                    BoundError error = new BoundError(chNodes[i], null, e);
                    error.setTopLevelSyntaxNode(chNodes[i]);
                    chNodes[i].addError(error);
                    moduleContext.addError(error);

                    OpenLMessagesUtils.addError(error.getMessage());
                }
            }
        }

        return new ModuleNode(moduleNode, moduleContext.getModule());
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

    private String getOpenLName(OpenlSyntaxNode osn) {

        return osn == null ? "org.openl.rules.java" : osn.getOpenlName();
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

    private OpenL makeOpenL(XlsModuleSyntaxNode moduleNode) {

        String openlName = getOpenLName(moduleNode.getOpenlNode());
        String allImports = moduleNode.getAllImportString();

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

    public IVocabulary makeVocabulary(XlsModuleSyntaxNode moduleNode) throws BoundError {

        final IdentifierNode vocabularyNode = moduleNode.getVocabularyNode();

        if (vocabularyNode == null) {
            return null;
        }

        final ClassLoader cl = userContext.getUserClassLoader();

        IVocabulary ivoc = null;

        try {
            Thread.currentThread().setContextClassLoader(cl);
            ivoc = (IVocabulary) userContext.execute(new IExecutable() {

                public Object execute() {

                    IVocabulary voc;
                    try {
                        Class<?> vClass = cl.loadClass(vocabularyNode.getIdentifier());
                        voc = (IVocabulary) vClass.newInstance();
                        return voc;
                    } catch (Throwable t) {
                        throw RuntimeExceptionWrapper.wrap(t);
                    }
                }
            });

            return ivoc;
        } catch (Throwable t) {
            BoundError error = new BoundError(vocabularyNode, "Can't Load Vocabulary", t);
            OpenLMessagesUtils.addError(error.getMessage());

            throw error;
        }
    }

    private IMemberBoundNode preBindXlsNode(ISyntaxNode syntaxNode,
                                            OpenL openl,
                                            RulesModuleBindingContext cxt,
                                            XlsModuleOpenClass module) throws Exception {

        String type = syntaxNode.getType();

        AXlsTableBinder binder = getBinderFactory().get(type);

        if (binder == null) {
            
            String message = String.format("Unknown table type '%s'", type);
            OpenLMessagesUtils.addWarn(message);
            // throw new Exception("Unknown Table Type: " + type);
            return null;
        }
        
        TableSyntaxNode tsn = (TableSyntaxNode) syntaxNode;

        PropertiesLoader propLoader = new PropertiesLoader(openl, cxt, module, binder);
        propLoader.loadProperties(tsn);

        return binder.preBind(tsn, openl, cxt, module);
    }
}
