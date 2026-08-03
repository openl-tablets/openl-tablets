package org.openl.studio.projects.service.tables;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.table.properties.def.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.studio.projects.model.tables.TableProperty;

/**
 * Reads a table's own properties for the copy dialog, in the display form the Table Details editor shows.
 * <p>
 * Values cross the wire as strings, in the display form the property's definition gives them, so the copy dialog can
 * prefill them and send them back unchanged.
 *
 * @author Vladyslav Pikus
 */
@Service
public class TablePropertiesServiceImpl implements TablePropertiesService {

    @Override
    public List<TableProperty> read(IOpenLTable table) {
        var properties = table.getProperties();
        if (properties == null) {
            return List.of();
        }
        var defined = properties.getTableProperties();
        var ordered = new ArrayList<TableProperty>(defined.size());
        var placed = new HashSet<String>();
        // Known properties first, in the order the Table Details editor lists them (the default definitions order), so
        // the copy dialog prefills deterministically rather than in HashMap iteration order.
        for (var definition : DefaultPropertyDefinitions.getDefaultDefinitions()) {
            var name = definition.getName();
            if (defined.containsKey(name) && placed.add(name)) {
                ordered.add(new TableProperty(name, format(name, defined.get(name))));
            }
        }
        // Any property the definitions do not know, appended in a stable alphabetical order.
        defined.entrySet().stream()
                .filter(entry -> !placed.contains(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> ordered.add(new TableProperty(entry.getKey(), format(entry.getKey(), entry.getValue()))));
        return ordered;
    }

    /** The property value in its display string form, using the definition's formatter when the property is known. */
    private static @Nullable String format(String name, @Nullable Object value) {
        if (value == null) {
            return null;
        }
        var definition = TablePropertyDefinitionUtils.getPropertyByName(name);
        if (definition == null) {
            return String.valueOf(value);
        }
        return FormattersManager.getFormatter(definition.getType().getInstanceClass(), definition.getFormat())
                .format(value);
    }
}
