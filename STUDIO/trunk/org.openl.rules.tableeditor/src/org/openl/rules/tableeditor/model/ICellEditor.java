package org.openl.rules.tableeditor.model;

import org.openl.rules.tableeditor.event.TableEditorController.EditorTypeResponse;

/**
 *
 * @author snshor
 *
 * This interface is base interface for all cell editors that are created by
 * Table Editor
 *
 */

public interface ICellEditor {

    static final public String CE_TEXT = "text",
                               CE_NUMERIC = "numeric",
                               CE_MULTILINE = "multiline",
                               CE_COMBO = "combo",
                               CE_DATE = "date",
                               CE_MULTISELECT = "multiselect",
                               CE_FORMULA = "formula",
                               CE_BOOLEAN = "boolean";

    /**
     *
     * @return bean containing information that will be processed on the client
     *         to initialize JS editor
     */
    EditorTypeResponse getEditorTypeAndMetadata();

    /**
     * @return null if editor does not need server part to interact during
     *         editing. Most "value" editors will return null. Some editors like
     *         code editor with code assist or lookup editors may use server
     *         part to do complex processing
     */
    ICellEditorServerPart getServerPart();

}
