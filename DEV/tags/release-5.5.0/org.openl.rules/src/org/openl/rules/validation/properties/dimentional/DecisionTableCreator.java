package org.openl.rules.validation.properties.dimentional;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.GridSplitter;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.StringTool;

public class DecisionTableCreator {
    
    private static final int DECISION_TABLE_HEADER_ROWS_NUMBER = 5; // number 5 - is a number of first development rows in table.
    private static final int CONDITION_TITLE_ROW_INDEX = 4;
    private static final int CONDITION_PARAMETER_ROW_INDEX = 3;
    private static final int CODE_EXPRESSION_ROW_INDEX = 2;
    private static final int CONDITION_NAME_ROW_INDEX = 1; // condition name always is the next row after header row.
    private static final String RESULT_VAR = "result";    
    public static final String LOCAL_PARAM_SUFFIX = "Local";
    
    //FIXME: remove this variable
    public static final String CURRENT_DATE_PARAM = "currentDate";
    
    private static String __src = "src/rules/Test.xls";;
    private static String FAKE_EXCEL_FILE = "/FAKE_EXCEL_FILE_FOR_DISPATCHER_TABLES.xls";;
    
    private int conditionNumber = 0;
    
    private String originalTableName;
    private String newTableName;
    private IMethodSignature originalSignature;
    private List<TableSyntaxNode> tablesGroup;
    private List<TablePropertyDefinition> simpleDimensionalProperties;
    private List<TablePropertyDefinition> arrayDimensionalProperties;
    
    private IOpenClass originalReturnType;
    private DecisionTable createdDecTable;
    private XlsSheetGridModel createdSheetGridModel;
    
    // income parameters are hardcoded
    private static final Map<String, IOpenClass> incomeParams;
    
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
    
    private static boolean belongsToExcluded(String methodName) {
        boolean result = false;
        if ("getValue".equals(methodName)) {
            result = true;
        }
        return result;
    }
    
    public DecisionTableCreator(List<TablePropertyDefinition> dimensionalProperties, List<TableSyntaxNode> tablesGroup, 
            String newTableName, IOpenClass originalReturnType, String originalTableName, 
            IMethodSignature originalSignature) { 
        this.tablesGroup = tablesGroup;
        this.originalSignature = originalSignature;
        this.originalReturnType = originalReturnType;
        this.originalTableName = originalTableName;
        this.newTableName = newTableName;
        
        // tablesGroup should already be initialized 
        initDimensionPropertiesLists(dimensionalProperties);
        
    }

    private void initDimensionPropertiesLists(List<TablePropertyDefinition> dimensionalTableProp) {
        simpleDimensionalProperties = new ArrayList<TablePropertyDefinition>();
        arrayDimensionalProperties = new ArrayList<TablePropertyDefinition>();
        
        for (TablePropertyDefinition property : dimensionalTableProp) {
            if (isPropertyValueSetInTables(property.getName())) {
                if (property.getType().getInstanceClass().isArray()) {
                    arrayDimensionalProperties.add(property);
                } else {
                    simpleDimensionalProperties.add(property);
                }
            }
        }
    }    
    
    private boolean isPropertyValueSetInTables(String propertyName) {
        boolean isPropertyValueSet = false;

        for (TableSyntaxNode tsn : tablesGroup) {
            String propertyValue = tsn.getTableProperties().getPropertyValueAsString(propertyName);            
            if (StringUtils.isNotEmpty(propertyValue)) {
                isPropertyValueSet = true;
                break;
            }
        }

        return isPropertyValueSet;        
    }

    public GridTable createGridTable() {
        Sheet sheet = writeTableByPOI();
        
        return getGridTable(sheet);
    }
    
    private Sheet writeTableByPOI() {
        Workbook wb = new HSSFWorkbook();
        
        Sheet sheet = wb.createSheet(DispatcherTableBuilder.DISPATCHER_TABLES_SHEET + originalTableName);
        
        // table writing starts from 0,0 indexes
        // create all needed rows for table. 
        createAllRows(sheet);
        
        int nextColumn = 0;
        
        // write simple conditions, that are not an array type.
        for (TablePropertyDefinition property : simpleDimensionalProperties) {
            writeSimpleConditionColumn(sheet, nextColumn, property);
            nextColumn++;
        }
        
        for (TablePropertyDefinition property : arrayDimensionalProperties) {
            int numberOfWrittenColumns = writeArrayConditionColumn(sheet, nextColumn, property);
            nextColumn += numberOfWrittenColumns;
        }
        
        int lastColumnNumber = nextColumn;
        
        writeReturnColumn(sheet, lastColumnNumber);
        
        writeHeaderRow(sheet, lastColumnNumber);

        //writeTableToFile(wb);
        
        return sheet;
    }

    private void writeHeaderRow(Sheet sheet, int lastColNum) {
        int rowNumForHeader = 0;
        String tableName = buildMethodHeader();
        
        Cell cell_0_0 = sheet.getRow(rowNumForHeader).createCell(0);
        cell_0_0.setCellValue(tableName);
        sheet.addMergedRegion(new CellRangeAddress(rowNumForHeader, rowNumForHeader, 
                cell_0_0.getColumnIndex(), lastColNum));
    }

    private void createAllRows(Sheet sheet) {        
        for (int i = 0; i < tablesGroup.size() + DECISION_TABLE_HEADER_ROWS_NUMBER; i++) {
            sheet.createRow((short)i);
        }
    }

    //******************SIMPLE CONDITIONS WRITING***********************
    
    private void writeSimpleConditionColumn(Sheet sheet, int columnNumber, TablePropertyDefinition property) {   
        conditionNumber++;
        writeSimpleConditionName(sheet, columnNumber);
        writeSimpleConditionExpression(sheet, columnNumber, property);
        writeSimpleConditionInitialization(sheet, columnNumber, property);
        writeSimpleConditionDisplayName(sheet, columnNumber, property);
        writeSimpleRuleValue(sheet, columnNumber, property);
    }
    


    /**
     * Condition name is always in the first row of the table.
     * 
     * @param sheet  
     * @param colNum
     */
    private void writeSimpleConditionName(Sheet sheet, int colNum) {
        Cell cell = sheet.getRow(CONDITION_NAME_ROW_INDEX).createCell(colNum);
        cell.setCellValue(String.format("C%s", conditionNumber));
    }
    
    /**
     * Condition expression is in the second row of table.
     * 
     * @param sheet
     * @param colNum
     */
    private void writeSimpleConditionExpression(Sheet sheet, int columnNumber, TablePropertyDefinition property) {        
        Cell cell = sheet.getRow(CODE_EXPRESSION_ROW_INDEX).createCell(columnNumber);
        cell.setCellValue(getSimpleConditionCodeExpression(property));        
    }
    
    /**
     * Condition expression is in the third row of table.
     * 
     * @param sheet
     * @param colNum
     */
    private void writeSimpleConditionInitialization(Sheet sheet, int colNum, TablePropertyDefinition property) {
        Cell cell = sheet.getRow(CONDITION_PARAMETER_ROW_INDEX).createCell(colNum);
        cell.setCellValue(getSimpleConditionInitialization(property));        
    }
    
    private void writeSimpleConditionDisplayName(Sheet sheet, int colNum, TablePropertyDefinition property) {
        Cell cell = sheet.getRow(CONDITION_TITLE_ROW_INDEX).createCell(colNum);
        cell.setCellValue(property.getDisplayName());
    }
    
    /**
     * Fills rule values for all rules in this condition. Starts writing from the fourth row.
     * 
     * @param sheet
     * @param columnNumber
     */
    private void writeSimpleRuleValue(Sheet sheet, int columnNumber, TablePropertyDefinition property) {
        for (int i = 0; i < tablesGroup.size(); i++) {
            Cell cell = sheet.getRow(DECISION_TABLE_HEADER_ROWS_NUMBER + i).createCell(columnNumber);
            
            TableSyntaxNode tsn = tablesGroup.get(i);
            String propertyValue = tsn.getTableProperties().getPropertyValueAsString(property.getName());
            cell.setCellValue(propertyValue);
        }        
    }    
    //******************END SIMPLE CONDITIONS WRITING***********************
    
    
    //******************ARRAY ENUM WRITING***********************
    
    
    /**
     * @return number of written columns
     */
    private int writeArrayConditionColumn(Sheet sheet, int columnNumber, TablePropertyDefinition property) {   
        int numberOfValues = findMaxNumberOfArrayValuesForProperty(property.getName());
        
        if (numberOfValues > 0) {
            conditionNumber++; //is used further in writing logic
            
            writeArrayConditionName(sheet, columnNumber, numberOfValues);
            writeArrayConditionExpression(sheet, columnNumber, numberOfValues, property);
            writeArrayConditionInitialization(sheet, columnNumber, numberOfValues, property);
            writeArrayConditionDisplayName(sheet, columnNumber, numberOfValues, property);
            writeArrayRuleValue(sheet, columnNumber, numberOfValues, property);
        }
        
        return numberOfValues;        
    }    
    
    private int findMaxNumberOfArrayValuesForProperty(String propertyName) {
        int maxNumberOfArrayValues = 0;
        
        for (TableSyntaxNode tsn : tablesGroup) {
            Object[] values = (Object[])tsn.getTableProperties().getPropertyValue(propertyName);
            if (values != null) {
                int numberOfValues = values.length;
                if (numberOfValues > maxNumberOfArrayValues) {
                    maxNumberOfArrayValues = numberOfValues;
                }
            }
        }
        
        return maxNumberOfArrayValues;
    }

    private void writeArrayConditionName(Sheet sheet, int columnIndex, int numberOfValues) {
        Cell cell = sheet.getRow(CONDITION_NAME_ROW_INDEX).createCell(columnIndex);
        
        cell.setCellValue(String.format("C%s", conditionNumber)); 
        
        // counting begins from 0
        int lastMergedColumnIndex = columnIndex + numberOfValues - 1;
        sheet.addMergedRegion(new CellRangeAddress(CONDITION_NAME_ROW_INDEX, CONDITION_NAME_ROW_INDEX, 
                columnIndex, lastMergedColumnIndex));         
    }
    
    private void writeArrayConditionExpression(Sheet sheet, int columnIndex, int numberOfValues, TablePropertyDefinition property) {
        Cell cell = sheet.getRow(CODE_EXPRESSION_ROW_INDEX).createCell(columnIndex);
        
        cell.setCellValue(getCodeExpressionForArrayCondition(property, numberOfValues));   
        
        // counting begins from 0
        int lastMergedColumnIndex = columnIndex + numberOfValues - 1;
        sheet.addMergedRegion(new CellRangeAddress(CODE_EXPRESSION_ROW_INDEX,
                CODE_EXPRESSION_ROW_INDEX, columnIndex, lastMergedColumnIndex));       
    }
    
    private void writeArrayConditionInitialization(Sheet sheet, int columnNumber, int numOfElements, TablePropertyDefinition property) {
        Class<?> componentType = property.getType().getInstanceClass().getComponentType();        
        for (int i = 0; i < numOfElements; i ++) {
            Cell cell = sheet.getRow(CONDITION_PARAMETER_ROW_INDEX).createCell(columnNumber);
            cell.setCellValue(String
                    .format("%s %s", componentType.getSimpleName(), property.getName() + LOCAL_PARAM_SUFFIX + (i + 1)));
            columnNumber++;
        }        
    }
    
    private void writeArrayConditionDisplayName(Sheet sheet, int columnIndex, int numberOfValues, TablePropertyDefinition property) {
        Cell cell = sheet.getRow(CONDITION_TITLE_ROW_INDEX).createCell(columnIndex);
        cell.setCellValue(property.getDisplayName());
        // counting begins from 0
        int lastMergedColumnIndex = columnIndex + numberOfValues - 1;
        sheet.addMergedRegion(new CellRangeAddress(CONDITION_TITLE_ROW_INDEX,
                CONDITION_TITLE_ROW_INDEX, columnIndex, lastMergedColumnIndex));
    }

    private void writeArrayRuleValue(Sheet sheet, int colNum, int numCountriesColumns, TablePropertyDefinition property) {
        int startCol = colNum;
        for (int i = 0; i < tablesGroup.size(); i++) {
            for (int j = 0; j < numCountriesColumns; j++) {
                Cell cell = sheet.getRow(i + DECISION_TABLE_HEADER_ROWS_NUMBER).createCell(colNum);
                cell.setCellValue(getArrayRuleValue(tablesGroup.get(i), colNum - startCol, property.getName()));                
                colNum++;
            }
            colNum = startCol;
        }        
    }

    private String getArrayRuleValue(TableSyntaxNode tableSyntaxNode, int elementNum, String propertyName) {        
        String valuesThroughComma = tableSyntaxNode.getTableProperties().getPropertyValueAsString(propertyName);
        String[] values = StringUtils.split(valuesThroughComma, ",");
        if (values != null && (values.length > elementNum)) {
            return values[elementNum];
        } else {
            return null;
        }         
    }    
    
    //******************END ARRAY ENUM WRITING***********************
    
    //******************RETURN SECTION WRITING***********************
    
    private void writeReturnColumn(Sheet sheet, int colNum) {
        Cell cell = sheet.getRow(CONDITION_NAME_ROW_INDEX).createCell(colNum);
        cell.setCellValue("RET");
        
        Cell cell1 = sheet.getRow(CODE_EXPRESSION_ROW_INDEX).createCell(colNum);
        cell1.setCellValue(RESULT_VAR);
        
        Cell cell2 = sheet.getRow(CONDITION_PARAMETER_ROW_INDEX).createCell(colNum);
        cell2.setCellValue(String.format("%s %s", originalReturnType.getDisplayName(0), RESULT_VAR));
        
        Cell cell3 = sheet.getRow(CONDITION_TITLE_ROW_INDEX).createCell(colNum);
        cell3.setCellValue(RESULT_VAR.toUpperCase());
        
        for (int i = 0; i< tablesGroup.size(); i++) {
            Cell cellRule = sheet.getRow(i + DECISION_TABLE_HEADER_ROWS_NUMBER).createCell(colNum);
            cellRule.setCellValue(String.format("=%s(%s)", originalTableName, originalParamsThroughComma())); 
        }
    }
    
    //******************END RETURN SECTION WRITING***********************
    
    private void writeTableToFile(org.apache.poi.ss.usermodel.Workbook wb) {
        FileOutputStream fileOut;
        try {
            fileOut = new FileOutputStream(__src);
            wb.write(fileOut);
            fileOut.close();
        } catch (FileNotFoundException e) {            
            e.printStackTrace();
        } catch (IOException e) {            
            e.printStackTrace();
        }
    }


    private String getSimpleConditionInitialization(TablePropertyDefinition property) {        
        String propertyTypeName = property.getType().getInstanceClass().getSimpleName();
        return String.format("%s %s", propertyTypeName, property.getName() + LOCAL_PARAM_SUFFIX);
    }
    
    //FIXME refactor code expression generation for simple and array properties: extract match expression processing and process errors
    
    private String getSimpleConditionCodeExpression(TablePropertyDefinition property) {
        String result = StringUtils.EMPTY;
        
        String matchExpression = property.getExpression();
        
        //FIXME extract to utility and reuse in org.openl.codegen.tools.type.TablePropertyDefinitionWrapper#init()
        String operation = null; // one of le, gt, eq
        String contextAttribute = null; //name of attribute in context to match with
        if (StringUtils.isNotEmpty(matchExpression)) {
            int openBracketIndex = matchExpression.indexOf("(");
            int closeBracketIndex = matchExpression.indexOf(")");

            operation = matchExpression.substring(0, openBracketIndex).toUpperCase();
            contextAttribute = matchExpression.substring(openBracketIndex + 1, closeBracketIndex);
        }
        
        String propertyName = property.getName();
        
        //FIXME: move code to the proper class
        //addRelationBetweenPropertyAndContextAttribute(contextAttribute, propertyName);
        
        // simple values can have one of le, gt, eq operations
                
        if (StringUtils.isNotEmpty(operation) && StringUtils.isNotEmpty(contextAttribute)) {
            String codeFormat = null;
            
            if ("le".equalsIgnoreCase(operation)){
                codeFormat = "%s <= %s";
            } else if ("gt".equalsIgnoreCase(operation)){
                codeFormat = "%s >= %s";
            } else if ("eq".equalsIgnoreCase(operation)){
                codeFormat = "%s == %s";
            } else {              
                String message = String.format("Can`t create expression for \"%s\" property validation. Unknown match expression operation \"%s\"", propertyName, operation);
                OpenLMessagesUtils.addWarn(message);
            }
            
            if (codeFormat != null) {
                result = String.format(codeFormat, propertyName + LOCAL_PARAM_SUFFIX, contextAttribute);
            }
        } else {
            String message = String
                    .format("Can`t create expression for \"%s\" property validation. Wrong match expression \"%s\"",
                            propertyName, matchExpression);
            OpenLMessagesUtils.addWarn(message);
        }
        return result;
    }
        
    private String getCodeExpressionForArrayCondition(TablePropertyDefinition property, int numberOfValues) {
        String propertyName = property.getName();
        
        String matchExpression = property.getExpression();
        
        //FIXME extract to utility and reuse in org.openl.codegen.tools.type.TablePropertyDefinitionWrapper#init()
        String operation = null; // one of le, gt, eq, contains
        String contextAttribute = null; //name of attribute in context to match with
        if (StringUtils.isNotEmpty(matchExpression)) {
            int openBracketIndex = matchExpression.indexOf("(");
            int closeBracketIndex = matchExpression.indexOf(")");

            operation = matchExpression.substring(0, openBracketIndex).toUpperCase();
            contextAttribute = matchExpression.substring(openBracketIndex + 1, closeBracketIndex);
        }
        
        // array values can have only "contains" operation
        
        StringBuffer codeExpression = new StringBuffer();
        
        if (StringUtils.isNotEmpty(operation) && StringUtils.isNotEmpty(contextAttribute)) {
            if ("contains".equalsIgnoreCase(operation)){
                // building condition like "<propertyName>Local1 == <contextValue> || <propertyName>Local2 == <contextValue> || ..."
                for (int i = 1; i <= numberOfValues; i++) {
                    if (i > 1){
                        codeExpression.append(" || ");
                    }
                    
                    String expressionForOneValue = String.format(
                            "%s%s%d == %s", propertyName,
                            LOCAL_PARAM_SUFFIX, i, contextAttribute);
                    codeExpression.append(expressionForOneValue);
                }
            } else {
                String message = String
                    .format("Can`t create expression for \"%s\" property validation. Unknown match expression operation \"%s\"", 
                            propertyName, operation);
                OpenLMessagesUtils.addWarn(message);
            }
        } else {
            String message = String
                .format("Can`t create expression for \"%s\" property validation. Wrong match expression \"%s\"",
                    propertyName, matchExpression);
            OpenLMessagesUtils.addWarn(message);
        }
        
        return codeExpression.toString();        
    }

    private String buildMethodHeader() {
        String start = String.format("%s %s %s(", IXlsTableNames.DECISION_TABLE2, originalReturnType.getDisplayName(0), 
                newTableName);
        StringBuffer strBuf = new StringBuffer();
        strBuf.append(start);
        strBuf.append(originalParamsWithTypesThroughComma());  
        strBuf.append(", ");
        strBuf.append(paramsWithTypesThroughComma(incomeParams));
        strBuf.append(")");
        
        return strBuf.toString();
    }

    private String paramsWithTypesThroughComma(Map<String, IOpenClass> params) {
        List<String> values = new ArrayList<String>();
        for (Map.Entry<String, IOpenClass> param : params.entrySet()) {
            values.add(String.format("%s %s", param.getValue().getInstanceClass().getSimpleName(), param.getKey()));
        }
        
        return StringTool.listToStringThroughCommas(values);
    }
    
    private String originalParamsWithTypesThroughComma() {        
        List<String> values = new ArrayList<String>();        
        for (int j = 0; j < originalSignature.getNumberOfParameters(); j++) {
            values.add(String.format("%s %s", originalSignature.getParameterType(j).getInstanceClass().getSimpleName(), 
                    originalSignature.getParameterName(j)));            
        }   
        return StringTool.listToStringThroughCommas(values);
    }
    
    private String originalParamsThroughComma() {
        List<String> values = new ArrayList<String>();        
        for (int i = 0; i < originalSignature.getNumberOfParameters(); i++) {            
            values.add(originalSignature.getParameterName(i));            
        }
        return StringTool.listToStringThroughCommas(values);
    }
    
    private GridTable getGridTable(Sheet sheet) {
        Workbook wb = sheet.getWorkbook();

        //it is just for correct work of webstudio
        XlsWorkbookSourceCodeModule mockWorkbookSource = 
            new XlsWorkbookSourceCodeModule(new FileSourceCodeModule(FAKE_EXCEL_FILE, null), wb);
        XlsSheetSourceCodeModule mockSheetSource = new XlsSheetSourceCodeModule(sheet, sheet.getSheetName(), mockWorkbookSource);

        createdSheetGridModel = new XlsSheetGridModel(mockSheetSource);
        GridSplitter gridSplitter = new GridSplitter(createdSheetGridModel);
        GridTable[] gridTables = gridSplitter.split();
        
        GridTable gridTable = gridTables[0];        
        
        createDecisionTable();
        
        return gridTable;
    }
    
    private void createDecisionTable() {
        int paramsNum = originalSignature.getNumberOfParameters() + incomeParams.size(); 
        IOpenClass[] paramTypes = new IOpenClass[paramsNum];
        String[] paramNames = new String[paramsNum];
        int i = 0;
        
        // add original table params
        for (int j = 0; j < originalSignature.getNumberOfParameters(); j++) {
            paramTypes[i] = originalSignature.getParameterType(j);
            paramNames[i] = originalSignature.getParameterName(j);
            i++;
        }
        
        // add new income params
        for (Map.Entry<String, IOpenClass> param : incomeParams.entrySet()) {
            paramTypes[i] = param.getValue();
            paramNames[i] = param.getKey();
            i++;
        }
        
        IMethodSignature signature = new MethodSignature(paramTypes, paramNames);
        IOpenClass declaringClass = null; // can be null.        
        
        IOpenMethodHeader methodHeader = new OpenMethodHeader(newTableName, originalReturnType, signature, declaringClass);
        createdDecTable = new DecisionTable(methodHeader);                
    }

    public DecisionTable getCreatedDecisionTable() {
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
