package org.openl.rules.validation;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.GridSplitter;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;

public class DecisionTableCreator {
    
    private static final String RUNTIME_CXT_PARAM = "runtimeContext";
    
    private static String __src = "src/rules/Test.xls";
    
    private static final String STATE_PROP = "state";
    private static final String COUNTRY_PROP = "country";
    private static final String US_REGION_PROP = "usregion";
    private static final String LOB_PROP = "lob";
    private static final String EXPIRATION_DATE_PROP = "expirationDate";
    private static final String EFFECTIVE_DATE_PROP = "effectiveDate";
    private static final String ACTIVE_PROP = "active";
    private int conditionsWidth;
    private String originalTableName;
    private String newTableName;
    private Map<String, IOpenClass> originalParameters;
    private List<TableSyntaxNode> tablesGroup;
    private String[] dimensionalTableProp;
    private IOpenClass originalReturnType;
    private DecisionTable createdDecTable;
    private XlsSheetGridModel createdSheetGridModel;
    
    public DecisionTableCreator(String originalTableName, Map<String, IOpenClass> originalParameters,
            List<TableSyntaxNode> tablesGroup, String[] dimensionalTableProp,
            IOpenClass originalReturnType) {
        this.conditionsWidth = dimensionalTableProp.length;
        this.dimensionalTableProp = dimensionalTableProp;
        this.originalParameters = originalParameters;
        this.originalReturnType = originalReturnType;
        this.originalTableName = originalTableName;
        this.newTableName = DispatcherTableBuilder.DEFAULT_METHOD_NAME + "_" + this.originalTableName;
        this.tablesGroup = tablesGroup;
    }

    public GridTable createGridTable() {
        
        Sheet sheet = writeTableByPOI();
        
        return getGridTable(sheet);
        
    }
    
    private Sheet writeTableByPOI() {
        String tableName = buildMethodHeader();        
        
        Workbook wb = new HSSFWorkbook();
        
        Sheet sheet = wb.createSheet(DispatcherTableBuilder.DISPATCHER_TABLES_SHEET + originalTableName);

        int rowNum = 0;
        Row firstRow = sheet.createRow((short)rowNum);
        
        Cell cell_0_0 = firstRow.createCell(0);
        cell_0_0.setCellValue(tableName);
        sheet
        .addMergedRegion(new CellRangeAddress(firstRow.getRowNum(), firstRow.getRowNum(), 
                cell_0_0.getColumnIndex(), conditionsWidth));
        
        rowNum++;
        writeSecondRow(sheet, rowNum);
        
        rowNum++;        
        writeAlgorithmRow(sheet, rowNum);
        
        rowNum++;
        writeInitializeRow(sheet, rowNum);
        
        rowNum++;
        writeNameRow(sheet, rowNum);
        
        for (TableSyntaxNode tsn : tablesGroup) {
            rowNum++;
            writeRuleRow(tsn,sheet, rowNum);
        }
        //writeTableToFile(wb);
        
        return sheet;
    }

    private void writeTableToFile(org.apache.poi.ss.usermodel.Workbook wb) {
        FileOutputStream fileOut;
        try {
            fileOut = new FileOutputStream(__src);
            wb.write(fileOut);
            fileOut.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void writeNameRow(Sheet sheet, int rowNum) {
        Row fifhRow = sheet.createRow((short)rowNum);
        
        for (int colNum = 0; colNum < conditionsWidth; colNum++) {
            Cell cell = fifhRow.createCell(colNum);
            cell.setCellValue(TablePropertyDefinitionUtils.getPropertyDisplayName(dimensionalTableProp[colNum]));
        }
        Cell cellRet = fifhRow.createCell(conditionsWidth);
        cellRet.setCellValue("Result");
    }

    private String getRuleCellValue(TableSyntaxNode tsn, int colNum) {
        String dimPropName = dimensionalTableProp[colNum];
        return tsn.getTableProperties().getPropertyValueAsString(dimPropName);        
    }
    
    private void writeRuleRow(TableSyntaxNode tsn, Sheet sheet, int rowNum) {
        Row ruleRow = sheet.createRow((short)rowNum);
        
        for (int colNum = 0; colNum < conditionsWidth; colNum++) {
            Cell cell = ruleRow.createCell(colNum);
            cell.setCellValue(getRuleCellValue(tsn, colNum));
        }
        
        Cell cellRet = ruleRow.createCell(conditionsWidth);        
        cellRet.setCellValue(String.format("=%s(%s)", originalTableName, originalParamsThroughComma()));        
    }

    private void writeInitializeRow(Sheet sheet, int rowNum) {
        Row fourthRow = sheet.createRow((short)rowNum);
        
        for (int colNum = 0; colNum < conditionsWidth; colNum++) {
            Cell cell = fourthRow.createCell(colNum);
            cell.setCellValue(getValueInitializeCell(colNum));
        }
        Cell cellRet = fourthRow.createCell(conditionsWidth);
        cellRet.setCellValue(originalReturnType.getName() + " result");
    }

    private String getValueInitializeCell(int colNum) {        
        String dimPropName = dimensionalTableProp[colNum];
        TablePropertyDefinition propDef = TablePropertyDefinitionUtils.getPropertyByName(dimPropName);
        return String.format("%s %s", propDef.getType().getInstanceClass().getSimpleName(), dimPropName);
    }

    private void writeAlgorithmRow(Sheet sheet, int rowNum) {
        Row thirdRow = sheet.createRow((short)rowNum);
        for (int colNum = 0; colNum < conditionsWidth; colNum++) {
            Cell cell = thirdRow.createCell(colNum);
            cell.setCellValue(getValueAlgorithmCell(colNum));
        }
        
        Cell cellRet = thirdRow.createCell(conditionsWidth);
        cellRet.setCellValue("result");
    }

    private String getValueAlgorithmCell(int colNum) {
        String result = null;
        String dimPropName = dimensionalTableProp[colNum];        
        if (EFFECTIVE_DATE_PROP.equals(dimPropName)) {
            result = String.format("%s.after(%s.currentDate)", EFFECTIVE_DATE_PROP, RUNTIME_CXT_PARAM);
        } else if (EXPIRATION_DATE_PROP.equals(dimPropName)) {            
            result = String.format("%s.before(%s.currentDate)", EXPIRATION_DATE_PROP, RUNTIME_CXT_PARAM);
        } else if (LOB_PROP.equals(dimPropName)) {
            result = String.format("%s.equals(%s.lob)", LOB_PROP, RUNTIME_CXT_PARAM);
        } else if (US_REGION_PROP.equals(dimPropName)) {
            result = String.format("%s.equals(%s.usRegion)", US_REGION_PROP, RUNTIME_CXT_PARAM);
        } else if (COUNTRY_PROP.equals(dimPropName)) {
            result = String.format("(Arrays.asList(%s)).contains(%s.country)", COUNTRY_PROP, RUNTIME_CXT_PARAM);
        } else if (STATE_PROP.equals(dimPropName)) {
            result = String.format("%s.equals(%s.usState)", STATE_PROP, RUNTIME_CXT_PARAM);
        } else if (ACTIVE_PROP.equals(dimPropName)) {
            result = String.format("%s.booleanValue()", ACTIVE_PROP);
        }
        return result;
    }

    private void writeSecondRow(Sheet sheet, int rowNum) {
        Row secondRow = sheet.createRow((short)rowNum);
        for (int colNum = 0; colNum < conditionsWidth; colNum++) {
            Cell cell = secondRow.createCell(colNum);
            cell.setCellValue("C"+(colNum+1));
        }        
        Cell cellRet = secondRow.createCell(conditionsWidth);
        cellRet.setCellValue("RET");
    }

    private String buildMethodHeader() {
        String start = String.format("%s %s %s(", IXlsTableNames.DECISION_TABLE2, originalReturnType.getName(), 
                newTableName);
        StringBuffer strBuf = new StringBuffer();
        strBuf.append(start);
        strBuf.append(originalParamsWithTypesThroughComma());        
        strBuf.append(", ").append(DefaultRulesRuntimeContext.class.getSimpleName()).append(" ").append(RUNTIME_CXT_PARAM).append(")");
        return strBuf.toString();
    }

    private String originalParamsWithTypesThroughComma() {
        StringBuffer strBuf = new StringBuffer();
        int paramNum = originalParameters.size();
        for (Map.Entry<String, IOpenClass> param : originalParameters.entrySet()) {
            paramNum--;
            strBuf.append(param.getValue().getName()).append(" ").append(param.getKey());
            if (paramNum > 0) {
                strBuf.append(", ");
            }
        }
        return strBuf.toString();
    }
    
    private String originalParamsThroughComma() {
        StringBuffer strBuf = new StringBuffer();
        int paramNum = originalParameters.size();
        for (Map.Entry<String, IOpenClass> param : originalParameters.entrySet()) {
            paramNum--;
            strBuf.append(param.getKey());
            if (paramNum > 0) {
                strBuf.append(", ");
            }
        }
        return strBuf.toString();
    }
    
    private GridTable getGridTable(Sheet sheet) {
        createdSheetGridModel = new XlsSheetGridModel(sheet);
        GridSplitter gridSplitter = new GridSplitter(createdSheetGridModel);
        GridTable[] gridTables = gridSplitter.split();
        
        GridTable gridTable = gridTables[0];        
        
        createDecisionTable();
        
        return gridTable;
    }
    
    private void createDecisionTable() {         
        IOpenClass[] paramTypes = new IOpenClass[originalParameters.size() + 1];
        String[] paramNames = new String[originalParameters.size() + 1];
        int i = 0;
        for (Map.Entry<String, IOpenClass> param : originalParameters.entrySet()) {
            paramTypes[i] = param.getValue();
            paramNames[i] = param.getKey();
            i++;
        }
        paramTypes[i] = JavaOpenClass.getOpenClass(DefaultRulesRuntimeContext.class);
        paramNames[i] = RUNTIME_CXT_PARAM;
        
        IMethodSignature signature = new MethodSignature(paramTypes, paramNames);
        IOpenClass declaringClass = null; // can be null.        
        
        IOpenMethodHeader methodHeader = new OpenMethodHeader(newTableName, originalReturnType, signature, declaringClass);
        createdDecTable = new DecisionTable(methodHeader);                
    }

    public DecisionTable getCreatedDecTable() {
        if (createdDecTable == null) {
            createGridTable();
        }
        return createdDecTable;
    }

    public XlsSheetGridModel getCreatedSheetGridModel() {
        if (createdSheetGridModel == null) {
            createGridTable();
        }
        return createdSheetGridModel;
    }

}
