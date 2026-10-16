package org.openl.studio.projects.service.tables.format;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.openl.rules.table.IGridTable;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.studio.projects.model.tables.RawTableGroups;
import org.openl.studio.projects.model.tables.RawTableRegion;
import org.openl.studio.projects.model.tables.RawTableSignaturePiece;

/**
 * The tables a save left formatted with the OpenL table style, so a later save of such a table formats only the lines
 * its edit touched.
 *
 * <p>Formatting a table reads every one of its cells, which costs a large table a noticeable share of its save. A table
 * is known formatted only while its workbook file is the one that save wrote: any other write of the file, by OpenL
 * Studio or anything else, changes the file and so the table is formatted in full at its next save.
 *
 * <p>Only the most recent tables are remembered; a table forgotten is formatted in full at its next save.
 *
 * @author Yury Molchan
 */
public final class FormattedTables {

    /** How many tables are remembered. */
    private static final int REMEMBERED = 64;

    private static final Map<Position, Mark> MARKS = Collections.synchronizedMap(
            new LinkedHashMap<>(REMEMBERED, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Position, Mark> eldest) {
                    return size() > REMEMBERED;
                }
            });

    private FormattedTables() {
    }

    /**
     * Where a table stands: its workbook file, its sheet and its top left cell. Read while the table is open for
     * writing, as a save closes it.
     *
     * @param grid the table on its sheet, open for writing
     * @return where the table stands, or {@code null} for a table read from no workbook file
     */
    public static @Nullable Position positionOf(IGridTable grid) {
        if (!(grid.getGrid() instanceof XlsSheetGridModel sheet)) {
            return null;
        }
        var file = sheet.getSheetSource().getWorkbookSource().getSourceFile();
        if (file == null) {
            return null;
        }
        var region = grid.getRegion();
        return new Position(file.getAbsoluteFile(), sheet.getSheetSource().getSheetName(), region.getTop(),
                region.getLeft());
    }

    /**
     * Whether a table is formatted in the given regions, as its workbook file stands. The types of the values do not
     * count, as the look of a cell does not follow them.
     *
     * @param position where the table stands, or {@code null} for a table read from no workbook file
     * @param regions  the regions the table is laid out in
     * @return whether a save left the table formatted in these regions and nothing has written its workbook since
     */
    public static boolean isFormatted(@Nullable Position position, TableRegions regions) {
        var mark = position == null ? null : MARKS.get(position);
        return mark != null && mark.look().equals(Look.of(regions))
                && mark.file().equals(FileState.of(position.file()));
    }

    /**
     * Remembers that a table is formatted in the given regions, as its workbook file was just saved.
     *
     * @param position where the table stands, or {@code null} for a table read from no workbook file
     * @param regions  the regions the table is formatted in
     */
    public static void formatted(@Nullable Position position, TableRegions regions) {
        if (position != null) {
            MARKS.put(position, new Mark(Look.of(regions), FileState.of(position.file())));
        }
    }

    /**
     * Where a table stands.
     *
     * @param file  the workbook file, as an absolute path
     * @param sheet the name of the sheet
     * @param top   the row of the sheet the table starts in
     * @param left  the column of the sheet the table starts in
     */
    public record Position(File file, String sheet, int top, int left) {
    }

    /** What a save left: the look of the table, and its workbook file as written. */
    private record Mark(Look look, FileState file) {
    }

    /** What the look of a table follows: its regions, its groups and the pieces of its header. */
    private record Look(List<RawTableRegion> regions, RawTableGroups groups, List<RawTableSignaturePiece> signature) {

        static Look of(TableRegions regions) {
            return new Look(regions.regions(), regions.groups(), regions.signature());
        }
    }

    /** A file as it stands: when it was written and how long it is. */
    private record FileState(long modified, long length) {

        static FileState of(File file) {
            return new FileState(file.lastModified(), file.length());
        }
    }
}
