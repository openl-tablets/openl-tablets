package org.openl.rules.lang.xls.syntax;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.ITable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenMember;

public class TableSyntaxNodeAdapter implements ITable {

    private TableSyntaxNode tsn;

    public TableSyntaxNodeAdapter(TableSyntaxNode tsn) {
        if (tsn == null) {
            throw new IllegalArgumentException("TableSyntaxNode is null");
        }
        this.tsn = tsn;
    }

    public IGridTable getGridTable() {
        return tsn.getTable().getGridTable();
    }

    public IGridTable getGridTable(String view) {
        if (view != null) {
            ILogicalTable gtx = tsn.getSubTables().get(view);
            if (gtx != null) {
                return gtx.getGridTable();
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
        ITableProperties properties = getProperties();
        if (properties != null) {
            String name = properties.getName();
            if (StringUtils.isNotBlank(name)) {
                return name;
            }
        }
        return getNameFromHeader();
    }

    public boolean isExecutable() {
        return tsn.isExecutableNode();
    }

    public String getUri() {
        return tsn.getUri();
    }

}
