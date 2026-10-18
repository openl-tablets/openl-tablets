package org.openl.studio.projects.service.tables;

import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.RawTableSourceAction;
import org.openl.studio.projects.model.tables.StyleTarget;
import org.openl.studio.projects.model.tables.UpdateTarget;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.StringTool;

/**
 * What the table editor may not do to a table of a Markdown module.
 *
 * <p>A Markdown file keeps the values of its table as text and keeps no formatting: OpenL Studio draws its table in
 * the OpenL table style. Its cells, rows, columns, merged cells, properties and indents are written into the file, but
 * any other style is refused, saying so, and so are marked pieces of the text of a cell. The indent is written as
 * non-breaking spaces before the text of a cell: it carries the structure of a TBasic and a ColumnMatch table.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MarkdownTables {

    /**
     * Tells whether a table comes from a Markdown module.
     */
    public static boolean isMarkdown(IOpenLTable table) {
        return FileTypeHelper.isMarkdownFile(fileOf(table));
    }

    /**
     * Refuses to style a table of a Markdown module with anything but the indent, or to mark pieces of the text of
     * its cells.
     *
     * @param table   the table the edits are asked for
     * @param actions the edits
     * @throws BadRequestException when an edit sets a style other than the indent, or marks a cell text, on a table of
     *                             a Markdown module
     */
    public static void refuseStyle(IOpenLTable table, List<RawTableSourceAction> actions) {
        if (actions.stream().anyMatch(MarkdownTables::formatsMoreThanTheIndent)) {
            refuse(table, "table.markdown.style.message");
        }
    }

    private static boolean formatsMoreThanTheIndent(RawTableSourceAction action) {
        return switch (action) {
            case RawTableSourceAction.Style(StyleTarget.Cells cells) -> !cells.style().isIndentOnly();
            case RawTableSourceAction.Update(UpdateTarget.Cell cell) -> cell.marks() != null;
            default -> false;
        };
    }

    private static void refuse(IOpenLTable table, String message) {
        var file = fileOf(table);
        if (FileTypeHelper.isMarkdownFile(file)) {
            throw new BadRequestException(message, new Object[]{FileUtils.getName(file)});
        }
    }

    /**
     * The file a table is written in: its address without the sheet and the cells.
     */
    private static @Nullable String fileOf(IOpenLTable table) {
        var uri = table.getUri();
        if (uri == null) {
            return null;
        }
        var query = uri.indexOf('?');
        return StringTool.decodeURL(query < 0 ? uri : uri.substring(0, query));
    }
}
