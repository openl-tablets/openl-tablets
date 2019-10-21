package org.openl.excel.parser.sax;

import static org.apache.poi.xssf.usermodel.XSSFRelation.NS_SPREADSHEETML;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.openl.excel.parser.AlignedValue;
import org.openl.excel.parser.ExcelParseException;
import org.openl.excel.parser.MergedCell;
import org.openl.excel.parser.ParserDateUtil;
import org.openl.util.NumberUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class SheetHandler extends DefaultHandler {
    private static final int MAX_ESTIMATED_CELLS_COUNT = 10_000 * 256;

    private final Logger log = LoggerFactory.getLogger(SheetHandler.class);

    private final SharedStringsTable sharedStringsTable;
    private final ParserDateUtil parserDateUtil;
    private final boolean use1904Windowing;
    private final MinimalStyleTable stylesTable;

    private final LruCache<Integer, String> lruCache = new LruCache<>(50);
    private Object[][] cells = new Object[0][];

    private CellAddress start = CellAddress.A1;
    private CellAddress effectiveStart = null;
    private CellAddress effectiveEnd = null;
    private CellAddress current;

    // Set when V start element is seen
    private boolean vIsOpen;
    // Set when an Inline String "is" is seen
    private boolean isInlineStringOpen;

    private StringBuilder value = new StringBuilder();

    // Set when cell start element is seen;
    // used when cell close element is seen.
    private XmlCellType nextDataType;

    // Used to format numeric cell values.
    private int formatIndex;
    private String formatString;

    // Cell indent
    private Short indent;

    private List<CellRangeAddress> mergedCells = new ArrayList<>();

    SheetHandler(SharedStringsTable sharedStringsTable,
            boolean use1904Windowing,
            MinimalStyleTable stylesTable,
            ParserDateUtil parserDateUtil) {
        this.sharedStringsTable = sharedStringsTable;
        this.use1904Windowing = use1904Windowing;
        this.stylesTable = stylesTable;
        this.parserDateUtil = parserDateUtil;
    }

    public Object[][] getCells() {
        return cells;
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) {
        if (uri != null && !uri.equals(NS_SPREADSHEETML)) {
            return;
        }

        if ("dimension".equals(name)) {
            // According to specification "dimension" is optional and is not required
            initializeCells(attributes.getValue("ref"));
        } else if (isTextTag(localName)) {
            vIsOpen = true;
            // Clear contents cache
            value.setLength(0);
        } else if ("is".equals(localName)) {
            // Inline string outer tag
            isInlineStringOpen = true;
        } else if ("c".equals(localName)) {
            // c => cell
            // Set up defaults.
            this.nextDataType = XmlCellType.NUMBER;
            this.formatIndex = -1;
            this.formatString = null;

            String cellRef = attributes.getValue("r");
            current = new CellAddress(cellRef);

            String cellStyleStr = attributes.getValue("s");
            int styleIndex = cellStyleStr != null ? Integer.parseInt(cellStyleStr) : 0;
            indent = stylesTable == null ? null : stylesTable.getIndent(styleIndex);

            String cellType = attributes.getValue("t");
            if ("b".equals(cellType)) {
                nextDataType = XmlCellType.BOOLEAN;
            } else if ("e".equals(cellType)) {
                nextDataType = XmlCellType.ERROR;
            } else if ("inlineStr".equals(cellType)) {
                nextDataType = XmlCellType.INLINE_STRING;
            } else if ("s".equals(cellType)) {
                nextDataType = XmlCellType.SHARED_STRING_TABLE_STRING;
            } else if ("str".equals(cellType)) {
                nextDataType = XmlCellType.FORMULA;
            } else if (stylesTable != null) {
                // Number. We must get retrieve format to determine if it's a date.
                NumberFormat numberFormat = stylesTable.getFormat(styleIndex);
                if (numberFormat != null) {
                    formatIndex = numberFormat.getFormatIndex();
                    formatString = numberFormat.getFormatString();
                }
            }
        } else if ("mergeCell".equals(localName)) {
            String ref = attributes.getValue("ref");
            String[] cellsRefs = ref.split(":");
            // No need to mark the cell as merged if it's merged with itself.
            if (cellsRefs.length > 1) {
                mergedCells.add(CellRangeAddress.valueOf(ref));
                CellAddress from = new CellAddress(cellsRefs[0]);
                CellAddress to = new CellAddress(cellsRefs[1]);

                int firstMergeRow = from.getRow();
                int firstMergeCol = from.getColumn();
                int lastMergeRow = to.getRow();
                int lastMergeCol = to.getColumn();
                // Mark cells merged with Left. Don't include first column.
                for (int row = firstMergeRow; row <= lastMergeRow; row++) {
                    for (int col = firstMergeCol + 1; col <= lastMergeCol; col++) {
                        setCell(row - start.getRow(), col - start.getColumn(), MergedCell.MERGE_WITH_LEFT);
                    }
                }

                // Mark cells merged with Up. Only first column starting from second row.
                for (int row = firstMergeRow + 1; row <= lastMergeRow; row++) {
                    setCell(row - start.getRow(), firstMergeCol - start.getColumn(), MergedCell.MERGE_WITH_UP);
                }
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String name) {
        if (uri != null && !uri.equals(NS_SPREADSHEETML)) {
            return;
        }

        Object parsedValue = null;

        if (isTextTag(localName)) {
            vIsOpen = false;

            // Process the value contents as required, now we have it all
            switch (nextDataType) {
                case BOOLEAN:
                    char first = value.charAt(0);
                    parsedValue = first != '0';
                    break;
                case FORMULA:
                    // To be precise it's a formula with String type. But we care only about a value.
                    // Fallback to INLINE_STRING
                case INLINE_STRING:
                    parsedValue = StringUtils.trimToNull(value.toString());
                    break;
                case SHARED_STRING_TABLE_STRING:
                    String sstIndex = value.toString();
                    try {
                        int idx = Integer.parseInt(sstIndex);
                        String strValue = lruCache.get(idx);
                        if (strValue == null && !lruCache.containsKey(idx)) {
                            strValue = sharedStringsTable.getItemAt(idx).toString();
                            lruCache.put(idx, strValue);
                        }
                        parsedValue = StringUtils.trimToNull(strValue);
                    } catch (NumberFormatException ex) {
                        throw new ExcelParseException("Failed to parse SST index '" + sstIndex, ex);
                    }
                    break;
                case NUMBER:
                    String n = value.toString();
                    try {
                        if (n.isEmpty()) {
                            parsedValue = null;
                        } else {
                            double d = Double.parseDouble(n);
                            if (DateUtil.isValidExcelDate(d) && parserDateUtil.isADateFormat(formatIndex,
                                formatString)) {
                                parsedValue = DateUtil.getJavaDate(d, use1904Windowing);
                            } else {
                                parsedValue = NumberUtils.intOrDouble(d);
                            }
                        }
                    } catch (NumberFormatException e) {
                        throw new ExcelParseException("Cannot get a number from string " + n, e);
                    }
                    break;
                default:
                    // Skip
                    log.debug("Skipped data type: {}", nextDataType);
                    break;
            }

            int row = current.getRow() - start.getRow();
            int col = current.getColumn() - start.getColumn();

            if (indent != null && indent != 0) {
                parsedValue = new AlignedValue(parsedValue, indent);
            }
            setCell(row, col, parsedValue);
        } else if ("is".equals(localName)) {
            isInlineStringOpen = false;
        }
    }

    private void setCell(int row, int col, Object parsedValue) {
        // Sometimes sheet dimension is defined like C1:E63 but exists cell in B65 in same sheet. It's a strange case
        // but we must support it too.
        int rowShift = 0;
        int colShift = 0;

        if (row < 0) {
            rowShift = -row;
            row = 0;
        }

        if (col < 0) {
            colShift = -col;
            col = 0;
        }

        // According to specification "dimension" is optional and is not required. We must expand array if it's too
        // small
        int rowCount = cells.length;
        int maxRows = Math.max(row + 1, rowCount + rowShift);

        int columnCount = rowCount == 0 ? 0 : cells[0].length;
        int maxCols = Math.max(col + 1, columnCount + colShift);

        if (rowShift > 0 || colShift > 0) {
            start = new CellAddress(start.getRow() - rowShift, start.getColumn() - colShift);
        }

        if (maxRows > rowCount || maxCols > columnCount) {
            // Increase the size in advance (1.5 times) to reduce too many array copy operations during parsing.
            // In endDocument() the size will be reduced to effective size.
            int newRows = maxRows > rowCount ? Math.max(maxRows, rowCount + (rowCount >> 1)) : maxRows;
            int newCols = maxCols > columnCount ? Math.max(maxCols, columnCount + (columnCount >> 1)) : columnCount;
            log.debug("Extend cells array. Current: {}:{}, new: {}:{}", rowCount, columnCount, newRows, newCols);
            Object[][] copy = new Object[newRows][newCols];
            arrayCopy(cells, copy, rowShift, colShift);
            cells = copy;
        }

        cells[row][col] = parsedValue;

        if (parsedValue != null && !(parsedValue instanceof MergedCell)) {
            int curRow = row + start.getRow();
            int curCol = col + start.getColumn();

            if (effectiveStart == null) {
                effectiveStart = new CellAddress(curRow, curCol);
                effectiveEnd = effectiveStart;
            } else {
                if (curRow < effectiveStart.getRow() || curCol < effectiveStart.getColumn()) {
                    int minRow = Math.min(curRow, effectiveStart.getRow());
                    int minCol = Math.min(curCol, effectiveStart.getColumn());
                    effectiveStart = new CellAddress(minRow, minCol);
                }
                if (curRow > effectiveEnd.getRow() || curCol > effectiveEnd.getColumn()) {
                    int maxRow = Math.max(curRow, effectiveEnd.getRow());
                    int maxCol = Math.max(curCol, effectiveEnd.getColumn());
                    effectiveEnd = new CellAddress(maxRow, maxCol);
                }
            }
        }
    }

    private boolean isTextTag(String name) {
        return "v".equals(name) || "inlineStr".equals(name) || "t".equals(name) && isInlineStringOpen;
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (vIsOpen) {
            value.append(ch, start, length);
        }
    }

    @Override
    public void endDocument() {
        int rowCount = cells.length;
        if (rowCount == 0 || cells[0].length == 0) {
            // No need to optimize cells[][] size
            return;
        }

        if (effectiveStart == null || effectiveEnd == null) {
            cells = new Object[0][];
            return;
        }

        for (CellRangeAddress mergedCell : mergedCells) {
            int r = mergedCell.getFirstRow() - start.getRow();
            int c = mergedCell.getFirstColumn() - start.getColumn();
            if (r >= 0 && c >= 0 && cells[r][c] != null) {
                if (mergedCell.getLastRow() > effectiveEnd.getRow() || mergedCell.getLastColumn() > effectiveEnd
                    .getColumn()) {
                    int maxRow = Math.max(mergedCell.getLastRow(), effectiveEnd.getRow());
                    int maxCol = Math.max(mergedCell.getLastColumn(), effectiveEnd.getColumn());
                    effectiveEnd = new CellAddress(maxRow, maxCol);
                }
            }
        }

        int rows = effectiveEnd.getRow() - effectiveStart.getRow() + 1;
        int cols = effectiveEnd.getColumn() - effectiveStart.getColumn() + 1;

        int columnCount = cells[0].length;
        if (rows < rowCount || cols < columnCount) {
            log.debug("Optimize cells array. Current: {}:{}, new: {}:{}", rowCount, columnCount, rows, cols);
            int fromRow = effectiveStart.getRow() - start.getRow();
            int fromCol = effectiveStart.getColumn() - start.getColumn();
            Object[][] copy = new Object[rows][cols];
            for (int i = 0; i < copy.length; i++) {
                System.arraycopy(cells[fromRow + i], fromCol, copy[i], 0, cols);
            }
            cells = copy;
            start = effectiveStart;
        }
    }

    private void initializeCells(String dimension) {
        String[] cellsRefs = dimension.split(":");
        start = new CellAddress(cellsRefs[0]);
        if (cellsRefs.length == 1) {
            log.debug("Array size: 1:1");
            cells = new Object[1][1];
        } else {
            int startRow = start.getRow();
            int startColumn = start.getColumn();

            CellAddress end = new CellAddress(cellsRefs[1]);
            int endRow = end.getRow();
            int endColumn = end.getColumn();

            int rows = endRow - startRow + 1;
            int cols = endColumn - startColumn + 1;
            if (rows * cols > MAX_ESTIMATED_CELLS_COUNT) {
                // Can consume too much memory. Restrict initial size and increment it on demand.
                rows = Math.max(1, MAX_ESTIMATED_CELLS_COUNT / cols);
            }
            log.debug("Array size: {}:{}", rows, cols);
            cells = new Object[rows][cols];
        }
    }

    private void arrayCopy(Object[][] from, Object[][] to, int toRow, int toCol) {
        for (int i = 0; i < from.length; i++) {
            System.arraycopy(from[i], 0, to[toRow + i], toCol, from[i].length);
        }
    }

    public CellAddress getStart() {
        return start;
    }

    private static class LruCache<A, B> extends LinkedHashMap<A, B> {
        private static final long serialVersionUID = -6937158218983475882L;
        private final int maxEntries;

        LruCache(final int maxEntries) {
            super(maxEntries + 1, 1.0f, true);
            this.maxEntries = maxEntries;
        }

        @Override
        protected boolean removeEldestEntry(final Map.Entry<A, B> eldest) {
            return super.size() > maxEntries;
        }
    }

    private enum XmlCellType {
        BOOLEAN,
        ERROR,
        FORMULA,
        INLINE_STRING,
        SHARED_STRING_TABLE_STRING,
        NUMBER,
    }

}
