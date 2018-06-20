package org.openl.excel.parser.sax;

import static org.apache.poi.xssf.usermodel.XSSFRelation.NS_SPREADSHEETML;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
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
    private final Logger log = LoggerFactory.getLogger(SheetHandler.class);

    private final SharedStringsTable sharedStringsTable;
    private final ParserDateUtil parserDateUtil;
    private final boolean use1904Windowing;
    private final MinimalStyleTable stylesTable;

    private final LruCache<Integer, String> lruCache = new LruCache<>(50);
    private Object[][] cells = new Object[0][];

    private CellAddress start = new CellAddress(0, 0);
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

    SheetHandler(SharedStringsTable sharedStringsTable, boolean use1904Windowing, MinimalStyleTable stylesTable, ParserDateUtil parserDateUtil) {
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
            indent = stylesTable.getIndent(styleIndex);

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
            } else {
                // Number. We must get retrieve format to determine if it's a date.
                NumberFormat numberFormat = stylesTable.getFormat(styleIndex);
                if (numberFormat != null) {
                    formatIndex = numberFormat.getFormatIndex();
                    formatString = numberFormat.getFormatString();
                }
            }
        } else if ("mergeCell".equals(localName)) {
            String[] cellsRefs = attributes.getValue("ref").split(":");
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
                            strValue = new XSSFRichTextString(sharedStringsTable.getEntryAt(idx)).toString();
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
                            if (DateUtil.isValidExcelDate(d) && parserDateUtil.isADateFormat(formatIndex, formatString)) {
                                parsedValue = DateUtil.getJavaDate(d, use1904Windowing);
                            } else {
                                parsedValue = NumberUtils.intOrDouble(d);
                            }
                        }
                    } catch (NumberFormatException e) {
                        throw new ExcelParseException("Can't get a number from string " + n, e);
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

        // According to specification "dimension" is optional and is not required. We must expand array if it's too small
        int maxRows = Math.max(row + 1, cells.length + rowShift);

        int columnCount = cells.length == 0 ? 0 : cells[0].length;
        int maxCols = Math.max(col + 1, columnCount + colShift);

        if (rowShift > 0 || colShift > 0) {
            start = new CellAddress(start.getRow() - rowShift, start.getColumn() - colShift);
        }

        if (maxRows > cells.length || maxCols > columnCount) {
            // Should not occur in theory.
            log.debug("Extend cells array. Current: {}:{}, new: {}:{}", cells.length, columnCount, maxRows, maxCols);
            Object[][] copy = new Object[maxRows][maxCols];
            arrayCopy(cells, copy, rowShift, colShift);
            cells = copy;
        }

        cells[row][col] = parsedValue;
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
