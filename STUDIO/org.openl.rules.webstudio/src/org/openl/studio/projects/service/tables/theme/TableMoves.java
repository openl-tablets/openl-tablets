package org.openl.studio.projects.service.tables.theme;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * The rows and the columns that edits inserted into a table or deleted from it since the table was compiled, in the
 * order the edits made them.
 *
 * <p>A decision table is themed by where the compiler found its parts. A theme written in the same change as such
 * edits finds each part where the edits moved it.
 *
 * <p>A row or a column is counted from the top or the left of the table, header included, as the edits count them.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class TableMoves {

    /** No row or column moved: the table stands as it was compiled. */
    public static final TableMoves NONE = new TableMoves(List.of());

    /** Where a row or a column stands once an edit deleted it. */
    static final int DELETED = -1;

    private final List<Move> moves;

    /**
     * These moves, then rows inserted into the table.
     *
     * @param position the first row inserted: the row that stood there and every row under it move down
     * @param count    how many rows are inserted
     * @return the moves with the insert after them
     */
    public TableMoves insertedRows(int position, int count) {
        return then(new Move(Axis.ROWS, position, count));
    }

    /**
     * These moves, then columns inserted into the table.
     *
     * @param position the first column inserted: the column that stood there and every column after it move right
     * @param count    how many columns are inserted
     * @return the moves with the insert after them
     */
    public TableMoves insertedColumns(int position, int count) {
        return then(new Move(Axis.COLUMNS, position, count));
    }

    /**
     * These moves, then rows deleted from the table.
     *
     * @param position the first row deleted: every row under the rows deleted moves up
     * @param count    how many rows are deleted
     * @return the moves with the delete after them
     */
    public TableMoves deletedRows(int position, int count) {
        return then(new Move(Axis.ROWS, position, -count));
    }

    /**
     * These moves, then columns deleted from the table.
     *
     * @param position the first column deleted: every column after the columns deleted moves left
     * @param count    how many columns are deleted
     * @return the moves with the delete after them
     */
    public TableMoves deletedColumns(int position, int count) {
        return then(new Move(Axis.COLUMNS, position, -count));
    }

    /**
     * Where a row of the table as compiled stands now.
     *
     * @param row the row of the table as compiled
     * @return the row of the table now, or {@link #DELETED} for a row an edit deleted
     */
    int row(int row) {
        return follow(Axis.ROWS, row);
    }

    /**
     * Where a column of the table as compiled stands now.
     *
     * @param column the column of the table as compiled
     * @return the column of the table now, or {@link #DELETED} for a column an edit deleted
     */
    int column(int column) {
        return follow(Axis.COLUMNS, column);
    }

    private int follow(Axis axis, int index) {
        var at = index;
        for (var move : moves) {
            if (move.axis() == axis && at != DELETED) {
                at = move.of(at);
            }
        }
        return at;
    }

    private TableMoves then(Move move) {
        var next = new ArrayList<>(moves);
        next.add(move);
        return new TableMoves(List.copyOf(next));
    }

    /** What an edit inserts or deletes: rows or columns. */
    private enum Axis {
        ROWS,
        COLUMNS
    }

    /**
     * One edit that moved rows or columns.
     *
     * @param axis     what the edit inserted or deleted
     * @param position the first row or column the edit inserted or deleted
     * @param count    how many it inserted, or minus how many it deleted
     */
    private record Move(Axis axis, int position, int count) {

        /** Where a row or a column that stood at an index stands once the edit is made. */
        int of(int index) {
            if (index < position) {
                return index;
            }
            if (count > 0) {
                return index + count;
            }
            return index < position - count ? DELETED : index + count;
        }
    }
}
