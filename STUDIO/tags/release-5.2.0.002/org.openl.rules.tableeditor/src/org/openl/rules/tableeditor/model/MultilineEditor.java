package org.openl.rules.tableeditor.model;

import org.openl.rules.tableeditor.event.TableEditorController;

public class MultilineEditor implements ICellEditor{
    /**
     * @return bean containing information that will be processed on the client to initialize JS editor
     */
    public TableEditorController.EditorTypeResponse getEditorTypeAndMetadata() {
        return new TableEditorController.EditorTypeResponse(CE_MULTILINE);
    }

    
    public ICellEditorServerPart getServerPart() {
        return null;
    }
}
