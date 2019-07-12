package org.openl.excel.parser.event;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.record.*;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.util.CellRangeAddress;
import org.openl.excel.parser.AlignedValue;
import org.openl.excel.parser.MergedCell;
import org.openl.excel.parser.ParserDateUtil;
import org.openl.excel.parser.SheetDescriptor;
import org.openl.util.NumberUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkbookListener implements HSSFListener {
    private final Logger log = LoggerFactory.getLogger(WorkbookListener.class);

    private final List<EventSheetDescriptor> sheets = new ArrayList<>();
    private final ParserDateUtil parserDateUtil = new ParserDateUtil();
    private Map<String, Object[][]> cellsMap = new HashMap<>();

    private boolean use1904Windowing = false;

    private StyleTrackingListener formatListener;
    private boolean sheetsSorted = false;
    private int sheetIndex = -1;
    private SSTRecord sstRecord;
    private boolean outputNextStringRecord;
    private int nextRow;
    private int nextColumn;
    private short indent;

    void process(String fileName) throws IOException {
        try (POIFSFileSystem poifs = new POIFSFileSystem(new File(fileName))) {
            formatListener = new StyleTrackingListener(this);
            HSSFEventFactory factory = new HSSFEventFactory();
            HSSFRequest request = new HSSFRequest();
            request.addListenerForAllRecords(formatListener);
            factory.processWorkbookEvents(request, poifs);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void processRecord(Record record) {
        int row;
        int column;
        Object value;

        switch (record.getSid()) {
            case DateWindow1904Record.sid:
                DateWindow1904Record d1904 = (DateWindow1904Record) record;
                use1904Windowing = d1904.getWindowing() != 0;
                break;
            case BoundSheetRecord.sid:
                BoundSheetRecord bsr = (BoundSheetRecord) record;
                sheets.add(new EventSheetDescriptor(bsr.getSheetname(), sheets.size(), bsr.getPositionOfBof()));
                break;
            case BOFRecord.sid:
                BOFRecord bof = (BOFRecord) record;
                if (bof.getType() == BOFRecord.TYPE_WORKSHEET) {
                    sheetIndex++;

                    if (!sheetsSorted) {
                        Collections.sort(sheets, (o1, o2) -> o1.getOffset() - o2.getOffset());
                        sheetsSorted = true;
                    }
                }
                break;
            case DimensionsRecord.sid:
                DimensionsRecord dr = (DimensionsRecord) record;
                getSheet().setFirstRowNum(dr.getFirstRow());
                getSheet().setFirstColNum(dr.getFirstCol());

                int rowsCount = dr.getLastRow() - dr.getFirstRow();
                int colsCount = dr.getLastCol() - dr.getFirstCol();
                log.debug("Array size: {}:{}", rowsCount, colsCount);
                Object[][] cells = new Object[rowsCount][colsCount];
                cellsMap.put(getSheet().getName(), cells);
                break;
            case SSTRecord.sid: // Holds all the strings for LabelSSTRecords
                sstRecord = (SSTRecord) record;
                break;
            case BoolErrRecord.sid:
                BoolErrRecord berec = (BoolErrRecord) record;

                if (berec.isBoolean()) {

                    value = berec.getBooleanValue();
                    indent = formatListener.getIndent(berec);
                    if (indent > 0) {
                        value = new AlignedValue(value, indent);
                    }

                    setValue(berec.getRow(), berec.getColumn(), value);
                }
                break;
            case FormulaRecord.sid: // Cell value from a formula
                FormulaRecord frec = (FormulaRecord) record;

                row = frec.getRow();
                column = frec.getColumn();

                CellType cellType = CellType.forInt(frec.getCachedResultType());
                switch (cellType) {
                    case NUMERIC:
                        value = getDateOrIntOrDouble(frec, frec.getValue());
                        if (indent > 0) {
                            value = new AlignedValue(value, indent);
                        }
                        setValue(row, column, value);
                        break;
                    case BOOLEAN:
                        setValue(row, column, frec.getCachedBooleanValue());
                        break;
                    case STRING:
                        // Formula result is a string
                        // This is stored in the next record
                        outputNextStringRecord = true;
                        nextRow = frec.getRow();
                        nextColumn = frec.getColumn();
                        break;
                }
                indent = formatListener.getIndent(frec);

                break;
            case StringRecord.sid:
                if (outputNextStringRecord) {
                    // String for formula
                    StringRecord srec = (StringRecord) record;
                    value = StringUtils.trimToNull(srec.getString());
                    row = nextRow;
                    column = nextColumn;
                    outputNextStringRecord = false;

                    if (value != null && indent > 0) {
                        value = new AlignedValue(value, indent);
                        indent = 0;
                    }
                    setValue(row, column, value);
                }
                break;
            case LabelRecord.sid: // Strings stored directly in the cell
                LabelRecord lrec = (LabelRecord) record;

                row = lrec.getRow();
                column = lrec.getColumn();
                value = StringUtils.trimToNull(lrec.getValue());
                indent = formatListener.getIndent(lrec);
                if (value != null && indent > 0) {
                    value = new AlignedValue(value, indent);
                }
                setValue(row, column, value);
                break;
            case LabelSSTRecord.sid: // String in the shared string table
                LabelSSTRecord lsrec = (LabelSSTRecord) record;

                row = lsrec.getRow();
                column = lsrec.getColumn();
                if (sstRecord == null) {
                    throw new IllegalStateException("No SST Record, can't identify string");
                } else {
                    value = StringUtils.trimToNull(sstRecord.getString(lsrec.getSSTIndex()).toString());
                    indent = formatListener.getIndent(lsrec);
                    if (value != null && indent > 0) {
                        value = new AlignedValue(value, indent);
                    }
                    setValue(row, column, value);
                }
                break;
            case NumberRecord.sid: // Numeric cell value
                NumberRecord numrec = (NumberRecord) record;

                row = numrec.getRow();
                column = numrec.getColumn();

                value = getDateOrIntOrDouble(numrec, numrec.getValue());
                indent = formatListener.getIndent(numrec);
                if (indent > 0) {
                    value = new AlignedValue(value, indent);
                }
                setValue(row, column, value);
                break;
            case RKRecord.sid: // Excel internal number record
                RKRecord rkrec = (RKRecord) record;

                row = rkrec.getRow();
                column = rkrec.getColumn();
                value = getDateOrIntOrDouble(rkrec, rkrec.getRKNumber());
                indent = formatListener.getIndent(rkrec);
                if (indent > 0) {
                    value = new AlignedValue(value, indent);
                }
                setValue(row, column, value);
                break;
            case MergeCellsRecord.sid:
                MergeCellsRecord mergeRec = (MergeCellsRecord) record;

                short numAreas = mergeRec.getNumAreas();
                for (int i = 0; i < numAreas; i++) {
                    CellRangeAddress rangeAddress = mergeRec.getAreaAt(i);
                    int firstMergeRow = rangeAddress.getFirstRow();
                    int firstMergeCol = rangeAddress.getFirstColumn();
                    int lastMergeRow = rangeAddress.getLastRow();
                    int lastMergeCol = rangeAddress.getLastColumn();

                    // Mark cells merged with Left. Don't include first column.
                    for (int r = firstMergeRow; r <= lastMergeRow; r++) {
                        for (int c = firstMergeCol + 1; c <= lastMergeCol; c++) {
                            setValue(r, c, MergedCell.MERGE_WITH_LEFT);
                        }
                    }

                    // Mark cells merged with Up. Only first column starting
                    // from second row.
                    for (int r = firstMergeRow + 1; r <= lastMergeRow; r++) {
                        setValue(r, firstMergeCol, MergedCell.MERGE_WITH_UP);
                    }
                }
                break;
            default:
                break;
        }
    }

    private Object getDateOrIntOrDouble(CellValueRecordInterface record, double d) {
        Object value;
        int formatIndex = formatListener.getFormatIndex(record);
        String formatString = formatListener.getFormatString(formatIndex);
        if (DateUtil.isValidExcelDate(d) && parserDateUtil.isADateFormat(formatIndex, formatString)) {
            value = DateUtil.getJavaDate(d, use1904Windowing);
        } else {
            value = NumberUtils.intOrDouble(d);
        }
        return value;
    }

    private void setValue(int row, int column, Object value) {
        EventSheetDescriptor sheet = getSheet();
        int rowInArray = row - sheet.getFirstRowNum();
        int columnInArray = column - sheet.getFirstColNum();

        ensureCorrectSize(sheet.getName(), rowInArray, columnInArray);
        cellsMap.get(sheet.getName())[rowInArray][columnInArray] = value;
    }

    private void ensureCorrectSize(String sheetName, int row, int col) {
        Object[][] cells = cellsMap.get(sheetName);
        int maxRows = Math.max(row + 1, cells.length);

        int columnCount = cells.length == 0 ? 0 : cells[0].length;
        int maxCols = Math.max(col + 1, columnCount);

        if (maxRows > cells.length || maxCols > columnCount) {
            // Can occur when merged region is greater than last row and column
            log.debug("Extend cells array. Current: {}:{}, new: {}:{}", cells.length, columnCount, maxRows, maxCols);
            Object[][] copy = new Object[maxRows][maxCols];

            arrayCopy(cells, copy);

            cells = copy;

            cellsMap.put(sheetName, cells);
        }
    }

    private void arrayCopy(Object[][] from, Object[][] to) {
        for (int i = 0; i < from.length; i++) {
            System.arraycopy(from[i], 0, to[i], 0, from[i].length);
        }
    }

    private EventSheetDescriptor getSheet() {
        return sheets.get(sheetIndex);
    }

    public Object[][] getCells(SheetDescriptor sheet) {
        return cellsMap.get(sheet.getName());
    }

    public List<EventSheetDescriptor> getSheets() {
        return sheets;
    }

    public boolean isUse1904Windowing() {
        return use1904Windowing;
    }
}
