package org.openl.rules.liveexcel.usermodel;

import org.openl.rules.liveexcel.EvaluationContext;

public interface LiveExcelWorkbook extends org.apache.poi.ss.usermodel.Workbook {
    
    EvaluationContext getEvaluationContext();
    
}
