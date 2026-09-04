package org.openl.studio.projects.service.tables;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.studio.projects.model.tables.TableProperty;

/**
 * Copies a table into a sheet of the same project, preserving the source's formatting.
 *
 * @author Vladyslav Pikus
 */
public interface TableCopyService {

    /**
     * Write a copy of {@code source} into {@code destGrid} and return the copy's identifier.
     * <p>
     * The body is copied cell by cell with its styles, merges and comments, exactly as it stands in the source. The
     * header is renamed after {@code newName}. When {@code properties} is {@code null} the source's own properties are
     * kept as they are; otherwise they replace the source's — an empty list removes them. The copy is written in one
     * pass, so a copy that keeps the source's name never exists as an indistinguishable duplicate.
     *
     * <p>A copy that keeps the source's name, answers the same requests and declares a version of its own is a new
     * version of that table. Only one version is active at a time, so the source stops being the active one. That
     * change is written into the source's workbook, which is the destination's own — the copy is a new version only
     * where the two are compiled together — so the caller's save persists both.
     *
     * <p>A new version is refused a number any version of the table already carries, so the engine is never left
     * with two versions it cannot tell apart.
     *
     * @param source       the table being copied, which stands down when the copy is a new version of it
     * @param newName      the name the copy is given
     * @param properties   the copy's properties, or {@code null} to keep the source's
     * @param destGrid     the sheet the copy is written to
     * @param moduleTables the tables compiled together with the copy, which a new version is checked against
     * @return the copy's identifier at its written position
     */
    String copyInto(IOpenLTable source, String newName, @Nullable List<TableProperty> properties,
            XlsSheetGridModel destGrid, TableSyntaxNode[] moduleTables);
}
