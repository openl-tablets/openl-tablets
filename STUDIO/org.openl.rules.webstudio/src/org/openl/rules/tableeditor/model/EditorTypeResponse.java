package org.openl.rules.tableeditor.model;

/**
 * The editor a cell asks for, and what that editor needs to know about the cell.
 *
 * @param editor the kind of editor, one of the {@code CE_*} names of {@link ICellEditor}
 * @param params what bounds the editor — the choices to pick from, the range to stay within, the editor of an
 *               element — or {@code null} when the kind alone says everything
 */
public record EditorTypeResponse(String editor, Object params) {

    public EditorTypeResponse(String editor) {
        this(editor, null);
    }
}
