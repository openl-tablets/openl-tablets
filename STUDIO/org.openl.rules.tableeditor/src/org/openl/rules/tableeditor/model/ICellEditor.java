package org.openl.rules.tableeditor.model;

import org.openl.rules.tableeditor.event.TableEditorController.EditorTypeResponse;

/**
 *
 * @author snshor
 *
 *         This interface is base interface for all cell editors that are created by Table Editor.
 *
 */

public interface ICellEditor {

    String CE_TEXT = "text";
    String CE_NUMERIC = "numeric";
    String CE_MULTILINE = "multiline";
    String CE_COMBO = "combo";
    String CE_DATE = "date";
    String CE_MULTISELECT = "multiselect";
    String CE_FORMULA = "formula";
    String CE_BOOLEAN = "boolean";
    String CE_ARRAY = "array";
    String CE_RANGE = "range";
    String CE_INTEGER = "integer";
    String CE_DOUBLE = "double";

    /**
     *
     * @return bean containing information that will be processed on the client to initialize JS editor
     */
    EditorTypeResponse getEditorTypeAndMetadata();

}
