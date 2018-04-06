package org.openl.excel.parser.event;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.record.*;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.openl.excel.parser.TableStyles;
import org.openl.excel.parser.event.style.EventTableStyles;
import org.openl.rules.table.IGridRegion;

public class TableStyleListener implements HSSFListener {
    private final EventSheetDescriptor sheet;
    private final IGridRegion tableRegion;
    private TableStyles tableStyles;

    private final List<EventSheetDescriptor> sheets = new ArrayList<>();
    private int sheetIndex = -1;
    private boolean sheetsSorted = false;
    private final int[][] cellIndexes;
    private PaletteRecord palette;

    public TableStyleListener(EventSheetDescriptor sheet, IGridRegion tableRegion) {
        this.sheet = sheet;
        this.tableRegion = tableRegion;
        cellIndexes = new int[IGridRegion.Tool.height(tableRegion)][IGridRegion.Tool.width(tableRegion)];
    }

    void process(String fileName) throws IOException {
        try (POIFSFileSystem poifs = new POIFSFileSystem(new File(fileName))) {
            StyleTrackingListener formatListener = new StyleTrackingListener(this);
            HSSFEventFactory factory = new HSSFEventFactory();
            HSSFRequest request = new HSSFRequest();
            request.addListenerForAllRecords(formatListener);
            factory.processWorkbookEvents(request, poifs);
            if (palette == null) {
                palette = new PaletteRecord();
            }
            tableStyles = new EventTableStyles(
                    tableRegion,
                    cellIndexes,
                    formatListener.getExtendedFormats(),
                    formatListener.getCustomFormats(),
                    palette,
                    formatListener.getFonts()
            );
        }
    }

    public TableStyles getTableStyles() {
        return tableStyles;
    }

    @Override
    public void processRecord(Record record) {
        switch (record.getSid()) {
            case BoundSheetRecord.sid:
                BoundSheetRecord bsr = (BoundSheetRecord) record;
                if (bsr.getSheetname().equals(sheet.getName())) {
                    sheets.add(new EventSheetDescriptor(bsr.getSheetname(), sheets.size(), bsr.getPositionOfBof()));
                }
                break;
            case BOFRecord.sid:
                BOFRecord bof = (BOFRecord) record;
                if (bof.getType() == BOFRecord.TYPE_WORKSHEET) {
                    if (!sheetsSorted) {
                        Collections.sort(sheets, new Comparator<EventSheetDescriptor>() {
                            @Override
                            public int compare(EventSheetDescriptor o1, EventSheetDescriptor o2) {
                                return o1.getOffset() - o2.getOffset();
                            }
                        });
                        sheetsSorted = true;
                    }

                    sheetIndex++;
                }
                break;
            case PaletteRecord.sid:
                palette = (PaletteRecord) record;
                break;
            case SSTRecord.sid: // Holds all the strings for LabelSSTRecords
            case BoolErrRecord.sid:
            case FormulaRecord.sid: // Cell value from a formula
            case LabelRecord.sid: // Strings stored directly in the cell
            case LabelSSTRecord.sid: // String in the shared string table
            case NumberRecord.sid: // Numeric cell value
            case RKRecord.sid: // Excel internal number record
            case BlankRecord.sid:
                if (isNeededSheet()) {
                    CellValueRecordInterface r = (CellValueRecordInterface) record;
                    int row = r.getRow();
                    short column = r.getColumn();

                    if (IGridRegion.Tool.contains(tableRegion, column, row)) {
                        short styleIndex = r.getXFIndex();
                        int internalRow = row - tableRegion.getTop();
                        int internalCol = column - tableRegion.getLeft();
                        cellIndexes[internalRow][internalCol] = styleIndex;
                    }
                }

                break;

        }
    }

    private boolean isNeededSheet() {
        return sheetIndex == sheet.getIndex();
    }
}
