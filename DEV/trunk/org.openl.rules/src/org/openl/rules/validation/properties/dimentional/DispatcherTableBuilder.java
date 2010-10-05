package org.openl.rules.validation.properties.dimentional;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.openl.OpenL;
import org.openl.binding.MethodUtil;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.DecisionTableLoader;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.HeaderSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeKey;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.apache.poi.ss.usermodel.Sheet;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesLoader;
import org.openl.rules.table.properties.TableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.AMethod;
import org.openl.types.impl.MethodKey;
import org.openl.types.java.JavaOpenClass;

public class DispatcherTableBuilder {
    
    private static final Map<String, IOpenClass> incomeParams;
    
    /**
     * Initialize a map of parameters from context, that will be used as income parameters to newly created dispatcher tables.
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
    
    public static final String DEFAULT_DISPATCHER_TABLE_NAME = "validateGapOverlap";
    
    /**
     * Checks whether the specified TableSyntaxNode is auto generated gap/overlap table or not.  
     * @param tsn TableSyntaxNode to check.
     * @return <code>true</code> if table is dispatcher table.
     */
    public static boolean isDispatcherTable(TableSyntaxNode tsn) {
        IOpenMember member = tsn.getMember();
        if (member instanceof IOpenMethod) {
            return member.getName().startsWith(DEFAULT_DISPATCHER_TABLE_NAME);
        }
        return false;
    }
    
    private OpenL openl;    
    private XlsModuleOpenClass moduleOpenClass;
    private RulesModuleBindingContext moduleContext;
    
    public DispatcherTableBuilder(OpenL openl, XlsModuleOpenClass moduleOpenClass, 
            RulesModuleBindingContext moduleContext) {
        this.openl = openl;
        this.moduleContext = moduleContext;
        this.moduleOpenClass = moduleOpenClass;        
    }
    
    /**
     * Builds dispatcher tables for every group of overloaded tables.
     * As a result new {@link TableSyntaxNode} objects appears in module.
     */
    public void buildDispatcherTables() {        
        Map<MethodKey, List<TableSyntaxNode>> groupedTables = groupExecutableTables();        
        for (List<TableSyntaxNode> tablesGroup : groupedTables.values()) {
            List<TableSyntaxNode> overloadedTablesGroup = excludeOveloadedByVersion(tablesGroup);
            if (overloadedTablesGroup.size() > 1) {
                buildTableForGroup(overloadedTablesGroup);
            }
        }
    }

    private List<TableSyntaxNode> excludeOveloadedByVersion(List<TableSyntaxNode> tablesGroup) {
        Set<TableSyntaxNodeKey> differentTables = new HashSet<TableSyntaxNodeKey>();
        List<TableSyntaxNode> result = new ArrayList<TableSyntaxNode>();        
        for (TableSyntaxNode tsn : tablesGroup) {
            TableSyntaxNodeKey key = new TableSyntaxNodeKey(tsn);
            if (!differentTables.contains(key)) {                
                differentTables.add(key);
                result.add(tsn);
            }
        }
        return result;
    }

    private void addNewTsnToTopNode(TableSyntaxNode tsn) {        
        XlsMetaInfo xlsMetaInfo = moduleOpenClass.getXlsMetaInfo();
        ((WorkbookSyntaxNode)xlsMetaInfo.getXlsModuleNode().getWorkbookSyntaxNodes()[0])
            .getWorksheetSyntaxNodes()[0].addNode(tsn);     
    }
    
    /**
     * Build dispatcher table for dimensional properties for particular overloaded tables group.
     * 
     * @param tablesGroup group of overloaded tables.
     */
    private void buildTableForGroup(List<TableSyntaxNode> tablesGroup) {
        
        // as we have a group of overloaded tables, we need to take one it`s 
        // member to get all common settings for the whole group
        TableSyntaxNode groupMember = tablesGroup.get(0);
        String originalTableName = getOriginalTableName(groupMember);
        
        // table name for dispatcher table.
        String newTableName = DEFAULT_DISPATCHER_TABLE_NAME + "_" + originalTableName;
        
        // properties values from tables in group that will be used to build dispatcher table by dimensional properties.
        List<ITableProperties> propertiesValues = getPropertiesValues(tablesGroup);  
        
        // create the table by POI builder. gets the sheet containing this table.
        Sheet sheetWithTable = createTable(groupMember, newTableName, propertiesValues);
        
        DecisionTableCreator decisionTableCreator = new DecisionTableCreator();

        IGridTable gridTable = decisionTableCreator.createGridTable(sheetWithTable);

        XlsSheetGridModel sheetGridModel = decisionTableCreator.createSheetGridModel(sheetWithTable);
        
        TableSyntaxNode tsn = createTableSyntaxNode(sheetGridModel, gridTable);
        
        IMethodSignature originalSignature = getOriginalTableSignature(groupMember);
        IOpenClass originalReturnType = getOriginalTableReturnType(groupMember);
        
        DecisionTable decisionTable = decisionTableCreator.createDecisionTable(newTableName, originalReturnType, originalSignature, incomeParams);
        tsn.setMember(decisionTable);        
        
        loadCreatedTable(decisionTable, tsn);
        
        IOpenMethod groupMemberMethod = (IOpenMethod)groupMember.getMember();
        setPropertiesForDispatcherTable(groupMemberMethod, tsn);
    }
    
    /**
     * Creates the memory representation of dispatcher table by POI.
     * 
     * @param groupMember member of tables group.
     * @param newTableName table name for dispatcher table.
     * @param propertiesValues properties values from tables in group.
     * @return sheet that contains created table.
     */
    private Sheet createTable(TableSyntaxNode groupMember, String newTableName, List<ITableProperties> propertiesValues) {
        DecisionTablePOIBuilder tableBulder = initTableBuilder(groupMember, newTableName, propertiesValues);
        
        return tableBulder.buildTable();
        
    }
    
    /**
     * Gets properties values from tables in group that will be used to build dispatcher table by dimensional properties.
     * 
     * @param tablesGroup group of overloaded tables.
     * @return properties values from tables in group.
     */
    private List<ITableProperties> getPropertiesValues(List<TableSyntaxNode> tablesGroup) {
        List<ITableProperties> propertiesValues = new ArrayList<ITableProperties>();
        for (TableSyntaxNode tsn : tablesGroup) {
            propertiesValues.add(tsn.getTableProperties());
        }
        return propertiesValues;
    }
    
    /**
     * As all tables in group have the similar name, so it is possible do get any member and get it`s name.
     * 
     * @param groupMember member of the overloaded tables group
     * @return name of the tables in group.
     */
    private String getOriginalTableName(TableSyntaxNode groupMember) {
        return ((AMethod)groupMember.getMember()).getHeader().getName();
    }
    
    /**
     * As all tables in group have the similar type, so it is possible do get any member and get it`s type.
     * 
     * @param groupMember member of the overloaded tables group
     * @return type of the tables in group.
     */
    private IOpenClass getOriginalTableReturnType(TableSyntaxNode groupMember) {        
        return ((AMethod)groupMember.getMember()).getHeader().getType();        
    }    
    
    /**
     * As all tables in group have the similar signature, so it is possible do get any member and get it`s signature.
     * 
     * @param groupMember member of the overloaded tables group
     * @return method signature of the tables in group.
     */
    private IMethodSignature getOriginalTableSignature(TableSyntaxNode tsn) {        
        return ((AMethod)tsn.getMember()).getHeader().getSignature();
    }
    
    /**
     * Initialize POI table builder with columns and return column, number of rules.
     * 
     * @param groupMember
     * @param newTableName
     * @param propertiesValues
     * @return
     */
    private DecisionTablePOIBuilder initTableBuilder(TableSyntaxNode groupMember, String newTableName, List<ITableProperties> propertiesValues) {
        List<TablePropertyDefinition> dimensionalPropertiesDef = TablePropertyDefinitionUtils.getDimensionalTableProperties();
        
        String originalTableName = getOriginalTableName(groupMember);
        IMethodSignature originalSignature = getOriginalTableSignature(groupMember);
        IOpenClass originalReturnType = getOriginalTableReturnType(groupMember);
                
        DimensionPropertiesReturnColumn returnColumn = new DimensionPropertiesReturnColumn(originalReturnType, originalTableName, originalSignature, incomeParams);
        
        List<IDecisionTableColumn> conditions = new ArrayList<IDecisionTableColumn>();
        DimensionPropertiesRules rules = new DimensionPropertiesRules(propertiesValues);
        for (TablePropertyDefinition dimensionProperty : dimensionalPropertiesDef) {
            if (isPropertyValueSetInTables(dimensionProperty.getName(), propertiesValues)) {
                conditions.add(DimensionProperiesColumnMaker.makeColumn(dimensionProperty, rules));
            }
        }
        
        return new DecisionTablePOIBuilder(newTableName, conditions, returnColumn, rules.getRulesNumber());
    }
    
    /**
     * Checks if there is any value of particular property represented in collection, that will be used as rules.
     * If no, we don`t need to create column for this property.
     * 
     */
    private boolean isPropertyValueSetInTables(String propertyName, List<ITableProperties> tableProperties) {
        boolean isPropertyValueSet = false;

        for (ITableProperties properties : tableProperties) {
            String propertyValue = properties.getPropertyValueAsString(propertyName);            
            if (StringUtils.isNotEmpty(propertyValue)) {
                isPropertyValueSet = true;
                break;
            }
        }

        return isPropertyValueSet;        
    }
    
    /**
     * Load and bind the decision table by OpenL.
     * 
     * @param decisionTable created decision table.
     * @param tsn created table syntax node.
     */
    private void loadCreatedTable(DecisionTable decisionTable, TableSyntaxNode tsn) {
        PropertiesLoader propLoader = new PropertiesLoader(openl, moduleContext, (XlsModuleOpenClass)moduleOpenClass);
        propLoader.loadDefaultProperties(tsn);
          
        DecisionTableLoader dtLoader = new DecisionTableLoader();
        try {
            dtLoader.loadAndBind(tsn, decisionTable, openl, null, moduleContext);
            addNewTsnToTopNode(tsn);
        } catch (SyntaxNodeException e) {            
            e.printStackTrace();
        } catch (Exception e) {            
            e.printStackTrace();
        }
    }
    
    /**
     * Set properties to newly created table syntax node.
     * 
     */
    private void setPropertiesForDispatcherTable(IOpenMethod groupMemberMethod, TableSyntaxNode tsn) {
        TableProperties properties = (TableProperties) tsn.getTableProperties();
        properties.setFieldValue("category", "Autogenerated - Dispatch by Properties");
        properties.setFieldValue("description",
                " Automatically created table to dispatch by dimensional properties values for method: "
                        + MethodUtil.printMethod(groupMemberMethod, 0, true)
                        + ". Please, edit original tables to make any change to the overloading logic.");
    }    
    
    private TableSyntaxNode createTableSyntaxNode(XlsSheetGridModel sheetGridModel, IGridTable gridTable) {        
        String type = ITableNodeTypes.XLS_DT;
        
        GridLocation pos = new GridLocation(gridTable);
        HeaderSyntaxNode headerSyntaxNode = new HeaderSyntaxNode(null, new IdentifierNode(null, null, "Rules", null));

        return new TableSyntaxNode(type, pos, sheetGridModel.getSheetSource(), gridTable, headerSyntaxNode);
    }
        
    private Map<MethodKey, List<TableSyntaxNode>> groupExecutableTables() {
        TableSyntaxNode[] tableSyntaxNodes = getTableSyntaxNodes();
        
        Map<MethodKey, List<TableSyntaxNode>> groupedTables = new HashMap<MethodKey, List<TableSyntaxNode>>();
        for (TableSyntaxNode tsn : tableSyntaxNodes) {
            if (tsn.getMember() instanceof IOpenMethod) {                
                MethodKey key = new MethodKey((IOpenMethod) tsn.getMember());
                if (!groupedTables.containsKey(key)) {
                    groupedTables.put(key, new ArrayList<TableSyntaxNode>());
                }
                groupedTables.get(key).add(tsn);
            }
        }
        return groupedTables;
    }
    
    /**
     * 
     * @return all table syntax nodes from module open class.
     */
    private TableSyntaxNode[] getTableSyntaxNodes() {
        XlsMetaInfo xlsMetaInfo = moduleOpenClass.getXlsMetaInfo();
        return xlsMetaInfo.getXlsModuleNode().getXlsTableSyntaxNodes();
    }
    
}
