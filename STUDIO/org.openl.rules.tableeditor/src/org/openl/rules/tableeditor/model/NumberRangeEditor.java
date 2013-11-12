package org.openl.rules.tableeditor.model;

import org.openl.rules.tableeditor.event.TableEditorController.EditorTypeResponse;

public class NumberRangeEditor implements ICellEditor {

    private NumberRangeParams params = new NumberRangeParams();

    public NumberRangeEditor(String entryEditor) {
        this.params.setEntryEditor(entryEditor);
    }

    public EditorTypeResponse getEditorTypeAndMetadata() {
        EditorTypeResponse typeResponse = new EditorTypeResponse(CE_RANGE);
        typeResponse.setParams(params);

        return typeResponse;
    }

    public class NumberRangeParams {

        private String entryEditor;

        public String getEntryEditor() {
            return entryEditor;
        }

        public void setEntryEditor(String entryEditor) {
            this.entryEditor = entryEditor;
        }

    }

}
