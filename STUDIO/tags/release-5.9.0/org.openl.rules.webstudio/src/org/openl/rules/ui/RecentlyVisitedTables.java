package org.openl.rules.ui;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;

import org.openl.rules.table.IOpenLTable;

public class RecentlyVisitedTables {

    public static final int DEFAULT_SIZE = 10;

    public int size;
    public Deque<IOpenLTable> tables = new LinkedList<IOpenLTable>();

    public RecentlyVisitedTables() {
        size = DEFAULT_SIZE;
    }

    public RecentlyVisitedTables(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException();
        }
        this.size = size;
    }

    public void add(IOpenLTable table) {
        if (tables.contains(table)) {
            tables.remove(table);
        }

        if (tables.size() >= size) {
            tables.removeLast();
        }

        tables.addFirst(table);
    }

    public Collection<IOpenLTable> getTables() {
        return tables;
    }

    public void clear() {
        tables.clear();
    }

    public int getSize() {
        return tables.size();
    }

}
