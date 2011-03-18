package org.openl.rules.tableeditor.event;

/**
 * This interface contains methods that TableEditor javascript object expects to
 * be present in a bean it communicates with.
 *
 */
public interface ITableEditorController {

    String load() throws Exception;

    String insertRowBefore() throws Exception;

    String insertColumnBefore() throws Exception;

    String removeRow() throws Exception;

    String removeColumn() throws Exception;

    String setCellValue() throws Exception;

    String setProperty() throws Exception;

    String setAlign() throws Exception;

    String setIndent() throws Exception;

    String saveTable() throws Exception;

    String undo() throws Exception;

    String redo() throws Exception;

}
