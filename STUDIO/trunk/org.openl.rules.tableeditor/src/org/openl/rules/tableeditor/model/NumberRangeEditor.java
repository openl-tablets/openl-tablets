package org.openl.rules.tableeditor.model;

import org.openl.rules.tableeditor.event.TableEditorController.EditorTypeResponse;

public class NumberRangeEditor implements ICellEditor {

    public static final String DEFAULT_SEPARATOR = "-";

    private NumberRangeParams params = new NumberRangeParams();

    public NumberRangeEditor() {
        this.params.setSeparator(DEFAULT_SEPARATOR);
    }

    public NumberRangeEditor(String separator, String entryEditor) {
        this.params.setSeparator(separator);
        this.params.setEntryEditor(entryEditor);
    }

    public EditorTypeResponse getEditorTypeAndMetadata() {
        EditorTypeResponse typeResponse = new EditorTypeResponse(CE_RANGE);
        typeResponse.setParams(params);

        return typeResponse;
    }

    public class NumberRangeParams {

        private String separator;
        private String entryEditor;

        public String getSeparator() {
            return separator;
        }

        public void setSeparator(String separator) {
            this.separator = separator;
        }

        public String getEntryEditor() {
            return entryEditor;
        }

        public void setEntryEditor(String entryEditor) {
            this.entryEditor = entryEditor;
        }

    }

}
