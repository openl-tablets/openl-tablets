package org.openl.rules.validation.properties.dimentional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.OpenL;
import org.openl.binding.MethodUtil;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.DecisionTableLoader;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.HeaderSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeKey;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.properties.PropertiesLoader;
import org.openl.rules.table.properties.TableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.AMethod;
import org.openl.types.impl.AOpenClass.MethodKey;

public class DispatcherTableBuilder {
    
    public static final String DISPATCHER_TABLES_SHEET = "Dispatcher Tables Sheet";
    public static final String DEFAULT_METHOD_NAME = "validateGapOverlap";
    
    private OpenL openl;    
    private XlsModuleOpenClass moduleOpenClass;
    private RulesModuleBindingContext moduleContext;
    
    public DispatcherTableBuilder(OpenL openl, XlsModuleOpenClass moduleOpenClass, 
            RulesModuleBindingContext moduleContext) {
        this.openl = openl;
        this.moduleContext = moduleContext;
        this.moduleOpenClass = moduleOpenClass;        
    }
    
    public void buildTable() {        
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

    private void buildTableForGroup(List<TableSyntaxNode> tablesGroup) {
        List<TablePropertyDefinition> dimensionalTableProp = TablePropertyDefinitionUtils.getDimensionalTableProperties();    
        
        TableSyntaxNode groupMember = tablesGroup.get(0);
        String originalTableName = ((AMethod)groupMember.getMember()).getHeader().getName();
        IMethodSignature originalSignature = getOriginalTableSignature(groupMember);
        IOpenClass originalReturnType = getOtiginalTableReturnType(groupMember);
        
        String tableName = DEFAULT_METHOD_NAME + "_" + originalTableName;
        
        DecisionTableCreator decisionTableWriter = new DecisionTableCreator(dimensionalTableProp, tablesGroup, 
                tableName, originalReturnType, originalTableName, originalSignature);
        
        GridTable createdGridTable = decisionTableWriter.createGridTable();        
        DecisionTable decisionTable = decisionTableWriter.getCreatedDecisionTable(); 
        
        TableSyntaxNode tsn = createTableSyntaxNode(decisionTableWriter.getCreatedSheetGridModel(), createdGridTable);
        tsn.setMember(decisionTable);        
        
        loadCreatedTable(decisionTable, tsn);
        
        IOpenMethod validatedMethod = (IOpenMethod)groupMember.getMember();
        setDispatcherProperties(validatedMethod, tsn);
    }

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

    private void setDispatcherProperties(IOpenMethod validatedMethod, TableSyntaxNode tsn) {
        TableProperties properties = (TableProperties) tsn.getTableProperties();
        properties.setFieldValue("name", "Dispatcher by properties for: " + validatedMethod.getName());
        properties.setFieldValue("category", "Autogenerated - Dispatch by Properties");
        properties.setFieldValue("description",
                " Automatically created table to dispatch by dimensional properties values for method: "
                        + MethodUtil.printMethod(validatedMethod, 0, true)
                        + ". Please, edit original tables to make any change to the overloading logic.");
    }

    private IOpenClass getOtiginalTableReturnType(TableSyntaxNode groupMember) {        
        return ((AMethod)groupMember.getMember()).getHeader().getType();        
    }    

    private IMethodSignature getOriginalTableSignature(TableSyntaxNode tsn) {        
        return ((AMethod)tsn.getMember()).getHeader().getSignature();
    }
    
    private TableSyntaxNode createTableSyntaxNode(XlsSheetGridModel sheetGridModel, GridTable gridTable) {        
        String type = ITableNodeTypes.XLS_DT;
        
        GridLocation pos = new GridLocation(gridTable);
        HeaderSyntaxNode headerSyntaxNode = new HeaderSyntaxNode(null, null);

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
    
    private TableSyntaxNode[] getTableSyntaxNodes() {
        XlsMetaInfo xlsMetaInfo = moduleOpenClass.getXlsMetaInfo();
        return xlsMetaInfo.getXlsModuleNode().getXlsTableSyntaxNodes();
    }
    
}
