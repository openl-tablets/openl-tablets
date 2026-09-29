package org.openl.rules.tableeditor.model;

public class ArrayCellEditor implements ICellEditor {

    public static final String DEFAULT_SEPARATOR = ",";

    private final ArrayEditorParams params;

    public ArrayCellEditor(String separator, String entryEditor, boolean intOnly) {
        this.params = new ArrayEditorParams(separator, entryEditor, intOnly);
    }

    @Override
    public EditorTypeResponse getEditorTypeAndMetadata() {
        return new EditorTypeResponse(CE_ARRAY, params);
    }

    /**
     * How the entries of an array are written into one cell.
     *
     * @param separator   what separates the entries
     * @param entryEditor the editor one entry is written with
     * @param intOnly     {@code true} when only whole numbers are accepted as entries
     */
    public record ArrayEditorParams(String separator, String entryEditor, boolean intOnly) {
    }

}
