package org.openl.rules.tableeditor.model;

import org.openl.rules.tableeditor.event.TableEditorController.EditorTypeResponse;

public class ArrayCellEditor implements ICellEditor {

    public static final String DEFAULT_SEPARATOR = ",";

    private ArrayEditorParams params = new ArrayEditorParams();

    public ArrayCellEditor() {
        this.params.setSeparator(DEFAULT_SEPARATOR);
    }

    public ArrayCellEditor(String separator, String entryEditor) {
        this.params.setSeparator(separator);
        this.params.setEntryEditor(entryEditor);
    }

    public EditorTypeResponse getEditorTypeAndMetadata() {
        EditorTypeResponse typeResponse = new EditorTypeResponse(CE_ARRAY);
        typeResponse.setParams(params);

        return typeResponse;
    }

    public class ArrayEditorParams {

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
