package org.openl.rules.tableeditor.model;

import lombok.Getter;
import lombok.Setter;

import org.openl.rules.tableeditor.event.TableEditorController.EditorTypeResponse;

public class ArrayCellEditor implements ICellEditor {

    public static final String DEFAULT_SEPARATOR = ",";

    private final ArrayEditorParams params = new ArrayEditorParams();

    public ArrayCellEditor(String separator, String entryEditor, boolean intOnly) {
        this.params.setSeparator(separator);
        this.params.setEntryEditor(entryEditor);
        this.params.setIntOnly(intOnly);
    }

    @Override
    public EditorTypeResponse getEditorTypeAndMetadata() {
        var typeResponse = new EditorTypeResponse(CE_ARRAY);
        typeResponse.setParams(params);

        return typeResponse;
    }

    public static class ArrayEditorParams {

        @Getter
        @Setter
        private String separator;
        @Getter
        @Setter
        private String entryEditor;
        @Getter
        @Setter
        private boolean intOnly;
    }

}
