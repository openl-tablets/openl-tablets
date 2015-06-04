/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.binding;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.binding.impl.module.DeferredMethod;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.engine.ExtendableModuleOpenClass;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.data.DataOpenField;
import org.openl.rules.data.IDataBase;
import org.openl.rules.data.ITable;
import org.openl.rules.dt2.DecisionTable;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.binding.wrapper.AlgorithmWrapper;
import org.openl.rules.lang.xls.binding.wrapper.AlgorithmSubroutineMethodWrapper;
import org.openl.rules.lang.xls.binding.wrapper.ColumnMatchWrapper;
import org.openl.rules.lang.xls.binding.wrapper.CompositeMethodWrapper;
import org.openl.rules.lang.xls.binding.wrapper.DecisionTable2Wrapper;
import org.openl.rules.lang.xls.binding.wrapper.DecisionTableWrapper;
import org.openl.rules.lang.xls.binding.wrapper.DeferredMethodWrapper;
import org.openl.rules.lang.xls.binding.wrapper.DispatchWrapperMark;
import org.openl.rules.lang.xls.binding.wrapper.JavaOpenMethodWrapper;
import org.openl.rules.lang.xls.binding.wrapper.MatchingOpenMethodDispatcherWrapper;
import org.openl.rules.lang.xls.binding.wrapper.OverloadedMethodsDispatcherTableWrapper;
import org.openl.rules.lang.xls.binding.wrapper.SpreadsheetWrapper;
import org.openl.rules.lang.xls.binding.wrapper.TableMethodDelegate;
import org.openl.rules.lang.xls.binding.wrapper.TestSuiteMethodWrapper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.method.table.TableMethod;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.tbasic.Algorithm;
import org.openl.rules.tbasic.AlgorithmSubroutineMethod;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.rules.types.impl.OverloadedMethodsDispatcherTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IModuleInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenSchema;
import org.openl.types.impl.AMethod;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.MethodKey;
import org.openl.types.java.JavaOpenMethod;

/**
 * @author snshor
 * 
 */
public class XlsModuleOpenClass extends ModuleOpenClass implements ExtendableModuleOpenClass {

    private IDataBase dataBase = null;

    /**
     * Whether DecisionTable should be used as a dispatcher for overloaded
     * tables. By default(this flag equals false) dispatching logic will be
     * performed in Java code.
     */
    private boolean useDescisionTableDispatcher;

    private Collection<String> imports = new HashSet<String>();

    public XlsModuleOpenClass(IOpenSchema schema,
            String name,
            XlsMetaInfo metaInfo,
            OpenL openl,
            IDataBase dbase,
            boolean useDescisionTableDispatcher) {
        this(schema, name, metaInfo, openl, dbase, null, useDescisionTableDispatcher);
    }

    /**
     * Constructor for module with dependent modules
     * 
     */
    public XlsModuleOpenClass(IOpenSchema schema,
            String name,
            XlsMetaInfo metaInfo,
            OpenL openl,
            IDataBase dbase,
            Set<CompiledDependency> usingModules,
            boolean useDescisionTableDispatcher) {
        super(schema, name, openl, usingModules);
        this.dataBase = dbase;
        this.metaInfo = metaInfo;
        this.useDescisionTableDispatcher = useDescisionTableDispatcher;
        initImports(metaInfo.getXlsModuleNode());
        additionalInitDependencies(); // Required for data tables.
    }

    private void initImports(XlsModuleSyntaxNode xlsModuleSyntaxNode) {
        imports.addAll(xlsModuleSyntaxNode.getImports());
    }

    // TODO: should be placed to ModuleOpenClass
    public IDataBase getDataBase() {
        return dataBase;
    }

    /**
     * Populate current module fields with data from dependent modules. Requered
     * for data tables inheriting from dependend modules.
     */
    private void additionalInitDependencies() {
        for (CompiledDependency dependency : this.getDependencies()) {
            addDataTables(dependency.getCompiledOpenClass());
        }
    }

    public Collection<String> getImports() {
        return imports;
    }

    @Override
    public void applyToDependentParsedCode(IParsedCode parsedCode) {
        if (parsedCode == null) {
            throw new IllegalArgumentException("parsedCode argument can't be null!");
        }
        if (parsedCode.getTopNode() instanceof XlsModuleSyntaxNode) {
            XlsModuleSyntaxNode xlsModuleSyntaxNode = (XlsModuleSyntaxNode) parsedCode.getTopNode();
            for (String value : getImports()) {
                xlsModuleSyntaxNode.addImport(value);
            }
        }
    }

    private void addDataTables(CompiledOpenClass dependency) {
        IOpenClass openClass = dependency.getOpenClassWithErrors();

        Map<String, IOpenField> fieldsMap = openClass.getFields();

        Set<String> tableUrls = new HashSet<String>();
        Map<String, IOpenField> fields = getFields();
        for (IOpenField openField : fields.values()) {
            if (openField instanceof DataOpenField) {
                tableUrls.add(((DataOpenField) openField).getTableUri());
            }
        }
        for (String key : fieldsMap.keySet()) {
            IOpenField field = fieldsMap.get(key);
            if (field instanceof DataOpenField) {
                try {
                    String tableUrl = ((DataOpenField) field).getTableUri();
                    if (!tableUrls.contains(tableUrl)) {
                        addField(field);
                        tableUrls.add(tableUrl);
                    }
                } catch (OpenlNotCheckedException e) {
                    addError(e);
                }
            }
        }

        if (openClass instanceof XlsModuleOpenClass) {
            XlsModuleOpenClass xlsModuleOpenClass = (XlsModuleOpenClass) openClass;
            if (xlsModuleOpenClass.getDataBase() != null) {
                for (ITable table : xlsModuleOpenClass.getDataBase().getTables()) {
                    if (XlsNodeTypes.XLS_DATA.toString().equals(table.getTableSyntaxNode().getType())) {
                        try {
                            getDataBase().registerTable(table);
                        } catch (DuplicatedTableException e) {
                            addError(e);
                        } catch (OpenlNotCheckedException e) {
                            addError(e);
                        }
                    }
                }
            }
        }
    }

    public XlsMetaInfo getXlsMetaInfo() {
        return (XlsMetaInfo) metaInfo;
    }

    private IOpenMethod decorateForMultimoduleDispatching(final IOpenMethod openMethod) { // Dispatching
                                                                                          // fix
                                                                                          // for
                                                                                          // mul1ti-module
        if (openMethod instanceof DispatchWrapperMark || openMethod instanceof TestSuiteMethod) {
            return openMethod;
        }
        if (openMethod instanceof OverloadedMethodsDispatcherTable) {
            return new OverloadedMethodsDispatcherTableWrapper(this, (OverloadedMethodsDispatcherTable) openMethod);
        }
        if (openMethod instanceof MatchingOpenMethodDispatcher) {
            return new MatchingOpenMethodDispatcherWrapper(this, (MatchingOpenMethodDispatcher) openMethod);
        }
        if (openMethod instanceof DeferredMethod) {
            return new DeferredMethodWrapper(this, (DeferredMethod) openMethod);
        }
        if (openMethod instanceof CompositeMethod) {
            return new CompositeMethodWrapper(this, (CompositeMethod) openMethod);
        }
        if (openMethod instanceof Algorithm) {
            return new AlgorithmWrapper(this, (Algorithm) openMethod);
        }
        if (openMethod instanceof AlgorithmSubroutineMethod) {
            return new AlgorithmSubroutineMethodWrapper(this, (AlgorithmSubroutineMethod) openMethod);
        }
        if (openMethod instanceof DecisionTable) {
            return new DecisionTable2Wrapper(this, (DecisionTable) openMethod);
        }
        if (openMethod instanceof org.openl.rules.dt.DecisionTable) {
            return new DecisionTableWrapper(this, (org.openl.rules.dt.DecisionTable) openMethod);
        }
        if (openMethod instanceof ColumnMatch) {
            return new ColumnMatchWrapper(this, (ColumnMatch) openMethod); 
        }
        if (openMethod instanceof TestSuiteMethod) {
            return new TestSuiteMethodWrapper(this, (TestSuiteMethod) openMethod);
        }
        if (openMethod instanceof Spreadsheet) {
            return new SpreadsheetWrapper(this, (Spreadsheet) openMethod);
        }
        if (openMethod instanceof TableMethod) {
            return new TableMethodDelegate(this, (TableMethod) openMethod);
        }

        if (openMethod instanceof JavaOpenMethod) {
            return new JavaOpenMethodWrapper(this, (JavaOpenMethod) openMethod);
        }

        /*
         * if (log.isWarnEnabled()) {
         * log.warn("Method wasn't wrapped. Dispatching will not work properly!"
         * ); }
         */
        return openMethod;
    }

    /**
     * Adds method to <code>XlsModuleOpenClass</code>.
     * 
     * @param method method object
     */
    @Override
    public void addMethod(IOpenMethod method) {
    	methodList = null;
        if (method instanceof OpenMethodDispatcher) {
            addDispatcherMethod((OpenMethodDispatcher) method);
            return;
        }
        IOpenMethod m = decorateForMultimoduleDispatching(method);

        // Workaround needed to set the module name in the method while compile
        if (m instanceof AMethod) {
            if (((AMethod) m).getModuleName() == null) {
                XlsMetaInfo metaInfo = getXlsMetaInfo();
                if (metaInfo != null) {
                    IOpenSourceCodeModule sourceCodeModule = metaInfo.getXlsModuleNode().getModule();
                    if (sourceCodeModule instanceof IModuleInfo) {
                        ((AMethod) m).setModuleName(((IModuleInfo) sourceCodeModule).getModuleName());
                    }
                }
            }
        }
        // Get method key.
        //
        MethodKey key = new MethodKey(method);

        Map<MethodKey, IOpenMethod> methods = methodMap();

        // Checks that method already exists in method map. If it already
        // exists then "overload" it using decorator; otherwise - just add to
        // method map.
        //
        if (methods.containsKey(key)) {

            // Gets the existed method from map.
            //
            IOpenMethod existedMethod = methods.get(key);

            if (!existedMethod.getType().equals(method.getType())) {
                throw new DuplicatedMethodException(String.format("Method \"%s\" with return type \"%s\" has already been defined with another return type (\"%s\")",
                    method.getName(),
                    method.getType().getDisplayName(0),
                    existedMethod.getType().getDisplayName(0)),
                    method);
            }

            // Checks the instance of existed method. If it's the
            // OpenMethodDecorator then just add the method-candidate to
            // decorator; otherwise - replace existed method with new instance
            // of OpenMethodDecorator for existed method and add new one.
            //
            try {
                if (existedMethod instanceof OpenMethodDispatcher) {
                    OpenMethodDispatcher decorator = (OpenMethodDispatcher) existedMethod;
                    decorator.addMethod(m);
                } else {
                    if (m != existedMethod) {
                        OpenMethodDispatcher dispatcher = createDispatcherMethod(existedMethod, key);
                        dispatcher.addMethod(m);
                    }
                }
            } catch (DuplicatedMethodException e) {
                SyntaxNodeException error = null;
                if (m instanceof IMemberMetaInfo) {
                    IMemberMetaInfo memberMetaInfo = (IMemberMetaInfo) m;
                    if (memberMetaInfo.getSyntaxNode() != null) {
                        if (memberMetaInfo.getSyntaxNode() instanceof TableSyntaxNode) {
                            error = SyntaxNodeExceptionUtils.createError(e.getMessage(),
                                    e,
                                    memberMetaInfo.getSyntaxNode());
                            ((TableSyntaxNode) memberMetaInfo.getSyntaxNode()).addError(error);
                        }
                    }
                }
                boolean f = false;
                for (Throwable t : getErrors()) {
                    if (t instanceof DuplicatedMethodException) {
                        if (t.getMessage().equals(e.getMessage())) {
                            f = true;
                            break;
                        }
                    }
                }
                if (!f) {
                    addError(e);
                    if (error != null) {
                        OpenLMessagesUtils.addError(error);
                    } else {
                        OpenLMessagesUtils.addError(e);
                    }
                }
            }
        } else {
            // Just wrap original method with dispatcher functionality.
            //
            if (!dimensionalPropertyPresented(m) || m instanceof TestSuiteMethod) {
                methodMap().put(key, m);
            } else {
                createDispatcherMethod(m, key);
            }
        }
    }

    private boolean dimensionalPropertyPresented(IOpenMethod m) {
        List<TablePropertyDefinition> dimensionalPropertiesDef = TablePropertyDefinitionUtils.getDimensionalTableProperties();
        ITableProperties propertiesFromMethod = PropertiesHelper.getTableProperties(m);
        for (TablePropertyDefinition dimensionProperty : dimensionalPropertiesDef) {
            String propertyValue = propertiesFromMethod.getPropertyValueAsString(dimensionProperty.getName());
            if (StringUtils.isNotEmpty(propertyValue)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Dispatcher method should be added by adding all candidates of the
     * specified dispatcher to current XlsModuleOpenClass(it will cause adding
     * methods to dispatcher of current module or creating new dispatcher in
     * current module).
     * 
     * Previously there was problems because dispatcher from dependency was
     * either added to dispatcher of current module(dispatcher as a candidate in
     * another dispatcher) or added to current module and was modified during
     * the current module processing. FIXME
     * 
     * @param dispatcher Dispatcher methods to add.
     */
    public void addDispatcherMethod(OpenMethodDispatcher dispatcher) {
        for (IOpenMethod candidate : dispatcher.getCandidates()) {
            addMethod(candidate);
        }
    }

    private OpenMethodDispatcher createDispatcherMethod(IOpenMethod method, MethodKey key) {
        // Create decorator for existed method.
        //
        OpenMethodDispatcher decorator;

        if (useDescisionTableDispatcher) {
            decorator = new OverloadedMethodsDispatcherTable(method, this);
        } else {
            decorator = new MatchingOpenMethodDispatcher(method, this);
        }

        IOpenMethod openMethod = decorateForMultimoduleDispatching(decorator);

        methodMap().put(key, openMethod);

        return decorator;
    }

    @Override
    public void clearOddDataForExecutionMode() {
        super.clearOddDataForExecutionMode();
        dataBase = null;
    }

}
