package org.openl.rules.tableeditor.model;


public class TextCellEditor implements ICellEditor {

    @Override
    public EditorTypeResponse getEditorTypeAndMetadata() {
        return new EditorTypeResponse(CE_TEXT);
    }

}
