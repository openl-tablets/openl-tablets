package org.openl.rules.webstudio.web.tableeditor.js;

/**
 * This interface contains methods that TableEditor javascript object expects to be present in a bean it communicates
 * with.
 *
 * All methods do not take parameters, all information should be pulled out of <code>FacesContext</code> and return
 * a string - JSF navigation outcome.
 */
public interface JSTableEditor {
    String load() throws Exception;

    String save() throws Exception;

    String getCellType() throws Exception;

    String undo() throws Exception;

    String redo() throws Exception;

    String addRowColBefore() throws Exception;

    String removeRowCol() throws Exception;

    String saveTable() throws Exception;

    String setAlign() throws Exception;
}
