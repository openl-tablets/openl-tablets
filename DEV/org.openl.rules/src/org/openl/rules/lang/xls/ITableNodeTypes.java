/*
 * Created on Oct 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls;

/**
 * @deprecated use {@link XlsNodeTypes} instead.
 * @author snshor
 * 
 */
@Deprecated
public interface ITableNodeTypes {
    
    String WORKBOOK = XlsNodeTypes.WORKBOOK.toString();
    String WORKSHEET = XlsNodeTypes.WORKSHEET.toString();
    String TABLE = XlsNodeTypes.TABLE.toString();
    String CELL = XlsNodeTypes.CELL.toString();

    String XLS_MODULE = XlsNodeTypes.XLS_MODULE.toString();
    String XLS_WORKBOOK = XlsNodeTypes.XLS_WORKBOOK.toString();
    String XLS_WORKSHEET = XlsNodeTypes.XLS_WORKSHEET.toString();
    String XLS_DT = XlsNodeTypes.XLS_DT.toString();
    String XLS_SPREADSHEET = XlsNodeTypes.XLS_SPREADSHEET.toString();
    String XLS_TBASIC = XlsNodeTypes.XLS_TBASIC.toString();
    String XLS_COLUMN_MATCH = XlsNodeTypes.XLS_COLUMN_MATCH.toString();
    String XLS_METHOD = XlsNodeTypes.XLS_METHOD.toString();
    String XLS_DATA = XlsNodeTypes.XLS_DATA.toString();
    String XLS_TEST_METHOD = XlsNodeTypes.XLS_TEST_METHOD.toString();
    String XLS_RUN_METHOD = XlsNodeTypes.XLS_RUN_METHOD.toString();
    String XLS_DATATYPE = XlsNodeTypes.XLS_DATATYPE.toString();
    String XLS_OPENL = XlsNodeTypes.XLS_OPENL.toString();
    String XLS_ENVIRONMENT = XlsNodeTypes.XLS_ENVIRONMENT.toString();
    String XLS_OTHER = XlsNodeTypes.XLS_OTHER.toString();
    String XLS_PROPERTIES = XlsNodeTypes.XLS_PROPERTIES.toString();
}
