package org.openl.studio.projects.service.tables;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.def.DefaultPropertyDefinitions;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.TableProperty;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * Reads and writes a table's own properties.
 * <p>
 * Values cross the wire as strings, in the form {@link TablePropertyText} writes them, so a dialog can prefill
 * them and send them back unchanged.
 * <p>
 * Writing touches the properties section alone: the rows the values sit on are added, changed or taken away, and
 * the body of the table is neither read nor sent. A property this kind of table does not accept is refused, and
 * one the dictionary does not know is refused as well — unless the table already carries it and the write is
 * taking it away.
 *
 * @author Vladyslav Pikus
 */
@Service
@RequiredArgsConstructor
public class TablePropertiesServiceImpl implements TablePropertiesService {

    private final SystemPropertiesService systemPropertiesService;

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

    @Override
    public String write(IOpenLTable table, List<TableProperty> properties) {
        if (!TablePropertyRules.canEditProperties(table)) {
            throw new BadRequestException("table.properties.unsupported.message");
        }
        var declared = table.getProperties() == null ? Set.<String>of() : table.getProperties().getTableProperties()
                .keySet();
        // What this kind of table takes is read once, not once per property.
        var writable = TablePropertyRules.writable(table.getType());
        properties.forEach(property -> requireWritable(declared, writable, property));
        var gridTable = table.getGridTable();
        gridTable.edit();
        try {
            var editor = new TableEditorModel(table, IXlsTableNames.VIEW_DEVELOPER, false);
            for (var property : properties) {
                editor.setProperty(property.name(), value(property));
            }
            // Who edited the table and when is recorded by the installation, not by the author, and this is an
            // edit like any other.
            systemPropertiesService.onEdit().forEach(editor::setProperty);
            return editor.save();
        } catch (IOException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        } finally {
            gridTable.stopEditing();
        }
    }

    /** The value to write, or {@code null} for a property the write takes away. */
    private static @Nullable Object value(TableProperty property) {
        var text = property.value();
        return StringUtils.isBlank(text) ? null : TablePropertyText.parse(property.name(), text);
    }

    /**
     * Refuses a property this table cannot be given.
     *
     * <p>What a table accepts is decided by its kind, and a property recorded by OpenL Studio is not written by
     * an author. A property the table already carries is always allowed, so a value that no longer belongs there
     * can still be taken away.
     */
    private static void requireWritable(Set<String> declared, Set<String> writable, TableProperty property) {
        var name = property.name();
        if (declared.contains(name) || writable.contains(name)) {
            return;
        }
        throw new BadRequestException("table.properties.unsupported-property.message", new Object[]{name});
    }

    private static TableProperty property(String name, @Nullable Object value) {
        return new TableProperty(name, TablePropertyText.format(name, value));
    }
}
