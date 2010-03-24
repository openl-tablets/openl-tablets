package org.openl.rules.webstudio.web.tableeditor;

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
