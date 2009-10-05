package org.openl.rules.tableeditor.model;

/**
 *
 * @author snshor
 *
 * Some editors like code editor with code assist or lookup editors may use
 * server part to do complex processing
 */

public interface ICellEditorServerPart {

    /**
     * Provides server-side validation for input data
     *
     *
     * @param input
     * @return null if input is valid or error message
     */

    public String validate(String input);

}
