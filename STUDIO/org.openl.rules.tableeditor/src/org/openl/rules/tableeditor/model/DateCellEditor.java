package org.openl.rules.tableeditor.model;


public class DateCellEditor implements ICellEditor {

    @Override
    public EditorTypeResponse getEditorTypeAndMetadata() {
        return new EditorTypeResponse(CE_DATE);
    }

}
