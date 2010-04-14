package org.openl.rules.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.OpenL;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.DecisionTableLoader;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.HeaderSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.properties.PropertiesLoader;
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
    public static final String DEFAULT_METHOD_NAME = "valdateGapOverlap";
    
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
        for (MethodKey key : groupedTables.keySet()) {
            List<TableSyntaxNode> tablesGroup = groupedTables.get(key);
            if (tablesGroup.size() > 1) {
                buildTableForGroup(tablesGroup);
            }
        }
    }

    private void addNewTsnToTopNode(TableSyntaxNode tsn) {        
        XlsMetaInfo xlsMetaInfo = moduleOpenClass.getXlsMetaInfo();
        ((WorkbookSyntaxNode)xlsMetaInfo.getXlsModuleNode().getWorkbookSyntaxNodes()[0])
            .getWorksheetSyntaxNodes()[0].addNode(tsn);     
    }

    private void buildTableForGroup(List<TableSyntaxNode> tablesGroup) {
        String[] dimensionalTableProp = TablePropertyDefinitionUtils.getDimensionalTableProperties();    
        TableSyntaxNode groupMember = tablesGroup.get(0);
        String originalTableName = ((AMethod)groupMember.getMember()).getHeader().getName();
        
        Map<String, IOpenClass> originalParameters = getOriginalTableParameters(groupMember);
        
        IOpenClass originalReturnType = getOtiginalTableReturnType(groupMember);
        
        DecisionTableCreator dtTableWriter = new DecisionTableCreator(originalTableName, 
                originalParameters, tablesGroup, dimensionalTableProp, originalReturnType);
        GridTable createdGridTable = dtTableWriter.createGridTable();        
        
        DecisionTable decisionTable = dtTableWriter.getCreatedDecTable(); 
        
        TableSyntaxNode tsn = createTableSyntaxNode(dtTableWriter.getCreatedSheetGridModel(), createdGridTable);
        tsn.setMember(decisionTable);        
        
        loadCreatedTable(decisionTable, tsn);        
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

    private IOpenClass getOtiginalTableReturnType(TableSyntaxNode groupMember) {        
        return ((AMethod)groupMember.getMember()).getHeader().getType();        
    }    

    private Map<String, IOpenClass> getOriginalTableParameters(TableSyntaxNode tsn) {
        Map<String, IOpenClass> result = new HashMap<String, IOpenClass>();
        IMethodSignature methodSignature = ((AMethod)tsn.getMember()).getHeader().getSignature();
        for (int i=0; i<methodSignature.getNumberOfParameters(); i++) {
            result.put(methodSignature.getParameterName(i), methodSignature.getParameterType(i));
        }
        return result;
    }
    
    private TableSyntaxNode createTableSyntaxNode(XlsSheetGridModel sheetGridModel, GridTable gridTable) {        
        String type = ITableNodeTypes.XLS_DT;
        
        GridLocation pos = new GridLocation(gridTable);
        
        HeaderSyntaxNode headerSyntaxNode = new HeaderSyntaxNode(null, null);
        TableSyntaxNode tsn = new TableSyntaxNode(type, pos, sheetGridModel.getSheetSource(), gridTable, headerSyntaxNode);
        return tsn;
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
