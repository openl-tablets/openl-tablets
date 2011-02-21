package com.exigen.le.usermodel.hssf;

import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.hssf.model.InternalWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import com.exigen.le.evaluator.function.UDFFinderLE;
import com.exigen.le.usermodel.LiveExcelWorkbook;

public class LiveExcelHSSFWorkbook extends HSSFWorkbook implements LiveExcelWorkbook {

    private UDFFinderLE udfFinder;

    /** All constructors from superclass */

    public LiveExcelHSSFWorkbook(String projectNamae) {
        super();
        udfFinder = new UDFFinderLE(this);
    }

    public LiveExcelHSSFWorkbook(DirectoryNode directory, POIFSFileSystem fs, boolean preserveNodes) throws IOException {
        super(directory, fs, preserveNodes);
        udfFinder = new UDFFinderLE(this);
    }

    public LiveExcelHSSFWorkbook(InputStream s, boolean preserveNodes) throws IOException {
        super(s, preserveNodes);
        udfFinder = new UDFFinderLE(this);
    }

    public LiveExcelHSSFWorkbook(InputStream s) throws IOException {
        super(s);
        udfFinder = new UDFFinderLE(this);
    }

    public LiveExcelHSSFWorkbook(POIFSFileSystem fs, boolean preserveNodes) throws IOException {
        super(fs, preserveNodes);
        udfFinder = new UDFFinderLE(this);
    }

    public LiveExcelHSSFWorkbook(POIFSFileSystem fs) throws IOException {
        super(fs);
        udfFinder = new UDFFinderLE(this);
    }

    public LiveExcelHSSFWorkbook(InternalWorkbook book) {
        super(book);
        udfFinder = new UDFFinderLE(this);
    }

    public UDFFinderLE getUDFFinder() {
        return udfFinder;
    }
}
