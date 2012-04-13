/**
 * 
 */
package com.exigen.le.project;

import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.poi.ss.formula.IExternalWorkbookResolver;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.PathUtils;

import com.exigen.le.evaluator.ThreadEvaluationContext;

/**
 * @author vabramovs
 * 
 */
public class ExternalBranchedWorkbookResolver implements IExternalWorkbookResolver {

    public InputStream resolveExternalExcel(String externalWorkbookReference) throws FileNotFoundException {
        String file = PathUtils.extractFile(externalWorkbookReference);
        return ProjectLoader.getExcelFile(ThreadEvaluationContext.getProject(), file);
    }

    public Workbook resolveExternalWorkbook(String externalWorkbookReference) {
        try {
            String file = PathUtils.extractFile(externalWorkbookReference);
            return ProjectLoader.getWorkbook(ThreadEvaluationContext.getProject(), file);
        } catch (Exception e) {
            return null;
        }
    }
}
