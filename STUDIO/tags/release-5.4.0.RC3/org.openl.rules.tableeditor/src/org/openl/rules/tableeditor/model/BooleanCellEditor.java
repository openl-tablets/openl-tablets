package org.openl.rules.tableeditor.model;

import org.openl.rules.tableeditor.event.TableEditorController.EditorTypeResponse;

public class BooleanCellEditor implements ICellEditor {

    public EditorTypeResponse getEditorTypeAndMetadata() {
        EditorTypeResponse typeResponse = new EditorTypeResponse(CE_BOOLEAN);
        return typeResponse;
    }

    public ICellEditorServerPart getServerPart() {
        // TODO Auto-generated method stub
        return null;
    }

}
