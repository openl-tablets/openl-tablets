package org.openl.rules.lang.xls.syntax;

import java.util.Collection;
import java.util.Objects;

import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.inherit.PropertiesChecker;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenMember;
import org.openl.util.StringUtils;

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
            ILogicalTable gtx = tsn.getTable(view);
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

    public Collection<OpenLMessage> getMessages() {
        SyntaxNodeException[] errors = tsn.getErrors();
        return OpenLMessagesUtils.newErrorMessages(errors);
    }

    public String getName() {
        IOpenMember member = tsn.getMember();
        if (member != null) {
            return member.getName();
        }

        return StringUtils.EMPTY;
    }

    public String getDisplayName() {
        ITableProperties properties = getProperties();
        if (properties != null) {
            String name = properties.getName();
            if (StringUtils.isNotBlank(name)) {
                String version = properties.getVersion();
                if (StringUtils.isNotBlank(version)) {
                    return String.format("%s: %s", name, version);
                }
                return name;
            }
        }
        return getName();
    }

    public boolean isExecutable() {
        return tsn.isExecutableNode();
    }

    public String getUri() {
        return tsn.getUri();
    }

    @Override
    public XlsUrlParser getUriParser() {
        return tsn.getUriParser();
    }

    public String getId() {
        return tsn.getId();
    }

    public boolean isVersionable() {
        return PropertiesChecker.isPropertySuitableForTableType("version", tsn.getType());        
    }

    public int hashCode() {
        return Objects.hashCode(getUri());
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

        TableSyntaxNodeAdapter table = (TableSyntaxNodeAdapter) obj;

        return Objects.equals(getUri(), table.getUri());
    }

    public boolean isCanContainProperties() {
        String tableType = getType();
        return tableType != null
                && !tableType.equals(XlsNodeTypes.XLS_OTHER.toString())
                && !tableType.equals(XlsNodeTypes.XLS_ENVIRONMENT.toString())
                && !tableType.equals(XlsNodeTypes.XLS_PROPERTIES.toString());
    }

    @Override
    public MetaInfoReader getMetaInfoReader() {
        return tsn.getMetaInfoReader();
    }
}
