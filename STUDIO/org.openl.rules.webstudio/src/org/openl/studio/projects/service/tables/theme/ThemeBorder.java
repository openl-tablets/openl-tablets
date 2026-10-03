package org.openl.studio.projects.service.tables.theme;

import org.jspecify.annotations.Nullable;

/**
 * The cell borders a theme draws. A side the theme leaves out keeps the border the cell has.
 *
 * @param top    the top side, or {@code null} to keep the cell border
 * @param right  the right side, or {@code null} to keep the cell border
 * @param bottom the bottom side, or {@code null} to keep the cell border
 * @param left   the left side, or {@code null} to keep the cell border
 */
public record ThemeBorder(@Nullable ThemeBorderLine top,
                          @Nullable ThemeBorderLine right,
                          @Nullable ThemeBorderLine bottom,
                          @Nullable ThemeBorderLine left) {

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
}
