package org.openl.excel.parser.event;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.model.HSSFFormulaParser;
import org.apache.poi.hssf.record.*;
import org.apache.poi.hssf.usermodel.HSSFComment;
import org.apache.poi.hssf.usermodel.HSSFShapeFactory;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.util.CellAddress;
import org.openl.excel.parser.TableStyles;
import org.openl.excel.parser.event.style.CommentsCollector;
import org.openl.excel.parser.event.style.EventTableStyles;
import org.openl.rules.table.IGridRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TableStyleListener implements HSSFListener {
    private final Logger log = LoggerFactory.getLogger(TableStyleListener.class);

    private final EventSheetDescriptor sheet;
    private final IGridRegion tableRegion;
    private TableStyles tableStyles;
    private List<HSSFComment> comments;
    private final Map<CellAddress, String> formulas = new HashMap<>();

    private final List<EventSheetDescriptor> sheets = new ArrayList<>();
    private int sheetIndex = -1;
    private boolean sheetsSorted = false;
    private final int[][] cellIndexes;
    private PaletteRecord palette;
    private DirectoryNode directory;
    private List<RecordBase> shapeRecords = new ArrayList<>();

    public TableStyleListener(EventSheetDescriptor sheet, IGridRegion tableRegion) {
        this.sheet = sheet;
        this.tableRegion = tableRegion;
        cellIndexes = new int[IGridRegion.Tool.height(tableRegion)][IGridRegion.Tool.width(tableRegion)];
    }

    void process(String fileName) throws IOException {
        try (POIFSFileSystem poifs = new POIFSFileSystem(new File(fileName))) {
            this.directory = poifs.getRoot();

            final StyleTrackingListener formatListener = new StyleTrackingListener(this);

            // Default HSSFEventFactory doesn't include ContinueRecord items in the stream and it breaks Comments parsing
            // for some cases. So we used to override processEvents() and initialize RecordFactoryInputStream
            // to include ContinueRecord items in the stream.
            HSSFEventFactory factory = new HSSFEventFactory() {
                @Override
                public void processEvents(HSSFRequest req, InputStream in) {
                    // Include ContinueRecord items
                    RecordFactoryInputStream recordStream = new RecordFactoryInputStream(in, true);

                    Record r;
                    while ((r = recordStream.nextRecord()) != null) {
                        formatListener.processRecord(r);
                    }
                }
            };

            HSSFRequest request = new HSSFRequest();
            request.addListenerForAllRecords(formatListener);
            factory.processWorkbookEvents(request, poifs);

            if (palette == null) {
                palette = new PaletteRecord();
            }
            collectComments();

            tableStyles = new EventTableStyles(
                    tableRegion,
                    cellIndexes,
                    formatListener.getExtendedFormats(),
                    formatListener.getCustomFormats(),
                    palette,
                    formatListener.getFonts(),
                    comments,
                    formulas
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
            case FormulaRecord.sid: // Cell value from a formula
                if (isNeededSheet()) {
                    FormulaRecord r = (FormulaRecord) record;
                    int row = r.getRow();
                    short column = r.getColumn();

                    if (IGridRegion.Tool.contains(tableRegion, column, row)) {
                        try {
                            String formula = HSSFFormulaParser.toFormulaString(null, r.getParsedExpression());
                            formulas.put(new CellAddress(row, column), formula);
                        } catch (Exception e) {
                            log.error("Can't read formula in sheet '{}' row {} column {}", sheet.getName(), row, column, e);
                        }

                        // Don't forget to save style index
                        saveStyleIndex(r, row, column);
                    }
                }
                break;
            case SSTRecord.sid: // Holds all the strings for LabelSSTRecords
            case BoolErrRecord.sid:
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
                        saveStyleIndex(r, row, column);
                    }
                }

                break;
            case NoteRecord.sid:
            case ContinueRecord.sid:
            case ObjRecord.sid:
            case TextObjectRecord.sid:
            case DrawingRecord.sid:
                if (isNeededSheet()) {
                    shapeRecords.add(record);
                }
                break;
        }
    }

    private void saveStyleIndex(CellValueRecordInterface r, int row, short column) {
        short styleIndex = r.getXFIndex();
        int internalRow = row - tableRegion.getTop();
        int internalCol = column - tableRegion.getLeft();
        cellIndexes[internalRow][internalCol] = styleIndex;
    }

    private boolean isNeededSheet() {
        return sheetIndex == sheet.getIndex();
    }

    private void collectComments() {
        int loc = findFirstDrawingRecord();
        if (loc >= 0) {
            EscherAggregate r;
            try {
                r = EscherAggregate.createAggregate(shapeRecords, loc);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                comments = Collections.emptyList();
                return;
            }
            EscherContainerRecord dgContainer = r.getEscherContainer();
            if (dgContainer == null) {
                return;
            }

            EscherContainerRecord spgrContainer = dgContainer.getChildContainers().get(0);
            List<EscherContainerRecord> spgrChildren = spgrContainer.getChildContainers();

            CommentsCollector commentCollector = new CommentsCollector();
            for (int i = 1; i < spgrChildren.size(); i++) {
                EscherContainerRecord spContainer = spgrChildren.get(i);
                HSSFShapeFactory.createShapeTree(spContainer, r, commentCollector, directory);
            }
            comments = commentCollector.getComments();
        }
    }

    private int findFirstDrawingRecord() {
        int size = shapeRecords.size();
        for (int i = 0; i < size; i++) {
            RecordBase rb = shapeRecords.get(i);
            if (rb instanceof Record && ((Record) rb).getSid() == DrawingRecord.sid) {
                return i;
            }
        }
        return -1;
    }
}
