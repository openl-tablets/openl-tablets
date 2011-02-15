package com.exigen.le.usermodel.xssf;

import java.io.IOException;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.exigen.le.evaluator.function.UDFFinderLE;
import com.exigen.le.project.VersionDesc;
import com.exigen.le.usermodel.LiveExcelWorkbook;

public class LiveExcelXSSFWorkbook extends XSSFWorkbook implements LiveExcelWorkbook {

	private UDFFinderLE udfFinder;

    /** All constructors from superclass */
    public LiveExcelXSSFWorkbook(String projectNamae,VersionDesc versionDesc) {
        super();
        udfFinder = new UDFFinderLE(this);
        initializeContext(projectNamae,versionDesc);
    }

    public LiveExcelXSSFWorkbook(OPCPackage pkg, String projectNamae,VersionDesc versionDesc) throws IOException {
        super(pkg);
        udfFinder = new UDFFinderLE(this);
        initializeContext(projectNamae,versionDesc);
    }

    public LiveExcelXSSFWorkbook(String path, String projectNamae,VersionDesc versionDesc) throws IOException {
        super(path);
        udfFinder = new UDFFinderLE(this);
        initializeContext(projectNamae,versionDesc);
    }
    
    private void initializeContext(String projectName,VersionDesc versionDesc) {
    }

    public UDFFinderLE getUDFFinder() {
		return udfFinder;
	}
}
