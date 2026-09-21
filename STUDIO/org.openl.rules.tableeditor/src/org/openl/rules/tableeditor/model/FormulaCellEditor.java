package org.openl.rules.tableeditor.model;


public class FormulaCellEditor implements ICellEditor {

    @Override
    public EditorTypeResponse getEditorTypeAndMetadata() {
        return new EditorTypeResponse(CE_FORMULA);
    }

}
