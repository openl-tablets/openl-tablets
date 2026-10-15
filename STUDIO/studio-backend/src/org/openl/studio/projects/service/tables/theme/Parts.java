package org.openl.studio.projects.service.tables.theme;

import java.util.Optional;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * The parts of a theme a region of a table takes its look from: the name of the region.
 *
 * <p>A theme gives each part a look, such as the titles, the values or the returns, and a cell takes several of them
 * laid one over another, such as a value with the look of the IDs over it. The parts name the look without the theme,
 * so a table is laid out once and drawn in any theme ({@link #lookIn}).
 *
 * <p>Two cells naming the same parts take the same look in every theme, so they can share a region.
 *
 * @author Yury Molchan
 */
sealed interface Parts {

    /** The look every cell of a table starts from: the style of the theme. */
    Parts BASE = new Base();

    /**
     * The look the parts take in the look of a kind of table.
     *
     * @param look the look of the kind of table the region belongs to
     * @return the look of the region
     */
    ThemeStyle lookIn(TableTheme.Look look);

    /**
     * These parts with another part laid over them.
     *
     * @param part the part laid on top, or {@code null} for none
     * @return the parts together
     */
    default Parts with(@Nullable Part part) {
        return part == null ? this : new Over(this, part, false);
    }

    /**
     * These parts with another part laid over them turned, a line above being a line on the left.
     *
     * @param part the part laid on top, turned
     * @return the parts together
     */
    default Parts withTurned(Part part) {
        return new Over(this, part, true);
    }

    /**
     * These parts for a cell of a block they go round: their lines above and below where the cell reaches the edge
     * of the block, and the lines of the parts inside the block elsewhere, see {@link ThemeStyle#atEdges}.
     *
     * @param inside the parts of a cell within the block
     * @param first  whether the cell reaches the first line of the block
     * @param last   whether the cell reaches the last line of the block
     * @return the parts fitted to the cell
     */
    default Parts atEdges(Parts inside, boolean first, boolean last) {
        return new AtEdges(this, inside, first, last);
    }

    /**
     * These parts with another part laid over them that goes round a block of cells: the lines the part draws above
     * and below are kept where the cell reaches the edge of the block, and the lines of these parts elsewhere. A part
     * the theme gives no look leaves these parts as they are.
     *
     * @param part  the part going round the block
     * @param first whether the cell reaches the first line of the block
     * @param last  whether the cell reaches the last line of the block
     * @return the parts fitted to the cell
     */
    default Parts withAround(Part part, boolean first, boolean last) {
        return new Around(this, part, first, last);
    }

    /**
     * These parts turned over the diagonal from the top left corner, see {@link ThemeStyle#transposed()}.
     *
     * @return the parts turned
     */
    default Parts transposed() {
        return new Turned(this);
    }

    /**
     * These parts lined up across the cell as other parts are.
     *
     * @param as the parts whose alignment the cell takes
     * @return the parts lined up
     */
    default Parts alignedAs(Parts as) {
        return new Aligned(this, as);
    }

    /**
     * These parts keeping the line above and the line on the left only where they set the cell apart: a side they do
     * not keep is the one of other parts.
     *
     * @param base  the parts whose top and left sides the cell takes where it keeps none of its own
     * @param above whether the cell keeps the line above it
     * @param left  whether the cell keeps the line on its left
     * @return the parts fitted to the cell
     */
    default Parts apart(Parts base, boolean above, boolean left) {
        return above && left ? this : new Apart(this, base, above, left);
    }

    /** A part of the look of a kind of table, as the theme file names it. */
    @RequiredArgsConstructor
    enum Part {
        HEADER(look -> Optional.ofNullable(look.header()).map(TableTheme.Header::style).orElse(null)),
        PROPERTIES(TableTheme.Look::properties),
        TITLES(TableTheme.Look::titles),
        STEP_TITLE(TableTheme.Look::stepTitle),
        TYPE(TableTheme.Look::type),
        NAME(TableTheme.Look::name),
        STEPS(TableTheme.Look::steps),
        VALUES(TableTheme.Look::values),
        CONDITION(TableTheme.Look::condition),
        SECTIONS(TableTheme.Look::sections),
        MARKED(TableTheme.Look::marked),
        RESULT(TableTheme.Look::result),
        RESULT_ROW(TableTheme.Look::resultRow),
        IDS(TableTheme.Look::ids),
        EMPTY(TableTheme.Look::empty),
        CODE(TableTheme.Look::code),
        HORIZONTALS(TableTheme.Look::horizontals),
        RETURN_TITLES(TableTheme.Look::returnTitles),
        RETURNS(TableTheme.Look::returns),
        GROUPS(TableTheme.Look::groups),
        LAST_ROW(TableTheme.Look::lastRow);

        private final Function<TableTheme.Look, @Nullable ThemeStyle> look;

        /** The look the part takes in the look of a kind of table, or {@code null} where the theme gives it none. */
        @Nullable
        ThemeStyle in(TableTheme.Look kind) {
            return look.apply(kind);
        }
    }

    /** The look every cell starts from. */
    record Base() implements Parts {

        @Override
        public ThemeStyle lookIn(TableTheme.Look look) {
            return ThemeStyle.NONE.with(look.style());
        }
    }

    /** A part laid over other parts, turned or not. */
    record Over(Parts under, Part part, boolean turned) implements Parts {

        @Override
        public ThemeStyle lookIn(TableTheme.Look look) {
            var over = part.in(look);
            return under.lookIn(look).with(turned && over != null ? over.transposed() : over);
        }
    }

    /** Parts going round a block, fitted to one of its cells. */
    record AtEdges(Parts around, Parts inside, boolean first, boolean last) implements Parts {

        @Override
        public ThemeStyle lookIn(TableTheme.Look look) {
            return around.lookIn(look).atEdges(inside.lookIn(look), first, last);
        }
    }

    /** A part going round a block, laid over the parts of one of its cells. */
    record Around(Parts under, Part part, boolean first, boolean last) implements Parts {

        @Override
        public ThemeStyle lookIn(TableTheme.Look look) {
            var inside = under.lookIn(look);
            var over = part.in(look);
            return over == null ? inside : inside.with(over).atEdges(inside, first, last);
        }
    }

    /** Parts turned over the diagonal. */
    record Turned(Parts parts) implements Parts {

        @Override
        public ThemeStyle lookIn(TableTheme.Look look) {
            return parts.lookIn(look).transposed();
        }
    }

    /** Parts lined up as other parts are. */
    record Aligned(Parts parts, Parts as) implements Parts {

        @Override
        public ThemeStyle lookIn(TableTheme.Look look) {
            return parts.lookIn(look).withAlign(as.lookIn(look).align());
        }
    }

    /** Parts keeping their line above and on the left only where they set the cell apart. */
    record Apart(Parts parts, Parts base, boolean above, boolean left) implements Parts {

        @Override
        public ThemeStyle lookIn(TableTheme.Look look) {
            var style = parts.lookIn(look);
            var border = style.border();
            if (border == null) {
                return style;
            }
            var sides = Optional.ofNullable(base.lookIn(look).border()).orElse(ThemeBorder.KEEP);
            return style.withBorder(border.withTop(above ? border.top() : sides.top())
                    .withLeft(left ? border.left() : sides.left()));
        }
    }
}
