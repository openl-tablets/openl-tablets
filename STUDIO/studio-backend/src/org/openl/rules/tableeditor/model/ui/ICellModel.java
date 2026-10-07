package org.openl.rules.tableeditor.model.ui;

/**
 * A place in the table's grid.
 *
 * <p>A place either holds a cell of its own or is reached over by a merged one; which of the two it is decides
 * whether the place is read or passed by.
 */
public sealed interface ICellModel permits CellModel, CellModelDelegator {
}
