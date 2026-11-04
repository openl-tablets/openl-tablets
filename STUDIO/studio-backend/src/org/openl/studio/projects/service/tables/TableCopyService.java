package org.openl.studio.projects.service.tables;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.studio.projects.model.tables.TableProperty;

/**
 * Copies a table into a sheet of the same project, preserving the source's formatting.
 *
 * <p>The copy takes the format of its destination: a sheet of a workbook, or a new file of a module of Markdown files,
 * which keeps the values and the merged cells and no formatting. A table of a Markdown module is copied the same way,
 * its values and merged cells read from its file.
 *
 * @author Vladyslav Pikus
 */
public interface TableCopyService {

    /**
     * Write a copy of {@code source} into {@code destGrid}, save it and return the copy's identifier.
     * <p>
     * The body is copied cell by cell with its styles, merges and comments, exactly as it stands in the source. The
     * header is renamed after {@code newName}. When {@code properties} is {@code null} the source's own properties are
     * kept as they are; otherwise they replace the source's — an empty list removes them. The copy is written in one
     * pass, so a copy that keeps the source's name never exists as an indistinguishable duplicate.
     *
     * <p>A copy that keeps the source's name, answers the same requests and declares a version of its own is a new
     * version of that table. Only one version is active at a time, so the source stops being the active one. The
     * copy is a new version only where the two are compiled together: in the workbook of the source, or in the module
     * of a Markdown source, whose file is saved with the change.
     *
     * <p>A new version is refused a number any version of the table already carries, so the engine is never left
     * with two versions it cannot tell apart.
     *
     * @param source       the table being copied, which stands down when the copy is a new version of it
     * @param newName      the name the copy is given
     * @param properties   the copy's properties, or {@code null} to keep the source's
     * @param destGrid     the sheet or the new Markdown file the copy is written to
     * @param moduleTables the tables compiled together with the copy, which a new version is checked against
     * @return the identifier the copy is known by once its module is compiled again
     */
    String copyInto(IOpenLTable source, String newName, @Nullable List<TableProperty> properties,
            IWritableGrid destGrid, TableSyntaxNode[] moduleTables);
}
