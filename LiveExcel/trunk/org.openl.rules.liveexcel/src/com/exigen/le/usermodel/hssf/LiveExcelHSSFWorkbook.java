package com.exigen.le.usermodel.hssf;

import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.hssf.model.InternalWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import com.exigen.le.evaluator.function.UDFFinderLE;
import com.exigen.le.project.VersionDesc;
import com.exigen.le.usermodel.LiveExcelWorkbook;

public class LiveExcelHSSFWorkbook extends HSSFWorkbook implements LiveExcelWorkbook {

	private UDFFinderLE udfFinder;

    /** All constructors from superclass */
    
    public LiveExcelHSSFWorkbook(String projectNamae) {
        super();
        udfFinder = new UDFFinderLE(this);
    }

    public LiveExcelHSSFWorkbook(DirectoryNode directory, POIFSFileSystem fs, boolean preserveNodes, String projectNamae) throws IOException {
        super(directory, fs, preserveNodes);
        udfFinder = new UDFFinderLE(this);
    }

    public LiveExcelHSSFWorkbook(InputStream s, boolean preserveNodes, String projectNamae,VersionDesc versionDesc) throws IOException {
        super(s, preserveNodes);
        udfFinder = new UDFFinderLE(this);
        initializeContext(projectNamae,versionDesc);
    }

    public LiveExcelHSSFWorkbook(InputStream s, String projectNamae,VersionDesc versionDesc) throws IOException {
        super(s);
        udfFinder = new UDFFinderLE(this);
        initializeContext(projectNamae,versionDesc);
    }

    public LiveExcelHSSFWorkbook(POIFSFileSystem fs, boolean preserveNodes, String projectNamae,VersionDesc versionDesc) throws IOException {
        super(fs, preserveNodes);
        udfFinder = new UDFFinderLE(this);
        initializeContext(projectNamae,versionDesc);
    }

    public LiveExcelHSSFWorkbook(POIFSFileSystem fs, String projectNamae,VersionDesc versionDesc) throws IOException {
        super(fs);
        udfFinder = new UDFFinderLE(this);
        initializeContext(projectNamae,versionDesc);
    }

    public LiveExcelHSSFWorkbook(InternalWorkbook book, String projectNamae,VersionDesc versionDesc) {
        super(book);
        udfFinder = new UDFFinderLE(this);
        initializeContext(projectNamae,versionDesc);
    }
    
    private void initializeContext(String projectName,VersionDesc versionDesc) {
    }

	public UDFFinderLE getUDFFinder() {
		return udfFinder;
	}
}
