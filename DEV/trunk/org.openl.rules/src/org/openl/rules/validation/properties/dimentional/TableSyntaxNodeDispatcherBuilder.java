package org.openl.rules.validation.properties.dimentional;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Sheet;
import org.openl.OpenL;
import org.openl.binding.MethodUtil;
import org.openl.binding.impl.MethodUsagesSearcher.MethodUsage;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.DecisionTableLoader;
import org.openl.rules.dt.element.IAction;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesLoader;
import org.openl.rules.table.properties.TableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.xls.XlsSheetGridHelper;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * Builder for {@link TableSyntaxNode} that is wrapping generated dispatcher decision table.
 * 
 * @author DLiauchuk
 *
 */
public class TableSyntaxNodeDispatcherBuilder {
    
    private static Log LOG = LogFactory.getLog(TableSyntaxNodeDispatcherBuilder.class);
    
    private static final Map<String, IOpenClass> incomeParams;
    
    /**
     * Initialize a map of parameters from context, that will be used as income parameters to newly 
     * created dispatcher tables.
     * 
     */
    static {
        incomeParams = new HashMap<String, IOpenClass>();
        Method[] methods = IRulesRuntimeContext.class.getDeclaredMethods();
        for (Method method : methods) {
            String methodName = method.getName(); 
            if(methodName.startsWith("get") && !belongsToExcluded(methodName)) {
                String fieldName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
                incomeParams.put(fieldName, JavaOpenClass.getOpenClass(method.getReturnType()));
            }
        }
    }
    
    /**
     * Exclude those methods, that are not used as context variables.
     * 
     * @param methodName
     * @return
     */
    private static boolean belongsToExcluded(String methodName) {
        boolean result = false;
        if ("getValue".equals(methodName)) {
            result = true;
        }
        return result;
    } 
    
    private static String VIRTUAL_EXCEL_FILE = "/FAKE_EXCEL_FILE_FOR_DISPATCHER_TABLES.xls";
    
    private OpenL openl;
    private RulesModuleBindingContext moduleContext;
    private XlsModuleOpenClass moduleOpenClass;
    private List<ExecutableRulesMethod> methodsGroup;
    
    public TableSyntaxNodeDispatcherBuilder(OpenL openl, RulesModuleBindingContext moduleContext, 
            XlsModuleOpenClass moduleOpenClass, List<ExecutableRulesMethod> methodsGroup) {
        this.openl = openl;
        this.moduleContext = moduleContext;
        this.moduleOpenClass = moduleOpenClass;
        this.methodsGroup = new ArrayList<ExecutableRulesMethod>(methodsGroup);
    }
    
    /**
     * Build dispatcher table for dimensional properties for particular overloaded method group.
     * 
     * @param methodsGroup group of overloaded tables.
     */
    public TableSyntaxNode build() {
        
        XlsSheetGridModel sheetGridModel = createSheetGridModel();
        
        // build TableSyntaxNode
        TableSyntaxNode tsn = new TableSyntaxNodeBuilder(XlsNodeTypes.XLS_DT.toString(), sheetGridModel,
                sheetGridModel.getTables()[0]).build();
        
        // build Openl decision table
        DecisionTable decisionTable = initDTOpenlBuilder().build(tsn, openl, moduleOpenClass);
        
        loadCreatedTable(decisionTable, tsn);
        addCellMetaInfo(decisionTable, tsn);
        
        return tsn;
    }
    
    /**
     * Sets meta info into cells with correct links to overloaded methods.
     * 
     * @param decisionTable generated {@link DecisionTable} of the dispatcher
     *            table.
     * @param tsn generated {@link TableSyntaxNode} of the dispatcher table.
     */
    private void addCellMetaInfo(DecisionTable decisionTable, TableSyntaxNode tsn){
        IAction action = decisionTable.getActionRows()[0];
        for (int i = 0; i < methodsGroup.size(); i++) {
            ILogicalTable valueCell = action.getValueCell(i);
            List<MethodUsage> usedMethods = new ArrayList<MethodUsage>();
            ExecutableRulesMethod executableRulesMethod = methodsGroup.get(i);
            usedMethods.add(new MethodUsage(1, executableRulesMethod.getMethod().getName().length(),
                    executableRulesMethod));
            if (valueCell.getSource().getCell(0, 0).getMetaInfo() != null) {
                valueCell.getSource().getCell(0, 0).getMetaInfo().setUsedMethods(usedMethods);
            } else {
                RuleRowHelper.setCellMetaInfo(valueCell, null, getMethodReturnType(), false, usedMethods);
            }
        }
    }

    private XlsSheetGridModel createSheetGridModel() {
        // create the table by POI builder. gets the sheet containing this table.
        Sheet sheetWithTable = initDTPOIBuilder().build();
        
        return XlsSheetGridHelper.createVirtualGrid(sheetWithTable, VIRTUAL_EXCEL_FILE);        
    }
    
    private DecisionTableOpenlBuilder initDTOpenlBuilder() {
        IOpenClass originalReturnType = getMethodReturnType();
        Map<String, IOpenClass> updatedIncomeParams = updateIncomeParams();
        
        // table name for dispatcher table
        String tableName = getDispatcherTableName();
        
        return new DecisionTableOpenlBuilder(tableName, originalReturnType, updatedIncomeParams);
    }
    
    /**
     * Initialize POI table builder with columns and return column, number of rules.
     *     
     * @return {@link DecisionTablePOIBuilder}
     */
    private DecisionTablePOIBuilder initDTPOIBuilder() {
        // properties values from methods in group that will be used 
        // to build dispatcher table by dimensional properties.
        //
        List<ITableProperties> propertiesFromMethods = getMethodsProperties();
        
        DispatcherTableRules rules = new DispatcherTableRules(propertiesFromMethods);

        List<IDecisionTableColumn> conditions = getDispatcherTableConditions(propertiesFromMethods, rules);       
        
        // table name for dispatcher table
        //
        String newTableName = getDispatcherTableName();
        DispatcherTableReturnColumn returnColumn = getReturnColumn();
        
        return new DecisionTablePOIBuilder(newTableName, conditions, returnColumn, rules.getRulesNumber());
    }
    
    private List<IDecisionTableColumn> getDispatcherTableConditions(List<ITableProperties> propertiesFromMethods, 
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
        return new DispatcherTableReturnColumn(getMember(), incomeParams);        
    }
    
    /**
     * Checks if there is any value of particular property represented in collection, that will be used as rules.
     * If no, we don`t need to create column for this property.
     * 
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

    private ExecutableRulesMethod getMember() {
        // as we have a group of overloaded methods, we need to take one it`s 
        // member to get all common settings for the whole group
        return methodsGroup.get(0);        
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
        Map<String, IOpenClass> updatedIncomeParams = new HashMap<String, IOpenClass>(incomeParams);
        IMethodSignature originalSignature = getMethodSignature();
        for (int j = 0; j < originalSignature.getNumberOfParameters(); j++) {
            updatedIncomeParams.put(originalSignature.getParameterName(j), originalSignature.getParameterType(j));
        }        
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
        for (ExecutableRulesMethod method : methodsGroup) {
            if (method.getMethodProperties() != null) {
                propertiesValues.add(method.getMethodProperties());
            }            
        }
        return propertiesValues;
    }
    
    /**
     * As all methods in group have the similar name, so it is possible do get any member and get it`s name.
     *
     * @return name of the method in group.
     */
    private String getMethodName() {
        return getMember().getHeader().getName();
    }
    
    /**
     * As all methods in group have the similar type, so it is possible do get any member and get it`s type.
     *
     * @return type of the method in group.
     */
    private IOpenClass getMethodReturnType() {        
        return getMember().getHeader().getType();        
    }
    
    /**
     * As all methods in group have the similar signature, so it is possible do get any member and get it`s signature.
     *
     * @return method signature of the method in group.
     */
    private IMethodSignature getMethodSignature() {        
        return getMember().getHeader().getSignature();
    }
    
    /**
     * Load and bind the decision table by OpenL.
     * 
     * @param decisionTable created decision table.
     * @param tsn created table syntax node.
     */
    private TableSyntaxNode loadCreatedTable(DecisionTable decisionTable, TableSyntaxNode tsn) {
        tsn.setMember(decisionTable);
        
        PropertiesLoader propLoader = new PropertiesLoader(openl, moduleContext, (XlsModuleOpenClass)moduleOpenClass);
        propLoader.loadDefaultProperties(tsn);
        
        setTableProperties(tsn);
          
        DecisionTableLoader dtLoader = new DecisionTableLoader();
        try {
            dtLoader.loadAndBind(tsn, decisionTable, openl, null, moduleContext);            
        } catch (Exception e) {            
            LOG.error(e);
        }
        return tsn;
    }
    
    /**
     * Set properties to newly created table syntax node.
     * 
     */
    private void setTableProperties(TableSyntaxNode tsn) {
        TableProperties properties = (TableProperties) tsn.getTableProperties();
        properties.setFieldValue("category", "Autogenerated - Dispatch by Properties");
        properties.setFieldValue("description",
                " Automatically created table to dispatch by dimensional properties values for method: "
                        + MethodUtil.printMethod(getMember(), 0, true)
                        + ". Please, edit original tables to make any change to the overloading logic.");
    }

}
