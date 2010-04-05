package org.openl.rules.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.openl.OpenL;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.dt.DecisionTableLoader;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.HeaderSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeKey;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.table.GridSplitter;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.properties.PropertiesLoader;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;

public class DispatcherTableBuilder {
    
    private static final String METHOD_NAME = "valdateGapOverlap";
    private static final String RETURN_TYPE = "void";
    
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
        
        Map<TableSyntaxNodeKey, List<TableSyntaxNode>> groupedTables = groupExecutableTables();        
//        for (TableSyntaxNodeKey key : groupedTables.keySet()) {                         
//            buildTableForGroup(groupedTables.get(key));
//        }
        
        buildTestTable();
    }
    
    private void buildTestTable() {
        String tableName = IXlsTableNames.DECISION_TABLE2 + " " + RETURN_TYPE + " " + METHOD_NAME + "(int currentValue)";
        
        int condNum = 2;
        Sheet sheetWithNewTable = createTableByPOI(tableName, condNum);
        
        XlsSheetGridModel sheetGridModel = new XlsSheetGridModel(sheetWithNewTable);
        GridSplitter gridSplitter = new GridSplitter(sheetGridModel);
        GridTable[] gridTables = gridSplitter.split();
        
        GridTable gridTable = gridTables[0];        
        
        DecisionTable decTable = createDecisionTable();
        
        TableSyntaxNode tsn = createTableSyntaxNode(sheetGridModel, gridTable);
        tsn.setMember(decTable);
        PropertiesLoader propLoader = new PropertiesLoader(openl, moduleContext, (XlsModuleOpenClass)moduleOpenClass);
        propLoader.loadDefaultProperties(tsn);
        
        DecisionTableLoader dtLoader = new DecisionTableLoader();
        try {
            dtLoader.loadAndBind(tsn, decTable, openl, null, moduleContext);
            addNewTsnToTopNode(tsn);
        } catch (SyntaxNodeException e) {            
            e.printStackTrace();
        } catch (Exception e) {            
            e.printStackTrace();
        } 
    }

    private void addNewTsnToTopNode(TableSyntaxNode tsn) {        
        XlsMetaInfo xlsMetaInfo = moduleOpenClass.getXlsMetaInfo();
        ((WorkbookSyntaxNode)xlsMetaInfo.getXlsModuleNode().getWorkbookSyntaxNodes()[0])
            .getWorksheetSyntaxNodes()[0].addNode(tsn);     
    }

    private void buildTableForGroup(List<TableSyntaxNode> tablesGroup) {
        //List<TableSyntaxNode> tablesGroup = tablesGroup;        
    }    

    private TableSyntaxNode createTableSyntaxNode(XlsSheetGridModel sheetGridModel, GridTable gridTable) {        
        String type = ITableNodeTypes.XLS_DT;
        
        GridLocation pos = new GridLocation(gridTable);
        
        HeaderSyntaxNode headerSyntaxNode = new HeaderSyntaxNode(null, null);
        TableSyntaxNode tsn = new TableSyntaxNode(type, pos, sheetGridModel.getSheetSource(), gridTable, headerSyntaxNode);
        return tsn;
    }

    private DecisionTable createDecisionTable() {
        IOpenClass returnType = JavaOpenClass.VOID; // it will be rule to fire. but now try VOID type
        IOpenClass[] paramTypes = new JavaOpenClass[]{JavaOpenClass.INT}; // types of the income parameters.
        IMethodSignature signature = new MethodSignature(paramTypes, new String[]{"currentValue"});
        IOpenClass declaringClass = null; // can be null.        
        
        IOpenMethodHeader methodHeader = new OpenMethodHeader("valdateGapOverlap", returnType, signature, declaringClass);
        DecisionTable decTable = new DecisionTable(methodHeader);
        return decTable;        
    }
    
    private Sheet createTableByPOI(String tableName, int condNums) {
        // as counting starts from 0, colNum will be condNumbers + returnCol.
        int columnNum = condNums;
        Workbook wb = new HSSFWorkbook();
        
        Sheet sheet = wb.createSheet("new sheet");

        int rowNum = 0;
        Row firstRow = sheet.createRow((short)rowNum);
        
        Cell cell_0_0 = firstRow.createCell(0);
        cell_0_0.setCellValue(tableName);
        sheet
        .addMergedRegion(new CellRangeAddress(firstRow.getRowNum(), firstRow.getRowNum(), 
                cell_0_0.getColumnIndex(), columnNum));
        
        rowNum++;
        Row secondRow = sheet.createRow((short)rowNum);
        for (int colNum = 0; colNum < columnNum; colNum++) {
            Cell cell = secondRow.createCell(colNum);
            cell.setCellValue("C"+(colNum+1));
        }
        
        Cell cellRet = secondRow.createCell(condNums);
        cellRet.setCellValue("RET");
        
        rowNum++;
        Row thirdRow = sheet.createRow((short)rowNum);
        for (int colNum = 0; colNum <= columnNum; colNum++) {
            Cell cell = thirdRow.createCell(colNum);
            cell.setCellValue(getValueForTheCell(rowNum, colNum));
        }
        
        Row fourthRow = sheet.createRow((short)3);
        Cell cell_3_0 = fourthRow.createCell(0);
        cell_3_0.setCellValue("int min");
        
        Cell cell_3_1 = fourthRow.createCell(1);
        cell_3_1.setCellValue("int max");
        
        Cell cell_3_2 = fourthRow.createCell(2);
        cell_3_2.setCellValue("String result");
        
        Row fifthRow = sheet.createRow((short)4);
        Cell cell_4_0 = fifthRow.createCell(0);
        cell_4_0.setCellValue("From");
        
        Cell cell_4_1 = fifthRow.createCell(1);
        cell_4_1.setCellValue("To");
        
        Cell cell_4_2 = fifthRow.createCell(2);
        cell_4_2.setCellValue("Greeting");        
        
        Row sixthRow = sheet.createRow((short)5);
        Cell cell_5_0 = sixthRow.createCell(0);
        cell_5_0.setCellValue("0");
        
        Cell cell_5_1 = sixthRow.createCell(1);
        cell_5_1.setCellValue("45");
        
        Cell cell_5_2 = sixthRow.createCell(2);
        cell_5_2.setCellValue("Rule1 fires");
        
        Row seventhRow = sheet.createRow((short)6);
        Cell cell_6_0 = seventhRow.createCell(0);
        cell_6_0.setCellValue("47");
        
        Cell cell_6_1 = seventhRow.createCell(1);
        cell_6_1.setCellValue("50");
        
        Cell cell_6_2 = seventhRow.createCell(2);
        cell_6_2.setCellValue("Rule2 fires");
        
        return sheet;
    }

    private String getValueForTheCell(int rowNum, int colNum) {
        String result = null;
        // algorithm row
        if (rowNum == 2 && colNum == 0) {
            result = "min <= currentValue";
        } else if (rowNum == 2 && colNum == 1) {
           result = "currentValue <= max";
        } else if (rowNum == 2 && colNum == 2) {
            result = "System.out.println(result)";
        }
        return result;
    }
    
    private Map<TableSyntaxNodeKey, List<TableSyntaxNode>> groupExecutableTables() {
        TableSyntaxNode[] tableSyntaxNodes = getTableSyntaxNodes();
        
        Map<TableSyntaxNodeKey, List<TableSyntaxNode>> groupedTables = new HashMap<TableSyntaxNodeKey, List<TableSyntaxNode>>();
        for (TableSyntaxNode tsn : tableSyntaxNodes) {
            if (tsn.getMember() instanceof IOpenMethod) {
                TableSyntaxNodeKey key = new TableSyntaxNodeKey(tsn);
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
