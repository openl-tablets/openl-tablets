package org.openl.studio.projects.service.tables.read;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.studio.projects.service.tables.TableModules;
import org.openl.studio.projects.service.tables.theme.ThemedTable;

/**
 * What a raw read of a table answers: the window of its rows, and what it reports besides the values of the cells.
 *
 * <p>A read that sets nothing answers every row with the values alone, and sends a reader to no module.
 *
 * @param startRow     the zero-based index of the first row to answer, or {@code null} for the top
 * @param maxRows      the most rows to answer from {@code startRow}, or {@code null} for every remaining row
 * @param withStyles   whether to attach the Excel style of each cell (background, font, alignment) and the pieces
 *                     of its text formatted with fonts of their own
 * @param withMetaInfo whether to attach what the compiler knows about each cell — the pieces of its text that refer
 *                     to something, the type it holds, the editor it asks for
 * @param modules      the modules the table of a usage is looked up in, so a reader can be sent to it
 * @param theme        the look the table theme gives each cell, reported in place of its Excel style, which the
 *                     cells the theme does not reach are read with; or {@code null} to report no theme
 */
@Builder(toBuilder = true)
public record RawTableRead(@Nullable Integer startRow,
                           @Nullable Integer maxRows,
                           boolean withStyles,
                           boolean withMetaInfo,
                           TableModules modules,
                           @Nullable ThemedTable theme) {

    /**
     * A read that answers every row with the values alone, until it is told otherwise.
     *
     * @return the builder of the read
     */
    public static RawTableReadBuilder builder() {
        return new RawTableReadBuilder().modules(TableModules.none());
    }
}
