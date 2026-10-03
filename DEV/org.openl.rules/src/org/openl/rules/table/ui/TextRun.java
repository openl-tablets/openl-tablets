package org.openl.rules.table.ui;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A piece of a cell's text that is formatted with its own font.
 *
 * <p>A cell whose text is formatted in pieces, such as a header with a grey keyword and a bold name, is read as
 * a list of runs. The texts of the runs, put together, give the text of the cell.
 *
 * @param text the text of the run
 * @param font the font of the run, or {@code null} when the run takes the font of the cell
 */
public record TextRun(@NonNull String text, @Nullable ICellFont font) {
}
