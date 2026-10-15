package org.openl.studio.projects.service.tables.format;

import java.util.Arrays;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.xls.PoiExcelHelper.ThemedColor;
import org.openl.studio.projects.model.tables.RawTableRole;

/**
 * The OpenL table style: the look of each role of a table, written into a workbook as an Excel built-in cell style.
 *
 * <p>Each role is a built-in style of the Cell Styles gallery of Excel, so a selected cell shows its meaning there. The
 * look holds only what the theme of the workbook offers: its fonts, its fill colours and its text colours. Changing
 * the theme of the workbook in Excel changes the look of its tables.
 *
 * <p>Every role takes the font size of the Normal style of the workbook and is centred vertically. The lines of a
 * one-row role, the signature and a section, belong to its style, and so do the lines of the result.
 *
 * @author Yury Molchan
 */
@Getter
@RequiredArgsConstructor
enum TableStyle {

    SIGNATURE(RawTableRole.SIGNATURE, "Title", 15, Look.BASE.toBuilder()
            .major(true)
            .align(HorizontalAlignment.LEFT)
            .top(Line.MAJOR)
            .bottom(Line.MAJOR)),
    PROPERTIES(RawTableRole.PROPERTIES, "Explanatory Text", 53, Look.BASE.toBuilder()
            .italic(true)
            .colour(Look.MUTED)),
    CODE(RawTableRole.CODE, "Calculation", 22, Look.BASE.toBuilder()
            .colour(Look.MUTED)),
    INPUT_TITLE(RawTableRole.INPUT_TITLE, "Heading 3", 18, Look.BASE.toBuilder()
            .major(true)
            .bold(true)
            .align(HorizontalAlignment.CENTER)
            .fill(ThemeSlot.LT2.tinted(0))),
    OUTPUT_TITLE(RawTableRole.OUTPUT_TITLE, "40% - Accent1", 31, Look.BASE.toBuilder()
            .major(true)
            .bold(true)
            .align(HorizontalAlignment.CENTER)
            .fill(ThemeSlot.ACCENT1.tinted(600))),
    KEY(RawTableRole.KEY, "Heading 4", 19, Look.BASE.toBuilder()
            .bold(true)
            .align(HorizontalAlignment.LEFT)),
    VALUE(RawTableRole.VALUE, "Input", 20, Look.BASE.toBuilder()),
    OUTPUT(RawTableRole.OUTPUT, "Output", 21, Look.BASE.toBuilder()
            .fill(ThemeSlot.ACCENT1.tinted(800))),
    SECTION(RawTableRole.SECTION, "Heading 2", 17, Look.BASE.toBuilder()
            .major(true)
            .bold(true)
            .align(HorizontalAlignment.LEFT)
            .fill(ThemeSlot.LT2.tinted(0))
            .bottom(Line.MINOR)),
    RESULT(RawTableRole.RESULT, "Total", 25, Look.BASE.toBuilder()
            .bold(true)
            .top(Line.RESULT_ABOVE)
            .bottom(Line.RESULT_BELOW));

    /** The role the style is the look of. */
    private final RawTableRole role;

    /** The name of the built-in style, as Excel names it. */
    private final String styleName;

    /** The id of the built-in style, as ECMA-376 Part 1, §18.8.7, numbers it. */
    private final int builtinId;

    /** The look of the role. */
    private final Look look;

    TableStyle(RawTableRole role, String styleName, int builtinId, Look.LookBuilder look) {
        this(role, styleName, builtinId, look.build());
    }

    /**
     * The line below a region of the role: the line of its style, or the minor line below the properties, which the
     * cell format of the last row draws.
     */
    @Nullable
    Line lineBelow() {
        return this == PROPERTIES ? Line.MINOR : look.bottom();
    }

    /** The style of a role. */
    static TableStyle of(RawTableRole role) {
        return Arrays.stream(values()).filter(style -> style.role == role).findFirst().orElseThrow();
    }

    /**
     * A colour of the theme of a workbook, by its slot.
     *
     * <p>SpreadsheetML numbers the slots with the first two pairs swapped: the first light colour is {@code 0}, the
     * first dark colour {@code 1}, the second light colour {@code 2}, the second dark colour {@code 3}, and the accents
     * follow from {@code 4}.
     */
    @RequiredArgsConstructor
    enum ThemeSlot {
        DK1(1),
        LT2(2),
        DK2(3),
        ACCENT1(4);

        private final int index;

        /**
         * The colour of the slot, made lighter or darker.
         *
         * @param tint how much lighter, above 0, or darker, below 0, in thousandths
         * @return the colour
         */
        ThemedColor tinted(int tint) {
            return new ThemedColor(index, tint);
        }
    }

    /**
     * A line of the style.
     *
     * @param style  the line
     * @param colour its colour
     */
    record Line(BorderStyle style, ThemedColor colour) {

        /** A line that sets the sections of a table apart: above and below the signature, below the body. */
        static final Line MAJOR = new Line(BorderStyle.THIN, ThemeSlot.DK2.tinted(0));

        /** A line that sets a part apart within a section: below the properties and below a section. */
        static final Line MINOR = new Line(BorderStyle.THIN, ThemeSlot.LT2.tinted(-250));

        /** The line above a group, in the colour of the minor line. */
        static final Line GROUP = new Line(BorderStyle.HAIR, MINOR.colour());

        /** The line above the result. */
        static final Line RESULT_ABOVE = new Line(BorderStyle.THIN, ThemeSlot.ACCENT1.tinted(0));

        /** The double line below the result. */
        static final Line RESULT_BELOW = new Line(BorderStyle.DOUBLE, ThemeSlot.ACCENT1.tinted(0));
    }

    /**
     * What a role looks like.
     *
     * @param major  whether the text is in the heading font of the theme, rather than its body font
     * @param bold   whether the text is bold
     * @param italic whether the text is italic
     * @param colour the colour of the text
     * @param fill   the fill, or {@code null} for none
     * @param align  how the text is aligned across the cell
     * @param top    the line above the role, or {@code null} for none
     * @param bottom the line below the role, or {@code null} for none
     */
    @Builder(toBuilder = true)
    record Look(boolean major, boolean bold, boolean italic, ThemedColor colour, @Nullable ThemedColor fill,
                HorizontalAlignment align, @Nullable Line top, @Nullable Line bottom) {

        /** The text colour of what matters most: the first dark colour of the theme. */
        static final ThemedColor TEXT = ThemeSlot.DK1.tinted(0);

        /** The text colour of what matters less: the first dark colour of the theme, lighter by half. */
        static final ThemedColor MUTED = ThemeSlot.DK1.tinted(500);

        /** The body font in the text colour, with no fill and no lines, in the General alignment. */
        static final Look BASE = new Look(false, false, false, TEXT, null, HorizontalAlignment.GENERAL, null, null);
    }
}
