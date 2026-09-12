package org.openl.studio.projects.service.tables;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.studio.projects.model.tables.PropertyInheritance;
import org.openl.studio.projects.model.tables.TableDetailsView;
import org.openl.studio.projects.model.tables.TablePropertyDetailView;
import org.openl.studio.projects.model.tables.TablePropertyGroupView;

/**
 * Reads the properties that apply to a table, the way the Table Details panel has always read them.
 *
 * <p>A table carries some of its properties in its own header and inherits the rest from the properties table
 * of its module or category. Both kinds are answered here, in the order and the groups the property dictionary
 * declares, and each says where it came from — which is the only way a reader can tell, especially when the
 * table's header is not shown.
 *
 * <p>Values are answered as the rest of the table API writes them — a date in ISO-8601, everything else through
 * the formatter its type declares — so the reader shows them in its own format.
 *
 * @author Vladyslav Pikus
 */
@Service
public class TableDetailsServiceImpl implements TableDetailsService {

    @Override
    public TableDetailsView read(IOpenLTable table) {
        var details = TableDetailsView.builder().name(table.getDisplayName());
        var properties = table.getProperties();
        if (!table.isCanContainProperties() || properties == null) {
            return details.groups(List.of()).build();
        }
        var byGroup = new LinkedHashMap<String, List<TablePropertyDetailView>>();
        for (TablePropertyDefinition definition : TablePropertyDefinitionUtils
                .getDefaultDefinitionsForTable(table.getType())) {
            var property = read(definition, properties);
            if (property != null) {
                byGroup.computeIfAbsent(definition.getGroup(), group -> new ArrayList<>()).add(property);
            }
        }
        return details.groups(byGroup.entrySet()
                .stream()
                .map(group -> TablePropertyGroupView.builder().name(group.getKey()).properties(group.getValue()).build())
                .toList()).build();
    }

    /**
     * One property of the dictionary as it applies to this table, or nothing when it does not.
     *
     * <p>A property counts as applying only when some level actually declares it: the value alone is not enough,
     * since a property the engine defaults is not something the author wrote anywhere.
     */
    private static @Nullable TablePropertyDetailView read(TablePropertyDefinition definition,
                                                          ITableProperties properties) {
        var name = definition.getName();
        var value = properties.getPropertyValue(name);
        if (value == null || !isDeclared(name, properties)) {
            return null;
        }
        var level = properties.getPropertyLevelDefinedOn(name);
        var inheritance = PropertyInheritance.of(level);
        return TablePropertyDetailView.builder()
                .name(name)
                .displayName(definition.getDisplayName())
                .value(TablePropertyText.format(name, value))
                .inheritedFrom(inheritance)
                .inheritedTableId(inheritance == null ? null : inheritedTableId(level, properties))
                .build();
    }

    private static boolean isDeclared(String name, ITableProperties properties) {
        return properties.getTableProperties().containsKey(name)
                || properties.getCategoryProperties().containsKey(name)
                || properties.getModuleProperties().containsKey(name)
                || properties.getGlobalProperties().containsKey(name)
                || properties.getExternalProperties().containsKey(name);
    }

    /** The properties table a value is inherited from, so a reader can open it. */
    private static @Nullable String inheritedTableId(InheritanceLevel level, ITableProperties properties) {
        TableSyntaxNode node = properties.getInheritedPropertiesTableSyntaxNode(level);
        return node == null ? null : node.getId();
    }
}
