package org.openl.rules.tableeditor.model;

import java.util.List;


public class NumericCellEditor implements ICellEditor {

    private final RangeParam params;

    public NumericCellEditor(Number min, Number max, boolean intOnly, List<String> constants) {
        this.params = new RangeParam(min, max, intOnly, constants);
    }

    @Override
    public EditorTypeResponse getEditorTypeAndMetadata() {
        return new EditorTypeResponse(CE_NUMERIC, params);
    }

}
