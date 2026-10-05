package org.openl.excel.parser.sax;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.openxml4j.opc.PackageRelationshipTypes;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.CommentsTable;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.model.ThemesTable;
import org.apache.poi.xssf.usermodel.XSSFRelation;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.jspecify.annotations.Nullable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRst;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.openl.excel.parser.ExcelParseException;
import org.openl.excel.parser.ExcelReader;
import org.openl.excel.parser.ExcelUtils;
import org.openl.excel.parser.ParserDateUtil;
import org.openl.excel.parser.SheetDescriptor;
import org.openl.excel.parser.TableStyles;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ui.TextRun;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.util.FileTool;
import org.openl.util.FileUtils;

public class SAXReader implements ExcelReader {

    /** The element of the shared strings that holds one string. */
    private static final String SHARED_STRING = "si";

    /** Reads one shared string from where the stream stands: what its element holds, without the element. */
    private static final XmlOptions SHARED_STRING_OPTIONS = new XmlOptions(POIXMLTypeLoader.DEFAULT_XML_OPTIONS)
            .setLoadReplaceDocumentElement(null);

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
        // Save to temp file because using an InputStream has a higher memory footprint than using a File. See POI
        // javadocs.
        tempFile = FileTool.toTempFile(is, "stream.xlsx");
        this.fileName = tempFile.getAbsolutePath();
        ExcelUtils.configureZipBombDetection();
    }

    @Override
    public List<SheetDescriptor> getSheets() {
        if (sheets == null) {
            try (ReadOnlyOPCPackage pkg = ReadOnlyOPCPackage.open(fileName)) {

                XMLReader parser = XMLHelper.newXMLReader();
                var handler = new WorkbookHandler();
                parser.setContentHandler(handler);

                // process the first sheet
                var r = new XSSFReader(pkg.pck);
                try (var workbookData = r.getWorkbookData()) {
                    parser.parse(new InputSource(workbookData));
                }

                use1904Windowing = handler.isUse1904Windowing();

                sheets = handler.getSheetDescriptors();
            } catch (IOException | OpenXML4JException | SAXException | ParserConfigurationException e) {
                throw new ExcelParseException(e);
            }
        }

        return Collections.unmodifiableList(sheets);
    }

    @Override
    public Object[][] getCells(SheetDescriptor sheet) {
        var saxSheet = (SAXSheetDescriptor) sheet;
        try (ReadOnlyOPCPackage pkg = ReadOnlyOPCPackage.open(fileName)) {
            var r = new XSSFReader(pkg.pck);

            initializeNeededData(r, pkg.pck);

            XMLReader parser = XMLHelper.newXMLReader();
            var handler = new SheetHandler(r.getSharedStringsTable(),
                    use1904Windowing,
                    styleTable,
                    parserDateUtil);
            parser.setContentHandler(handler);

            try (var sheetData = r.getSheet(saxSheet.getRelationId())) {
                parser.parse(new InputSource(sheetData));
            }

            var start = handler.getStart();
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
        var saxSheet = (SAXSheetDescriptor) sheet;
        try (ReadOnlyOPCPackage pkg = ReadOnlyOPCPackage.open(fileName)) {

            var r = new XSSFReader(pkg.pck);

            initializeNeededData(r, pkg.pck);

            XMLReader parser = XMLHelper.newXMLReader();
            var styleIndexHandler = new StyleIndexHandler(tableRegion, saxSheet.getIndex());
            parser.setContentHandler(styleIndexHandler);

            try (var sheetData = r.getSheet(saxSheet.getRelationId())) {
                parser.parse(new InputSource(sheetData));
            }

            var stylesTable = r.getStylesTable();
            return new SAXTableStyles(tableRegion,
                    styleIndexHandler.getCellIndexes(),
                    stylesTable,
                    getSheetComments(pkg.pck, saxSheet),
                    styleIndexHandler.getFormulas(),
                    readTextRuns(r, stylesTable, styleIndexHandler.getSharedStrings()));
        } catch (IOException | OpenXML4JException | SAXException | ParserConfigurationException e) {
            throw new ExcelParseException(e);
        }
    }

    /**
     * Reads the runs of the shared strings the cells of a table hold. A cell whose text takes the font of its cell
     * is left out.
     *
     * <p>The shared strings of the whole workbook are streamed, and only the strings the table holds are read with
     * their runs: the strings of a large workbook are never held at once.
     */
    private static Map<CellAddress, List<TextRun>> readTextRuns(XSSFReader r,
                                                               StylesTable stylesTable,
                                                               Map<CellAddress, Integer> sharedStrings)
            throws IOException, InvalidFormatException {
        if (sharedStrings.isEmpty()) {
            return Map.of();
        }
        var items = readItemRuns(r, stylesTable, new HashSet<>(sharedStrings.values()));
        var runs = new HashMap<CellAddress, List<TextRun>>();
        sharedStrings.forEach((cell, index) -> {
            Optional.ofNullable(items.get(index)).ifPresent(textRuns -> runs.put(cell, textRuns));
        });
        return runs;
    }

    /**
     * The runs of the shared strings asked for, by their index; a string with no font of its own is left out.
     *
     * @param wanted the indexes of the strings to read; each is taken out once its string is read
     */
    private static Map<Integer, List<TextRun>> readItemRuns(XSSFReader r,
                                                           StylesTable stylesTable,
                                                           Set<Integer> wanted) throws IOException,
            InvalidFormatException {
        var themes = stylesTable.getTheme();
        var items = new HashMap<Integer, List<TextRun>>();
        try (var data = r.getSharedStringsData()) {
            var reader = XMLHelper.newXMLInputFactory().createXMLStreamReader(data);
            try {
                var index = 0;
                // The reading stops once the last string asked for is read.
                while (!wanted.isEmpty() && reader.hasNext()) {
                    if (reader.next() == XMLStreamConstants.START_ELEMENT
                            && SHARED_STRING.equals(reader.getLocalName())) {
                        if (wanted.remove(index)) {
                            readItemRuns(reader, index, themes, items);
                        }
                        index++;
                    }
                }
            } finally {
                reader.close();
            }
        } catch (XMLStreamException | XmlException e) {
            throw new ExcelParseException(e);
        }
        return items;
    }

    /** Reads the runs of the shared string the reader stands at; a string with no font of its own is left out. */
    private static void readItemRuns(XMLStreamReader reader,
                                     int index,
                                     @Nullable ThemesTable themes,
                                     Map<Integer, List<TextRun>> items) throws XmlException {
        var item = CTRst.Factory.parse(reader, SHARED_STRING_OPTIONS);
        var runs = PoiExcelHelper.getTextRuns(new XSSFRichTextString(item), null, themes);
        if (!runs.isEmpty()) {
            items.put(index, runs);
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

    private void initializeNeededData(XSSFReader r, OPCPackage pkg) {
        // Ensure that needed settings were read from workbook and styles files
        if (sheets == null) {
            getSheets();
        }

        if (styleTable == null) {
            parseStyles(r, pkg);
        }
    }

    private void parseStyles(XSSFReader r, OPCPackage pkg) {
        var parts = pkg.getPartsByContentType(XSSFRelation.STYLES.getContentType());
        if (parts.isEmpty()) {
            return;
        }

        try (var stylesData = r.getStylesData()) {
            XMLReader styleParser = XMLHelper.newXMLReader();
            var styleHandler = new StyleHandler();
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
            var workbookRel = pkg.getRelationshipsByType(PackageRelationshipTypes.CORE_DOCUMENT)
                    .getRelationship(0);
            var workbookPart = pkg.getPart(workbookRel);

            // Find sheet part by relation id
            var sheetRel = workbookPart.getRelationship(sheet.getRelationId());
            var sheetPart = pkg.getPart(PackagingURIHelper.createPartName(sheetRel.getTargetURI()));

            var commentRelList = sheetPart
                    .getRelationshipsByType(XSSFRelation.SHEET_COMMENTS.getRelation());
            if (commentRelList.size() > 0) {
                // Comments have only one relationship
                var commentRel = commentRelList.getRelationship(0);
                var commentPart = pkg.getPart(PackagingURIHelper.createPartName(commentRel.getTargetURI()));

                return new CommentsTable(commentPart);
            }

            return null;
        } catch (InvalidFormatException | IOException e) {
            return null;
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class ReadOnlyOPCPackage implements AutoCloseable {
        final OPCPackage pck;

        static ReadOnlyOPCPackage open(String fileName) throws InvalidFormatException {
            OPCPackage pck = OPCPackage.open(fileName, PackageAccess.READ);
            return new ReadOnlyOPCPackage(pck);
        }

        @Override
        public void close() {
            // OPCPackage implementation makes SAVE on close() method. It is unacceptable for the READ only package.
            // Instead, it is required to call revert() for the READ only package.
            // On the other side it is easy to use try-with-resource to close a stream.
            // So to achieve it we wrap OPCPackage to call revert() on close().
            pck.revert();
        }
    }
}
