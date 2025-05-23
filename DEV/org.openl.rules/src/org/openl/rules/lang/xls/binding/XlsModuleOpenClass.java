/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.binding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.base.INamedThing;
import org.openl.binding.IBindingContext;
import org.openl.binding.MethodUtil;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.exception.DuplicatedFieldException;
import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.binding.impl.module.ModuleSpecificOpenField;
import org.openl.binding.impl.module.ModuleSpecificType;
import org.openl.classloader.OpenLClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.engine.ExtendableModuleOpenClass;
import org.openl.engine.OpenLSystemProperties;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.binding.OpenLMessageSource;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.calc.CombinedSpreadsheetResultOpenClass;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResultOpenClass;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.convertor.ObjectToDataOpenCastConvertor;
import org.openl.rules.data.DataOpenField;
import org.openl.rules.data.IDataBase;
import org.openl.rules.data.ITable;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.binding.wrapper.ConstantOpenFieldWrapper;
import org.openl.rules.lang.xls.binding.wrapper.DataOpenFieldWrapper;
import org.openl.rules.lang.xls.binding.wrapper.WrapperLogic;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.types.DuplicateMemberThrowExceptionHelper;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.syntax.code.IParsedCode;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.impl.MethodSignature;
import org.openl.util.StringUtils;

/**
 * @author snshor
 */
public class XlsModuleOpenClass extends ModuleOpenClass implements ExtendableModuleOpenClass {

    private static final Logger LOG = LoggerFactory.getLogger(XlsModuleOpenClass.class);

    private final IDataBase dataBase;

    private final boolean dispatchingValidationEnabled;

    private Collection<String> imports = Collections.emptySet();

    private final ClassLoader classLoader;

    private final OpenLClassLoader classGenerationClassLoader;

    private RulesModuleBindingContext rulesModuleBindingContext;

    private final XlsDefinitions xlsDefinitions = new XlsDefinitions();

    private SpreadsheetResultOpenClass spreadsheetResultOpenClass;

    private ITableProperties globalTableProperties;

    private final Map<String, List<IOpenField>> hiddenFields = new HashMap<>();
    private final Map<String, List<IOpenField>> hiddenLowerCasedFields = new HashMap<>();
    private final Collection<XlsModuleOpenClass> externalXlsModuleOpenClasses = new HashSet<>();

    private final ObjectToDataOpenCastConvertor objectToDataOpenCastConvertor = new ObjectToDataOpenCastConvertor();

    public RulesModuleBindingContext getRulesModuleBindingContext() {
        return rulesModuleBindingContext;
    }

    public final Map<CustomSpreadsheetResultOpenClassesKey, CombinedSpreadsheetResultOpenClass> combinedSpreadsheetResultOpenClasses = new HashMap<>();
    private final AtomicLong combinedSpreadsheetResultOpenClassesCounter = new AtomicLong(0);
    private final Set<String> uniqueCombinedSpreadsheetResultClassNames = new HashSet<>();

    private final OpenLMessageSource messageSource;

    private final boolean appliedChangesToClasspath;

    /**
     * Constructor for module with dependent modules
     */
    public XlsModuleOpenClass(String moduleName,
                              XlsMetaInfo xlsMetaInfo,
                              OpenL openl,
                              IDataBase dbase,
                              Set<CompiledDependency> usingModules,
                              ClassLoader classLoader,
                              boolean appliedChangesToClasspath,
                              IBindingContext bindingContext) {
        super(moduleName, openl);
        this.dataBase = dbase;
        this.xlsMetaInfo = xlsMetaInfo;
        this.dispatchingValidationEnabled = OpenLSystemProperties
                .isDispatchingValidationEnabled(bindingContext.getExternalParams());
        this.classLoader = classLoader;
        this.messageSource = new OpenLMessageSource(classLoader);
        this.classGenerationClassLoader = new OpenLClassLoader(null);
        this.classGenerationClassLoader.addClassLoader(classLoader);
        this.rulesModuleBindingContext = new RulesModuleBindingContext(bindingContext, this);
        this.appliedChangesToClasspath = appliedChangesToClasspath;

        this.globalTableProperties = TablePropertyDefinitionUtils.buildGlobalTableProperties();

        this.spreadsheetResultOpenClass = new SpreadsheetResultOpenClass(this);
        if (usingModules != null) {
            setDependencies(usingModules);
            initDependencies();
        }
        initImports(xlsMetaInfo.getXlsModuleNode());
    }

    public CustomSpreadsheetResultOpenClass buildOrGetCombinedSpreadsheetResult(
            CustomSpreadsheetResultOpenClass... customSpreadsheetResultOpenClasses) {
        Set<CustomSpreadsheetResultOpenClass> c = new HashSet<>();
        for (CustomSpreadsheetResultOpenClass t : customSpreadsheetResultOpenClasses) {
            if (t instanceof CombinedSpreadsheetResultOpenClass) {
                c.addAll(((CombinedSpreadsheetResultOpenClass) t).getCombinedTypes());
            } else {
                c.add(t);
            }
        }
        if (c.size() == 1) {
            return c.iterator().next();
        }
        CustomSpreadsheetResultOpenClassesKey key = new CustomSpreadsheetResultOpenClassesKey(
                c.toArray(new CustomSpreadsheetResultOpenClass[0]));
        CombinedSpreadsheetResultOpenClass combinedSpreadsheetResultOpenClass = combinedSpreadsheetResultOpenClasses
                .get(key);
        if (combinedSpreadsheetResultOpenClass == null) {
            combinedSpreadsheetResultOpenClass = new CombinedSpreadsheetResultOpenClass(this);
            for (CustomSpreadsheetResultOpenClass t : c) {
                combinedSpreadsheetResultOpenClass.updateWithType(t);
            }
            combinedSpreadsheetResultOpenClasses.put(key, combinedSpreadsheetResultOpenClass);
        }
        return combinedSpreadsheetResultOpenClass;
    }

    public IOpenClass toModuleType(IOpenClass type) {
        if (type instanceof SpreadsheetResultOpenClass) {
            SpreadsheetResultOpenClass spreadsheetResultOpenClass = (SpreadsheetResultOpenClass) type;
            if (isDependencyModule(spreadsheetResultOpenClass.getModule(), new IdentityHashMap<>())) {
                return getSpreadsheetResultOpenClassWithResolvedFieldTypes();
            }
        } else if (type instanceof ModuleSpecificType) {
            if (isDependencyModule((XlsModuleOpenClass) ((ModuleSpecificType) type).getModule(),
                    new IdentityHashMap<>())) {
                if (type instanceof CombinedSpreadsheetResultOpenClass) {
                    return ((CombinedSpreadsheetResultOpenClass) type).convertToModuleType(this, false);
                }
                IOpenClass p = findType(type.getName());
                if (p == null) {
                    return ((ModuleSpecificType) type).convertToModuleTypeAndRegister(this);
                }
                return p;
            }
        }
        return type;
    }

    public AtomicLong getCombinedSpreadsheetResultOpenClassesCounter() {
        return combinedSpreadsheetResultOpenClassesCounter;
    }

    public synchronized String generateUniqueCombinedSpreadsheetResultClassName(String name) {
        String d = name;
        int i = 0;
        while (uniqueCombinedSpreadsheetResultClassNames.contains(d)) {
            d = name + i;
            i++;
        }
        uniqueCombinedSpreadsheetResultClassNames.add(d);
        return d;
    }

    public Collection<CombinedSpreadsheetResultOpenClass> getCombinedSpreadsheetResultOpenClasses() {
        return new ArrayList<>(combinedSpreadsheetResultOpenClasses.values());
    }

    public ITableProperties getGlobalTableProperties() {
        return globalTableProperties;
    }

    public ObjectToDataOpenCastConvertor getObjectToDataOpenCastConvertor() {
        return objectToDataOpenCastConvertor;
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

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public OpenLClassLoader getClassGenerationClassLoader() {
        return classGenerationClassLoader;
    }

    private void initImports(XlsModuleSyntaxNode xlsModuleSyntaxNode) {
        imports = Set.copyOf(xlsModuleSyntaxNode.getImports());
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
                return ((ModuleSpecificType) type).convertToModuleTypeAndRegister(this);
            }
        }
        return super.processDependencyTypeBeforeAdding(type);
    }

    protected IOpenField processFieldBeforeAdding(IOpenField openField) {
        IOpenClass type = WrapperLogic.toModuleType(openField.getType(), this, new IdentityHashMap<>());
        if (type != openField.getType()) {
            if (openField instanceof DataOpenField) {
                DataOpenField f = openField instanceof DataOpenFieldWrapper ? ((DataOpenFieldWrapper) openField)
                        .getDelegate() : (DataOpenField) openField;
                return new DataOpenFieldWrapper(f, type);
            } else if (openField instanceof ConstantOpenField) {
                ConstantOpenField f = openField instanceof ConstantOpenFieldWrapper ? ((ConstantOpenFieldWrapper) openField)
                        .getDelegate() : (ConstantOpenField) openField;
                return new ConstantOpenFieldWrapper(f, type);
            } else {
                return new ModuleSpecificOpenField(openField, type);
            }
        }
        return openField;
    }

    /**
     * Populate current module fields with data from dependent modules.
     */
    protected void initDependencies() {// Reduce iterators over dependencies for
        List<IOpenField> fields = new ArrayList<>();
        Map<String, ITable> dataTables = new HashMap<>();
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
            collectDataTables(dependency, dataTables); // Required for
            // data tables.
            collectDependencyFields(dependency, fields);
            addExternalXlsModuleOpenClassesFromDependency(dependency);
        }

        addDataTablesFromDependencies(dataTables);
        addFieldsFromDependencies(fields);
    }

    private void addExternalXlsModuleOpenClassesFromDependency(CompiledDependency dependency) {
        if (dependency.getCompiledOpenClass().getOpenClassWithErrors() instanceof XlsModuleOpenClass) {
            ((XlsModuleOpenClass) (dependency.getCompiledOpenClass().getOpenClassWithErrors()))
                    .getExternalXlsModuleOpenClasses()
                    .forEach(this::addExternalXlsModuleOpenClass);
        }
    }

    @Override
    public IOpenField getField(String fname, boolean strictMatch) throws AmbiguousFieldException {
        IOpenField field = super.getField(fname, strictMatch);
        if (field == null) {
            if (strictMatch && hiddenFields.containsKey(fname) || !strictMatch && hiddenLowerCasedFields
                    .containsKey(fname)) {
                throw new AmbiguousFieldException(fname,
                        strictMatch ? hiddenFields.get(fname) : hiddenLowerCasedFields.get(fname.toLowerCase()));
            }
        }
        return field;
    }

    private void addFieldsFromDependencies(List<IOpenField> fields) {
        Set<Integer> fieldsToHide = new HashSet<>();
        for (int i = 0; i < fields.size() - 1; i++) {
            for (int j = i + 1; j < fields.size(); j++) {
                IOpenField openField1 = fields.get(i);
                IOpenField openField2 = fields.get(j);
                if (Objects.equals(openField1.getName(),
                        openField2
                                .getName()) && openField1 instanceof DataOpenField && openField2 instanceof DataOpenField && XlsNodeTypes.XLS_DATA
                        .equals(((DataOpenField) openField1).getNodeType()) && XlsNodeTypes.XLS_DATA
                        .equals(((DataOpenField) openField2).getNodeType())) {
                    if (!Objects.equals(((DataOpenField) openField1).getUri(), ((DataOpenField) openField2).getUri())) {
                        fieldsToHide.add(i);
                        fieldsToHide.add(j);
                    }
                }
            }
        }
        for (int i = 0; i < fields.size(); i++) {
            if (!fieldsToHide.contains(i)) {
                addField(fields.get(i));
            } else {
                IOpenField f = fields.get(i);
                this.hiddenFields.computeIfAbsent(f.getName(), e -> new ArrayList<>()).add(f);
                this.hiddenLowerCasedFields.computeIfAbsent(f.getName().toLowerCase(), e -> new ArrayList<>()).add(f);
            }
        }
    }

    private void collectDependencyFields(CompiledDependency dependency, List<IOpenField> depFields) {
        CompiledOpenClass compiledOpenClass = dependency.getCompiledOpenClass();
        for (IOpenField depField : compiledOpenClass.getOpenClassWithErrors().getFields()) {
            if (isDependencyFieldInheritable(depField)) {
                depFields.add(depField);
            }
        }
    }

    private void collectDataTables(CompiledDependency dependency, Map<String, ITable> dataTables) {
        IOpenClass openClass = dependency.getCompiledOpenClass().getOpenClassWithErrors();
        if (openClass instanceof XlsModuleOpenClass) {
            XlsModuleOpenClass xlsModuleOpenClass = (XlsModuleOpenClass) openClass;
            if (xlsModuleOpenClass.getDataBase() != null) {
                for (ITable table : xlsModuleOpenClass.getDataBase().getTables()) {
                    if (XlsNodeTypes.XLS_DATA.equals(table.getXlsNodeType())) {
                        if (!dataTables.containsKey(table.getName())) {
                            dataTables.put(table.getName(), table);
                        } else {
                            ITable existingTable = dataTables.get(table.getName());
                            if (existingTable != null && !Objects.equals(existingTable.getUri(), table.getUri())) {
                                dataTables.put(table.getName(), null);
                            }
                        }
                    }
                }
            }
        }
    }

    private void addDataTablesFromDependencies(Map<String, ITable> dataTables) {
        for (ITable table : dataTables.values()) {
            if (table != null) {
                try {
                    getDataBase().registerTable(table);
                } catch (DuplicatedTableException e) {
                    rulesModuleBindingContext.addError(e);
                } catch (OpenlNotCheckedException e) {
                    addError(e);
                }
            }
        }
    }

    private void addDependencyTypes(CompiledDependency dependency) {
        CompiledOpenClass compiledOpenClass = dependency.getCompiledOpenClass();
        for (IOpenClass type : compiledOpenClass.getTypes()) {
            try {
                addType(processDependencyTypeBeforeAdding(type));
            } catch (OpenlNotCheckedException e) {
                addError(e);
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

    /**
     * Add methods form dependent modules to current one.
     *
     * @param dependency compiled dependency module
     */
    protected void addMethods(CompiledDependency dependency) throws DuplicatedMethodException {
        CompiledOpenClass compiledOpenClass = dependency.getCompiledOpenClass();
        for (IOpenMethod dependencyMethod : compiledOpenClass.getOpenClassWithErrors().getMethods()) {
            // filter constructor and getOpenClass methods of dependency modules
            //
            if (!dependencyMethod.isConstructor() && !(dependencyMethod instanceof GetOpenClass)) {
                try {
                    if (isDependencyMethodInheritable(dependencyMethod)) {
                        addMethod(dependencyMethod);
                    }
                } catch (OpenlNotCheckedException e) {
                    LOG.debug("An exception occurred during adding the method '{}'.",
                            MethodUtil.printMethod(dependencyMethod.getName(),
                                    dependencyMethod.getSignature().getParameterTypes()),
                            e);
                    addError(e);
                }
            }
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
        if (openField instanceof ConstantOpenField || openField instanceof DataOpenField && XlsNodeTypes.XLS_DATA
                .equals(((DataOpenField) openField).getNodeType())) {
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

    public XlsMetaInfo getXlsMetaInfo() {
        return (XlsMetaInfo) xlsMetaInfo;
    }

    @Override
    public void addField(IOpenField openField) {
        Map<String, IOpenField> fields = fieldMap();
        IOpenField existedField = fields.get(openField.getName());
        if (existedField != null) {
            if (openField instanceof DataOpenField && existedField instanceof DataOpenField && XlsNodeTypes.XLS_DATA
                    .equals(((DataOpenField) openField).getNodeType()) && XlsNodeTypes.XLS_DATA
                    .equals(((DataOpenField) existedField).getNodeType())) {
                return;
            }
            if (openField instanceof ConstantOpenField && existedField instanceof ConstantOpenField) {
                // Ignore constants with the same values
                if (Objects.equals(((ConstantOpenField) openField).getValue(),
                        ((ConstantOpenField) existedField).getValue()) && openField.getType()
                        .equals(existedField.getType())) {
                    return;
                }
            }
            throw new DuplicatedFieldException("", openField.getName());
        }
        IOpenField f = processFieldBeforeAdding(openField);
        fields.put(f.getName(), f);
        addFieldToLowerCaseMap(f);
    }

    private static String extractContextParameter(IMethodSignature methodSignature, int index) {
        if (methodSignature instanceof MethodSignature) {
            IParameterDeclaration parameterDeclaration = ((MethodSignature) methodSignature)
                    .getParameterDeclaration(index);
            return parameterDeclaration.getContextProperty();
        }
        return null;
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
        IOpenMethod m = WrapperLogic.wrapOpenMethod(method, this, false);

        // Checks that method already exists in the class. If it already
        // exists then "overload" it's using decorator; otherwise - just add to
        // the class.
        //
        IOpenMethod existedMethod = getDeclaredMethod(m.getName(), m.getSignature().getParameterTypes());

        if (existedMethod != null) {
            if (method instanceof TestSuiteMethod) {
                DuplicateMemberThrowExceptionHelper.throwDuplicateMethodExceptionIfMethodsAreNotTheSame(method,
                        existedMethod);
                return;
            }

            if (!existedMethod.getType().equals(m.getType())) {
                String message = String.format("Method '%s' is already defined with another return type '%s'.",
                        MethodUtil.printSignature(m, INamedThing.REGULAR),
                        existedMethod.getType().getDisplayName(0));
                throw new DuplicatedMethodException(message, existedMethod, method);
            }

            IMethodSignature existedMethodSignature = existedMethod.getSignature();
            IMethodSignature candidateMethodSignature = method.getSignature();
            for (int i = 0; i < existedMethodSignature.getNumberOfParameters(); i++) {
                if (!Objects.equals(extractContextParameter(existedMethodSignature, i),
                        extractContextParameter(candidateMethodSignature, i))) {
                    String message = String.format(
                            "Method '%s' is already defined with another set of context parameters.",
                            MethodUtil.printSignature(method, INamedThing.REGULAR));
                    throw new DuplicatedMethodException(message, existedMethod, method);
                }
            }

            // Checks the instance of existed method. If it's the
            // OpenMethodDecorator then just add the method-candidate to
            // decorator; otherwise - replace existed method with new instance
            // of OpenMethodDecorator for existed method and add new one.
            //
            if (existedMethod instanceof OpenMethodDispatcher) {
                super.removeMethod(existedMethod);
                try {
                    OpenMethodDispatcher dispatcher = (OpenMethodDispatcher) existedMethod;
                    dispatcher.addMethod(m);
                } finally {
                    super.addMethod(existedMethod);
                }
            } else {
                if (!m.equals(existedMethod)) {
                    // Create decorator for existed method.
                    //
                    OpenMethodDispatcher dispatcher = getOpenMethodDispatcher(existedMethod);
                    OpenMethodDispatcher wrappedDispatcher = (OpenMethodDispatcher) WrapperLogic
                            .wrapOpenMethod(dispatcher, this, false);
                    wrappedDispatcher.addMethod(m);
                    super.removeMethod(existedMethod);
                    super.addMethod(wrappedDispatcher);
                }
            }
        } else {
            // Just wrap original method with dispatcher functionality.
            //

            if (dispatchingValidationEnabled && !(m instanceof TestSuiteMethod) && isDimensionalPropertyPresented(m)) {
                // Create dispatcher for existed method.
                //
                OpenMethodDispatcher dispatcher = getOpenMethodDispatcher(m);

                IOpenMethod openMethod = WrapperLogic.wrapOpenMethod(dispatcher, this, false);

                super.addMethod(openMethod);

            } else {
                super.addMethod(m);
            }
        }
    }

    private boolean isDimensionalPropertyPresented(IOpenMethod m) {
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
        return new MatchingOpenMethodDispatcher(method, this);
    }

    @Override
    public void clearForExecutionMode() {
        super.clearForExecutionMode();
        dataBase.clearOddDataForExecutionMode();
        rulesModuleBindingContext = null;
        for (IOpenMethod openMethod : getMethods()) {
            clearForExecutionMode(openMethod);
        }
    }

    private void clearForExecutionMode(IOpenMethod openMethod) {
        if (openMethod instanceof OpenMethodDispatcher) {
            for (IOpenMethod candidate : ((OpenMethodDispatcher) openMethod).getCandidates()) {
                clearForExecutionMode(candidate);
            }
        } else if (openMethod instanceof ExecutableRulesMethod) {
            ((ExecutableRulesMethod) openMethod).clearForExecutionMode();
        }
    }

    public void completeOpenClassBuilding() {
        addTestSuiteMethodsFromDependencies(); // Test method from dependencies
        // should use methods from this
        // class.
    }

    private TestSuiteMethod createNewTestSuiteMethod(TestSuiteMethod testSuiteMethod) {
        IOpenMethod method = testSuiteMethod.getTestedMethod();
        IOpenMethod newTargetMethod = getDeclaredMethod(method.getName(), method.getSignature().getParameterTypes());
        if (newTargetMethod == null) {
            newTargetMethod = method;
        }
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
                                "This module does not support adding '%s' custom spreadsheet result types. Bean class has already been initialized for existing custom spreadsheet result type.",
                                csrType.getName()));
                    }
                }
            }
        }
    }

    public boolean isAppliedChangesToClasspath() {
        return appliedChangesToClasspath;
    }

    @Override
    public void addType(IOpenClass type) {
        validateType(type);
        if (type instanceof DomainOpenClass) {
            addType(type.getName(), type, false);
        } else {
            addType(type.getName(), type, true);
            if (type instanceof CustomSpreadsheetResultOpenClass) {
                addType(
                        Spreadsheet.SPREADSHEETRESULT_SHORT_TYPE_PREFIX + type.getName()
                                .substring(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX.length()),
                        type,
                        false);
            } else {
                addType(type.getJavaName(), type, false);
            }
        }
    }

    protected void addTestSuiteMethodsFromDependencies() {
        for (CompiledDependency dependency : this.getDependencies()) {
            for (IOpenMethod dependencyMethod : dependency.getCompiledOpenClass()
                    .getOpenClassWithErrors()
                    .getMethods()) {
                if (dependencyMethod instanceof TestSuiteMethod) {
                    TestSuiteMethod testSuiteMethod = (TestSuiteMethod) dependencyMethod;
                    try {
                        TestSuiteMethod newTestSuiteMethod = createNewTestSuiteMethod(testSuiteMethod);
                        addMethod(newTestSuiteMethod);
                    } catch (OpenlNotCheckedException e) {
                        addError(e);
                    }
                }
            }
        }
    }

    public void addExternalXlsModuleOpenClass(XlsModuleOpenClass xlsModuleOpenClass) {
        if (xlsModuleOpenClass != null) {
            this.externalXlsModuleOpenClasses.add(xlsModuleOpenClass);
        }
    }

    public Collection<XlsModuleOpenClass> getExternalXlsModuleOpenClasses() {
        return Collections.unmodifiableCollection(this.externalXlsModuleOpenClasses);
    }

    private void addError(Throwable e) {
        BindHelper.processError(e, rulesModuleBindingContext);
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public OpenLMessageSource getMessageSource() {
        return messageSource;
    }
}
