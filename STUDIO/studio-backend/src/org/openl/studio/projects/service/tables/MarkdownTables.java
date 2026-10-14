package org.openl.studio.projects.service.tables;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.StringTool;

/**
 * Keeps the tables of a Markdown module for reading only.
 *
 * <p>A Markdown module is plain text: its tables are changed by editing the file as text. Writing, theming and
 * changing the properties of such a table from the table editor are refused, saying so. A copy is refused too: it
 * reads the formatting of its source from a workbook, which the file is not.
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
     * Refuses to write a table of a Markdown module.
     *
     * @param table the table the write is asked for
     * @throws BadRequestException when the table comes from a Markdown module
     */
    public static void refuseWrite(IOpenLTable table) {
        refuse(table, "table.markdown.message");
    }

    /**
     * Refuses to copy a table of a Markdown module.
     *
     * @param table the table to copy
     * @throws BadRequestException when the table comes from a Markdown module
     */
    public static void refuseCopy(IOpenLTable table) {
        refuse(table, "table.markdown.copy.message");
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
