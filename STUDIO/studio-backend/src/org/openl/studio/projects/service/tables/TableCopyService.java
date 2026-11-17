package org.openl.studio.projects.service.tables;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.CopyTableRequest;
import org.openl.studio.projects.model.tables.TableKind;
import org.openl.studio.projects.model.tables.TableProperty;

/**
 * Copies a table into a sheet of the same project, preserving the source's formatting.
 *
 * <p>The copy takes the format of its destination: a sheet of a workbook, or a new file of a module of Markdown files,
 * which keeps the values and the merged cells and no formatting. A table of a Markdown module is copied the same way,
 * its values and merged cells read from its file. A Data, Test or Run table written into a Markdown file takes a row
 * for each record, each element of a collection in columns of its own, as a file merges none of its cells.
 *
 * @author Vladyslav Pikus
 */
public interface TableCopyService {

    /**
     * Write a copy of {@code source} into {@code destGrid}, save it and return the copy's identifier.
     * <p>
     * The body is copied cell by cell with its styles, merges and comments, exactly as it stands in the source. The
     * header is renamed after {@code newName}; a copy of a kind whose name is optional is left unnamed without one, its
     * header the keyword and, for a Test or a Run table, the table it calls. When {@code properties} is {@code null} the source's own properties are
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
     * <p>The source is one a copy can be made of: the caller checks it with
     * {@link #requireCopyable(IOpenLTable, CopyTableRequest)} before writing anything for the copy.
     *
     * @param source       the table being copied, which stands down when the copy is a new version of it
     * @param newName      the name the copy is given, or {@code null} to leave it unnamed
     * @param properties   the copy's properties, or {@code null} to keep the source's
     * @param destGrid     the sheet or the new Markdown file the copy is written to
     * @param moduleTables the tables compiled together with the copy, which a new version is checked against
     * @param compiled     the compiled module the copy is written to, which the copy is bound alone against to be
     *                     formatted in the roles it takes there, or {@code null} to format it in the roles of its
     *                     source
     * @return the identifier the copy is known by once its module is compiled again
     */
    String copyInto(IOpenLTable source, @Nullable String newName, @Nullable List<TableProperty> properties,
            IWritableGrid destGrid, TableSyntaxNode[] moduleTables, @Nullable XlsModuleOpenClass compiled);

    /**
     * Refuses a copy that cannot be made of the table as the request asks.
     *
     * <p>A Datatype and a table of the type Other are never copied: they carry no properties to tell a copy from the
     * table it was made of. The table toolbar of OpenL Studio offers no copy of them either.
     *
     * <p>The name of a Test, a Run, a Properties and an Environment table is optional, as OpenL names each of them on
     * its own, so their copy may be left without one. Every other copy needs a name.
     *
     * <p>A Properties and an Environment table are copied as they stand: the rows of a Properties table are its
     * properties, and an Environment table has none, so the request cannot give the copy properties.
     *
     * @param source  the table asked to be copied
     * @param request the copy asked for
     * @throws BadRequestException for a Datatype or a table of the type Other, for a copy without a name that needs
     *                             one, and for properties given to a copy of a Properties or an Environment table
     */
    static void requireCopyable(IOpenLTable source, CopyTableRequest request) {
        var kind = OpenLTableUtils.kindOf(source);
        switch (kind) {
            case DATATYPE, OTHER -> throw new BadRequestException("table.copy.kind.not-copyable.message",
                    new Object[]{kind.value()});
            case PROPERTIES, ENVIRONMENT -> requireNoProperties(kind, request);
            case TEST, RUN -> {
                // OpenL names a Test or a Run table on its own, so its copy may be left without a name.
            }
            default -> requireName(request);
        }
    }

    private static void requireNoProperties(TableKind kind, CopyTableRequest request) {
        if (request.properties() != null) {
            throw new BadRequestException("table.copy.properties.not-applicable.message", new Object[]{kind.value()});
        }
    }

    private static void requireName(CopyTableRequest request) {
        if (request.name() == null) {
            throw new BadRequestException("table.name.required.message");
        }
    }
}
