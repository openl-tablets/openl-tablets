package org.openl.rules.liveexcel.hssf.usermodel;

import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.hssf.model.Workbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.openl.rules.liveexcel.EvaluationContext;
import org.openl.rules.liveexcel.usermodel.ContextFactory;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbook;

public class LiveExcelHSSFWorkbook extends HSSFWorkbook implements LiveExcelWorkbook {

    private EvaluationContext context;

    /** All constructors from superclass */
    
    public LiveExcelHSSFWorkbook(String projectNamae) {
        super();
    }

    public LiveExcelHSSFWorkbook(DirectoryNode directory, POIFSFileSystem fs, boolean preserveNodes, String projectNamae) throws IOException {
        super(directory, fs, preserveNodes);
    }

    public LiveExcelHSSFWorkbook(InputStream s, boolean preserveNodes, String projectNamae) throws IOException {
        super(s, preserveNodes);
        initializeContext(projectNamae);
    }

    public LiveExcelHSSFWorkbook(InputStream s, String projectNamae) throws IOException {
        super(s);
        initializeContext(projectNamae);
    }

    public LiveExcelHSSFWorkbook(POIFSFileSystem fs, boolean preserveNodes, String projectNamae) throws IOException {
        super(fs, preserveNodes);
        initializeContext(projectNamae);
    }

    public LiveExcelHSSFWorkbook(POIFSFileSystem fs, String projectNamae) throws IOException {
        super(fs);
        initializeContext(projectNamae);
    }

    public LiveExcelHSSFWorkbook(Workbook book, String projectNamae) {
        super(book);
        initializeContext(projectNamae);
    }
    
    private void initializeContext(String projectName) {
        context = ContextFactory.getEvaluationContext(projectName);
    }

    public EvaluationContext getEvaluationContext() {
        return context;
    }

}
