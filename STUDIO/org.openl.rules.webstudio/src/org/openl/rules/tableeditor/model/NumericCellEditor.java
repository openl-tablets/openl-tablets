package org.openl.rules.tableeditor.model;


public class NumericCellEditor implements ICellEditor {

    private final RangeParam params;

    public NumericCellEditor(Number min, Number max, boolean intOnly) {
        this.params = new RangeParam(min, max, intOnly);
    }

    @Override
    public EditorTypeResponse getEditorTypeAndMetadata() {
        return new EditorTypeResponse(CE_NUMERIC, params);
    }

}
