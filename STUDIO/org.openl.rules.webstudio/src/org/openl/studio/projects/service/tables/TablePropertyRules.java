package org.openl.studio.projects.service.tables;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;

/**
 * Which properties a table may be given, and whether it may be given any at all.
 *
 * <p>Read once and answered to both sides: the panel offers what is written here, and a write refuses what is
 * not. Asked in two places, the two would drift — and a property offered but refused, or refused but offered,
 * is a rule the reader cannot see.
 *
 * @author Vladyslav Pikus
 */
final class TablePropertyRules {

    /** Given to a table by copying it, never by writing it. */
    static final String VERSION_PROPERTY = TableVersionService.VERSION_PROPERTY;

    private TablePropertyRules() {
    }

    /**
     * Whether properties may be written on this table.
     *
     * <p>A table generated to dispatch between the versions of another is written by nobody: it is built again
     * from those versions whenever the module is compiled. Neither is a table assembled from parts written on
     * several sheets — it stands on no sheet of its own to be written to. And some kinds carry no properties at
     * all: the environment, a properties table, whatever OpenL could not name.
     */
    static boolean canEditProperties(IOpenLTable table) {
        return table.isCanContainProperties()
                && !table.getName().startsWith(DispatcherTablesBuilder.DEFAULT_DISPATCHER_TABLE_NAME)
                && !isAssembledFromParts(table);
    }

    /** Whether the table is assembled from parts written apart from one another. */
    static boolean isAssembledFromParts(IOpenLTable table) {
        var grid = table.getGridTable();
        return grid != null && grid.getGrid() instanceof CompositeGrid;
    }

    /**
     * The properties this kind of table may be given on a table of its own.
     *
     * <p>A property recorded by OpenL Studio rather than typed, one the dictionary has retired, and the version
     * — which a table is given by being copied — are not among them.
     */
    static Set<String> writable(String tableType) {
        return Arrays.stream(TablePropertyDefinitionUtils
                        .getDefaultDefinitionsForTable(tableType, InheritanceLevel.TABLE, true))
                .filter(definition -> definition.getDeprecation() == null)
                .map(TablePropertyDefinition::getName)
                .filter(name -> !VERSION_PROPERTY.equals(name))
                .collect(Collectors.toSet());
    }

    /** The same, less the properties the table already shows: those are changed where they stand. */
    static List<String> available(IOpenLTable table, ITableProperties properties) {
        return Arrays.stream(TablePropertyDefinitionUtils
                        .getDefaultDefinitionsForTable(table.getType(), InheritanceLevel.TABLE, true))
                .filter(definition -> definition.getDeprecation() == null)
                .map(TablePropertyDefinition::getName)
                .filter(name -> !VERSION_PROPERTY.equals(name))
                .filter(name -> !isDeclared(name, properties))
                .toList();
    }

    /** Whether some level declares the property, which is what makes it apply to the table. */
    static boolean isDeclared(String name, ITableProperties properties) {
        return properties.getTableProperties().containsKey(name)
                || properties.getCategoryProperties().containsKey(name)
                || properties.getModuleProperties().containsKey(name)
                || properties.getGlobalProperties().containsKey(name)
                || properties.getExternalProperties().containsKey(name);
    }
}
