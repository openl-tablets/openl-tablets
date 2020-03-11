package org.openl.rules.validation.properties.dimentional;

import java.lang.reflect.Method;
import java.util.*;

import org.openl.binding.IBindingContext;
import org.openl.binding.MethodUtil;
import org.openl.binding.impl.BindingContextDelegator;
import org.openl.engine.OpenLSystemProperties;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.dt.*;
import org.openl.rules.dt.algorithm.IDecisionTableAlgorithm;
import org.openl.rules.lang.xls.XlsHelper;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.DecisionTableMetaInfoReader;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.table.properties.PropertiesLoader;
import org.openl.rules.table.properties.TableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.types.*;
import org.openl.types.impl.*;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder for {@link TableSyntaxNode} that is wrapping generated dispatcher decision table.
 *
 * @author DLiauchuk
 */
class TableSyntaxNodeDispatcherBuilder {

    private final Logger log = LoggerFactory.getLogger(TableSyntaxNodeDispatcherBuilder.class);

    // LinkedHashMap to save the sequence of params
    static final LinkedHashMap<String, IOpenClass> INCOME_PARAMS;
    static final String AUXILIARY_METHOD_DELIMETER = "$";
    private static final String ARGUMENT_PREFIX_IN_SIGNATURE = "arg_";

    /*
     * Initialize a map of parameters from context, that will be used as income parameters to newly created dispatcher
     * tables.
     *
     */
    static {
        INCOME_PARAMS = new LinkedHashMap<>();
        Method[] methods = IRulesRuntimeContext.class.getDeclaredMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.startsWith("get") && !belongsToExcluded(methodName)) {
                String fieldName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
                INCOME_PARAMS.put(fieldName, JavaOpenClass.getOpenClass(method.getReturnType()));
            }
        }
    }

    private RulesModuleBindingContext rulesModuleBindingContext;
    private XlsModuleOpenClass moduleOpenClass;
    private MatchingOpenMethodDispatcher dispatcher;

    TableSyntaxNodeDispatcherBuilder(RulesModuleBindingContext rulesModuleBindingContext,
            XlsModuleOpenClass moduleOpenClass,
            MatchingOpenMethodDispatcher dispatcher) {
        if (rulesModuleBindingContext == null || moduleOpenClass == null || dispatcher == null) {
            throw new IllegalArgumentException("None of the constructor parameters can be null");
        }
        this.rulesModuleBindingContext = rulesModuleBindingContext;
        this.moduleOpenClass = moduleOpenClass;
        this.dispatcher = dispatcher;
    }

    static String getDispatcherParameterNameForOriginalParameter(String parameterName) {
        return ARGUMENT_PREFIX_IN_SIGNATURE + parameterName;
    }

    private static IDecisionTableColumn makeColumn(TablePropertyDefinition dimensionProperty,
            DispatcherTableRules rules) {
        if (dimensionProperty.getType().isArray()) {
            return new ArrayParameterColumn(dimensionProperty, rules);
        } else {
            return new SimpleParameterColumn(dimensionProperty, rules);
        }
    }

    /**
     * Exclude those methods, that are not used as context variables.
     */
    private static boolean belongsToExcluded(String methodName) {
        boolean result = false;
        if ("getValue".equals(methodName)) {
            result = true;
        }
        return result;
    }

    /**
     * Build dispatcher table for dimensional properties for particular overloaded method group.
     */
    TableSyntaxNode build() {
        TableSyntaxNode tsn = null;
        if (needToBuild()) {
            // build source of decision table
            //
            // properties values from methods in group that will be used
            // to build dispatcher table by dimensional properties.
            //
            List<ITableProperties> propertiesFromMethods = getMethodsProperties();
            DispatcherTableRules rules = new DispatcherTableRules(propertiesFromMethods);
            List<IDecisionTableColumn> conditions = getConditions(propertiesFromMethods, rules);
            DispatcherTableReturnColumn returnColumn = new DispatcherTableReturnColumn(dispatcher.getType(),
                dispatcher.getName(),
                dispatcher.getSignature());

            DecisionTableBuilder decisionTableBuilder = new DecisionTableBuilder();
            decisionTableBuilder.setConditions(conditions);
            decisionTableBuilder.setReturnColumn(returnColumn);
            decisionTableBuilder.setTableName(getDispatcherTableName());
            decisionTableBuilder.setMethodName(getMethodName());
            decisionTableBuilder.setRulesNumber(rules.getRulesNumber());

            XlsSheetGridModel sheetWithTable = decisionTableBuilder.build();
            IGridTable decisionTableSource = sheetWithTable.getTables()[0];
            XlsSheetSourceCodeModule sheetSource = sheetWithTable.getSheetSource();

            // build TableSyntaxNode
            //
            try {
                tsn = XlsHelper.createTableSyntaxNode(decisionTableSource, sheetSource);
            } catch (OpenLCompilationException e) {
                rulesModuleBindingContext.addMessages(OpenLMessagesUtils.newErrorMessages(e));
                return null;
            }

            // build Openl decision table
            //
            IOpenClass originalReturnType = getMethodReturnType();
            Map<String, IOpenClass> updatedIncomeParams = updateIncomeParams();

            // table name for dispatcher table
            String tableName = getDispatcherTableName();

            IParameterDeclaration[] params = new IParameterDeclaration[updatedIncomeParams.size()];
            int i = 0;
            for (Map.Entry<String, IOpenClass> field : updatedIncomeParams.entrySet()) {
                params[i] = new ParameterDeclaration(field.getValue(), field.getKey());
                i++;
            }
            IMethodSignature signature = new MethodSignature(params);
            OpenMethodHeader header = new OpenMethodHeader(tableName, originalReturnType, signature, moduleOpenClass);

            DecisionTableBoundNode boundNode = null;
            if (moduleOpenClass != null) {
                boundNode = new DecisionTableBoundNode(tsn, moduleOpenClass.getOpenl(), header, moduleOpenClass);
            }
            DecisionTable decisionTable = new DecisionTable(header, boundNode);
            // Dispatcher tables are shown in Trace
            tsn.setMetaInfoReader(
                new DecisionTableMetaInfoReader((DecisionTableBoundNode) decisionTable.getBoundNode(), decisionTable));

            loadCreatedTable(decisionTable, tsn);

            dispatcher.setDecisionTableOpenMethod(decisionTable);

            IDecisionTableAlgorithm algorithm = decisionTable.getAlgorithm();
            if (algorithm != null) {
                algorithm.cleanParamValuesForIndexedConditions();
            }

            if (rulesModuleBindingContext.isExecutionMode()) {
                removeDebugInformation(decisionTable, tsn);
            }
        }
        return tsn;
    }

    private void removeDebugInformation(DecisionTable decisionTable, TableSyntaxNode tsn) {
        decisionTable.setBoundNode(null);

        clearCompositeMethods(decisionTable);

        if (!OpenLSystemProperties.isDTDispatchingMode(rulesModuleBindingContext.getExternalParams())) {
            tsn.setMember(null);
        }
    }

    private void clearCompositeMethods(DecisionTable decisionTable) {
        // TODO consider more understandable implementation
        for (IBaseCondition condition : decisionTable.getConditionRows()) {
            condition.removeDebugInformation();
        }

        for (IBaseAction action : decisionTable.getActionRows()) {
            action.removeDebugInformation();
        }
    }

    /**
     * Build the table only if there is any property value for any table.
     *
     * @return flag if it is needed to build the table for given methods properties
     */
    private boolean needToBuild() {
        List<TablePropertyDefinition> dimensionalPropertiesDef = TablePropertyDefinitionUtils
            .getDimensionalTableProperties();

        List<ITableProperties> propertiesFromMethods = getMethodsProperties();

        for (TablePropertyDefinition dimensionProperty : dimensionalPropertiesDef) {
            if (isSuitable(dimensionProperty.getName(), propertiesFromMethods)) {
                return true;
            }
        }
        return false;
    }

    private List<IDecisionTableColumn> getConditions(List<ITableProperties> propertiesFromMethods,
            DispatcherTableRules rules) {

        List<TablePropertyDefinition> dimensionalPropertiesDef = TablePropertyDefinitionUtils
            .getDimensionalTableProperties();

        List<IDecisionTableColumn> conditions = new ArrayList<>();

        // get only dimensional properties from methods properties
        //
        for (TablePropertyDefinition dimensionProperty : dimensionalPropertiesDef) {
            if (isSuitable(dimensionProperty.getName(), propertiesFromMethods)) {
                conditions.add(makeColumn(dimensionProperty, rules));
            }
        }
        return conditions;
    }

    /**
     * Checks if there is any value of particular property represented in collection, that will be used as rules. If no,
     * we don`t need to create column for this property.
     */
    private boolean isPropertyPresented(String propertyName, List<ITableProperties> methodsProperties) {
        for (ITableProperties properties : methodsProperties) {
            if (StringUtils.isNotEmpty(properties.getPropertyValueAsString(propertyName))) {
                return true;
            }
        }

        return false;
    }

    private IOpenMethod getMember() {
        // as we have a group of overloaded methods, we need to take one it`s
        // member to get all common settings for the whole group
        return dispatcher.getCandidates().get(0);
    }

    /**
     * Generates name for dispatcher table being created.
     *
     * @return name for creating dispatcher table.
     */
    private String getDispatcherTableName() {
        String originalTableName = getMethodName();

        // table name for dispatcher table.
        return String.format("%s_%s", DispatcherTablesBuilder.DEFAULT_DISPATCHER_TABLE_NAME, originalTableName);
    }

    private Map<String, IOpenClass> updateIncomeParams() {
        // LinkedHashMap to save the sequence of params
        LinkedHashMap<String, IOpenClass> updatedIncomeParams = new LinkedHashMap<>();
        IMethodSignature originalSignature = getMethodSignature();
        for (int j = 0; j < originalSignature.getNumberOfParameters(); j++) {
            updatedIncomeParams.put(
                getDispatcherParameterNameForOriginalParameter(originalSignature.getParameterName(j)),
                originalSignature.getParameterType(j));
        }
        updatedIncomeParams.putAll(INCOME_PARAMS);
        return updatedIncomeParams;
    }

    /**
     * Gets properties values from methods in group that will be used to build dispatcher table by dimensional
     * properties.
     *
     * @return properties values from tables in group.
     */
    private List<ITableProperties> getMethodsProperties() {
        List<ITableProperties> propertiesValues = new ArrayList<>();
        for (IOpenMethod method : dispatcher.getCandidates()) {
            propertiesValues.add(PropertiesHelper.getTableProperties(method));
        }
        return propertiesValues;
    }

    /**
     * As all methods in group have the similar name, so it is possible do get any member and get it`s name.
     *
     * @return name of the method in group.
     */
    private String getMethodName() {
        return getMember().getName();
    }

    /**
     * As all methods in group have the similar type, so it is possible do get any member and get it`s type.
     *
     * @return type of the method in group.
     */
    private IOpenClass getMethodReturnType() {
        return getMember().getType();
    }

    /**
     * As all methods in group have the similar signature, so it is possible do get any member and get it`s signature.
     *
     * @return method signature of the method in group.
     */
    private IMethodSignature getMethodSignature() {
        return dispatcher.getSignature();
    }

    /**
     * Load and bind the decision table by OpenL.
     *
     * @param decisionTable created decision table.
     * @param tsn created table syntax node.
     */
    private void loadCreatedTable(DecisionTable decisionTable, TableSyntaxNode tsn) {
        tsn.setMember(decisionTable);

        PropertiesLoader propLoader = new PropertiesLoader(moduleOpenClass.getOpenl(),
            rulesModuleBindingContext,
            moduleOpenClass);
        propLoader.loadDefaultProperties(tsn);

        setTableProperties(tsn);

        DecisionTableLoader dtLoader = new DecisionTableLoader();
        try {
            dtLoader
                .loadAndBind(tsn, decisionTable, moduleOpenClass.getOpenl(), null, createContextWithAuxiliaryMethods());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            rulesModuleBindingContext.addMessages(OpenLMessagesUtils.newErrorMessages(e));
        }
    }

    private IOpenMethod generateAuxiliaryMethod(final IOpenMethod originalMethod, int index) {
        final String auxiliaryMethodName = originalMethod.getName() + AUXILIARY_METHOD_DELIMETER + index;
        return new InternalMethodDelegator(originalMethod, auxiliaryMethodName);
    }

    private IBindingContext createContextWithAuxiliaryMethods() {
        List<IOpenMethod> candidates = dispatcher.getCandidates();
        final Map<MethodKey, IOpenMethod> auxiliaryMethods = new HashMap<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            IOpenMethod auxiliaryMethod = generateAuxiliaryMethod(candidates.get(i), i);
            auxiliaryMethods.put(new MethodKey(auxiliaryMethod), auxiliaryMethod);
        }
        return new InternalBindingContextDelegator(rulesModuleBindingContext, auxiliaryMethods);
    }

    /**
     * Set properties to newly created table syntax node.
     */
    private void setTableProperties(TableSyntaxNode tsn) {
        TableProperties properties = (TableProperties) tsn.getTableProperties();
        properties.setFieldValue("category", "Autogenerated - Dispatch by Properties");

        StringBuilder buf = new StringBuilder(250);
        buf.append(" Automatically created table to dispatch by dimensional properties values for method: ");
        MethodUtil.printMethod(getMember(), buf);
        buf.append(". Please, edit original tables to make any change to the overloading logic.");
        properties.setFieldValue("description", buf.toString());
    }

    private boolean isSuitable(String dimensionPropertyName, List<ITableProperties> methodsProperties) {
        return isPropertyPresented(dimensionPropertyName, methodsProperties) && !"origin".equals(dimensionPropertyName);
    }

    private static class InternalMethodDelegator extends MethodDelegator {
        String auxiliaryMethodName;

        InternalMethodDelegator(IMethodCaller methodCaller, String auxiliaryMethodName) {
            super(methodCaller);
            this.auxiliaryMethodName = auxiliaryMethodName;
        }

        @Override
        public String getName() {
            return auxiliaryMethodName;
        }
    }

    private static class InternalBindingContextDelegator extends BindingContextDelegator {

        private Map<MethodKey, IOpenMethod> auxiliaryMethods;

        InternalBindingContextDelegator(RulesModuleBindingContext context,
                Map<MethodKey, IOpenMethod> auxiliaryMethods) {
            super(context);
            this.auxiliaryMethods = auxiliaryMethods;
        }

        @Override
        public IMethodCaller findMethodCaller(String namespace, String name, IOpenClass[] parTypes) {
            IOpenMethod auxiliaryMethod = auxiliaryMethods.get(new MethodKey(name, parTypes));
            if (auxiliaryMethod == null) {
                return super.findMethodCaller(namespace, name, parTypes);
            } else {
                return auxiliaryMethod;
            }
        }
    }

}
