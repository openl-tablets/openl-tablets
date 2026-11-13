package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * A rectangle of the cells of a table that take one role.
 *
 * <p>A region is named in table coordinates: the header row is row 0 and the first column of the table is column 0.
 * The regions of a table do not overlap and cover it, so a window of its rows needs no regions of its own.
 *
 * @param role     what the cells mean
 * @param type     the type of the values of the cells, when they all have one; a cell of another type names its own
 * @param top      the first row, in table coordinates
 * @param left     the first column, in table coordinates
 * @param bottom   the last row, in table coordinates
 * @param right    the last column, in table coordinates
 * @param returned whether the table returns the cells among others, such as the cells a {@code SpreadsheetResult}
 *                 marks: they keep their role and are bold
 * @param unframed whether the cells stand among the values of the body, such as the types of a Datatype: they keep
 *                 their role but draw none of its lines
 * @author Yury Molchan
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A rectangle of the cells of a table that take one role")
public record RawTableRegion(
        @Parameter(description = "What the cells mean")
        RawTableRole role,

        @Parameter(description = """
                The type of the values of the cells, when they all have one. A cell of another type names its own \
                type. Absent when the cells hold no values of a type, such as titles.""")
        @Nullable RawTableValueType type,

        @Parameter(description = "The first row, in table coordinates: the header row is row 0")
        int top,

        @Parameter(description = "The first column, in table coordinates: the first column of the table is column 0")
        int left,

        @Parameter(description = "The last row, in table coordinates")
        int bottom,

        @Parameter(description = "The last column, in table coordinates")
        int right,

        @Parameter(description = """
                Whether the table returns the cells among others, such as the cells of the steps and the columns a \
                SpreadsheetResult marks with *. They keep their role and are bold. Absent when they are not.""")
        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        boolean returned,

        @Parameter(description = """
                Whether the cells stand among the values of the body, such as the types of a Datatype. They keep their \
                role but draw none of its lines. Absent when they draw them.""")
        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        boolean unframed
) {

    /** A region of cells the table does not return among others, drawing the lines of their role. */
    public RawTableRegion(RawTableRole role, @Nullable RawTableValueType type, int top, int left, int bottom,
                          int right) {
        this(role, type, top, left, bottom, right, false, false);
    }

    /** The same cells moved to other bounds, in table coordinates: their role, type and marks stay. */
    public RawTableRegion withBounds(int top, int left, int bottom, int right) {
        return new RawTableRegion(role, type, top, left, bottom, right, returned, unframed);
    }

    /**
     * Other cells of the same role and type, in table coordinates, which the table does not return among others. They
     * draw the lines of their role as these cells do.
     */
    public RawTableRegion unreturnedAt(int top, int left, int bottom, int right) {
        return new RawTableRegion(role, type, top, left, bottom, right, false, unframed);
    }

    /** Whether the region holds a cell, named in table coordinates. */
    public boolean holds(int row, int column) {
        return top <= row && row <= bottom && left <= column && column <= right;
    }
}
