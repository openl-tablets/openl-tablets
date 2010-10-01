package org.openl.rules.table;

/**
 * Base interface that represents abstract table.
 * 
 * @author Andrei Astrouski
 */
public interface ITable<T extends ITable<T>> {

    int getWidth();

    int getHeight();

    boolean isNormalOrientation();

    ICell getCell(int column, int row);

    T getColumn(int column);

    T getColumns(int from);

    T getColumns(int from, int to);

    T getRow(int row);

    T getRows(int from);

    T getRows(int from, int to);

    T getSubtable(int column, int row, int width, int height);

    T transpose();

}
