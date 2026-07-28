package org.openl.rules.lang.xls.syntax;

import java.util.Objects;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.inherit.PropertiesChecker;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.util.StringUtils;

public class TableSyntaxNodeAdapter implements IOpenLTable {

    private final TableSyntaxNode tsn;

    public TableSyntaxNodeAdapter(TableSyntaxNode tsn) {
        this.tsn = Objects.requireNonNull(tsn, "tsn cannot be null");
    }

    @Override
    public IGridTable getGridTable() {
        return tsn.getGridTable();
    }

    @Override
    public IGridTable getGridTable(String view) {
        if (view != null) {
            var gtx = tsn.getTable(view);
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
    public String getName() {
        var member = tsn.getMember();
        if (member != null) {
            return member.getName();
        }

        return StringUtils.EMPTY;
    }

    @Override
    public String getDisplayName() {
        var properties = getProperties();
        if (properties != null) {
            var name = properties.getName();
            if (StringUtils.isNotBlank(name)) {
                var version = properties.getVersion();
                if (StringUtils.isNotBlank(version)) {
                    return "%s: %s".formatted(name, version);
                }
                return name;
            }
        }
        return getName();
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
    public XlsUrlParser getUriParser() {
        return tsn.getUriParser();
    }

    @Override
    public String getId() {
        return tsn.getId();
    }

    @Override
    public boolean isVersionable() {
        return PropertiesChecker.isPropertySuitableForTableType("version", tsn.getType());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getUri());
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

        var table = (TableSyntaxNodeAdapter) obj;

        return Objects.equals(getUri(), table.getUri());
    }

    @Override
    public boolean isCanContainProperties() {
        var tableType = getType();
        return tableType != null && !tableType.equals(XlsNodeTypes.XLS_OTHER.toString()) && !tableType.equals(
                XlsNodeTypes.XLS_ENVIRONMENT.toString()) && !tableType.equals(XlsNodeTypes.XLS_PROPERTIES.toString());
    }

    @Override
    public MetaInfoReader getMetaInfoReader() {
        return tsn.getMetaInfoReader();
    }

    @Override
    public TableSyntaxNode getSyntaxNode() {
        return tsn;
    }
}
