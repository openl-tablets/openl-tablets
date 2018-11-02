package org.openl.rules.ui;

import java.util.*;

import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

public class RecentlyVisitedTables {
    private VisitedTableWrapper lastVisitedTable = null;
    public static final int DEFAULT_SIZE = 10;

    public int size;
    public Deque<VisitedTableWrapper> tables = new LinkedList<VisitedTableWrapper>();

    private final Object lock = new Object();

    public RecentlyVisitedTables() {
        this(DEFAULT_SIZE);
    }

    public RecentlyVisitedTables(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException();
        }
        this.size = size;
    }

    public void add(IOpenLTable table) {
        VisitedTableWrapper vtw = new VisitedTableWrapper(table);

        synchronized (lock) {
            if (tables.contains(vtw)) {
                tables.remove(vtw);
            }

            if (tables.size() >= size) {
                tables.removeLast();
            }

            tables.addFirst(vtw);
        }
    }

    /**
     * Returns the copy of recently visited tables.
     *
     * @return copy of recently visited tables
     */
    public Collection<VisitedTableWrapper> getTables() {
        Collection<VisitedTableWrapper> tablesCopy;
        synchronized (lock) {
            checkTableAvailability();

            tablesCopy = new LinkedList<VisitedTableWrapper>(tables);
        }
        return tablesCopy;
    }

    public void clear() {
        synchronized (lock) {
            tables.clear();
        }
    }

    public int getSize() {
        int length;
        synchronized (lock) {
            length = tables.size();
        }
        return length;
    }

    private void checkTableAvailability() {
        List<VisitedTableWrapper> tableForRemove = new ArrayList<VisitedTableWrapper>();

        for (VisitedTableWrapper table : tables) {
            WebStudio studio = WebStudioUtils.getWebStudio();
            IOpenLTable refreshTable = studio.getModel().getTableById(table.getId());

            if (refreshTable == null) {
                tableForRemove.add(table);
            }
        }

        tables.removeAll(tableForRemove);
    }

    public void remove(IOpenLTable table) {
        synchronized (lock) {
            checkTableAvailability();
            tables.remove(new VisitedTableWrapper(table));
        }
    }

    public VisitedTableWrapper getLastVisitedTable() {
        return lastVisitedTable;
    }

    public void setLastVisitedTable(VisitedTableWrapper lastVisitedTable) {
        this.lastVisitedTable = lastVisitedTable;
    }

    public void setLastVisitedTable(IOpenLTable lastVisitedTable) {
        setLastVisitedTable(new VisitedTableWrapper(lastVisitedTable));
    }

    /*
     * This is the class wrapper. It is used for the properly name showing
     */
    public static class VisitedTableWrapper {
        private final String id;
        private final String type;
        private final String name;

        public VisitedTableWrapper(IOpenLTable table) {
            this.id = table.getId();
            this.type = table.getType();
            this.name = getName(table);
        }

        public String getId() {
            return id;
        }
        
        public String getType() {
            return type;
        }
        
        public String getName() {
            return name;
        }

        private String getName(IOpenLTable table) {
            String tableName = table.getDisplayName();

            if (tableName == null || tableName.isEmpty()) {
                tableName = TableSyntaxNodeUtils.str2name(table.getGridTable().getCell(0, 0).getStringValue()
                    , XlsNodeTypes.getEnumByValue(table.getType()));
            }

            String[] dimensionProps = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();
            ITableProperties tableProps = table.getProperties();
            StringBuilder dimensionBuilder = new StringBuilder();

            if (tableProps != null) {
                for (String dimensionProp : dimensionProps) {
                    String propValue = tableProps.getPropertyValueAsString(dimensionProp);

                    if (propValue != null && !propValue.isEmpty()) {
                        dimensionBuilder.append(dimensionBuilder.length() == 0 ? "" : ", ").append(dimensionProp).append(" = ").append(propValue);
                    }
                }
            }

            if (dimensionBuilder.length() > 0) {
                return tableName +"["+ dimensionBuilder.toString() +"]";
            } else {
                return tableName;
            }
        }

        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            
            if (obj == this) {
                return true;
            }
            
            if (obj.getClass() != getClass()) {
                return false;
            }
            
            VisitedTableWrapper wrapper = (VisitedTableWrapper) obj;
            
            return  Objects.equals(getId(), wrapper.getId());
        }
        
        public int hashCode() {
            return Objects.hashCode(getId());
        }
    }

}
