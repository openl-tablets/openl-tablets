package org.openl.rules.tableeditor.model;

import org.openl.rules.range.RangeParser;
import org.openl.rules.tableeditor.event.TableEditorController.EditorTypeResponse;

public class NumberRangeEditor implements ICellEditor {

    private final String entryEditor;
    private final String parsedValue;

    public NumberRangeEditor(String entryEditor, String initialValue) {
        this.entryEditor = entryEditor;
        this.parsedValue = parseValue(initialValue);
    }

    @Override
    public EditorTypeResponse getEditorTypeAndMetadata() {
        NumberRangeParams params = new NumberRangeParams();
        params.setEntryEditor(entryEditor);
        params.setParsedValue(parsedValue);

        EditorTypeResponse typeResponse = new EditorTypeResponse(CE_RANGE);
        typeResponse.setParams(params);
        return typeResponse;
    }

    private String parseValue(String input) {
        if (input == null) {
            return "";
        }
        try {
            return RangeParser.parse(input).toString();
        } catch (Exception ignore) {
            return input;
        }
    }

    public static class NumberRangeParams {

        private String entryEditor;
        private String parsedValue;

        public String getEntryEditor() {
            return entryEditor;
        }

        public void setEntryEditor(String entryEditor) {
            this.entryEditor = entryEditor;
        }

        public String getParsedValue() {
            return parsedValue;
        }

        public void setParsedValue(String parsedValue) {
            this.parsedValue = parsedValue;
        }
    }

}
