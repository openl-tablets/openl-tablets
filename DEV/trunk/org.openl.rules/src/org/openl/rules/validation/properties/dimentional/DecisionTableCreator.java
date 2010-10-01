package org.openl.rules.validation.properties.dimentional;

import java.util.Map;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.GridSplitter;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.OpenMethodHeader;

public class DecisionTableCreator {
        
    //FIXME: remove this variable
    public static final String CURRENT_DATE_PARAM = "currentDate";
        
    private static String FAKE_EXCEL_FILE = "/FAKE_EXCEL_FILE_FOR_DISPATCHER_TABLES.xls";    
    private XlsSheetGridModel sheetGridModel;
    
    public DecisionTableCreator() {
    }
    
    public IGridTable createGridTable(Sheet sheetWithTable) {
        createSheetGridModel(sheetWithTable);
        GridSplitter gridSplitter = new GridSplitter(sheetGridModel);
        IGridTable[] gridTables = gridSplitter.split();

        return gridTables[0];
    }

    public XlsSheetGridModel createSheetGridModel(Sheet sheetWithTable) {
        if (sheetGridModel == null) {
            Workbook wb = sheetWithTable.getWorkbook();

            //it is just for correct work of webstudio
            XlsWorkbookSourceCodeModule mockWorkbookSource = 
                new XlsWorkbookSourceCodeModule(new FileSourceCodeModule(FAKE_EXCEL_FILE, null), wb);
            XlsSheetSourceCodeModule mockSheetSource = new XlsSheetSourceCodeModule(sheetWithTable, sheetWithTable.getSheetName(), mockWorkbookSource);

            sheetGridModel = new XlsSheetGridModel(mockSheetSource);            
        } 
        return sheetGridModel;
    }
    
    public DecisionTable createDecisionTable(String tableName, IOpenClass returnType, IMethodSignature originalSignature, Map<String, IOpenClass> newIncomeParams) {
        int paramsNum = originalSignature.getNumberOfParameters() + newIncomeParams.size(); 
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
        for (Map.Entry<String, IOpenClass> param : newIncomeParams.entrySet()) {
            paramTypes[i] = param.getValue();
            paramNames[i] = param.getKey();
            i++;
        }
        
        IMethodSignature signature = new MethodSignature(paramTypes, paramNames);
        IOpenClass declaringClass = null; // can be null.        
        
        IOpenMethodHeader methodHeader = new OpenMethodHeader(tableName, returnType, signature, declaringClass);
          
        return new DecisionTable(methodHeader); 
    }    
}
