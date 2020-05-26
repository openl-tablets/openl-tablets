/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.binding;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.exception.DuplicatedFieldException;
import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.engine.ExtendableModuleOpenClass;
import org.openl.engine.OpenLSystemProperties;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetResultOpenClass;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.data.IDataBase;
import org.openl.rules.data.ITable;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.binding.wrapper.IRulesMethodWrapper;
import org.openl.rules.lang.xls.binding.wrapper.WrapperLogic;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.table.OpenLArgumentsCloner;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.types.UriMemberHelper;
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
import org.openl.types.impl.AMethod;
import org.openl.util.StringUtils;

import com.rits.cloning.Cloner;

/**
 * @author snshor
 *
 */
public class XlsModuleOpenClass extends ModuleOpenClass implements ExtendableModuleOpenClass {

    private IDataBase dataBase;

    /**
     * Whether DecisionTable should be used as a dispatcher for overloaded tables. By default(this flag equals false)
     * dispatching logic will be performed in Java code.
     */
    private final boolean useDecisionTableDispatcher;

    private final boolean dispatchingValidationEnabled;

    private Collection<String> imports = Collections.emptySet();

    private final ClassLoader classLoader;

    private final OpenLBundleClassLoader classGenerationClassLoader;

    private RulesModuleBindingContext rulesModuleBindingContext;

    private final XlsDefinitions xlsDefinitions = new XlsDefinitions();

    private SpreadsheetResultOpenClass spreadsheetResultOpenClass;

    private ITableProperties globalTableProperties;

    public RulesModuleBindingContext getRulesModuleBindingContext() {
        return rulesModuleBindingContext;
    }

    /**
     * Constructor for module with dependent modules
     *
     */
    public XlsModuleOpenClass(String name,
            XlsMetaInfo metaInfo,
            OpenL openl,
            IDataBase dbase,
            Set<CompiledDependency> usingModules,
            ClassLoader classLoader,
            IBindingContext bindingContext) {
        super(name, openl);

        this.dataBase = dbase;
        this.metaInfo = metaInfo;
        this.useDecisionTableDispatcher = OpenLSystemProperties.isDTDispatchingMode(bindingContext.getExternalParams());
        this.dispatchingValidationEnabled = OpenLSystemProperties
            .isDispatchingValidationEnabled(bindingContext.getExternalParams());
        this.classLoader = classLoader;
        this.classGenerationClassLoader = new OpenLBundleClassLoader(null);
        this.classGenerationClassLoader.addClassLoader(classLoader);
        this.rulesModuleBindingContext = new RulesModuleBindingContext(bindingContext, this);

        this.globalTableProperties = TablePropertyDefinitionUtils.buildGlobalTableProperties();

        if (OpenLSystemProperties.isCustomSpreadsheetTypesSupported(bindingContext.getExternalParams())) {
            this.spreadsheetResultOpenClass = new SpreadsheetResultOpenClass(this);
        }
        if (usingModules != null) {
            setDependencies(usingModules);
            initDependencies();
        }
        initImports(metaInfo.getXlsModuleNode());
    }

    public ITableProperties getGlobalTableProperties() {
        return globalTableProperties;
    }

    public void addGlobalTableProperties(ITableProperties globalProperties) {
        if (globalProperties != null) {
            if (this.getGlobalTableProperties().getPriority() < globalProperties.getPriority()) {
                this.globalTableProperties = globalProperties;
            } else if (Objects.equals(this.getGlobalTableProperties().getPriority(), globalProperties.getPriority())) {
                Map<String, Object> mergedTableProperties = TablePropertyDefinitionUtils.mergeGlobalProperties(
                    this.globalTableProperties.getGlobalProperties(),
                    globalProperties.getGlobalProperties());
                this.globalTableProperties = TablePropertyDefinitionUtils
                    .buildGlobalTableProperties(mergedTableProperties);
            }
        }
    }

    public boolean isUseDecisionTableDispatcher() {
        return useDecisionTableDispatcher;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public OpenLBundleClassLoader getClassGenerationClassLoader() {
        return classGenerationClassLoader;
    }

    private void initImports(XlsModuleSyntaxNode xlsModuleSyntaxNode) {
        imports = Collections.unmodifiableSet(new HashSet<>(xlsModuleSyntaxNode.getImports()));
    }

    // TODO: should be placed to ModuleOpenClass
    public IDataBase getDataBase() {
        return dataBase;
    }

    protected void addXlsDefinitions(CompiledDependency dependency) {
        IOpenClass openClass = dependency.getCompiledOpenClass().getOpenClassWithErrors();
        if (openClass instanceof XlsModuleOpenClass) {
            XlsModuleOpenClass xlsModuleOpenClass = (XlsModuleOpenClass) openClass;
            this.xlsDefinitions.addAll(xlsModuleOpenClass.getXlsDefinitions());
        }
    }

    public XlsDefinitions getXlsDefinitions() {
        return xlsDefinitions;
    }

    @Override
    protected IOpenClass processDependencyTypeBeforeAdding(IOpenClass type) {
        if (type instanceof ModuleSpecificType) {
            IOpenClass existingType = findType(type.getName());
            if (existingType != null) {
                ModuleSpecificType existingModuleRelatedType = (ModuleSpecificType) existingType;
                existingModuleRelatedType.updateWithType(type);
                return existingType;
            } else {
                return ((ModuleSpecificType) type).makeCopyForModule(this);
            }
        }
        return super.processDependencyTypeBeforeAdding(type);
    }

    /**
     * Populate current module fields with data from dependent modules.
     */
    @Override
    protected void initDependencies() {// Reduce iterators over dependencies for
        // compilation issue with lazy loading
        for (CompiledDependency dependency : this.getDependencies()) {
            // commented as there is no need to add each datatype to upper
            // module.
            // as now it`s will be impossible to validate from which module the
            // datatype is.
            //
            // addTypes(dependency);
            addDependencyTypes(dependency);

            addXlsDefinitions(dependency);

            addGlobalTableProperties(dependency);

            addMethods(dependency);
            // Populate current module fields with data from dependent modules.
            // Required
            // for data tables inheriting from dependent modules.
            addDataTables(dependency.getCompiledOpenClass()); // Required for
            // data tables.
            addFields(dependency);
        }

        for (IOpenClass type : getTypes()) {
            if (type instanceof CustomSpreadsheetResultOpenClass) {
                ((CustomSpreadsheetResultOpenClass) type).fixModuleFieldTypes();
            }
        }
    }

    protected void addGlobalTableProperties(CompiledDependency dependency) {
        IOpenClass openClass = dependency.getCompiledOpenClass().getOpenClassWithErrors();
        if (openClass instanceof XlsModuleOpenClass) {
            XlsModuleOpenClass xlsModuleOpenClass = (XlsModuleOpenClass) openClass;
            addGlobalTableProperties(xlsModuleOpenClass.getGlobalTableProperties());
        }
    }

    public Collection<String> getImports() {
        return imports;
    }

    @Override
    protected boolean isDependencyMethodInheritable(IOpenMethod openMethod) {
        if (openMethod instanceof TestSuiteMethod) {
            return false;
        }
        return super.isDependencyMethodInheritable(openMethod);
    }

    @Override
    protected boolean isDependencyFieldInheritable(IOpenField openField) {
        if (openField instanceof ConstantOpenField) {
            return true;
        }
        return super.isDependencyFieldInheritable(openField);
    }

    @Override
    public void applyToDependentParsedCode(IParsedCode parsedCode) {
        Objects.requireNonNull(parsedCode, "parsedCode cannot be null");
        if (parsedCode.getTopNode() instanceof XlsModuleSyntaxNode) {
            XlsModuleSyntaxNode xlsModuleSyntaxNode = (XlsModuleSyntaxNode) parsedCode.getTopNode();
            for (String value : getImports()) {
                xlsModuleSyntaxNode.addImport(value);
            }
        }
    }

    public SpreadsheetResultOpenClass getSpreadsheetResultOpenClassWithResolvedFieldTypes() {
        return spreadsheetResultOpenClass;
    }

    private void addDataTables(CompiledOpenClass dependency) {
        IOpenClass openClass = dependency.getOpenClassWithErrors();

        if (openClass instanceof XlsModuleOpenClass) {
            XlsModuleOpenClass xlsModuleOpenClass = (XlsModuleOpenClass) openClass;
            if (xlsModuleOpenClass.getDataBase() != null) {
                for (ITable table : xlsModuleOpenClass.getDataBase().getTables()) {
                    // TODO Test and fix with lazy compilation. It was fixed for Maven Plugin but wasn't tested.
                    if (XlsNodeTypes.XLS_DATA.toString().equals(table.getTableSyntaxNode().getType())) {
                        try {
                            getDataBase().registerTable(table);
                        } catch (DuplicatedTableException | OpenlNotCheckedException e) {
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

    protected IOpenMethod unwrapOpenMethod(IOpenMethod method) {
        if (method instanceof IRulesMethodWrapper) {
            IRulesMethodWrapper wrapper = (IRulesMethodWrapper) method;
            return wrapper.getDelegate();
        }
        return method;
    }

    protected IOpenMethod wrapOpenMethod(IOpenMethod method) {
        return WrapperLogic.wrapOpenMethod(method, this);
    }

    @Override
    public void addField(IOpenField openField) {
        Map<String, IOpenField> fields = fieldMap();
        if (fields.containsKey(openField.getName())) {
            IOpenField existedField = fields.get(openField.getName());
            if (openField instanceof ConstantOpenField && existedField instanceof ConstantOpenField) {
                // Ignore constants with the same values
                if (openField.getType().equals(existedField.getType()) && Objects
                    .equals(((ConstantOpenField) openField).getValue(), ((ConstantOpenField) existedField).getValue())) {
                    return;
                }

                throw new DuplicatedFieldException("", openField.getName());
            }
            UriMemberHelper.validateFieldDuplication(openField, existedField);
        }
        fieldMap().put(openField.getName(), openField);
        addFieldToLowerCaseMap(openField);
    }

    /**
     * Adds method to <code>XlsModuleOpenClass</code>.
     *
     * @param method method object
     */
    @Override
    public void addMethod(IOpenMethod method) {
        if (method instanceof OpenMethodDispatcher) {
            /*
             * Dispatcher method should be added by adding all candidates of the specified dispatcher to current
             * XlsModuleOpenClass(it will cause adding methods to dispatcher of current module or creating new
             * dispatcher in current module).
             *
             * Previously there was problems because dispatcher from dependency was either added to dispatcher of
             * current module(dispatcher as a candidate in another dispatcher) or added to current module and was
             * modified during the current module processing. FIXME
             */
            for (IOpenMethod candidate : ((OpenMethodDispatcher) method).getCandidates()) {
                addMethod(candidate);
            }
            return;
        }
        IOpenMethod m = wrapOpenMethod(method);

        // Workaround needed to set the module name in the method while compile
        if (m instanceof AMethod && ((AMethod) m).getModuleName() == null) {
            XlsMetaInfo metaInfo = getXlsMetaInfo();
            if (metaInfo != null) {
                IOpenSourceCodeModule sourceCodeModule = metaInfo.getXlsModuleNode().getModule();
                if (sourceCodeModule instanceof IModuleInfo) {
                    ((AMethod) m).setModuleName(((IModuleInfo) sourceCodeModule).getModuleName());
                }
            }
        }

        // Checks that method already exists in the class. If it already
        // exists then "overload" it using decorator; otherwise - just add to
        // the class.
        //
        IOpenMethod existingMethod = getDeclaredMethod(m.getName(), m.getSignature().getParameterTypes());
        if (existingMethod != null) {

            if (!existingMethod.getType().equals(m.getType())) {
                String message = String.format(
                    "Method '%s' with return type '%s' is already defined with another return type ('%s')",
                    m.getName(),
                    m.getType().getDisplayName(0),
                    existingMethod.getType().getDisplayName(0));
                throw new DuplicatedMethodException(message, existingMethod, method);
            }

            if (!m.equals(existingMethod) && method instanceof TestSuiteMethod) {
                UriMemberHelper.validateMethodDuplication(method, existingMethod);
                return;
            }

            // Checks the instance of existed method. If it's the
            // OpenMethodDecorator then just add the method-candidate to
            // decorator; otherwise - replace existed method with new instance
            // of OpenMethodDecorator for existed method and add new one.
            //
            try {
                if (existingMethod instanceof OpenMethodDispatcher) {
                    OpenMethodDispatcher decorator = (OpenMethodDispatcher) existingMethod;
                    decorator.addMethod(unwrapOpenMethod(m));
                } else {
                    if (!m.equals(existingMethod)) {
                        // Create decorator for existed method.
                        //
                        OpenMethodDispatcher dispatcher = getOpenMethodDispatcher(existingMethod);

                        IOpenMethod openMethod = wrapOpenMethod(dispatcher);

                        overrideMethod(openMethod);

                        dispatcher.addMethod(unwrapOpenMethod(m));
                    }
                }
            } catch (DuplicatedMethodException e) {
                SyntaxNodeException error = null;
                if (m instanceof IMemberMetaInfo) {
                    IMemberMetaInfo memberMetaInfo = (IMemberMetaInfo) m;
                    if (memberMetaInfo.getSyntaxNode() != null) {
                        if (memberMetaInfo.getSyntaxNode() instanceof TableSyntaxNode) {
                            error = SyntaxNodeExceptionUtils
                                .createError(e.getMessage(), e, memberMetaInfo.getSyntaxNode());
                            ((TableSyntaxNode) memberMetaInfo.getSyntaxNode()).addError(error);
                        }
                    }
                }
                boolean f = false;
                for (Throwable t : getErrors()) {
                    if (t.getMessage().equals(e.getMessage())) {
                        f = true;
                        break;
                    }
                }
                if (!f) {
                    if (error != null) {
                        addError(error);
                    } else {
                        addError(e);
                    }
                }
            }
        } else {
            // Just wrap original method with dispatcher functionality.
            //

            if (dispatchingValidationEnabled && !(m instanceof TestSuiteMethod) && dimensionalPropertyPresented(m)) {
                // Create dispatcher for existed method.
                //
                OpenMethodDispatcher dispatcher = getOpenMethodDispatcher(m);

                IOpenMethod openMethod = wrapOpenMethod(dispatcher);

                super.addMethod(openMethod);

            } else {
                super.addMethod(m);
            }
        }
    }

    private boolean dimensionalPropertyPresented(IOpenMethod m) {
        List<TablePropertyDefinition> dimensionalPropertiesDef = TablePropertyDefinitionUtils
            .getDimensionalTableProperties();
        ITableProperties propertiesFromMethod = PropertiesHelper.getTableProperties(m);
        for (TablePropertyDefinition dimensionProperty : dimensionalPropertiesDef) {
            String propertyValue = propertiesFromMethod.getPropertyValueAsString(dimensionProperty.getName());
            if (StringUtils.isNotEmpty(propertyValue)) {
                return true;
            }
        }
        return false;
    }

    private OpenMethodDispatcher getOpenMethodDispatcher(IOpenMethod method) {
        OpenMethodDispatcher decorator;
        IOpenMethod decorated = unwrapOpenMethod(method);
        if (useDecisionTableDispatcher) {
            decorator = new OverloadedMethodsDispatcherTable(decorated, this);
        } else {
            decorator = new MatchingOpenMethodDispatcher(decorated, this);
        }
        return decorator;
    }

    @Override
    public void clearOddDataForExecutionMode() {
        super.clearOddDataForExecutionMode();
        dataBase = null;
        rulesModuleBindingContext = null;
    }

    public void completeOpenClassBuilding() {
        addTestSuiteMethodsFromDependencies(); // Test method from dependencies
        // should use methods from this
        // class.
    }

    private TestSuiteMethod createNewTestSuiteMethod(TestSuiteMethod testSuiteMethod) {
        IOpenMethod method = testSuiteMethod.getTestedMethod();
        IOpenMethod newTargetMethod = getDeclaredMethod(method.getName(), method.getSignature().getParameterTypes());
        TestSuiteMethod copy = new TestSuiteMethod(newTargetMethod, testSuiteMethod);
        copy.setModuleName(testSuiteMethod.getModuleName());
        return copy;
    }

    private void validateType(IOpenClass type) {
        if (type instanceof CustomSpreadsheetResultOpenClass) {
            for (IOpenClass t : getTypes()) {
                if (t instanceof CustomSpreadsheetResultOpenClass) {
                    CustomSpreadsheetResultOpenClass csrType = (CustomSpreadsheetResultOpenClass) t;
                    if (Objects.equals(csrType.getName(), type.getName()) && csrType.isBeanClassInitialized()) {
                        throw new IllegalStateException(String.format(
                            "This module does not support adding '%s' custom spreadsheet result types. Bean class has already been initialized for this custom spreadsheet result type.",
                            csrType.getName()));
                    }
                }
            }
        }
    }

    @Override
    public void addType(IOpenClass type) {
        validateType(type);
        super.addType(type);
    }

    protected void addTestSuiteMethodsFromDependencies() {
        for (CompiledDependency dependency : this.getDependencies()) {
            for (IOpenMethod depMethod : dependency.getCompiledOpenClass().getOpenClassWithErrors().getMethods()) {
                if (depMethod instanceof TestSuiteMethod) {
                    TestSuiteMethod testSuiteMethod = (TestSuiteMethod) depMethod;
                    try {
                        // Workaround for set dependency names in method while
                        // compile
                        if (testSuiteMethod.getModuleName() == null) {
                            testSuiteMethod.setModuleName(dependency.getDependencyName());
                        }
                        TestSuiteMethod newTestSuiteMethod = createNewTestSuiteMethod(testSuiteMethod);
                        addMethod(newTestSuiteMethod);
                    } catch (OpenlNotCheckedException e) {
                        addError(e);
                    }
                }
            }
        }
    }

    private volatile OpenLArgumentsCloner cloner = null;

    public Cloner getCloner() {
        if (cloner == null) {
            synchronized (this) {
                if (cloner == null) {
                    cloner = new OpenLArgumentsCloner();
                }
            }
        }
        return cloner;
    }

}
