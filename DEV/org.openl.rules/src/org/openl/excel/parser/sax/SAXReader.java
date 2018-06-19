package org.openl.excel.parser.sax;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.*;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.util.SAXHelper;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.CommentsTable;
import org.apache.poi.xssf.usermodel.XSSFRelation;
import org.openl.excel.parser.*;
import org.openl.rules.table.IGridRegion;
import org.openl.util.FileTool;
import org.openl.util.FileUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class SAXReader implements ExcelReader {
    
    private final ParserDateUtil parserDateUtil = new ParserDateUtil();
    
    private final String fileName;
    private File tempFile;

    private boolean use1904Windowing;
    private List<SAXSheetDescriptor> sheets;
    private MinimalStyleTable styleTable;

    public SAXReader(String fileName) {
        this.fileName = fileName;
        ExcelUtils.configureZipBombDetection();
    }

    public SAXReader(InputStream is) {
        // Save to temp file because using an InputStream has a higher memory footprint than using a File. See POI javadocs.
        tempFile = FileTool.toTempFile(is, "stream.xlsx");
        this.fileName = tempFile.getAbsolutePath();
        ExcelUtils.configureZipBombDetection();
    }

    @Override
    public List<SAXSheetDescriptor> getSheets() throws ExcelParseException {
        if (sheets == null) {
            try (OPCPackage pkg = OPCPackage.open(fileName, PackageAccess.READ)) {

                XMLReader parser = SAXHelper.newXMLReader();
                WorkbookHandler handler = new WorkbookHandler();
                parser.setContentHandler(handler);

                // process the first sheet
                XSSFReader r = new XSSFReader(pkg);
                try (InputStream workbookData = r.getWorkbookData()) {
                    parser.parse(new InputSource(workbookData));
                }

                use1904Windowing = handler.isUse1904Windowing();

                sheets = handler.getSheetDescriptors();
            } catch (IOException | OpenXML4JException | SAXException | ParserConfigurationException e) {
                throw new ExcelParseException(e);
            }
        }

        return sheets;
    }

    @Override
    public Object[][] getCells(SheetDescriptor sheet) throws ExcelParseException {
        SAXSheetDescriptor saxSheet = (SAXSheetDescriptor) sheet;
        try (OPCPackage pkg = OPCPackage.open(fileName, PackageAccess.READ)) {
            XSSFReader r = new XSSFReader(pkg);

            initializeNeededData(r);

            XMLReader parser = SAXHelper.newXMLReader();
            SheetHandler handler = new SheetHandler(r.getSharedStringsTable(), use1904Windowing, styleTable, parserDateUtil);
            parser.setContentHandler(handler);

            try (InputStream sheetData = r.getSheet(saxSheet.getRelationId())) {
                parser.parse(new InputSource(sheetData));
            }

            CellAddress start = handler.getStart();
            saxSheet.setFirstRowNum(start.getRow());
            saxSheet.setFirstColNum(start.getColumn());

            return handler.getCells();
        } catch (IOException | OpenXML4JException | SAXException | ParserConfigurationException e) {
            throw new ExcelParseException(e);
        }
    }

    @Override
    public boolean isUse1904Windowing() {
        // Initialize use1904Windowing property if it's not initialized yet
        if (sheets == null) {
            getSheets();
        }

        return use1904Windowing;
    }

    @Override
    public TableStyles getTableStyles(SheetDescriptor sheet, IGridRegion tableRegion) {
        SAXSheetDescriptor saxSheet = (SAXSheetDescriptor) sheet;
        try (OPCPackage pkg = OPCPackage.open(fileName, PackageAccess.READ)) {

            XSSFReader r = new XSSFReader(pkg);

            initializeNeededData(r);

            XMLReader parser = SAXHelper.newXMLReader();
            StyleIndexHandler styleIndexHandler = new StyleIndexHandler(tableRegion, saxSheet.getIndex());
            parser.setContentHandler(styleIndexHandler);

            try (InputStream sheetData = r.getSheet(saxSheet.getRelationId())) {
                parser.parse(new InputSource(sheetData));
            }

            return new SAXTableStyles(tableRegion,
                    styleIndexHandler.getCellIndexes(),
                    r.getStylesTable(),
                    getSheetComments(pkg, saxSheet),
                    styleIndexHandler.getFormulas());
        } catch (IOException | OpenXML4JException | SAXException | ParserConfigurationException e) {
            throw new ExcelParseException(e);
        }
    }

    @Override
    public void close() {
        styleTable = null;
        sheets = null;
        use1904Windowing = false;

        FileUtils.deleteQuietly(tempFile);
        tempFile = null;
        parserDateUtil.reset();
    }

    private void initializeNeededData(XSSFReader r) {
        // Ensure that needed settings were read from workbook and styles files
        if (sheets == null) {
            getSheets();
        }

        if (styleTable == null) {
            parseStyles(r);
        }
    }

    private void parseStyles(XSSFReader r) throws ExcelParseException {
        try (InputStream stylesData = r.getStylesData()) {
            XMLReader styleParser = SAXHelper.newXMLReader();
            StyleHandler styleHandler = new StyleHandler();
            styleParser.setContentHandler(styleHandler);
            styleParser.parse(new InputSource(stylesData));
            styleTable = styleHandler.getStyleTable();
        } catch (IOException | OpenXML4JException | SAXException | ParserConfigurationException e) {
            throw new ExcelParseException(e);
        }
    }

    private CommentsTable getSheetComments(OPCPackage pkg, SAXSheetDescriptor sheet) {
        try {
            // Get workbook part
            PackageRelationship workbookRel = pkg.getRelationshipsByType(
                    PackageRelationshipTypes.CORE_DOCUMENT
            ).getRelationship(0);
            PackagePart workbookPart = pkg.getPart(workbookRel);

            // Find sheet part by relation id
            PackageRelationship sheetRel = workbookPart.getRelationship(sheet.getRelationId());
            PackagePart sheetPart = pkg.getPart(PackagingURIHelper.createPartName(sheetRel.getTargetURI()));

            PackageRelationshipCollection commentRelList = sheetPart.getRelationshipsByType(XSSFRelation.SHEET_COMMENTS.getRelation());
            if (commentRelList.size() > 0) {
                // Comments have only one relationship
                PackageRelationship commentRel = commentRelList.getRelationship(0);
                PackagePart commentPart = pkg.getPart(PackagingURIHelper.createPartName(commentRel.getTargetURI()));

                return new CommentsTable(commentPart);
            }

            return null;
        } catch (InvalidFormatException | IOException e) {
            return null;
        }
    }
}
