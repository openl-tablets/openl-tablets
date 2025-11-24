package org.openl.studio.projects.service.tables.read;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.studio.projects.model.tables.TableView;
import org.openl.studio.projects.service.tables.OpenLTableUtils;

/**
 * Base class for table readers.
 *
 * @author Vladyslav Pikus
 */
public abstract class TableReader<T extends TableView, R extends TableView.Builder<?>> {

    private final Supplier<R> builderCreator;

    public TableReader(Supplier<R> builderCreator) {
        this.builderCreator = builderCreator;
    }

    @SuppressWarnings("unchecked")
    public T read(IOpenLTable openLTable) {
        var builder = builderCreator.get();
        initialize(builder, openLTable);
        return (T) builder.build();
    }

    protected void initialize(R builder, IOpenLTable openLTable) {
        var tsn = openLTable.getSyntaxNode();
        var type = XlsNodeTypes.getEnumByValue(openLTable.getType());
        var header = tsn.getHeader();
        builder.id(openLTable.getId())
                .kind(OpenLTableUtils.getTableTypeItems().get(type.toString()))
                .name(TableSyntaxNodeUtils.str2name(header.getSourceString(), type));
        Optional.ofNullable(openLTable.getProperties())
                .map(ITableProperties::getTableProperties)
                .map(Map::copyOf)
                .ifPresent(builder::properties);
    }

}
