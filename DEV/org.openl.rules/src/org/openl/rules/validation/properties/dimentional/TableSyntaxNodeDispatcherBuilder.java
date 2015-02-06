package org.openl.rules.validation.properties.dimentional;

import org.apache.commons.lang3.StringUtils;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.MethodUtil;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.impl.BindingContextDelegator;
import org.openl.engine.OpenLSystemProperties;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.DecisionTableHelper;
import org.openl.rules.dt.DecisionTableLoader;
import org.openl.rules.dt.algorithm.DecisionTableOptimizedAlgorithm;
import org.openl.rules.dt.builder.ConditionsBuilder;
import org.openl.rules.dt.builder.DecisionTableBuilder;
import org.openl.rules.dt.builder.ReturnColumnBuilder;
import org.openl.rules.dt.builder.TableHeaderBuilder;
import org.openl.rules.dt.element.IAction;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.element.IDecisionRow;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.Point;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.table.properties.PropertiesLoader;
import org.openl.rules.table.properties.TableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.MethodDelegator;
import org.openl.types.impl.MethodKey;
import org.openl.types.java.JavaOpenClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Builder for {@link TableSyntaxNode} that is wrapping generated dispatcher decision table.
 *
 * @author DLiauchuk
 */
public class TableSyntaxNodeDispatcherBuilder implements Builder<TableSyntaxNode> {

    private static final String DISPATCHER_TABLES_SHEET_FORMAT = "$%sDispatcher Tables Sheet";
    public static final String ARGUMENT_PREFIX_IN_SIGNATURE = "arg_";
    private static final String VIRTUAL_EXCEL_FILE = "/VIRTUAL_EXCEL_FILE_FOR_DISPATCHER_TABLES.xls";

    private final Logger log = LoggerFactory.getLogger(TableSyntaxNodeDispatcherBuilder.class);

    //LinkedHashMap to save the sequence of params
    public static final LinkedHashMap<String, IOpenClass> incomeParams;

    /**
     * Initialize a map of parameters from context, that will be used as income parameters to newly 
     * created dispatcher tables.
     *
     */
    static {
        incomeParams = new LinkedHashMap<String, IOpenClass>();
        Method[] methods = IRulesRuntimeContext.class.getDeclaredMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.startsWith("get") && !belongsToExcluded(methodName)) {
                String fieldName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
                incomeParams.put(fieldName, JavaOpenClass.getOpenClass(method.getReturnType()));
            }
        }
    }

    private RulesModuleBindingContext moduleContext;
    private XlsModuleOpenClass moduleOpenClass;
    private MatchingOpenMethodDispatcher dispatcher;

    public TableSyntaxNodeDispatcherBuilder(RulesModuleBindingContext moduleContext,
                                            XlsModuleOpenClass moduleOpenClass, MatchingOpenMethodDispatcher dispatcher) {
        if (moduleContext == null || moduleOpenClass == null || dispatcher == null) {
            throw new IllegalArgumentException("None of the constructor parameters can be null");
        }
        this.moduleContext = moduleContext;
        this.moduleOpenClass = moduleOpenClass;
        this.dispatcher = dispatcher;
    }

    public static String getDispatcherParameterNameForOriginalParameter(String parameterName) {
        return ARGUMENT_PREFIX_IN_SIGNATURE + parameterName;
    }

    /**
     * Build dispatcher table for dimensional properties for particular overloaded method group.
     */
    public TableSyntaxNode build() {
        TableSyntaxNode tsn = null;
        if (needToBuild()) {
            // build source of decision table
            //
            Builder<IWritableGrid> dtSourceBuilder = initDecisionTableSourceBuilder();
            XlsSheetGridModel sheetWithTable = (XlsSheetGridModel) dtSourceBuilder.build();
            IGridTable decisionTableSource = sheetWithTable.getTables()[0];

            // build TableSyntaxNode
            //
            Builder<TableSyntaxNode> tsnBuilder = new TableSyntaxNodeBuilder(XlsNodeTypes.XLS_DT.toString(), sheetWithTable.getSheetSource(),
                    decisionTableSource);
            tsn = tsnBuilder.build();

            // build Openl decision table
            //
            Builder<DecisionTable> dtOpenLBuilder = initDecisionTableOpenlBuilder(tsn);
            DecisionTable decisionTable = dtOpenLBuilder.build();

            loadCreatedTable(decisionTable, tsn);

            if (moduleContext.isExecutionMode()){
                removeDebugInformation(decisionTable, tsn);
            }
        }
        return tsn;
    }

    private void removeDebugInformation(DecisionTable decisionTable, TableSyntaxNode tsn) {
        decisionTable.setBoundNode(null);

        DecisionTableOptimizedAlgorithm algorithm = decisionTable.getAlgorithm();
        if (algorithm != null) {
            algorithm.removeParamValuesForIndexedConditions();
        }

        clearCompositeMethods(decisionTable);

        if (!OpenLSystemProperties.isDTDispatchingMode(moduleContext.getExternalParams())) {
            tsn.setMember(null);
        }
    }

    private void clearCompositeMethods(DecisionTable decisionTable) {
        // TODO consider more understandable implementation
        for (ICondition condition : decisionTable.getConditionRows()) {
            clearDecisionRow(condition);
        }

        for (IAction action : decisionTable.getActionRows()) {
            clearDecisionRow(action);

            Object[][] paramValues = action.getParamValues();
            if (paramValues != null) {
                for (Object[] paramValueColumn : paramValues) {
                    for (Object paramValue : paramValueColumn) {
                        if (paramValue instanceof CompositeMethod) {
                            ((CompositeMethod) paramValue).removeDebugInformation();
                        }
                    }
                }
            }
        }
    }

    private void clearDecisionRow(IDecisionRow decisionRow) {
        IOpenMethod method = decisionRow.getMethod();
        if (method instanceof CompositeMethod) {
            ((CompositeMethod) method).removeDebugInformation();
        }
    }

    /**
     * Build the table only if there is any property value for any table.
     *
     * @return flag if it is needed to build the table for given methods properties
     */
    public boolean needToBuild() {
        List<TablePropertyDefinition> dimensionalPropertiesDef =
                TablePropertyDefinitionUtils.getDimensionalTableProperties();

        List<ITableProperties> propertiesFromMethods = getMethodsProperties();

        for (TablePropertyDefinition dimensionProperty : dimensionalPropertiesDef) {
            if (isPropertyPresented(dimensionProperty.getName(), propertiesFromMethods)) {
                return true;
            }
        }
        return false;
    }

    private int getNumberOfColumns(List<IDecisionTableColumn> conditions) {
        int numberOfAllLocalParameters = 0;
        for (int i = 0; i < conditions.size(); i++) {
            if (conditions.get(i).getNumberOfLocalParameters() > 0) {
                numberOfAllLocalParameters += conditions.get(i).getNumberOfLocalParameters();
            }
        }
        return numberOfAllLocalParameters;
    }

    private DecisionTableOpenlBuilder initDecisionTableOpenlBuilder(TableSyntaxNode tsn) {
        IOpenClass originalReturnType = getMethodReturnType();
        Map<String, IOpenClass> updatedIncomeParams = updateIncomeParams();

        // table name for dispatcher table
        String tableName = getDispatcherTableName();

        DecisionTableOpenlBuilder dtOpenLBuilder = new DecisionTableOpenlBuilder(tableName, originalReturnType, updatedIncomeParams);
        dtOpenLBuilder.setTableSyntaxNode(tsn);
        dtOpenLBuilder.setModuleOpenClass(moduleOpenClass);
        return dtOpenLBuilder;
    }

    private DecisionTableBuilder initDecisionTableSourceBuilder() {
        // properties values from methods in group that will be used
        // to build dispatcher table by dimensional properties.
        //
        List<ITableProperties> propertiesFromMethods = getMethodsProperties();

        DispatcherTableRules rules = new DispatcherTableRules(propertiesFromMethods);

        List<IDecisionTableColumn> conditions = getConditions(propertiesFromMethods, rules);

        int numberOfColumns = getNumberOfColumns(conditions) + 1; // + 1 for return column

        // TODO Excel has a maximum sheet name length limit. Find a solution for case when name is longer.
        String sheetName = String.format(DISPATCHER_TABLES_SHEET_FORMAT, getMethodName());
        IWritableGrid sheetForTable = DecisionTableHelper.createVirtualGrid(VIRTUAL_EXCEL_FILE, sheetName,
                numberOfColumns);

        DispatcherTableReturnColumn returnColumn = getReturnColumn();

        DecisionTableBuilder decisionTableBuilder = new DecisionTableBuilder(new Point(0, 0));

        decisionTableBuilder.setConditionsBuilder(new ConditionsBuilder(conditions));
        decisionTableBuilder.setReturnBuilder(new ReturnColumnBuilder(returnColumn));
        decisionTableBuilder.setHeaderBuilder(new TableHeaderBuilder(buildMethodHeader(getDispatcherTableName(),
                returnColumn)));
        decisionTableBuilder.setSheetWithTable(sheetForTable);
        decisionTableBuilder.setRulesNumber(rules.getRulesNumber());

        return decisionTableBuilder;
    }

    private String buildMethodHeader(String tableName, DispatcherTableReturnColumn returnColumn) {
        String start = String.format("%s %s %s(", IXlsTableNames.DECISION_TABLE2,
                returnColumn.getReturnType().getDisplayName(0), tableName);
        StringBuilder strBuf = new StringBuilder();
        strBuf.append(start);
        strBuf.append(returnColumn.paramsThroughComma());
        strBuf.append(")");

        return strBuf.toString();
    }

    private List<IDecisionTableColumn> getConditions(List<ITableProperties> propertiesFromMethods,
                                                     DispatcherTableRules rules) {

        List<TablePropertyDefinition> dimensionalPropertiesDef =
                TablePropertyDefinitionUtils.getDimensionalTableProperties();

        List<IDecisionTableColumn> conditions = new ArrayList<IDecisionTableColumn>();

        // get only dimensional properties from methods properties
        //
        for (TablePropertyDefinition dimensionProperty : dimensionalPropertiesDef) {
            if (isPropertyPresented(dimensionProperty.getName(), propertiesFromMethods)) {
                conditions.add(DispatcherTableColumnMaker.makeColumn(dimensionProperty, rules));
            }
        }
        return conditions;
    }

    private DispatcherTableReturnColumn getReturnColumn() {
        return new DispatcherTableReturnColumn(dispatcher, incomeParams);
    }

    /**
     * Checks if there is any value of particular property represented in collection, that will be used as rules.
     * If no, we don`t need to create column for this property.
     */
    private boolean isPropertyPresented(String propertyName, List<ITableProperties> methodsProperties) {
        boolean isPropertyPresented = false;

        for (ITableProperties properties : methodsProperties) {
            String propertyValue = properties.getPropertyValueAsString(propertyName);
            if (StringUtils.isNotEmpty(propertyValue)) {
                isPropertyPresented = true;
                break;
            }
        }

        return isPropertyPresented;
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
        //LinkedHashMap to save the sequence of params
        LinkedHashMap<String, IOpenClass> updatedIncomeParams = new LinkedHashMap<String, IOpenClass>();
        IMethodSignature originalSignature = getMethodSignature();
        for (int j = 0; j < originalSignature.getNumberOfParameters(); j++) {
            updatedIncomeParams.put(getDispatcherParameterNameForOriginalParameter(originalSignature.getParameterName(j)),
                    originalSignature.getParameterType(j));
        }
        updatedIncomeParams.putAll(incomeParams);
        return updatedIncomeParams;
    }

    /**
     * Gets properties values from methods in group that will be
     * used to build dispatcher table by dimensional properties.
     *
     * @return properties values from tables in group.
     */
    private List<ITableProperties> getMethodsProperties() {
        List<ITableProperties> propertiesValues = new ArrayList<ITableProperties>();
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
     * @param tsn           created table syntax node.
     */
    private TableSyntaxNode loadCreatedTable(DecisionTable decisionTable, TableSyntaxNode tsn) {
        tsn.setMember(decisionTable);

        PropertiesLoader propLoader = new PropertiesLoader(moduleOpenClass.getOpenl(), moduleContext, moduleOpenClass);
        propLoader.loadDefaultProperties(tsn);

        setTableProperties(tsn);

        DecisionTableLoader dtLoader = new DecisionTableLoader();
        try {
            dtLoader.loadAndBind(tsn, decisionTable, moduleOpenClass.getOpenl(), null, createContextWithAuxiliaryMethods());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            OpenLMessagesUtils.addWarn(e.getMessage(), tsn);
        }
        return tsn;
    }

    public static final String AUXILIARY_METHOD_DELIMETER = "$";

    private static class InternalMethodDelegator extends MethodDelegator {
        String auxiliaryMethodName;
        
        public InternalMethodDelegator(IMethodCaller methodCaller, String auxiliaryMethodName) {
            super(methodCaller);
            this.auxiliaryMethodName = auxiliaryMethodName;
        }
        
        @Override
        public String getName() {
            return auxiliaryMethodName;
        }
    }
    
    private IOpenMethod generateAuxiliaryMethod(final IOpenMethod originalMethod, int index) {
        final String auxiliaryMethodName = originalMethod.getName() + AUXILIARY_METHOD_DELIMETER + index;
        return new InternalMethodDelegator(originalMethod, auxiliaryMethodName);
    }
    
    private static class InternalBindingContextDelegator extends BindingContextDelegator{
        
        private Map<MethodKey, IOpenMethod> auxiliaryMethods;
        
        public InternalBindingContextDelegator(RulesModuleBindingContext context, Map<MethodKey, IOpenMethod> auxiliaryMethods) {
            super(context);
            this.auxiliaryMethods = auxiliaryMethods;
        }
        
        @Override
        public IMethodCaller findMethodCaller(String namespace, String name, IOpenClass[] parTypes) throws AmbiguousMethodException {
            IOpenMethod auxiliaryMethod = auxiliaryMethods.get(new MethodKey(name, parTypes));
            if (auxiliaryMethod == null) {
                return super.findMethodCaller(namespace, name, parTypes);
            } else {
                return auxiliaryMethod;
            }
        }
    }

    private IBindingContextDelegator createContextWithAuxiliaryMethods() {
        List<IOpenMethod> candidates = dispatcher.getCandidates();
        final Map<MethodKey, IOpenMethod> auxiliaryMethods = new HashMap<MethodKey, IOpenMethod>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            IOpenMethod auxiliaryMethod = generateAuxiliaryMethod(candidates.get(i), i);
            auxiliaryMethods.put(new MethodKey(auxiliaryMethod), auxiliaryMethod);
        }
        return new InternalBindingContextDelegator(moduleContext, auxiliaryMethods);
    }

    /**
     * Set properties to newly created table syntax node.
     */
    private void setTableProperties(TableSyntaxNode tsn) {
        TableProperties properties = (TableProperties) tsn.getTableProperties();
        properties.setFieldValue("category", "Autogenerated - Dispatch by Properties");
        properties.setFieldValue("description",
                " Automatically created table to dispatch by dimensional properties values for method: "
                        + MethodUtil.printMethod(getMember(), 0, true)
                        + ". Please, edit original tables to make any change to the overloading logic.");
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

}
