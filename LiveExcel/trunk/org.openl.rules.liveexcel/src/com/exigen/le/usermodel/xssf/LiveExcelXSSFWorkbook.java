package com.exigen.le.usermodel.xssf;

import java.io.IOException;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.exigen.le.evaluator.function.UDFFinderLE;
import com.exigen.le.usermodel.LiveExcelWorkbook;

public class LiveExcelXSSFWorkbook extends XSSFWorkbook implements LiveExcelWorkbook {

    private UDFFinderLE udfFinder;

    /** All constructors from superclass */
    public LiveExcelXSSFWorkbook() {
        super();
        udfFinder = new UDFFinderLE(this);
    }

    public LiveExcelXSSFWorkbook(OPCPackage pkg) throws IOException {
        super(pkg);
        udfFinder = new UDFFinderLE(this);
    }

    public LiveExcelXSSFWorkbook(String path) throws IOException {
        super(path);
        udfFinder = new UDFFinderLE(this);
    }

    public UDFFinderLE getUDFFinder() {
        return udfFinder;
    }
}
