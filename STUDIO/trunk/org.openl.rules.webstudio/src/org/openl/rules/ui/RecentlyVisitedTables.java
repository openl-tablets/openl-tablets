package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

public class RecentlyVisitedTables {
    private IOpenLTable lastVisitedTable = null;
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
        checkTableAvailability();
        
        return tables;
    }

    public void clear() {
        tables.clear();
    }

    public int getSize() {
        return tables.size();
    }
    
    public void checkTableAvailability() {
        List<IOpenLTable> tableForRemove = new ArrayList<IOpenLTable>();
        
        for (IOpenLTable table : tables) {
            WebStudio studio = WebStudioUtils.getWebStudio();
            IOpenLTable refreshTable = studio.getModel().getTable(table.getUri());

            if (refreshTable == null) {
                tableForRemove.add(table);
            }
        }
        
        tables.removeAll(tableForRemove);
    }

    public IOpenLTable getLastVisitedTable() {
        return lastVisitedTable;
    }

    public void setLastVisitedTable(IOpenLTable lastVisitedTable) {
        this.lastVisitedTable = lastVisitedTable;
    }

}
