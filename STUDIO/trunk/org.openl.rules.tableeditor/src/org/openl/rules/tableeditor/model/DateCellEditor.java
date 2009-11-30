package org.openl.rules.tableeditor.model;

import org.openl.rules.tableeditor.event.TableEditorController.EditorTypeResponse;

public class DateCellEditor implements ICellEditor {

    public EditorTypeResponse getEditorTypeAndMetadata() {
        EditorTypeResponse typeResponse = new EditorTypeResponse(CE_DATE);
        return typeResponse;
    }

    public ICellEditorServerPart getServerPart() {
        // TODO Auto-generated method stub
        return null;
    }

}
