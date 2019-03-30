package org.openl.rules.table;

/**
 * Base interface that represents abstract table.
 * 
 * @author Andrei Astrouski
 * @author DLiauchuk
 */
public interface ITable<T extends ITable<T>> {

    /**
     * 
     * @return width of the table
     */
    int getWidth();

    /**
     * 
     * @return height of the table
     */
    int getHeight();

    /**
     * 
     * @return checks if it is transposed or not. see {@link #transpose()} and {@link TransposedGridTable}.
     */
    boolean isNormalOrientation();

    /**
     * 
     * @param column
     * @param row
     * @return cell form the given column and row.
     */
    ICell getCell(int column, int row);

    /**
     * 
     * @param column
     * @return the column represented as {@link ITable} by it`s index.
     */
    T getColumn(int column);

    /**
     * 
     * @param from
     * @return the columns represented as {@link ITable} from given index and till the right last column, including
     *         borders.
     */
    T getColumns(int from);

    /**
     * 
     * @param from
     * @param to
     * @return the columns represented as {@link ITable} from given start index and till the given end.
     */
    T getColumns(int from, int to);

    /**
     * 
     * @param row
     * @return the row represented as {@link ITable} by it`s index.
     */
    T getRow(int row);

    /**
     * 
     * @param from
     * @return the rows represented as {@link ITable} from given index and till the bottom row, including borders.
     */
    T getRows(int from);

    /**
     * 
     * @param from
     * @param to
     * @return the rows represented as {@link ITable} from given start index and till the given end.
     */
    T getRows(int from, int to);

    /**
     * 
     * @param column from which we want to take the subtable, including border.
     * @param row from which we want to take the subtable, including border.
     * @param width of the needed table.
     * @param height of the needed table.
     * @return the subtable of this table.
     */
    T getSubtable(int column, int row, int width, int height);

    /**
     * 
     * @return transposed current table. See {@link TransposedGridTable}.
     */
    T transpose();

}
