package org.openl.rules.tableeditor.model;

import org.openl.rules.tableeditor.event.TableEditorController.EditorTypeResponse;

public class FormulaCellEditor implements ICellEditor {

    public EditorTypeResponse getEditorTypeAndMetadata() {
        EditorTypeResponse typeResponse = new EditorTypeResponse(CE_FORMULA);
        return typeResponse;
    }

    public ICellEditorServerPart getServerPart() {
        return null;
    }

}
