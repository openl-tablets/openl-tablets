package org.openl.studio.projects.service.tables;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.PropertiesChecker;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.table.xls.builder.TableBuilder;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.TableProperty;
import org.openl.util.BooleanUtils;

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

    private static final String VERSION_PROPERTY = "version";
    private static final String ACTIVE_PROPERTY = "active";
    /**
     * The version a table stood for while it declared none.
     *
     * <p>The wizard this dialog replaced stamped that version on a table it versioned for the first time, and
     * offered the next one to the copy, so a first copy of an unversioned table is still offered {@code 0.0.2}.
     */
    private static final String INITIAL_VERSION = "0.0.1";
    /** Major, minor and variant: the form the engine orders versions by. */
    private static final Pattern VERSION_FORMAT = Pattern.compile("\\d+\\.\\d+\\.\\d+");

    @Override
    public String copyInto(IOpenLTable source, String newName, @Nullable List<TableProperty> properties,
            XlsSheetGridModel destGrid) {
        var declared = properties == null ? null : toPropertyMap(properties);
        if (declared != null) {
            requireReadableVersion(source, declared.get(VERSION_PROPERTY));
        }
        // Decided before anything is written, so a request that is refused leaves the workbook as it was.
        var replacesTheSource = supersedes(source, newName, declared, destGrid);
        var original = source.getGridTable();
        // Read the source in edit mode (its writable form, so formulas read back as their source) and always leave
        // it, pairing edit()/stopEditing() the way the table writers do.
        original.edit();
        try {
            var copyId = build(source, original, newName, declared, destGrid);
            if (replacesTheSource) {
                standDown(source);
            }
            return copyId;
        } finally {
            original.stopEditing();
        }
    }

    /**
     * Whether the copy takes the source's place as the active version.
     *
     * <p>A copy that keeps the source's name, answers the same requests and declares a version of its own is another
     * version of the same table, and only one version of a table is active at a time. The copy is the active one
     * unless it says otherwise, and its version must differ from the one it replaces.
     *
     * <p>Both must be compiled together, which is taken here as the same workbook. A module may hold more than one
     * workbook, so a copy into an included workbook of the same module is left alone rather than answered wrongly.
     */
    private static boolean supersedes(IOpenLTable source, String newName, @Nullable Map<String, Object> declared,
            XlsSheetGridModel destGrid) {
        if (!newName.equals(source.getName())) {
            return false;
        }
        if (declared == null) {
            // Keeping the name and the source's properties writes the same table a second time, and the two are
            // then indistinguishable: such a copy has to say what makes it another table.
            throw new BadRequestException("table.copy.properties.required.message");
        }
        var version = declared.get(VERSION_PROPERTY);
        if (version == null || declaredInactive(declared) || !versionable(source)
                || !sameVersionGroup(source, declared) || !sameWorkbook(source, destGrid)) {
            return false;
        }
        requireActiveSource(source);
        requireVersionOfItsOwn(source, version);
        return true;
    }

    /**
     * Rejects a new version made from a version that is not the active one.
     *
     * <p>The copy becomes the active version, so the one it replaces has to step aside — and which table that is
     * cannot be told from an inactive source: the table that answers today is another one.
     */
    private static void requireActiveSource(IOpenLTable source) {
        var properties = source.getProperties();
        if (properties != null && Boolean.FALSE.equals(properties.getActive())) {
            throw new BadRequestException("table.copy.inactive-source.message");
        }
    }

    /** Whether the copy asks to be written inactive: a copy is the active version unless it says otherwise. */
    private static boolean declaredInactive(Map<String, Object> declared) {
        var active = declared.get(ACTIVE_PROPERTY);
        // Read the way the engine reads the cell, which takes 'no', 'off' and 'f' for false as well.
        return active != null && Boolean.FALSE.equals(BooleanUtils.toBooleanObject(active.toString().trim()));
    }

    /** Whether the table is one that carries versions at all: a data or datatype table is not. */
    private static boolean versionable(IOpenLTable table) {
        var type = table.getSyntaxNode().getType();
        return PropertiesChecker.isPropertySuitableForTableType(VERSION_PROPERTY, type)
                && PropertiesChecker.isPropertySuitableForTableType(ACTIVE_PROPERTY, type);
    }

    /**
     * Whether the copy answers the same requests as the source.
     *
     * <p>Dimension properties are what the engine dispatches on, so a copy that declares another set of them stands
     * beside the source rather than replacing it.
     *
     * <p>Only what the tables declare themselves is compared: what a table inherits from its module or category, the
     * copy inherits alike. The values are compared as the values they stand for, so the same date or the same list
     * written differently still reads as the same request.
     */
    private static boolean sameVersionGroup(IOpenLTable source, Map<String, Object> declared) {
        var properties = source.getProperties();
        var own = properties == null ? Map.<String, Object>of() : properties.getTableProperties();
        for (var name : TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames()) {
            if (!Objects.deepEquals(own.get(name), valueOf(name, declared.get(name)))) {
                return false;
            }
        }
        return true;
    }

    /**
     * The value a property text stands for, read as the table editor reads it when it writes the property.
     *
     * <p>A text the property cannot be read from is answered with itself, so it compares equal to nothing typed and
     * the copy is left to stand beside the source rather than replacing it.
     */
    private static @Nullable Object valueOf(String name, @Nullable Object text) {
        if (text == null) {
            return null;
        }
        var definition = TablePropertyDefinitionUtils.getPropertyByName(name);
        var type = definition == null ? null : definition.getType();
        if (type == null) {
            return text;
        }
        try {
            return FormattersManager.getFormatter(type.getInstanceClass(), definition.getFormat())
                    .parse(text.toString());
        } catch (RuntimeException e) {
            log.debug("Cannot read property '{}' from '{}'.", name, text, e);
            return text;
        }
    }

    /** The version the table stands for: the one it declares, or the initial one while it declares none. */
    private static String currentVersion(IOpenLTable table) {
        var properties = table.getProperties();
        var version = properties == null ? null : properties.getVersion();
        return version == null ? INITIAL_VERSION : version;
    }

    private static boolean sameWorkbook(IOpenLTable source, XlsSheetGridModel destGrid) {
        var sourceWorkbook = source.getSyntaxNode().getXlsSheetSourceCodeModule().getWorkbookSource();
        return Objects.equals(sourceWorkbook.getUri(), destGrid.getSheetSource().getWorkbookSource().getUri());
    }

    /** Rejects a version that repeats the one it replaces, which leaves two tables the engine cannot tell apart. */
    private static void requireVersionOfItsOwn(IOpenLTable source, Object version) {
        if (version.equals(currentVersion(source))) {
            throw new BadRequestException("table.copy.version.repeated.message", new Object[]{version});
        }
    }

    /**
     * Lets the superseded version step aside: it stops being the active one and keeps the version it stood for.
     *
     * <p>A table that declared no version is written the one it stood for, so the two versions stay
     * distinguishable. The change is left in the workbook the caller saves.
     */
    private static void standDown(IOpenLTable source) {
        var editor = new TableEditorModel(source, IXlsTableNames.VIEW_DEVELOPER, false);
        editor.setProperty(VERSION_PROPERTY, currentVersion(source));
        editor.setProperty(ACTIVE_PROPERTY, Boolean.FALSE.toString());
    }

    private static String build(IOpenLTable source, IGridTable original, String newName,
            @Nullable Map<String, Object> declared, XlsSheetGridModel destGrid) {
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
            if (declared != null) {
                newProperties = declared;
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

    /**
     * The requested properties as an ordered name-to-value map, dropping the blank ones that remove a property.
     *
     * <p>Names and values are taken without the space around them, so a value is written as it was checked.
     */
    private static Map<String, Object> toPropertyMap(List<TableProperty> properties) {
        var map = new LinkedHashMap<String, Object>();
        for (var property : properties) {
            var value = property.value();
            if (value != null && !value.isBlank()) {
                map.put(property.name().trim(), value.strip());
            }
        }
        return map;
    }

    /**
     * Rejects a version the engine cannot read.
     *
     * <p>Versions are ordered by their major, minor and variant numbers. A value of any other shape is read as the
     * same version as every other unreadable one, which makes two versions of a table indistinguishable and stops
     * the module from compiling.
     *
     * <p>The version the source already carries is let through as it stands. It was written when a shorter form was
     * documented as valid, and refusing it would leave such a table impossible to copy at all.
     */
    private static void requireReadableVersion(IOpenLTable source, @Nullable Object version) {
        if (version == null || VERSION_FORMAT.matcher(version.toString()).matches()) {
            return;
        }
        var properties = source.getProperties();
        if (properties != null && version.equals(properties.getVersion())) {
            return;
        }
        throw new BadRequestException("table.copy.version.invalid.message", new Object[]{version});
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
