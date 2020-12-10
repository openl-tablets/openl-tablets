package org.openl.excel.parser.event;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.record.ArrayRecord;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.SharedFormulaRecord;
import org.apache.poi.hssf.record.TableRecord;
import org.apache.poi.hssf.record.aggregates.SharedValueManager;
import org.apache.poi.ss.util.CellReference;

public class SharedValueListener implements HSSFListener {
    private final EventSheetDescriptor sheet;
    private int sheetIndex = -1;
    private final List<SharedFormulaRecord> sharedFormulaRecords = new ArrayList<>();
    private final List<CellReference> firstCellRefs = new ArrayList<>();
    private final List<ArrayRecord> arrayRecords = new ArrayList<>();
    private final List<TableRecord> tableRecords = new ArrayList<>();
    private FormulaRecord currentFormula;

    public SharedValueListener(EventSheetDescriptor sheet) {
        this.sheet = sheet;
    }

    @Override
    public void processRecord(org.apache.poi.hssf.record.Record record) {
        switch (record.getSid()) {
            case BOFRecord.sid:
                BOFRecord bof = (BOFRecord) record;
                if (bof.getType() == BOFRecord.TYPE_WORKSHEET) {
                    sheetIndex++;
                }
                break;
            case FormulaRecord.sid:
                if (isNeededSheet()) {
                    currentFormula = (FormulaRecord) record;
                }
                break;
            case SharedFormulaRecord.sid:
                if (isNeededSheet()) {
                    sharedFormulaRecords.add((SharedFormulaRecord) record);
                    firstCellRefs.add(new CellReference(currentFormula.getRow(), currentFormula.getColumn()));
                }
                break;
            case ArrayRecord.sid:
                if (isNeededSheet()) {
                    arrayRecords.add((ArrayRecord) record);
                }
                break;
            case TableRecord.sid:
                if (isNeededSheet()) {
                    tableRecords.add((TableRecord) record);
                }
                break;
        }
    }

    private boolean isNeededSheet() {
        return sheetIndex == sheet.getIndex();
    }

    public SharedValueManager getSharedValueManager() {
        return SharedValueManager.create(sharedFormulaRecords.toArray(new SharedFormulaRecord[0]),
            firstCellRefs.toArray(new CellReference[0]),
            arrayRecords.toArray(new ArrayRecord[0]),
            tableRecords.toArray(new TableRecord[0]));
    }
}
