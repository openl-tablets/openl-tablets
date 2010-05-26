package org.openl.rules.helpers;

public interface ITableAdaptor {
    Object get(int col, int row);

    int height();

    int maxWidth();

    int width(int row);
}
