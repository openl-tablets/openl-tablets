package org.openl.rules.tableeditor.model;


public class BooleanCellEditor implements ICellEditor {

    @Override
    public EditorTypeResponse getEditorTypeAndMetadata() {
        return new EditorTypeResponse(CE_BOOLEAN);
    }

}
