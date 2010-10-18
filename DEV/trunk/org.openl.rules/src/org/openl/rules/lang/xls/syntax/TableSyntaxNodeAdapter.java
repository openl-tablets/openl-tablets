package org.openl.rules.lang.xls.syntax;

import java.util.List;

import org.apache.commons.lang.StringUtils;
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

    public IGridTable getGridTable() {
        return tsn.getGridTable();
    }

    public IGridTable getGridTable(String view) {
        if (view != null) {
            ILogicalTable gtx = tsn.getSubTables().get(view);
            if (gtx != null) {
                return gtx.getSource();
            }
        }
        return getGridTable();
    }

    public ITableProperties getProperties() {
        return tsn.getTableProperties();
    }

    public String getType() {
        return tsn.getType();
    }

    public List<OpenLMessage> getMessages() {
        SyntaxNodeException[] errors = tsn.getErrors();
        return OpenLMessagesUtils.newMessages(errors);
    }

    public String getNameFromHeader() {
        IOpenMember member = tsn.getMember();
        if (member != null) {
            return member.getName();
        }
        return StringUtils.EMPTY;
    }

    public String getName() {
        String tableName = getNameFromHeader();
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

    public boolean isExecutable() {
        return tsn.isExecutableNode();
    }

    public String getUri() {
        return tsn.getUri();
    }

    public boolean isVersionable() {
        return PropertiesChecker.isPropertySuitableForTableType("version", tsn.getType());        
    }

}
