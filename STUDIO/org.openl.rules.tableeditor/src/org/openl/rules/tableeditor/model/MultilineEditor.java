package org.openl.rules.tableeditor.model;

import org.openl.rules.tableeditor.event.TableEditorController.EditorTypeResponse;

public class MultilineEditor implements ICellEditor {

    /**
     * @return bean containing information that will be processed on the client
     *         to initialize JS editor
     */
    @Override
    public EditorTypeResponse getEditorTypeAndMetadata() {
        return new EditorTypeResponse(CE_MULTILINE);
    }

}
