package org.openl.rules.tableeditor.model;

import org.openl.rules.tableeditor.event.TableEditorController.EditorTypeResponse;

public class FormulaCellEditor implements ICellEditor {

    public EditorTypeResponse getEditorTypeAndMetadata() {
        return new EditorTypeResponse(CE_FORMULA);
    }

}
