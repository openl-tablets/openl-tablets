package org.openl.rules.table.xls;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import lombok.Builder;
import lombok.With;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFOptimiser;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.model.ThemesTable;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.TextRun;

public final class PoiExcelHelper {

    private PoiExcelHelper() {
    }

    /**
     * For more information, see {@link HSSFWorkbook#MAX_STYLES}
     */
    private static final short MAX_STYLES = 4030;

    /**
     * The alpha of a colour that hides nothing, which is the only one a cell style uses.
     */
    private static final byte OPAQUE = (byte) 0xFF;

    /** The font an {@code .xls} workbook has no font at: its fonts are numbered past it. */
    private static final int HSSF_MISSING_FONT = 4;

    /** A character an {@code .xlsx} text escapes because XML cannot hold it, such as {@code _x000D_}. */
    private static final Pattern ESCAPED_CHARACTER = Pattern.compile("_x[0-9A-Fa-f]{4}_");

    /** How many characters longer an escaped character is written than shown: seven for one. */
    private static final int ESCAPE_EXTRA = 6;

    public static Cell getCell(int colIndex, int rowIndex, Sheet sheet) {
        var row = sheet.getRow(rowIndex);
        if (row != null) {
            return row.getCell(colIndex, Row.MissingCellPolicy.RETURN_NULL_AND_BLANK);
        }
        return null;
    }

    public static Cell getOrCreateCell(int colIndex, int rowIndex, Sheet sheet) {
        var row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        var cell = row.getCell(colIndex);
        return cell != null ? cell : row.createCell(colIndex);
    }

    /**
     * Evaluates formula in the cell to get new cell value.
     */
    public static void evaluateFormula(Cell cell) {
        var formulaEvaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
        formulaEvaluator.evaluateFormulaCell(cell);
    }

    public static <T extends CellStyle> T createCellStyle(Workbook workbook) {
        if (workbook instanceof HSSFWorkbook fWorkbook) {
            if (workbook.getNumCellStyles() == MAX_STYLES) {
                HSSFOptimiser.optimiseCellStyles(fWorkbook);
            }
            @SuppressWarnings("unchecked")
            var style = (T) workbook.createCellStyle();
            return style;
        } else {
            @SuppressWarnings("unchecked")
            var style = (T) workbook.createCellStyle();
            return style;
        }
    }

    public static CellStyle cloneStyleFrom(Cell cell) {
        var newStyle = createCellStyle(cell.getSheet().getWorkbook());
        newStyle.cloneStyleFrom(cell.getCellStyle());
        return newStyle;
    }

    public static Font getCellFont(Cell cell) {
        Font font = null;
        if (cell != null) {
            var style = cell.getCellStyle();
            var fontIndex = style.getFontIndex();
            font = cell.getSheet().getWorkbook().getFontAt(fontIndex);
        }
        return font;
    }

    public static void setCellFontBold(Cell cell, boolean boldweight) {
        Font font = getCellFont(cell);
        if (font != null) {
            setCellFont(cell, font, boldweight, font.getItalic(), font.getUnderline());
        }
    }

    public static void setCellFontItalic(Cell cell, boolean italic) {
        Font font = getCellFont(cell);
        if (font != null) {
            setCellFont(cell, font, font.getBold(), italic, font.getUnderline());
        }
    }

    public static void setCellFontUnderline(Cell cell, byte underline) {
        Font font = getCellFont(cell);
        if (font != null) {
            setCellFont(cell, font, font.getBold(), font.getItalic(), underline);
        }
    }

    /**
     * Sets the font of the cell to the given font with other bold, italic and underline settings.
     *
     * <p>Reuses a font of the workbook that has the same settings, or creates a new one. The text keeps its colour.
     */
    private static void setCellFont(Cell cell, Font base, boolean bold, boolean italic, byte underline) {
        var workbook = cell.getSheet().getWorkbook();
        var font = FontAttributes.of(base, workbook).withBold(bold).withItalic(italic).withUnderline(underline);
        CellUtil.setFont(cell, findOrCreateFont(workbook, font));
    }

    /**
     * What tells two fonts of a workbook apart, so a font the workbook has is found again rather than made twice.
     *
     * <p>The colour is compared as it is drawn. An {@code .xlsx} font coloured with an RGB or a theme colour has no
     * indexed colour, so comparing indexes would take a font of another colour for this one.
     *
     * <p>A font coloured by a colour of the theme of its workbook is another font than one of the same colour of its
     * own: the theme draws it, so its colour changes with the theme, and Excel offers it among the colours of the
     * theme.
     *
     * @param name       the name of the font
     * @param height     the size in twentieths of a point, which tells a font of 10.5 points from one of 10
     * @param bold       whether the font is bold
     * @param italic     whether the font is italic
     * @param underline  the underline, as {@link Font} names it
     * @param strikeout  whether the font is struck out
     * @param typeOffset the superscript or subscript, as {@link Font} names it
     * @param charset    the character set of the font
     * @param color      the colour as {@code 0xRRGGBB}, as it is drawn, or {@code null} for the automatic colour
     * @param themed     the colour of the theme the font is coloured by, or {@code null} for a colour of its own
     */
    @Builder(toBuilder = true)
    @With
    public record FontAttributes(String name,
                                 short height,
                                 boolean bold,
                                 boolean italic,
                                 byte underline,
                                 boolean strikeout,
                                 short typeOffset,
                                 int charset,
                                 @Nullable Integer color,
                                 @Nullable ThemedColor themed) {

        /**
         * The attributes of a font of a workbook.
         *
         * @param font     the font
         * @param workbook the workbook the font belongs to
         * @return the attributes of the font
         */
        public static FontAttributes of(Font font, Workbook workbook) {
            return uncoloured(font)
                    .withColor(Optional.ofNullable(getFontColor(font, workbook))
                            .map(PoiExcelHelper::toRgbValue)
                            .orElse(null))
                    .withThemed(font instanceof XSSFFont xssf ? ThemedColor.of(colourOf(xssf, workbook)) : null);
        }

        /** The attributes of a font but its colour, which is the costly one to read. */
        private static FontAttributes uncoloured(Font font) {
            return FontAttributes.builder()
                    .name(font.getFontName())
                    .height(font.getFontHeight())
                    .bold(font.getBold())
                    .italic(font.getItalic())
                    .underline(font.getUnderline())
                    .strikeout(font.getStrikeout())
                    .typeOffset(font.getTypeOffset())
                    .charset(font.getCharSet())
                    .build();
        }
    }

    /**
     * A colour of the theme of an {@code .xlsx} workbook, made lighter or darker, as the workbook writes it. The
     * workbook draws it from its theme, so the colour changes with the theme, and Excel offers it in its palette.
     *
     * <p>The tint counts in thousandths, as Excel draws it, so two tints Excel draws alike are one.
     *
     * @param index the colour of the theme, as a workbook numbers them: the first background and text 0 and 1, the
     *              second ones 2 and 3, the six accents 4 to 9, a hyperlink 10 and a followed one 11
     * @param tint  how much lighter, above 0, or darker, below 0, in thousandths from -1000 to 1000
     */
    public record ThemedColor(int index, int tint) {

        /** The steps Excel writes a tint in: Lighter 60% as 19660 of them, 0.59999389629810485. */
        private static final int WRITTEN_STEPS = 32767;

        /**
         * The colour of the theme a colour of a workbook is.
         *
         * @param color the colour, or {@code null} for none
         * @return the colour of the theme, or {@code null} for a colour that is none
         */
        public static @Nullable ThemedColor of(@Nullable Color color) {
            return color instanceof XSSFColor xssf && xssf.isThemed()
                    ? new ThemedColor(xssf.getTheme(), (int) Math.round(xssf.getTint() * Hls.TINT_STEPS))
                    : null;
        }

        /**
         * The tint as Excel writes it: in whole steps of its own, cut toward zero.
         *
         * @return the tint from -1 to 1
         */
        public double writtenTint() {
            return (double) Math.divideExact(tint * WRITTEN_STEPS, Hls.TINT_STEPS) / WRITTEN_STEPS;
        }

        /**
         * The colour as a colour of a workbook, which draws it from its theme.
         *
         * @param workbook the workbook
         * @return the colour
         */
        public XSSFColor toColor(XSSFWorkbook workbook) {
            var color = new XSSFColor(workbook.getStylesSource().getIndexedColors());
            color.setTheme(index);
            if (tint != 0) {
                color.setTint(writtenTint());
            }
            return color;
        }
    }

    /**
     * Every font of a workbook. An {@code .xls} workbook has no font 4: its fonts are numbered past it.
     *
     * @param workbook the workbook
     * @return the fonts of the workbook
     */
    static List<Font> getFonts(Workbook workbook) {
        var count = workbook.getNumberOfFonts();
        var fonts = new ArrayList<Font>(count);
        var skipsFour = workbook instanceof HSSFWorkbook;
        for (var index = 0; fonts.size() < count; index++) {
            if (!skipsFour || index != HSSF_MISSING_FONT) {
                fonts.add(workbook.getFontAt(index));
            }
        }
        return fonts;
    }

    /**
     * The font of a workbook with the given attributes: the one the workbook has, or a new one.
     *
     * @param workbook   the workbook
     * @param attributes the attributes of the font
     * @return the font
     */
    public static Font findOrCreateFont(Workbook workbook, FontAttributes attributes) {
        var uncoloured = attributes.withColor(null).withThemed(null);
        return getFonts(workbook).stream()
                // The colour is read only for a font alike in every other attribute.
                .filter(font -> FontAttributes.uncoloured(font).equals(uncoloured)
                        && FontAttributes.of(font, workbook).equals(attributes))
                .findFirst()
                .orElseGet(() -> createFont(workbook, attributes));
    }

    /**
     * A new font of a workbook with the given attributes.
     *
     * @param workbook   the workbook
     * @param attributes the attributes of the font
     * @return the font
     */
    private static Font createFont(Workbook workbook, FontAttributes attributes) {
        var font = workbook.createFont();
        font.setFontName(attributes.name());
        font.setFontHeight(attributes.height());
        font.setBold(attributes.bold());
        font.setItalic(attributes.italic());
        font.setUnderline(attributes.underline());
        font.setStrikeout(attributes.strikeout());
        font.setTypeOffset(attributes.typeOffset());
        font.setCharSet(attributes.charset());
        var themed = attributes.themed();
        var color = attributes.color();
        if (themed != null && font instanceof XSSFFont xssf) {
            setThemedColor(xssf, themed, (XSSFWorkbook) workbook);
        } else if (color != null) {
            setFontColor(font, toRgb(color), workbook);
        }
        return font;
    }

    /**
     * Colours an {@code .xlsx} font by a colour of the theme of its workbook.
     *
     * <p>POI colours a font by its theme only without a tint ({@link XSSFFont#setThemeColor}), and
     * {@link XSSFFont#setColor(XSSFColor)} takes the red, the green and the blue of a colour alone, so the colour is
     * set as the font writes it.
     */
    private static void setThemedColor(XSSFFont font, ThemedColor themed, XSSFWorkbook workbook) {
        font.getCTFont().setColorArray(new CTColor[]{themed.toColor(workbook).getCTColor()});
    }

    // The array is one RGB color, not a list: null stands for no color, which table views keep as missing.
    @SuppressWarnings("java:S1168")
    public static short[] toRgb(Color color) {
        if (color == null) {
            return null;
        }

        if (color instanceof HSSFColor fColor1) {
            return fColor1.getTriplet();

        } else if (color instanceof XSSFColor fColor) {
            var rgb = fColor.getRGB();
            if (rgb != null) {
                return applyTint(new short[]{toShort(rgb[0]), toShort(rgb[1]), toShort(rgb[2])}, fColor.getTint());
            }
        }

        return null;
    }

    /**
     * A colour made lighter or darker by a tint, as Excel draws it.
     *
     * <p>Excel draws a tinted colour in the hue, luminance and saturation model of Windows. A tint above 0 moves the
     * luminance that part of the way to white, and a tint below 0 that part of the way to black, the hue and the
     * saturation kept.
     *
     * <p>The tint counts in thousandths, as Excel draws it: a tint written as {@code 0.59999389629810485}, which is how
     * Excel writes Lighter 60%, draws as 0.6. A tint of less than a thousandth leaves the colour as it is.
     *
     * @param rgb  the red, the green and the blue of the colour
     * @param tint how much lighter the colour is, above 0, or darker, below 0, from -1 to 1
     * @return the red, the green and the blue of the colour drawn
     */
    public static short[] applyTint(short[] rgb, double tint) {
        var steps = (int) Math.round(tint * Hls.TINT_STEPS);
        if (steps == 0) {
            return rgb.clone();
        }
        var colour = Hls.of(rgb[0], rgb[1], rgb[2]);
        var lum = colour.lum();
        var tinted = steps < 0
                ? lum * (Hls.TINT_STEPS + steps) / Hls.TINT_STEPS
                : (lum * (Hls.TINT_STEPS - steps) + Hls.MAX * steps) / Hls.TINT_STEPS;
        return new Hls(colour.hue(), tinted, colour.sat()).toRgb();
    }

    /**
     * A colour in the hue, luminance and saturation model of Windows, which Excel tints a colour in. Each counts from
     * 0 to {@link #MAX}, and the colour turns into red, green and blue and back in whole numbers, as Windows turns it.
     *
     * @param hue the hue
     * @param lum the luminance: 0 is black, {@link #MAX} is white
     * @param sat the saturation: 0 is a grey
     */
    private record Hls(int hue, int lum, int sat) {

        /** The largest hue, luminance and saturation. */
        static final int MAX = 240;

        /** How finely a tint moves the luminance: in thousandths. */
        static final int TINT_STEPS = 1000;

        /** The largest red, green or blue. */
        private static final int RGB_MAX = 255;

        /** The hue of a grey, which has none. */
        private static final int GREY_HUE = MAX * 2 / 3;

        static Hls of(int red, int green, int blue) {
            var max = Math.max(red, Math.max(green, blue));
            var min = Math.min(red, Math.min(green, blue));
            var lum = ((max + min) * MAX + RGB_MAX) / (2 * RGB_MAX);
            if (max == min) {
                return new Hls(GREY_HUE, lum, 0);
            }
            var range = max - min;
            var sat = lum <= MAX / 2
                    ? (range * MAX + (max + min) / 2) / (max + min)
                    : (range * MAX + (2 * RGB_MAX - max - min) / 2) / (2 * RGB_MAX - max - min);
            var redDelta = delta(max, red, range);
            var greenDelta = delta(max, green, range);
            var blueDelta = delta(max, blue, range);
            int hue;
            if (red == max) {
                hue = blueDelta - greenDelta;
            } else if (green == max) {
                hue = MAX / 3 + redDelta - blueDelta;
            } else {
                hue = 2 * MAX / 3 + greenDelta - redDelta;
            }
            return new Hls(hue < 0 ? hue + MAX : hue, lum, sat);
        }

        /** How far one of red, green and blue is from the largest of them, in sixths of the hue. */
        private static int delta(int max, int channel, int range) {
            return ((max - channel) * (MAX / 6) + range / 2) / range;
        }

        short[] toRgb() {
            if (sat == 0) {
                var grey = channel(lum);
                return new short[]{grey, grey, grey};
            }
            var high = lum <= MAX / 2 ? (lum * (MAX + sat) + MAX / 2) / MAX : lum + sat - (lum * sat + MAX / 2) / MAX;
            var low = 2 * lum - high;
            return new short[]{channel(hueToLevel(low, high, hue + MAX / 3)), channel(hueToLevel(low, high, hue)),
                    channel(hueToLevel(low, high, hue - MAX / 3))};
        }

        /** The level of one of red, green and blue, counted to {@link #MAX}, at a hue. */
        private static int hueToLevel(int low, int high, int at) {
            var hue = at < 0 ? at + MAX : at;
            if (hue > MAX) {
                hue -= MAX;
            }
            if (hue < MAX / 6) {
                return low + ((high - low) * hue + MAX / 12) / (MAX / 6);
            }
            if (hue < MAX / 2) {
                return high;
            }
            if (hue < MAX * 2 / 3) {
                return low + ((high - low) * (MAX * 2 / 3 - hue) + MAX / 12) / (MAX / 6);
            }
            return low;
        }

        /** A level counted to {@link #MAX} as the red, the green or the blue of a colour. */
        private static short channel(int level) {
            return (short) ((level * RGB_MAX + MAX / 2) / MAX);
        }
    }

    private static short toShort(byte value) {
        return (short) (value & 0xFF);
    }

    public static short[] toRgb(short colorIndex, HSSFWorkbook workbook) {
        var cc = workbook.getCustomPalette().getColor(colorIndex);
        return toRgb(cc);
    }

    /**
     * The colour {@code #rrggbb} as the red, green and blue a workbook writes.
     *
     * @param hex the colour as {@code #rrggbb}
     * @return the red, green and blue of the colour
     */
    public static short[] toRgb(String hex) {
        return toRgb(Integer.parseInt(hex.substring(1), 16));
    }

    /**
     * The colour as one number.
     *
     * @param rgb the red, green and blue of the colour
     * @return the colour as {@code 0xRRGGBB}
     */
    public static int toRgbValue(short[] rgb) {
        return rgb[0] << 16 | rgb[1] << 8 | rgb[2];
    }

    /**
     * The colour a workbook holds once the colour {@code #rrggbb} is written into it: the colour itself, or the colour
     * of the palette of an {@code .xls} workbook it is written as.
     *
     * <p>A palette with no room left for another colour holds the nearest colour it has, so a cell written with the
     * colour reads back that one.
     *
     * @param hex      the colour as {@code #rrggbb}
     * @param workbook the workbook the colour is written into
     * @return the red, green and blue the workbook holds
     */
    public static short[] toStoredRgb(String hex, Workbook workbook) {
        return toStoredRgb(toRgb(hex), workbook);
    }

    /**
     * The colour a workbook holds once the colour is written into it, as {@link #toStoredRgb(String, Workbook)} tells.
     *
     * @param rgb      the red, green and blue of the colour
     * @param workbook the workbook the colour is written into
     * @return the red, green and blue the workbook holds
     */
    public static short[] toStoredRgb(short[] rgb, Workbook workbook) {
        return workbook instanceof HSSFWorkbook hssf
                ? Optional.ofNullable(toRgb(getOrAddColorIndex(rgb, hssf), hssf)).orElse(rgb)
                : rgb;
    }

    /** The colour {@code 0xRRGGBB} as the red, green and blue a workbook writes. */
    private static short[] toRgb(int rgb) {
        return new short[]{(short) (rgb >> 16 & 0xFF), (short) (rgb >> 8 & 0xFF), (short) (rgb & 0xFF)};
    }

    public static short[] getFontColor(Font font, Workbook workbook) {
        if (font instanceof XSSFFont fFont) {
            return toRgb(colourOf(fFont, workbook));
        } else {
            short x = font.getColor();
            return toRgb(x, (HSSFWorkbook) workbook);
        }
    }

    /**
     * The colour of an {@code .xlsx} font, or {@code null} for a font that names none.
     *
     * <p>The colour of a font of a workbook is read from a copy. POI writes the RGB of a theme colour into a colour it
     * reads ({@link ThemesTable#inheritFromThemeAsRequired}), so reading the font itself would change it, and a
     * workbook saved then would keep that RGB in every font that was only read.
     *
     * <p>A font read without its workbook, which is never saved, resolves its theme colour itself.
     */
    private static @Nullable XSSFColor colourOf(XSSFFont font, @Nullable Workbook workbook) {
        var written = font.getCTFont();
        if (!(workbook instanceof XSSFWorkbook xssf) || written.sizeOfColorArray() == 0) {
            return font.getXSSFColor();
        }
        var styles = xssf.getStylesSource();
        var colour = XSSFColor.from((CTColor) written.getColorArray(0).copy(), styles.getIndexedColors());
        Optional.ofNullable(styles.getTheme()).ifPresent(theme -> theme.inheritFromThemeAsRequired(colour));
        return colour;
    }

    /**
     * Splits a cell text into the pieces formatted with fonts of their own.
     *
     * <p>A text that takes the font of its cell gives an empty list, and a text formatted whole in a font of its
     * own gives one run. A piece of the text that no run formats, and a run that names no font, take the font of the
     * cell: their run has no font.
     *
     * <p>The theme is needed only for a text read without its workbook. It lets a run coloured by a theme colour
     * resolve that colour.
     *
     * @param text the text of the cell
     * @param workbook the workbook of the cell, needed for an {@code .xls} text
     * @param themes the theme of the workbook, or {@code null} when the text knows it
     * @return the runs of the text, or an empty list when the text takes the font of its cell
     */
    public static List<TextRun> getTextRuns(RichTextString text, @Nullable Workbook workbook,
                                            @Nullable ThemesTable themes) {
        var count = text.numFormattingRuns();
        var value = text.getString();
        if (count == 0 || value == null) {
            return List.of();
        }
        var runs = new ArrayList<TextRun>(count + 1);
        var starts = runStarts(text, count, value.length());
        if (starts[0] > 0) {
            runs.add(new TextRun(value.substring(0, starts[0]), null));
        }
        for (var run = 0; run < count; run++) {
            var start = starts[run];
            var end = starts[run + 1];
            if (end > start) {
                runs.add(new TextRun(value.substring(start, end), runFont(text, run, workbook, themes)));
            }
        }
        return runs.stream().anyMatch(run -> run.font() != null) ? List.copyOf(runs) : List.of();
    }

    /**
     * Where each run of a text starts in the text as the cell shows it, and where the text ends after them.
     *
     * <p>An {@code .xlsx} text escapes a character XML cannot hold, such as a carriage return, as {@code _xHHHH_}.
     * POI answers the text with such characters unescaped, but counts where its runs start in the text as it is
     * written, so each run is measured here as the cell shows it. A run never starts past the end of the text.
     */
    private static int[] runStarts(RichTextString text, int count, int length) {
        var starts = new int[count + 1];
        starts[count] = length;
        if (text instanceof XSSFRichTextString xssf) {
            var written = xssf.getCTRst();
            var at = 0;
            for (var run = 0; run < count; run++) {
                starts[run] = Math.min(at, length);
                at += shownLength(written.getRArray(run).getT());
            }
        } else {
            for (var run = 0; run < count; run++) {
                starts[run] = Math.min(text.getIndexOfFormattingRun(run), length);
            }
        }
        return starts;
    }

    /** How long a piece of an {@code .xlsx} text is as the cell shows it: an escaped character shows as one. */
    private static int shownLength(@Nullable String written) {
        return Optional.ofNullable(written)
                .map(text -> text.length() - ESCAPE_EXTRA * (int) ESCAPED_CHARACTER.matcher(text).results().count())
                .orElse(0);
    }

    private static @Nullable ICellFont runFont(RichTextString text, int run, @Nullable Workbook workbook,
                                              @Nullable ThemesTable themes) {
        if (text instanceof XSSFRichTextString xssf) {
            var font = xssf.getFontOfFormattingRun(run);
            if (font != null && themes != null) {
                font.setThemesTable(themes);
            }
            return Optional.ofNullable(font).map(found -> new XlsCellFont(found, workbook)).orElse(null);
        }
        if (text instanceof HSSFRichTextString hssf && workbook != null) {
            var index = hssf.getFontOfFormattingRun(run);
            return index == HSSFRichTextString.NO_FONT ? null : new XlsCellFont(workbook.getFontAt(index), workbook);
        }
        return null;
    }

    public static short[][] getCellBorderColors(CellStyle style, Workbook workbook) {
        short[][] colors = new short[4][];

        if (style instanceof HSSFCellStyle) {
            var hssfWorkbook = (HSSFWorkbook) workbook;
            colors[0] = toRgb(style.getTopBorderColor(), hssfWorkbook);
            colors[1] = toRgb(style.getRightBorderColor(), hssfWorkbook);
            colors[2] = toRgb(style.getBottomBorderColor(), hssfWorkbook);
            colors[3] = toRgb(style.getLeftBorderColor(), hssfWorkbook);

        } else if (style instanceof XSSFCellStyle xssfStyle) {
            colors[0] = toRgb(xssfStyle.getTopBorderXSSFColor());
            colors[1] = toRgb(xssfStyle.getRightBorderXSSFColor());
            colors[2] = toRgb(xssfStyle.getBottomBorderXSSFColor());
            colors[3] = toRgb(xssfStyle.getLeftBorderXSSFColor());
        }

        return colors;
    }

    public static BorderStyle[] getCellBorderStyles(CellStyle style) {
        BorderStyle[] styles = new BorderStyle[4];

        styles[0] = style.getBorderTop();
        styles[1] = style.getBorderRight();
        styles[2] = style.getBorderBottom();
        styles[3] = style.getBorderLeft();

        return styles;
    }

    public static void setCellBorderColors(CellStyle style, short[][] colors, Workbook workbook) {
        if (style instanceof HSSFCellStyle) {
            setHssfCellBorderColors(style, colors, (HSSFWorkbook) workbook);
        } else if (style instanceof XSSFCellStyle xssfStyle) {
            setXssfCellBorderColors(xssfStyle, colors, (XSSFWorkbook) workbook);
        }
    }

    private static void setHssfCellBorderColors(CellStyle style, short[][] colors, HSSFWorkbook hssfWorkbook) {
        if (colors[0] != null) {
            style.setTopBorderColor(getOrAddColorIndex(colors[0], hssfWorkbook));
        }
        if (colors[1] != null) {
            style.setRightBorderColor(getOrAddColorIndex(colors[1], hssfWorkbook));
        }
        if (colors[2] != null) {
            style.setBottomBorderColor(getOrAddColorIndex(colors[2], hssfWorkbook));
        }
        if (colors[3] != null) {
            style.setLeftBorderColor(getOrAddColorIndex(colors[3], hssfWorkbook));
        }
    }

    private static void setXssfCellBorderColors(XSSFCellStyle xssfStyle, short[][] colors, XSSFWorkbook xssfWorkbook) {
        if (colors[0] != null) {
            xssfStyle.setTopBorderColor(getColor(colors[0], xssfWorkbook));
        }
        if (colors[1] != null) {
            xssfStyle.setRightBorderColor(getColor(colors[1], xssfWorkbook));
        }
        if (colors[2] != null) {
            xssfStyle.setBottomBorderColor(getColor(colors[2], xssfWorkbook));
        }
        if (colors[3] != null) {
            xssfStyle.setLeftBorderColor(getColor(colors[3], xssfWorkbook));
        }
    }

    /**
     * Gives the style the fill colours it is asked for, as colours of the given workbook.
     *
     * <p>A colour that is {@code null} is left as the style already has it.
     */
    public static void setCellFillColors(CellStyle style, short[] foreground, short[] background, Workbook workbook) {
        if (style instanceof HSSFCellStyle) {
            var hssfWorkbook = (HSSFWorkbook) workbook;
            if (foreground != null) {
                style.setFillForegroundColor(getOrAddColorIndex(foreground, hssfWorkbook));
            }
            if (background != null) {
                style.setFillBackgroundColor(getOrAddColorIndex(background, hssfWorkbook));
            }
        } else if (style instanceof XSSFCellStyle xssfStyle) {
            var xssfWorkbook = (XSSFWorkbook) workbook;
            if (foreground != null) {
                xssfStyle.setFillForegroundColor(getColor(foreground, xssfWorkbook));
            }
            if (background != null) {
                xssfStyle.setFillBackgroundColor(getColor(background, xssfWorkbook));
            }
        }
    }

    /**
     * Gives the font the colour it is asked for, as a colour of the given workbook.
     *
     * <p>A colour that is {@code null} is left as the font already has it.
     */
    public static void setFontColor(Font font, short[] color, Workbook workbook) {
        if (color == null) {
            return;
        }
        if (font instanceof XSSFFont xssfFont) {
            xssfFont.setColor(getColor(color, (XSSFWorkbook) workbook));
        } else {
            font.setColor(getOrAddColorIndex(color, (HSSFWorkbook) workbook));
        }
    }

    /**
     * The given colour as a colour of the workbook.
     *
     * <p>It is written with the alpha the format asks for: a colour of three bytes is not one a workbook
     * can hold, and a spreadsheet application reads a file holding one as damaged content.
     */
    public static XSSFColor getColor(short[] color, XSSFWorkbook workbook) {
        byte[] argb = {OPAQUE, 0, 0, 0};
        for (var i = 0; i < 3; i++) {
            argb[i + 1] = (byte) (color[i] & 0xFF);
        }
        var indexedColors = workbook.getStylesSource().getIndexedColors();
        var xssfColor = new XSSFColor(indexedColors);
        xssfColor.setRGB(argb);
        return xssfColor;
    }

    private static short getOrAddColorIndex(short[] rgb, HSSFWorkbook wb) {
        var palette = wb.getCustomPalette();
        var color = palette.findColor((byte) rgb[0], (byte) rgb[1], (byte) rgb[2]);

        if (color == null) {
            try {
                color = palette.addColor((byte) rgb[0], (byte) rgb[1], (byte) rgb[2]);
            } catch (RuntimeException e) {
                // Could not find free color index
                color = palette.findSimilarColor(rgb[0], rgb[1], rgb[2]);
            }
        }

        return color.getIndex();
    }

}
