package org.openl.rules.tableeditor.model;

import org.openl.rules.tableeditor.model.RangeParam;
import org.openl.rules.tableeditor.event.TableEditorController.EditorTypeResponse;

public class NumericCellEditor implements ICellEditor {

    private Number min;
    private Number max;

    public NumericCellEditor(Number min, Number max) {
        this.min = min;
        this.max = max;
    }

    public EditorTypeResponse getEditorTypeAndMetadata() {
        EditorTypeResponse typeResponse = new EditorTypeResponse(CE_NUMERIC);
        typeResponse.setParams(new RangeParam(min, max));
        return typeResponse;
    }

}
