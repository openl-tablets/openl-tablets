package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
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
        VisitedTableWrapper vtw = new VisitedTableWrapper(table);
        
        if (tables.contains(vtw)) {
            tables.remove(vtw);
        }

        if (tables.size() >= size) {
            tables.removeLast();
        }

        tables.addFirst(vtw);
    }

    public Collection<VisitedTableWrapper> getTables() {
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
        List<VisitedTableWrapper> tableForRemove = new ArrayList<VisitedTableWrapper>();
        
        for (VisitedTableWrapper table : tables) {
            WebStudio studio = WebStudioUtils.getWebStudio();
            IOpenLTable refreshTable = studio.getModel().getTable(table.getUri());

            if (refreshTable == null) {
                tableForRemove.add(table);
            }
        }
        
        tables.removeAll(tableForRemove);
    }

    public VisitedTableWrapper getLastVisitedTable() {
        return lastVisitedTable;
    }
    
    public void setLastVisitedTable(VisitedTableWrapper lastVisitedTable) {
        this.lastVisitedTable = lastVisitedTable;
    }
    
    public void setLastVisitedTable(IOpenLTable lastVisitedTable) {
        this.lastVisitedTable = new VisitedTableWrapper(lastVisitedTable);
    }
    
    /*
     * This is the class wrapper. It is used for the properly name showing
     */
    public class VisitedTableWrapper {
        private final String uri;
        private final String type;
        private final String name;
        
        public VisitedTableWrapper(IOpenLTable table) {
            this.uri = table.getUri();
            this.type = table.getType();
            this.name = getName(table);
        }
        
        public String getUri() {
            return uri;
        }
        
        public String getType() {
            return type;
        }
        
        public String getName() {
            return name;
        }
        private String getName(IOpenLTable table) {
            String tableName = table.getName();
            
            if (tableName == null || tableName.isEmpty()) {
                tableName = TableSyntaxNodeUtils.str2name(table.getGridTable().getCell(0, 0).getStringValue()
                    , XlsNodeTypes.getEnumByValue(table.getType()));
            }
                
            String[] dimensionProps = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();
            ITableProperties tableProps = table.getProperties();
            String dimension = "";
            
            if (tableProps != null) {
                for (int i=0; i < dimensionProps.length; i++) {
                    String propValue = tableProps.getPropertyValueAsString(dimensionProps[i]);
                    
                    if (propValue != null && !propValue.isEmpty()) {
                        dimension += (dimension.isEmpty() ? "" : ", ") + dimensionProps[i] + " = " +propValue;
                    }
                }
            }
            
            if (!dimension.isEmpty()) {
                return tableName +"["+ dimension +"]";
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
            
            return  new EqualsBuilder().append(getUri(), wrapper.getUri()).isEquals();
        }
        
        public int hashCode() {
            return new HashCodeBuilder(17, 31).append(getUri()).toHashCode();
        }
    }

}
