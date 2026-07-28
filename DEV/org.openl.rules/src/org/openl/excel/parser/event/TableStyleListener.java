package org.openl.excel.parser.event;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.model.HSSFFormulaParser;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.BlankRecord;
import org.apache.poi.hssf.record.BoolErrRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.CellValueRecordInterface;
import org.apache.poi.hssf.record.ContinueRecord;
import org.apache.poi.hssf.record.DrawingRecord;
import org.apache.poi.hssf.record.EscherAggregate;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.LabelRecord;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.NoteRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.hssf.record.PaletteRecord;
import org.apache.poi.hssf.record.RKRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.RecordBase;
import org.apache.poi.hssf.record.RecordFactoryInputStream;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.hssf.record.StringRecord;
import org.apache.poi.hssf.record.TextObjectRecord;
import org.apache.poi.hssf.record.aggregates.FormulaRecordAggregate;
import org.apache.poi.hssf.record.aggregates.SharedValueManager;
import org.apache.poi.hssf.usermodel.HSSFComment;
import org.apache.poi.hssf.usermodel.HSSFShapeFactory;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.formula.WorkbookDependentFormula;
import org.apache.poi.ss.util.CellAddress;

import org.openl.excel.parser.TableStyles;
import org.openl.excel.parser.event.style.CommentsCollector;
import org.openl.excel.parser.event.style.EventTableStyles;
import org.openl.rules.table.IGridRegion;

@Slf4j
public class TableStyleListener implements HSSFListener {

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
    private final List<RecordBase> shapeRecords = new ArrayList<>();

    private FormulaRecord currentFormula;
    private SharedValueManager sharedValueManager;

    public TableStyleListener(EventSheetDescriptor sheet, IGridRegion tableRegion) {
        this.sheet = sheet;
        this.tableRegion = tableRegion;
        cellIndexes = new int[IGridRegion.Tool.height(tableRegion)][IGridRegion.Tool.width(tableRegion)];
    }

    void process(String fileName) throws IOException {
        try (var poifs = new POIFSFileSystem(new File(fileName))) {
            var factory = new HSSFEventFactory();
            var request = new HSSFRequest();
            var sharedFormulaListener = new SharedValueListener(sheet);
            request.addListenerForAllRecords(sharedFormulaListener);
            factory.processWorkbookEvents(request, poifs);
            sharedValueManager = sharedFormulaListener.getSharedValueManager();
        }

        try (var poifs = new POIFSFileSystem(new File(fileName))) {
            this.directory = poifs.getRoot();

            final var formatListener = new StyleTrackingListener(this);

            // Default HSSFEventFactory does not include ContinueRecord items in the stream and it breaks Comments
            // parsing
            // for some cases. So we used to override processEvents() and initialize RecordFactoryInputStream
            // to include ContinueRecord items in the stream.
            var factory = new HSSFEventFactory() {
                @Override
                public void processEvents(HSSFRequest req, InputStream in) {
                    // Include ContinueRecord items
                    var recordStream = new RecordFactoryInputStream(in, true);

                    Record r;
                    while ((r = recordStream.nextRecord()) != null) {
                        formatListener.processRecord(r);
                    }
                }
            };

            var request = new HSSFRequest();
            request.addListenerForAllRecords(formatListener);
            factory.processWorkbookEvents(request, poifs);

            if (palette == null) {
                palette = new PaletteRecord();
            }
            collectComments();

            tableStyles = new EventTableStyles(tableRegion,
                    cellIndexes,
                    formatListener.getExtendedFormats(),
                    formatListener.getCustomFormats(),
                    palette,
                    formatListener.getFonts(),
                    comments,
                    formulas);
        }
    }

    public TableStyles getTableStyles() {
        return tableStyles;
    }

    @Override
    public void processRecord(Record record) {
        processFormula(record);

        switch (record.getSid()) {
            case BoundSheetRecord.sid:
                var bsr = (BoundSheetRecord) record;
                if (bsr.getSheetname().equals(sheet.getName())) {
                    sheets.add(new EventSheetDescriptor(bsr.getSheetname(), sheets.size(), bsr.getPositionOfBof()));
                }
                break;
            case BOFRecord.sid:
                var bof = (BOFRecord) record;
                if (bof.getType() == BOFRecord.TYPE_WORKSHEET) {
                    if (!sheetsSorted) {
                        sheets.sort(Comparator.comparingInt(EventSheetDescriptor::getOffset));
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
                    var r = (FormulaRecord) record;
                    var row = r.getRow();
                    short column = r.getColumn();

                    if (IGridRegion.Tool.contains(tableRegion, column, row)) {
                        currentFormula = (FormulaRecord) record;
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
                    var r = (CellValueRecordInterface) record;
                    var row = r.getRow();
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

    private void processFormula(Record record) {
        if (currentFormula != null) {
            var row = currentFormula.getRow();
            short column = currentFormula.getColumn();
            try {
                StringRecord cachedText = null;
                if (record instanceof StringRecord stringRecord) {
                    cachedText = stringRecord;
                } else {
                    currentFormula.setCachedResultBoolean(false);
                }
                var formulaAggregate = new FormulaRecordAggregate(currentFormula,
                        cachedText,
                        sharedValueManager);
                var formulaTokens = formulaAggregate.getFormulaTokens();
                var workbookDependentFormula = Arrays.stream(formulaTokens)
                        .anyMatch(t -> t instanceof WorkbookDependentFormula);
                if (workbookDependentFormula) {
                    formulas.put(new CellAddress(row, column), "");
                } else {
                    String formula = HSSFFormulaParser.toFormulaString(null, formulaTokens);
                    formulas.put(new CellAddress(row, column), formula);
                }
            } catch (Exception e) {
                log.error("Cannot read formula in sheet '{}' row {} column {}", sheet.getName(), row, column, e);
            }
            currentFormula = null;
        }
    }

    private void saveStyleIndex(CellValueRecordInterface r, int row, short column) {
        short styleIndex = r.getXFIndex();
        var internalRow = row - tableRegion.getTop();
        var internalCol = column - tableRegion.getLeft();
        cellIndexes[internalRow][internalCol] = styleIndex;
    }

    private boolean isNeededSheet() {
        return sheetIndex == sheet.getIndex();
    }

    private void collectComments() {
        var loc = findFirstDrawingRecord();
        if (loc >= 0) {
            EscherAggregate r;
            try {
                r = EscherAggregate.createAggregate(shapeRecords, loc);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                comments = Collections.emptyList();
                return;
            }
            var dgContainer = r.getEscherContainer();
            if (dgContainer == null) {
                return;
            }

            var spgrContainer = dgContainer.getChildContainers().getFirst();
            List<EscherContainerRecord> spgrChildren = spgrContainer.getChildContainers();

            var commentCollector = new CommentsCollector();
            for (var i = 1; i < spgrChildren.size(); i++) {
                var spContainer = spgrChildren.get(i);
                HSSFShapeFactory.createShapeTree(spContainer, r, commentCollector, directory);
            }
            comments = commentCollector.getComments();
        }
    }

    private int findFirstDrawingRecord() {
        var size = shapeRecords.size();
        for (var i = 0; i < size; i++) {
            var rb = shapeRecords.get(i);
            if (rb instanceof Record record && record.getSid() == DrawingRecord.sid) {
                return i;
            }
        }
        return -1;
    }
}
