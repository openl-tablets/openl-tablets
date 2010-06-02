package org.openl.rules.tableeditor.model;

import org.openl.rules.tableeditor.model.RangeParam;
import org.openl.rules.tableeditor.event.TableEditorController.EditorTypeResponse;

public class NumericCellEditor implements ICellEditor {

    private int min;

    private int max;

    public NumericCellEditor(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public EditorTypeResponse getEditorTypeAndMetadata() {
        EditorTypeResponse typeResponse = new EditorTypeResponse(CE_NUMERIC);
        typeResponse.setParams(new RangeParam((long) min, (long) max));
        return typeResponse;
    }

}
