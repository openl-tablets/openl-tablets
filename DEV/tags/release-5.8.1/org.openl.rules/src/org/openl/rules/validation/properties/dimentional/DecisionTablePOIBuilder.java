//package org.openl.rules.validation.properties.dimentional;
//
//import org.apache.poi.ss.usermodel.Sheet;
//
///**
// * Creates the memory representation of DT table by POI.
// * 
// * @author DLiauchuk
// *
// */
//public class DecisionTablePOIBuilder {
//    
//    /** number 5 - is a number of first development rows in table.*/
//    public static final int DECISION_TABLE_HEADER_ROWS_NUMBER = 5;  
//    public static final int CONDITION_TITLE_ROW_INDEX = 4;
//    public static final int PARAMETER_DECLARATION_ROW_INDEX = 3;
//    public static final int CODE_EXPRESSION_ROW_INDEX = 2;
//    
//    /** condition name always is the next row after header row.*/
//    public static final int COLUMN_TYPE_ROW_INDEX = 1; 
//    
//    private TableHeaderPOIBuilder headerBuilder;
//    private IDecisionTableColumnBuilder conditionsBuilder;
//    private IDecisionTableColumnBuilder returnBuilder;
//    
//    public DecisionTablePOIBuilder(TableHeaderPOIBuilder headerBuilder, IDecisionTableColumnBuilder conditionsBuilder, 
//            IDecisionTableColumnBuilder returnBuilder) {
//        this.headerBuilder = headerBuilder;
//        this.conditionsBuilder = conditionsBuilder;
//        this.returnBuilder = returnBuilder;
//    }  
//    
//    /**
//     * Builds the decision table on the given sheet with given number of rules.
//     * Starts building from the top left corner ([0; 0] coordinates).
//     * 
//     * @param sheet sheet to build table on it.
//     * @param rulesNumber number of rules for decision table
//     * 
//     * @return sheet with builded table on it.
//     */
//    public Sheet build(Sheet sheet, int rulesNumber) {
//        // table writing starts from 0,0 indexes
//        // create all needed rows for table. 
//        //
//        createAllRows(sheet, rulesNumber);
//        
//        int lastColumnNumber = conditionsBuilder.build(sheet, rulesNumber, 0);
//        
//        returnBuilder.build(sheet, rulesNumber, lastColumnNumber);
//        
//        headerBuilder.build(sheet, lastColumnNumber);
//
//        //writeTableToFile();
//        
//        return sheet;
//    }
//    
//    public void createAllRows(Sheet sheet, int rulesNumber) {
//        int tableRowsNumber = rulesNumber + DECISION_TABLE_HEADER_ROWS_NUMBER;
//        for (int i = 0; i < tableRowsNumber; i++) {
//            sheet.createRow((short)i);
//        }        
//    }
//}
