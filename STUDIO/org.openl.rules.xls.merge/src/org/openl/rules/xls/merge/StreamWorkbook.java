package org.openl.rules.xls.merge;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.EvaluationWorkbook;
import org.apache.poi.ss.formula.udf.UDFFinder;
import org.apache.poi.ss.usermodel.CellReferenceType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.PictureData;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetVisibility;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.Removal;

import org.openl.util.FileTool;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

/**
 * Stream to File workbook delegator
 */
class StreamWorkbook implements Workbook {

    private File tempFile;
    private Workbook delegator;

    public StreamWorkbook(InputStream is, boolean readOnly) throws IOException {
        // Save to temp file because using an InputStream has a higher memory footprint than using a File. See POI
        // javadocs.
        this.tempFile = FileTool.toTempFile(is, "source.xls");
        // ZIP bomb detection tuning. Don't disable it by setting it in 0.
        // https://bz.apache.org/bugzilla/show_bug.cgi?id=58499
        // 0.001 is when 1MByte expands to 1 GByte
        ZipSecureFile.setMinInflateRatio(0.001);
        this.delegator = WorkbookFactory.create(this.tempFile, null, readOnly);
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(delegator);
        delegator = null;
        FileUtils.deleteQuietly(tempFile);
        tempFile = null;
    }

    public Workbook unwrap() {
        return delegator;
    }

    @Override
    public int getActiveSheetIndex() {
        return delegator.getActiveSheetIndex();
    }

    @Override
    public void setActiveSheet(int sheetIndex) {
        delegator.setActiveSheet(sheetIndex);
    }

    @Override
    public int getFirstVisibleTab() {
        return delegator.getFirstVisibleTab();
    }

    @Override
    public void setFirstVisibleTab(int sheetIndex) {
        delegator.setFirstVisibleTab(sheetIndex);
    }

    @Override
    public void setSheetOrder(String sheetname, int pos) {
        delegator.setSheetOrder(sheetname, pos);
    }

    @Override
    public void setSelectedTab(int index) {
        delegator.setSelectedTab(index);
    }

    @Override
    public void setSheetName(int sheet, String name) {
        delegator.setSheetName(sheet, name);
    }

    @Override
    public String getSheetName(int sheet) {
        return delegator.getSheetName(sheet);
    }

    @Override
    public int getSheetIndex(String name) {
        return delegator.getSheetIndex(name);
    }

    @Override
    public int getSheetIndex(Sheet sheet) {
        return delegator.getSheetIndex(sheet);
    }

    @Override
    public Sheet createSheet() {
        return delegator.createSheet();
    }

    @Override
    public Sheet createSheet(String sheetname) {
        return delegator.createSheet(sheetname);
    }

    @Override
    public Sheet cloneSheet(int sheetNum) {
        return delegator.cloneSheet(sheetNum);
    }

    @Override
    public Iterator<Sheet> sheetIterator() {
        return delegator.sheetIterator();
    }

    @Override
    public Iterator<Sheet> iterator() {
        return delegator.iterator();
    }

    @Override
    public Spliterator<Sheet> spliterator() {
        return delegator.spliterator();
    }

    @Override
    public int getNumberOfSheets() {
        return delegator.getNumberOfSheets();
    }

    @Override
    public Sheet getSheetAt(int index) {
        return delegator.getSheetAt(index);
    }

    @Override
    public Sheet getSheet(String name) {
        return delegator.getSheet(name);
    }

    @Override
    public void removeSheetAt(int index) {
        delegator.removeSheetAt(index);
    }

    @Override
    public Font createFont() {
        return delegator.createFont();
    }

    @Override
    public Font findFont(boolean bold, short color, short fontHeight, String name, boolean italic, boolean strikeout, short typeOffset, byte underline) {
        return delegator.findFont(bold, color, fontHeight, name, italic, strikeout, typeOffset, underline);
    }

    @Override
    public int getNumberOfFonts() {
        return delegator.getNumberOfFonts();
    }

    @Override
    @Removal(version = "6.0.0")
    @Deprecated
    public int getNumberOfFontsAsInt() {
        return delegator.getNumberOfFontsAsInt();
    }

    @Override
    public Font getFontAt(int idx) {
        return delegator.getFontAt(idx);
    }

    @Override
    public CellStyle createCellStyle() {
        return delegator.createCellStyle();
    }

    @Override
    public int getNumCellStyles() {
        return delegator.getNumCellStyles();
    }

    @Override
    public CellStyle getCellStyleAt(int idx) {
        return delegator.getCellStyleAt(idx);
    }

    @Override
    public void write(OutputStream stream) throws IOException {
        delegator.write(stream);
    }

    @Override
    public int getNumberOfNames() {
        return delegator.getNumberOfNames();
    }

    @Override
    public Name getName(String name) {
        return delegator.getName(name);
    }

    @Override
    public List<? extends Name> getNames(String name) {
        return delegator.getNames(name);
    }

    @Override
    public List<? extends Name> getAllNames() {
        return delegator.getAllNames();
    }

    @Override
    public Name createName() {
        return delegator.createName();
    }

    @Override
    public void removeName(Name name) {
        delegator.removeName(name);
    }

    @Override
    public int linkExternalWorkbook(String name, Workbook workbook) {
        return this.delegator.linkExternalWorkbook(name, workbook);
    }

    @Override
    public void setPrintArea(int sheetIndex, String reference) {
        delegator.setPrintArea(sheetIndex, reference);
    }

    @Override
    public void setPrintArea(int sheetIndex, int startColumn, int endColumn, int startRow, int endRow) {
        delegator.setPrintArea(sheetIndex, startColumn, endColumn, startRow, endRow);
    }

    @Override
    public String getPrintArea(int sheetIndex) {
        return delegator.getPrintArea(sheetIndex);
    }

    @Override
    public void removePrintArea(int sheetIndex) {
        delegator.removePrintArea(sheetIndex);
    }

    @Override
    public Row.MissingCellPolicy getMissingCellPolicy() {
        return delegator.getMissingCellPolicy();
    }

    @Override
    public void setMissingCellPolicy(Row.MissingCellPolicy missingCellPolicy) {
        delegator.setMissingCellPolicy(missingCellPolicy);
    }

    @Override
    public DataFormat createDataFormat() {
        return delegator.createDataFormat();
    }

    @Override
    public int addPicture(byte[] pictureData, int format) {
        return delegator.addPicture(pictureData, format);
    }

    @Override
    public List<? extends PictureData> getAllPictures() {
        return delegator.getAllPictures();
    }

    @Override
    public CreationHelper getCreationHelper() {
        return delegator.getCreationHelper();
    }

    @Override
    public boolean isHidden() {
        return delegator.isHidden();
    }

    @Override
    public void setHidden(boolean hiddenFlag) {
        delegator.setHidden(hiddenFlag);
    }

    @Override
    public boolean isSheetHidden(int sheetIx) {
        return delegator.isSheetHidden(sheetIx);
    }

    @Override
    public boolean isSheetVeryHidden(int sheetIx) {
        return delegator.isSheetVeryHidden(sheetIx);
    }

    @Override
    public void setSheetHidden(int sheetIx, boolean hidden) {
        delegator.setSheetHidden(sheetIx, hidden);
    }

    @Override
    public SheetVisibility getSheetVisibility(int sheetIx) {
        return delegator.getSheetVisibility(sheetIx);
    }

    @Override
    public void setSheetVisibility(int sheetIx, SheetVisibility visibility) {
        delegator.setSheetVisibility(sheetIx, visibility);
    }

    @Override
    public void addToolPack(UDFFinder toolpack) {
        delegator.addToolPack(toolpack);
    }

    @Override
    public void setForceFormulaRecalculation(boolean value) {
        delegator.setForceFormulaRecalculation(value);
    }

    @Override
    public boolean getForceFormulaRecalculation() {
        return delegator.getForceFormulaRecalculation();
    }

    @Override
    public SpreadsheetVersion getSpreadsheetVersion() {
        return delegator.getSpreadsheetVersion();
    }

    @Override
    public int addOlePackage(byte[] oleData, String label, String fileName, String command) throws IOException {
        return delegator.addOlePackage(oleData, label, fileName, command);
    }

    @Override
    public EvaluationWorkbook createEvaluationWorkbook() {
        return delegator.createEvaluationWorkbook();
    }

    @Override
    public CellReferenceType getCellReferenceType() {
        return delegator.getCellReferenceType();
    }

    @Override
    public void setCellReferenceType(CellReferenceType cellReferenceType) {
        delegator.setCellReferenceType(cellReferenceType);
    }
}
