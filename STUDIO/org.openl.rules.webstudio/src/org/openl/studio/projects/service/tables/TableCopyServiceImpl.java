package org.openl.studio.projects.service.tables;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.table.xls.builder.TableBuilder;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.TableProperty;

/**
 * Copies a table by rebuilding it on the destination sheet with {@link TableBuilder}, the way the table editor writes a
 * new table.
 * <p>
 * The body is copied as a live grid, so it keeps its styles, merged cells and comments. The header is written with the
 * source header's style and renamed after the copy; the properties are written with the source properties' style. The
 * copy is laid out in one pass — header, properties and body together — so a copy that keeps the source's name never
 * exists as an indistinguishable duplicate. This is what lets a copy keep the source's name to become a new version
 * dispatched by a different runtime context.
 *
 * @author Vladyslav Pikus
 */
@Slf4j
@Service
public class TableCopyServiceImpl implements TableCopyService {

    @Override
    public String copyInto(IOpenLTable source, String newName, @Nullable List<TableProperty> properties,
            XlsSheetGridModel destGrid) {
        var original = source.getGridTable();
        // Read the source in edit mode (its writable form, so formulas read back as their source) and always leave
        // it, pairing edit()/stopEditing() the way the table writers do.
        original.edit();
        try {
            return build(source, original, newName, properties, destGrid);
        } finally {
            original.stopEditing();
        }
    }

    private static String build(IOpenLTable source, IGridTable original, String newName,
            @Nullable List<TableProperty> properties, XlsSheetGridModel destGrid) {
        var node = source.getSyntaxNode();
        var type = node.getNodeType();

        var builder = new TableBuilder(destGrid);

        String header = null;
        ICellStyle headerStyle = null;
        Map<String, Object> newProperties = null;
        ICellStyle propertiesStyle = null;
        int bodyStartRow = 0;

        if (type != XlsNodeTypes.XLS_ENVIRONMENT) {
            header = rename(node.getHeaderLineValue().getValue(), source.getName(), newName,
                    type == XlsNodeTypes.XLS_OTHER);
            headerStyle = original.getCell(0, 0).getStyle();
            bodyStartRow = TableBuilder.HEADER_HEIGHT;
            // Replace the source's properties when the request carries them; keep them (copied with the body) otherwise.
            if (properties != null) {
                newProperties = toPropertyMap(properties);
                if (node.hasPropertiesDefinedInTable()) {
                    var propertiesSection = node.getTableProperties().getPropertiesSection();
                    propertiesStyle = propertiesSection.getSource().getCell(0, 0).getStyle();
                    bodyStartRow += propertiesSection.getHeight();
                } else {
                    propertiesStyle = headerStyle;
                }
            }
        }

        var body = original.getSubtable(0, bodyStartRow, original.getWidth(), original.getHeight() - bodyStartRow);
        int newPropertyRows = newProperties == null ? 0 : newProperties.size();
        int width = tableWidth(original.getWidth(), newPropertyRows > 0);
        int height = (header != null ? TableBuilder.HEADER_HEIGHT : 0) + newPropertyRows + body.getHeight();

        try {
            builder.beginTable(width, height);
            if (header != null) {
                builder.writeHeader(header, headerStyle);
            }
            if (newPropertyRows > 0) {
                builder.writeProperties(newProperties, propertiesStyle);
            }
            builder.writeGridTable(body);
            var uri = destGrid.getRangeUri(builder.getTableRegion());
            builder.endTable();
            return TableUtils.makeTableId(uri);
        } catch (CreateTableException e) {
            log.warn("Cannot lay out the copy '{}'.", newName, e);
            throw new BadRequestException("table.copy.failed.message");
        }
    }

    /** The requested properties as an ordered name-to-value map, dropping the blank ones that remove a property. */
    private static Map<String, Object> toPropertyMap(List<TableProperty> properties) {
        var map = new LinkedHashMap<String, Object>();
        for (var property : properties) {
            var value = property.value();
            if (value != null && !value.isBlank()) {
                map.put(property.name().trim(), value);
            }
        }
        return map;
    }

    /** Grow the width to fit a properties section (marker, name and value columns) when one is written. */
    private static int tableWidth(int sourceWidth, boolean hasProperties) {
        return hasProperties ? Math.max(sourceWidth, TableBuilder.PROPERTIES_MIN_WIDTH) : sourceWidth;
    }

    /**
     * The header of the copy, named after it.
     * <p>
     * A free-form table's header is its name, so it is replaced wholesale. Every other header opens with an OpenL
     * keyword and carries the name after it; that occurrence of the name is swapped, leaving the keyword in place.
     */
    private static String rename(String header, String sourceName, String copyName, boolean freeForm) {
        if (freeForm) {
            return copyName;
        }
        // The name, bounded by a start or a space before it and a space, an opening parenthesis or the end after it.
        var matcher = Pattern.compile("(^|\\s)(" + Pattern.quote(sourceName) + ")(?=\\s|\\(|$)").matcher(header);
        int start = -1;
        while (matcher.find()) {
            // Keep the last occurrence: a name repeated in the signature must not be mistaken for the declared name.
            start = matcher.start(2);
        }
        if (start < 0) {
            throw new BadRequestException("table.copy.name-not-found.message");
        }
        return header.substring(0, start) + copyName + header.substring(start + sourceName.length());
    }
}
