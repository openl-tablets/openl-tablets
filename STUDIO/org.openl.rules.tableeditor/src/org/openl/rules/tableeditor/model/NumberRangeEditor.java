package org.openl.rules.tableeditor.model;

import lombok.Getter;
import lombok.Setter;

public class NumberRangeEditor implements ICellEditor {

    private final String entryEditor;

    public NumberRangeEditor(String entryEditor) {
        this.entryEditor = entryEditor;
    }

    @Override
    public EditorTypeResponse getEditorTypeAndMetadata() {
        var params = new NumberRangeParams();
        params.setEntryEditor(entryEditor);

        return new EditorTypeResponse(CE_RANGE, params);
    }

    public static class NumberRangeParams {

        @Getter
        @Setter
        private String entryEditor;
    }

}
