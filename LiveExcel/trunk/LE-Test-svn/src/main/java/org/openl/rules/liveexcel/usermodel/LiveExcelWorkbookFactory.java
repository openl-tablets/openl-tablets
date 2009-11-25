package org.openl.rules.liveexcel.usermodel;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import org.apache.poi.POIXMLDocument;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.openl.rules.liveexcel.hssf.usermodel.LiveExcelHSSFWorkbook;
import org.openl.rules.liveexcel.xssf.usermodel.LiveExcelXSSFWorkbook;

/**
 * 
 * @author spetrakovsky
 * 
 * @see org.apache.poi.ss.usermodel.WorkbookFactory
 *
 */
public class LiveExcelWorkbookFactory {
    
    public static LiveExcelWorkbook create(InputStream inp, String projectName) throws IOException, InvalidFormatException {
        
        if(! inp.markSupported()) {
            inp = new PushbackInputStream(inp, 8);
        }
        
        if(POIFSFileSystem.hasPOIFSHeader(inp)) {
            return new LiveExcelHSSFWorkbook(inp, projectName);
        }
        
        if(POIXMLDocument.hasOOXMLHeader(inp)) {
            return new LiveExcelXSSFWorkbook(OPCPackage.open(inp), projectName);
        }
        
        throw new IllegalArgumentException("Your InputStream was neither an OLE2 stream, nor an OOXML stream");
    }

}
