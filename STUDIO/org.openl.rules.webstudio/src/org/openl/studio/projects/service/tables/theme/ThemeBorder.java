package org.openl.studio.projects.service.tables.theme;

import lombok.With;
import org.jspecify.annotations.Nullable;

/**
 * The cell borders a theme draws. A side the theme leaves out keeps the border the cell has.
 *
 * <p>A side drawn with {@link ThemeLineStyle#NONE} takes the border of the cell away.
 *
 * @param top    the top side, or {@code null} to keep the cell border
 * @param right  the right side, or {@code null} to keep the cell border
 * @param bottom the bottom side, or {@code null} to keep the cell border
 * @param left   the left side, or {@code null} to keep the cell border
 */
@With
public record ThemeBorder(@Nullable ThemeBorderLine top,
                          @Nullable ThemeBorderLine right,
                          @Nullable ThemeBorderLine bottom,
                          @Nullable ThemeBorderLine left) {

    /** Borders that keep every side the cell has. */
    static final ThemeBorder KEEP = new ThemeBorder(null, null, null, null);

    /**
     * Lays another border over this one: a side the other names wins.
     *
     * @param over the border laid on top, or {@code null} for none
     * @return the borders both give together
     */
    ThemeBorder with(@Nullable ThemeBorder over) {
        if (over == null) {
            return this;
        }
        return new ThemeBorder(over.top != null ? over.top : top,
                over.right != null ? over.right : right,
                over.bottom != null ? over.bottom : bottom,
                over.left != null ? over.left : left);
    }

    /**
     * The border of a cell of a block this border goes round: its top and its bottom where the cell reaches the edge
     * of the block, and the sides of the border inside the block elsewhere.
     *
     * @param inside the border of a cell within the block
     * @param first  whether the cell reaches the first line of the block
     * @param last   whether the cell reaches the last line of the block
     * @return the border the cell is drawn with
     */
    ThemeBorder atEdges(ThemeBorder inside, boolean first, boolean last) {
        return withTop(first ? top : inside.top).withBottom(last ? bottom : inside.bottom);
    }
}
