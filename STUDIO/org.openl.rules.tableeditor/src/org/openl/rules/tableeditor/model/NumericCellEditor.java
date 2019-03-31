package org.openl.rules.tableeditor.model;

import org.openl.rules.tableeditor.event.TableEditorController.EditorTypeResponse;

public class NumericCellEditor implements ICellEditor {

    private final RangeParam params;

    public NumericCellEditor(Number min, Number max, boolean intOnly) {
        this.params = new RangeParam(min, max, intOnly);
    }

    @Override
    public EditorTypeResponse getEditorTypeAndMetadata() {
        EditorTypeResponse typeResponse = new EditorTypeResponse(CE_NUMERIC);
        typeResponse.setParams(params);
        return typeResponse;
    }

}
