package org.openl.studio.projects.service.tables;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.def.DefaultPropertyDefinitions;
import org.openl.studio.projects.model.tables.TableProperty;

/**
 * Reads a table's own properties for the copy dialog.
 * <p>
 * Values cross the wire as strings, in the form {@link TablePropertyText} writes them, so the copy dialog can
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
                ordered.add(property(name, defined.get(name)));
            }
        }
        // Any property the definitions do not know, appended in a stable alphabetical order.
        defined.entrySet().stream()
                .filter(entry -> !placed.contains(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> ordered.add(property(entry.getKey(), entry.getValue())));
        return ordered;
    }

    private static TableProperty property(String name, @Nullable Object value) {
        return new TableProperty(name, TablePropertyText.format(name, value));
    }
}
