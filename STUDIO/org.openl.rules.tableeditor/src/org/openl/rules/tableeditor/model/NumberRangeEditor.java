package org.openl.rules.tableeditor.model;

public class NumberRangeEditor implements ICellEditor {

    private final NumberRangeParams params;

    public NumberRangeEditor(String entryEditor) {
        this.params = new NumberRangeParams(entryEditor);
    }

    @Override
    public EditorTypeResponse getEditorTypeAndMetadata() {
        return new EditorTypeResponse(CE_RANGE, params);
    }

    /**
     * How the bounds of a range are entered.
     *
     * @param entryEditor the editor one bound is written with
     */
    public record NumberRangeParams(String entryEditor) {
    }

}
