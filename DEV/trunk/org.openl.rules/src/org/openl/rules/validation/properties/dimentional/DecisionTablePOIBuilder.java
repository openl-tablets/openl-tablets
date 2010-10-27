package org.openl.rules.validation.properties.dimentional;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.openl.rules.lang.xls.IXlsTableNames;

public class DecisionTablePOIBuilder {
    
    public static final String DISPATCHER_TABLES_SHEET = "Dispatcher Tables Sheet";
    
    private static final int DECISION_TABLE_HEADER_ROWS_NUMBER = 5; // number 5 - is a number of first development rows in table.
    private static final int CONDITION_TITLE_ROW_INDEX = 4;
    private static final int CONDITION_PARAMETER_ROW_INDEX = 3;
    private static final int CODE_EXPRESSION_ROW_INDEX = 2;
    private static final int CONDITION_NAME_ROW_INDEX = 1; // condition name always is the next row after header row.
    private int conditionNumber = 0;
    private int rulesNumber = 0;
        
    private List<IDecisionTableColumn> simpleConditions;
    private List<IDecisionTableColumn> arrayConditions;    
    private IDecisionTableReturnColumn returnColumn;
    private String tableName;    
    
    private Workbook workbook;
    private Sheet sheet;
    
       
    
    public DecisionTablePOIBuilder(String tableName, List<IDecisionTableColumn> conditions,            
            IDecisionTableReturnColumn returnColumn, int rulesNumber) {
        this.tableName = tableName;
        initConditions(conditions);
        this.returnColumn = returnColumn;
        this.rulesNumber = rulesNumber;        
        this.workbook = new HSSFWorkbook();        
        this.sheet = workbook.createSheet(DISPATCHER_TABLES_SHEET + tableName);
    }    
    
    private void initConditions(List<IDecisionTableColumn> conditions) {              
        simpleConditions = new ArrayList<IDecisionTableColumn>();
        arrayConditions = new ArrayList<IDecisionTableColumn>();
      
        for (IDecisionTableColumn condition : conditions) {
            if (condition.isArrayCondition()) {                    
                arrayConditions.add(condition);
            } else {                    
                simpleConditions.add(condition);
              }
          }
    }

    public Sheet buildTable() {
        // table writing starts from 0,0 indexes
        // create all needed rows for table. 
        createAllRows();
        
        int nextColumn = 0;
        
        // write simple conditions, that are not an array type.
        for (IDecisionTableColumn condition : simpleConditions) {
            writeSimpleConditionColumn(nextColumn, condition);
            nextColumn++;
        }
        
        for (IDecisionTableColumn condition : arrayConditions) {
            int numberOfWrittenColumns = writeArrayConditionColumn(nextColumn, condition);
            nextColumn += numberOfWrittenColumns;
        }
        
        int lastColumnNumber = nextColumn;
        
        writeReturnColumn(lastColumnNumber);
        
        writeHeaderRow(lastColumnNumber);

        //writeTableToFile();
        
        return sheet;
    }
    
    private void createAllRows() {        
        int tableRowsNumber = rulesNumber + DECISION_TABLE_HEADER_ROWS_NUMBER;
        for (int i = 0; i < tableRowsNumber; i++) {
            sheet.createRow((short)i);
        }
    }
    
//******************SIMPLE CONDITIONS WRITING***********************
    
    private void writeSimpleConditionColumn(int columnNumber, IDecisionTableColumn condition) {   
        conditionNumber++;
        writeSimpleConditionName(columnNumber, condition);
        writeSimpleConditionExpression(columnNumber, condition);
        writeSimpleConditionInitialization(columnNumber, condition);
        writeSimpleConditionDisplayName(columnNumber, condition);
        writeSimpleRuleValue(columnNumber, condition);
    }

    /**
     * Condition name is always in the first row of the table.
     * 
     * @param sheet  
     * @param colNum
     */
    private void writeSimpleConditionName(int colNum, IDecisionTableColumn condition) {
        Cell cell = sheet.getRow(CONDITION_NAME_ROW_INDEX).createCell(colNum);
        cell.setCellValue(String.format("%s%s", condition.getColumnType(), conditionNumber));
    }
    
    /**
     * Condition expression is in the second row of table.
     * 
     * @param sheet
     * @param colNum
     */
    private void writeSimpleConditionExpression(int columnNumber, IDecisionTableColumn condition) {        
        Cell cell = sheet.getRow(CODE_EXPRESSION_ROW_INDEX).createCell(columnNumber);
        cell.setCellValue(condition.getCodeExpression());       
    }
    
    /**
     * Condition expression is in the third row of table.
     * 
     * @param sheet
     * @param colNum
     */
    private void writeSimpleConditionInitialization(int colNum, IDecisionTableColumn condition) {
        Cell cell = sheet.getRow(CONDITION_PARAMETER_ROW_INDEX).createCell(colNum);
        cell.setCellValue(condition.getParameterDeclaration());        
    }
    
    private void writeSimpleConditionDisplayName(int colNum, IDecisionTableColumn condition) {
        Cell cell = sheet.getRow(CONDITION_TITLE_ROW_INDEX).createCell(colNum);
        cell.setCellValue(condition.getTitle());
    }
    
    /**
     * Fills rule values for all rules in this condition. Starts writing from the fourth row.
     * 
     * @param sheet
     * @param columnNumber
     */
    private void writeSimpleRuleValue(int columnNumber, IDecisionTableColumn condition) {
        for (int i = 0; i < rulesNumber; i++) {
            Cell cell = sheet.getRow(DECISION_TABLE_HEADER_ROWS_NUMBER + i).createCell(columnNumber);
            cell.setCellValue(condition.getRuleValue(i));
        }        
    }    
    //******************END SIMPLE CONDITIONS WRITING***********************
    
//******************ARRAY ENUM WRITING***********************
    
    
    /**
     * @return number of written columns
     */
    private int writeArrayConditionColumn(int columnNumber, IDecisionTableColumn condition) {
        int numberOfValues = condition.getMaxNumberOfValuesForRules();
        
        if (numberOfValues > 0) {
            conditionNumber++; //is used further in writing logic
            
            writeArrayConditionName(columnNumber, numberOfValues, condition);
            writeArrayConditionExpression(columnNumber, numberOfValues, condition);
            writeArrayConditionInitialization(columnNumber, numberOfValues, condition);
            writeArrayConditionDisplayName(columnNumber, numberOfValues, condition);
            writeArrayRuleValue(columnNumber, numberOfValues, condition);
        }
        
        return numberOfValues;        
    }    
    
    

    private void writeArrayConditionName(int columnIndex, int numberOfValues, IDecisionTableColumn condition) {
        Cell cell = sheet.getRow(CONDITION_NAME_ROW_INDEX).createCell(columnIndex);
        
        cell.setCellValue(String.format("%s%s",  condition.getColumnType(), conditionNumber)); 
        
        mergeArrayCells(CONDITION_NAME_ROW_INDEX, columnIndex, numberOfValues);         
    }
    
    private void writeArrayConditionExpression(int columnIndex, int numberOfValues, IDecisionTableColumn condition) {
        Cell cell = sheet.getRow(CODE_EXPRESSION_ROW_INDEX).createCell(columnIndex);
        
        cell.setCellValue(condition.getCodeExpression());   
        
        mergeArrayCells(CODE_EXPRESSION_ROW_INDEX, columnIndex, numberOfValues);      
    }
    
    private void writeArrayConditionInitialization(int columnNumber, int numOfElements, IDecisionTableColumn condition) {                
        for (int i = 0; i < numOfElements; i ++) {
            Cell cell = sheet.getRow(CONDITION_PARAMETER_ROW_INDEX).createCell(columnNumber);
            cell.setCellValue(String.format("%s%d", condition.getParameterDeclaration(), (i + 1)));
            columnNumber++;
        }        
    }
    
    private void writeArrayConditionDisplayName(int columnIndex, int numberOfValues, IDecisionTableColumn condition) {
        Cell cell = sheet.getRow(CONDITION_TITLE_ROW_INDEX).createCell(columnIndex);
        cell.setCellValue(condition.getTitle());
        
        mergeArrayCells(CONDITION_TITLE_ROW_INDEX, columnIndex, numberOfValues);
    }
    
    private void mergeArrayCells(int rowIndex, int columnIndex, int numberOfValues) {
        // counting begins from 0
        int lastMergedColumnIndex = columnIndex + numberOfValues - 1;
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 
                columnIndex, lastMergedColumnIndex));
    }

    private void writeArrayRuleValue(int colNum, int numCountriesColumns, IDecisionTableColumn condition) {
        int startCol = colNum;
        for (int i = 0; i < rulesNumber; i++) {
            for (int j = 0; j < numCountriesColumns; j++) {
                Cell cell = sheet.getRow(i + DECISION_TABLE_HEADER_ROWS_NUMBER).createCell(colNum);
                cell.setCellValue(condition.getRuleValue(i, colNum - startCol));                
                colNum++;
            }
            colNum = startCol;
        }        
    }
    
    //******************END ARRAY ENUM WRITING***********************
    
    //******************RETURN SECTION WRITING***********************
    
    private void writeReturnColumn(int colNum) {
        Cell cell = sheet.getRow(CONDITION_NAME_ROW_INDEX).createCell(colNum);
        cell.setCellValue(returnColumn.getColumnType());
        
        Cell cell1 = sheet.getRow(CODE_EXPRESSION_ROW_INDEX).createCell(colNum);
        cell1.setCellValue(returnColumn.getCodeExpression());
        
        Cell cell2 = sheet.getRow(CONDITION_PARAMETER_ROW_INDEX).createCell(colNum);
        cell2.setCellValue(returnColumn.getParameterDeclaration());
        
        Cell cell3 = sheet.getRow(CONDITION_TITLE_ROW_INDEX).createCell(colNum);
        cell3.setCellValue(returnColumn.getTitle());
        
        for (int i = 0; i < rulesNumber; i++) {
            Cell cellRule = sheet.getRow(i + DECISION_TABLE_HEADER_ROWS_NUMBER).createCell(colNum);
            cellRule.setCellValue(returnColumn.getRuleValue(i)); 
        }
    }
    
    //******************END RETURN SECTION WRITING***********************

// ----method is using for testing----
//
//    private void writeTableToFile() {
//        String __src = "src/rules/Test.xls";
//        FileOutputStream fileOut;
//        try {
//            
//            fileOut = new FileOutputStream(__src);
//            workbook.write(fileOut);
//            fileOut.close();
//        } catch (FileNotFoundException e) {            
//            e.printStackTrace();
//        } catch (IOException e) {            
//            e.printStackTrace();
//        }
//    }
    
    private String buildMethodHeader() {
        String start = String.format("%s %s %s(", IXlsTableNames.DECISION_TABLE2, returnColumn.getReturnType().getDisplayName(0), 
                tableName);
        StringBuffer strBuf = new StringBuffer();
        strBuf.append(start);
        strBuf.append(returnColumn.paramsThroughComma());
        strBuf.append(")");
        
        return strBuf.toString();
    }
    
    private void writeHeaderRow(int lastColNum) {
        int rowNumForHeader = 0;
        String tableName = buildMethodHeader();
        
        Cell cell_0_0 = sheet.getRow(rowNumForHeader).createCell(0);
        cell_0_0.setCellValue(tableName);        
        sheet.addMergedRegion(new CellRangeAddress(rowNumForHeader, rowNumForHeader, 
                cell_0_0.getColumnIndex(), lastColNum));
    }
}
