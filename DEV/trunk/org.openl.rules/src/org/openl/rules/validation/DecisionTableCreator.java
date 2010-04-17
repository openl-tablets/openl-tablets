package org.openl.rules.validation;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.enumeration.CountriesEnum;
import org.openl.rules.enumeration.UsregionsEnum;
import org.openl.rules.enumeration.UsstatesEnum;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.GridSplitter;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;

public class DecisionTableCreator {
    
    private static final int SERVICE_ROWS_NUMBER = 5; // number 5 - is a number of first development rows.
    private static final int ROW_NUM_CONDITION_DISPLAY_NAME = 4;
    private static final int ROW_NUM_CONDITION_INITIALIZATION = 3;
    private static final int ROW_NUM_CONDITION_EXPRESSION = 2;
    private static final int ROW_NUM_CONDITION_NAME = 1; // condition name always is the next row after header row.
    private static final String RESULT_VAR = "result";    
    public static final String CURRENT_DATE_PARAM = "currentDate";
    public static final String LOCAL_PARAM_SUFFIX = "Local";
    
    private static String __src = "src/rules/Test.xls";;
    
    private static final String STATE_PROP = "state";
    private static final String COUNTRY_PROP = "country";
    private static final String US_REGION_PROP = "usregion";
    private static final String LOB_PROP = "lob";
    private static final String EXPIRATION_DATE_PROP = "expirationDate";
    private static final String EFFECTIVE_DATE_PROP = "effectiveDate";    
    
    private int simpleConditionsWidth = 0;
    private int mergedConditionsWidth = 0;
    private String originalTableName;
    private String newTableName;
    private IMethodSignature originalSignature;
    private List<TableSyntaxNode> tablesGroup;    
    private List<String> simpleDimProp = new ArrayList<String>();
    private List<String> arrayDimProp = new ArrayList<String>();
    private IOpenClass originalReturnType;
    private DecisionTable createdDecTable;
    private XlsSheetGridModel createdSheetGridModel;
    
    // income parameters are hardcoded
    private static final Map<String, IOpenClass> incomeParams;
    
    static {
        incomeParams = new HashMap<String, IOpenClass>();
        incomeParams.put(CURRENT_DATE_PARAM, JavaOpenClass.getOpenClass(Date.class));
        incomeParams.put(STATE_PROP, JavaOpenClass.getOpenClass(UsstatesEnum.class));
        incomeParams.put(LOB_PROP, JavaOpenClass.getOpenClass(String.class));
        incomeParams.put(US_REGION_PROP, JavaOpenClass.getOpenClass(UsregionsEnum.class));
        incomeParams.put(COUNTRY_PROP, JavaOpenClass.getOpenClass(CountriesEnum.class));
    }
    
    public DecisionTableCreator(String originalTableName, IMethodSignature originalSignature,
            List<TableSyntaxNode> tablesGroup, String[] dimensionalTableProp,
            IOpenClass originalReturnType) {        
        for (String propName : dimensionalTableProp) {
            if (TablePropertyDefinitionUtils.getPropertyTypeByPropertyName(propName).isArray()) {
                mergedConditionsWidth++;
                arrayDimProp.add(propName);
            } else {
                simpleConditionsWidth++;
                simpleDimProp.add(propName);
            }
        }
        this.originalSignature = originalSignature;
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
        Workbook wb = new HSSFWorkbook();
        
        Sheet sheet = wb.createSheet(DispatcherTableBuilder.DISPATCHER_TABLES_SHEET + originalTableName);
        
        modifySimpleConditionsWidth();
        
        // table writing starts from 0,0 indexes
        // create all needed rows for table. 
        createAllRows(sheet);
        
        // write simple conditions, that are not an array type.
        for (int i = 0; i < simpleConditionsWidth; i++) {
            writeSimpleConditionColumn(sheet, i);
        }
        
        int lastColNum = writeCountriesConditionColumn(sheet, simpleConditionsWidth);
        
        writeReturnColumn(sheet, lastColNum);
        
        writeHeaderRow(sheet, lastColNum);

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
    
    /**
     * We need to exclude those columns that for all rules have empty values.
     */
    private void modifySimpleConditionsWidth() {
         simpleConditionsWidth = simpleConditionsWidth();
    }
    
    /**
     * Exclude conditions that for all rules have empty values
     * 
     * @return new number of simple conditions.
     */
    private int simpleConditionsWidth() {
        int result = 0;        
        List<String> propertiesToRemove = new ArrayList<String>();
        for (int i = 0; i < simpleConditionsWidth; i++) {
            if (isEmptyValuesForAllRules(i)) {                
                propertiesToRemove.add(simpleDimProp.get(i));
            }
        }
        result = simpleDimProp.size() - propertiesToRemove.size();
        
        simpleDimProp.removeAll(propertiesToRemove);
        
        return result;
    }

    private void createAllRows(Sheet sheet) {        
        for (int i = 0; i < tablesGroup.size() + SERVICE_ROWS_NUMBER; i++) {
            sheet.createRow((short)i);
        }
    }

    //******************SIMPLE CONDITIONS WRITING***********************
    
    private void writeSimpleConditionColumn(Sheet sheet, int colNum) {        
        writeSimpleConditionName(sheet, colNum);
        writeSimpleConditionExpression(sheet, colNum);
        writeSimpleConditionInitialization(sheet, colNum);
        writeSimpleConditionDisplayName(sheet, colNum);
        writeSimpleRuleValue(sheet, colNum);
    }
    
    private boolean isEmptyValuesForAllRules(int colNum) {
        boolean result = false;
        int filledValues = 0;
        for (int i = 0; i< tablesGroup.size(); i++) {
            String propValue = getSimpleConditionRuleValue(tablesGroup.get(i), colNum);
            if (StringUtils.isNotEmpty(propValue)) {
                filledValues++;
            }
        }
        if (filledValues == 0) {
            result = true;
        }
        return result;
    }

    /**
     * Condition name is always in the first row of the table.
     * 
     * @param sheet  
     * @param colNum
     */
    private void writeSimpleConditionName(Sheet sheet, int colNum) {
        Cell cell = sheet.getRow(ROW_NUM_CONDITION_NAME).createCell(colNum);
        cell.setCellValue("C"+(colNum+1));
    }
    
    /**
     * Condition expression is in the second row of table.
     * 
     * @param sheet
     * @param colNum
     */
    private void writeSimpleConditionExpression(Sheet sheet, int colNum) {        
        Cell cell = sheet.getRow(ROW_NUM_CONDITION_EXPRESSION).createCell(colNum);
        cell.setCellValue(getSimpleConditionExpression(colNum));        
    }
    
    /**
     * Condition expression is in the third row of table.
     * 
     * @param sheet
     * @param colNum
     */
    private void writeSimpleConditionInitialization(Sheet sheet, int colNum) {
        Cell cell = sheet.getRow(ROW_NUM_CONDITION_INITIALIZATION).createCell(colNum);
        cell.setCellValue(getSimpleConditionInitialization(colNum));        
    }
    
    private void writeSimpleConditionDisplayName(Sheet sheet, int colNum) {
        Cell cell = sheet.getRow(ROW_NUM_CONDITION_DISPLAY_NAME).createCell(colNum);
        cell.setCellValue(getSimpleConditionDisplayName(colNum));
    }
    
    /**
     * Fills rule values for all rules in this condition. Starts writing from the fourth row.
     * 
     * @param sheet
     * @param colNum
     */
    private void writeSimpleRuleValue(Sheet sheet, int colNum) {
        for (int i = 0; i< tablesGroup.size(); i++) {
            Cell cell = sheet.getRow(i+SERVICE_ROWS_NUMBER).createCell(colNum);
            cell.setCellValue(getSimpleConditionRuleValue(tablesGroup.get(i), colNum));
        }        
    }    
    //******************END SIMPLE CONDITIONS WRITING***********************
    
    //******************COUNTRIES ENUM WRITING***********************
    
    
    /**
     * @return number of the column to write next column
     */
    private int writeCountriesConditionColumn(Sheet sheet, int colNum) {   
        int numCountriesColumns = findMaxNumberOfCountries(colNum);
        if (numCountriesColumns > 0) {
            writeCountriesConditionName(sheet, colNum, numCountriesColumns);
            writeCountriesConditionExpression(sheet, colNum, numCountriesColumns);
            writeCountriesConditionInitialization(sheet, colNum, numCountriesColumns);
            writeCountriesConditionDisplayName(sheet, colNum, numCountriesColumns);
            writeCountriesRuleValue(sheet, colNum, numCountriesColumns);
        }
        return colNum + numCountriesColumns;        
    }    
    
    private int findMaxNumberOfCountries(int colNum) {
        int result = 0;
        TreeSet<Integer> numOfCountries = new TreeSet<Integer>();
        for (int i = 0; i < tablesGroup.size(); i++) {
            CountriesEnum[] countries = (CountriesEnum[])tablesGroup.get(i).getTableProperties().getPropertyValue(COUNTRY_PROP);
            if (countries != null && countries.length > 0) {
                numOfCountries.add(Integer.valueOf(countries.length));
            }
        }
        if (numOfCountries.size() > 0) {
            result = numOfCountries.last().intValue();
        }
        return result;
    }

    private void writeCountriesConditionName(Sheet sheet, int colNum, int numCountriesColumns) {
        Cell cell = sheet.getRow(ROW_NUM_CONDITION_NAME).createCell(colNum);
        cell.setCellValue("C"+(colNum+1));   
        sheet.addMergedRegion(new CellRangeAddress(ROW_NUM_CONDITION_NAME, ROW_NUM_CONDITION_NAME, 
                colNum, colNum + numCountriesColumns - 1));         
    }
    
    private void writeCountriesConditionExpression(Sheet sheet, int colNum, int numCountriesColumns) {
        Cell cell = sheet.getRow(ROW_NUM_CONDITION_EXPRESSION).createCell(colNum);
        cell.setCellValue(getCountriesAlgorithm(colNum, numCountriesColumns));    
        sheet
        .addMergedRegion(new CellRangeAddress(ROW_NUM_CONDITION_EXPRESSION, ROW_NUM_CONDITION_EXPRESSION, 
                colNum, colNum + numCountriesColumns - 1));         
    }
    
    private void writeCountriesConditionInitialization(Sheet sheet, int colNum, int numCountriesColumns) {                
        String dimPropName = arrayDimProp.get(colNum - simpleConditionsWidth);
        for (int i = 0;i < numCountriesColumns; i++) {
            Cell cell = sheet.getRow(ROW_NUM_CONDITION_INITIALIZATION).createCell(colNum);
            cell.setCellValue(String.format("%s %s", CountriesEnum.class.getSimpleName(), dimPropName + LOCAL_PARAM_SUFFIX + (i+1)));
            colNum++;
        }        
    }
    
    private void writeCountriesConditionDisplayName(Sheet sheet, int colNum, int numCountriesColumns) {
        String dimPropName = arrayDimProp.get(colNum - simpleConditionsWidth);        
        TablePropertyDefinition propDef = TablePropertyDefinitionUtils.getPropertyByName(dimPropName);        
        for (int i = 0;i < numCountriesColumns; i++) {
            Cell cell = sheet.getRow(ROW_NUM_CONDITION_DISPLAY_NAME).createCell(colNum);
            cell.setCellValue(propDef.getDisplayName() + (i+1));
            colNum++;
        }
    }

    private void writeCountriesRuleValue(Sheet sheet, int colNum, int numCountriesColumns) {
        int startCol = colNum;
        for (int i = 0; i < tablesGroup.size(); i++) {
            for (int j = 0;j < numCountriesColumns; j++) {
                Cell cell = sheet.getRow(i+SERVICE_ROWS_NUMBER).createCell(colNum);
                cell.setCellValue(getCountriesRuleValue(tablesGroup.get(i), colNum - startCol));                
                colNum++;
            }
            colNum = startCol;
        }        
    }

    private String getCountriesRuleValue(TableSyntaxNode tableSyntaxNode, int elementNum) {
        String countriesThroughComma = tableSyntaxNode.getTableProperties().getPropertyValueAsString(COUNTRY_PROP);
        String[] countries = StringUtils.split(countriesThroughComma, ",");
        if (countries != null && (countries.length > elementNum)) {
            return countries[elementNum];
        } else {
            return null;
        }         
    }    

    private String getCountriesAlgorithm(int colNum, int numCountriesColumns) {
        StringBuffer strBuf = new StringBuffer();
        int paramNum = numCountriesColumns;
        for (int i = 1;i <= numCountriesColumns; i++) {
            paramNum--;
            strBuf.append(String.format("%s == %s", 
                    COUNTRY_PROP + LOCAL_PARAM_SUFFIX + i, 
                    COUNTRY_PROP));
            if (paramNum > 0) {
                strBuf.append(" || ");
            }
        }
        return strBuf.toString();        
    }
    //******************END COUNTRIES ENUM WRITING***********************
    
    
    //******************RETURN SECTION WRITING***********************
    
    private void writeReturnColumn(Sheet sheet, int colNum) {
        Cell cell = sheet.getRow(ROW_NUM_CONDITION_NAME).createCell(colNum);
        cell.setCellValue("RET");
        
        Cell cell1 = sheet.getRow(ROW_NUM_CONDITION_EXPRESSION).createCell(colNum);
        cell1.setCellValue(RESULT_VAR);
        
        Cell cell2 = sheet.getRow(ROW_NUM_CONDITION_INITIALIZATION).createCell(colNum);
        cell2.setCellValue(String.format("%s %s", originalReturnType.getDisplayName(0), RESULT_VAR));
        
        Cell cell3 = sheet.getRow(ROW_NUM_CONDITION_DISPLAY_NAME).createCell(colNum);
        cell3.setCellValue(RESULT_VAR.toUpperCase());
        
        for (int i = 0; i< tablesGroup.size(); i++) {
            Cell cellRule = sheet.getRow(i + SERVICE_ROWS_NUMBER).createCell(colNum);
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private String getSimpleConditionRuleValue(TableSyntaxNode tsn, int colNum) {
        String dimPropName = simpleDimProp.get(colNum);
        return tsn.getTableProperties().getPropertyValueAsString(dimPropName);        
    }

    private String getSimpleConditionInitialization(int colNum) {        
        String dimPropName = simpleDimProp.get(colNum);
        TablePropertyDefinition propDef = TablePropertyDefinitionUtils.getPropertyByName(dimPropName);
        return String.format("%s %s", propDef.getType().getInstanceClass().getSimpleName(), dimPropName + LOCAL_PARAM_SUFFIX);
    }
    
    private String getSimpleConditionDisplayName(int colNum) {        
        String dimPropName = simpleDimProp.get(colNum);
        TablePropertyDefinition propDef = TablePropertyDefinitionUtils.getPropertyByName(dimPropName);
        return propDef.getDisplayName();
    }

    private String getSimpleConditionExpression(int colNum) {
        String result = null;
        String dimPropName = simpleDimProp.get(colNum);        
        if (EFFECTIVE_DATE_PROP.equals(dimPropName)) {
            result = String.format("%s <= %s", 
                    EFFECTIVE_DATE_PROP + LOCAL_PARAM_SUFFIX, 
                    CURRENT_DATE_PARAM);
        } else if (EXPIRATION_DATE_PROP.equals(dimPropName)) {            
            result = String.format("%s <= %s", 
                    CURRENT_DATE_PARAM,
                    EXPIRATION_DATE_PROP + LOCAL_PARAM_SUFFIX);
        } else if (LOB_PROP.equals(dimPropName)) {
            result = String.format("%s == %s", 
                    LOB_PROP + LOCAL_PARAM_SUFFIX,
                    LOB_PROP);
        } else if (US_REGION_PROP.equals(dimPropName)) {
            result = String.format("%s == %s", 
                    US_REGION_PROP + LOCAL_PARAM_SUFFIX, 
                    US_REGION_PROP);
        } else if (STATE_PROP.equals(dimPropName)) {
            result = String.format("%s == %s", 
                    STATE_PROP + LOCAL_PARAM_SUFFIX, 
                    STATE_PROP);
        }
        return result;
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
        StringBuffer strBuf = new StringBuffer();
        int paramNum = params.size();
        for (Map.Entry<String, IOpenClass> param : params.entrySet()) {
            paramNum--;
            strBuf.append(param.getValue().getInstanceClass().getSimpleName()).append(" ").append(param.getKey());
            if (paramNum > 0) {
                strBuf.append(", ");
            }
        }
        return strBuf.toString();
    }
    
    private String originalParamsWithTypesThroughComma() {
        StringBuffer strBuf = new StringBuffer();
        int paramNum = originalSignature.getNumberOfParameters();
        for (int j = 0; j < originalSignature.getNumberOfParameters(); j++) {
            paramNum--;
            strBuf.append(originalSignature.getParameterType(j).getInstanceClass().getSimpleName()).append(" ")
                .append(originalSignature.getParameterName(j));
            if (paramNum > 0) {
                strBuf.append(", ");
            }
        }   
        return strBuf.toString();
    }
    
    private String originalParamsThroughComma() {
        StringBuffer strBuf = new StringBuffer();
        int paramNum = originalSignature.getNumberOfParameters();
        for (int i = 0; i < originalSignature.getNumberOfParameters(); i++) {
            paramNum--;
            strBuf.append(originalSignature.getParameterName(i));
            if (paramNum > 0) {
                strBuf.append(", ");
            }
        }
        return strBuf.toString();
    }
    
    private GridTable getGridTable(Sheet sheet) {
        Workbook wb = sheet.getWorkbook();

        //it is just for correct work of webstudio
        XlsWorkbookSourceCodeModule mockWorkbookSource = new XlsWorkbookSourceCodeModule(new FileSourceCodeModule(__src, null), wb);
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
