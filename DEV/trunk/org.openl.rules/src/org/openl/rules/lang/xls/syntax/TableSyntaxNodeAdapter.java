package org.openl.rules.lang.xls.syntax;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.inherit.PropertiesChecker;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenMember;

public class TableSyntaxNodeAdapter implements IOpenLTable {

    private TableSyntaxNode tsn;

    public TableSyntaxNodeAdapter(TableSyntaxNode tsn) {
        if (tsn == null) {
            throw new IllegalArgumentException("TableSyntaxNode is null");
        }
        this.tsn = tsn;
    }

    @Override
    public IGridTable getGridTable() {
        return tsn.getGridTable();
    }

    @Override
    public IGridTable getGridTable(String view) {
        if (view != null) {
            ILogicalTable gtx = tsn.getTable(view);
            if (gtx != null) {
                return gtx.getSource();
            }
        }
        return getGridTable();
    }

    @Override
    public ITableProperties getProperties() {
        return tsn.getTableProperties();
    }

    @Override
    public String getType() {
        return tsn.getType();
    }

    @Override
    public List<OpenLMessage> getMessages() {
        SyntaxNodeException[] errors = tsn.getErrors();
        return OpenLMessagesUtils.newMessages(errors);
    }

    @Override
    public String getTechnicalName() {
        IOpenMember member = tsn.getMember();
        if (member != null) {
            return member.getName();
        }
        return StringUtils.EMPTY;
    }

    @Override
    public String getName() {
        String tableName = getTechnicalName();
        ITableProperties properties = getProperties();
        if (properties != null) {
            String name = properties.getName();
            String version = properties.getVersion();
            if (StringUtils.isNotBlank(name)) {
                tableName = name;
            }            
            if (StringUtils.isNotBlank(version)) { 
                // version was added for links to target tables.
                // see showTable.xhtml, Target table section.
                //
                return String.format("%s: %s", tableName, version);
            }
        }
        return tableName;
    }

    @Override
    public boolean isExecutable() {
        return tsn.isExecutableNode();
    }

    @Override
    public String getUri() {
        return tsn.getUri();
    }

    @Override
    public boolean isVersionable() {
        return PropertiesChecker.isPropertySuitableForTableType("version", tsn.getType());        
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).append(getUri()).toHashCode();
    }

    @Override
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

        TableSyntaxNodeAdapter table = (TableSyntaxNodeAdapter) obj;

        return new EqualsBuilder().append(getUri(), table.getUri()).isEquals();
    }

}
