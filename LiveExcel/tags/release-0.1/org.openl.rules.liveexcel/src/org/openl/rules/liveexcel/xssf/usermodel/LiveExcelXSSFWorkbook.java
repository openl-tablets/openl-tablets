package org.openl.rules.liveexcel.xssf.usermodel;

import java.io.IOException;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openl.rules.liveexcel.EvaluationContext;
import org.openl.rules.liveexcel.usermodel.ContextFactory;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbook;

public class LiveExcelXSSFWorkbook extends XSSFWorkbook implements LiveExcelWorkbook {

    private EvaluationContext context;

    /** All constructors from superclass */
    public LiveExcelXSSFWorkbook(String projectNamae) {
        super();
        initializeContext(projectNamae);
    }

    public LiveExcelXSSFWorkbook(OPCPackage pkg, String projectNamae) throws IOException {
        super(pkg);
        initializeContext(projectNamae);
    }

    public LiveExcelXSSFWorkbook(String path, String projectNamae) throws IOException {
        super(path);
        initializeContext(projectNamae);
    }
    
    private void initializeContext(String projectName) {
        context = ContextFactory.getEvaluationContext(projectName);
    }

    public EvaluationContext getEvaluationContext() {
        return context;
    }



}
