package org.openl.rules.table.xls;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.Builder;
import lombok.With;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFOptimiser;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.model.ThemesTable;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor;

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
     * @param name       the name of the font
     * @param height     the size in twentieths of a point, which tells a font of 10.5 points from one of 10
     * @param bold       whether the font is bold
     * @param italic     whether the font is italic
     * @param underline  the underline, as {@link Font} names it
     * @param strikeout  whether the font is struck out
     * @param typeOffset the superscript or subscript, as {@link Font} names it
     * @param charset    the character set of the font
     * @param color      the colour as {@code 0xRRGGBB}, or {@code null} for the automatic colour
     */
    @Builder
    @With
    public record FontAttributes(String name,
                                 short height,
                                 boolean bold,
                                 boolean italic,
                                 byte underline,
                                 boolean strikeout,
                                 short typeOffset,
                                 int charset,
                                 @Nullable Integer color) {

        /**
         * The attributes of a font of a workbook.
         *
         * @param font     the font
         * @param workbook the workbook the font belongs to
         * @return the attributes of the font
         */
        public static FontAttributes of(Font font, Workbook workbook) {
            var rgb = getFontColor(font, workbook);
            return uncoloured(font).withColor(rgb == null ? null : rgb[0] << 16 | rgb[1] << 8 | rgb[2]);
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
    private static Font findOrCreateFont(Workbook workbook, FontAttributes attributes) {
        var uncoloured = attributes.withColor(null);
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
        var color = attributes.color();
        if (color != null) {
            setFontColor(font, new short[]{(short) (color >> 16 & 0xFF), (short) (color >> 8 & 0xFF),
                    (short) (color & 0xFF)}, workbook);
        }
        return font;
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

            // Byte to short
            if (rgb != null) {
                return applyTint(rgb, fColor.getTint());
            }
        }

        return null;
    }

    private static short[] applyTint(byte[] rgb, double tint) {

        short red = toShort(rgb[0]);
        short green = toShort(rgb[1]);
        short blue = toShort(rgb[2]);

        if (tint == 0.0) { // no changes
            return new short[]{red, green, blue};
        }

        if (red == green && green == blue) { // achromatic
            final var newLum = calculateLum(red, tint);
            short v = toShort(newLum);
            return new short[]{v, v, v};
        }

        // Find brightest and darkest components
        short max = green;
        short min = red;
        if (red > green) {
            max = red;
            min = green;
        }
        if (blue > max) {
            max = blue;
        } else if (blue < min) {
            min = blue;
        }

        // Calculate colors metrics
        var chroma = max - min;
        var lum = max + min;
        final var newLum = calculateLum(lum / 2, tint) * 2;
        // new amount of chroma
        var x = (255 - Math.abs(newLum - 255)) / (255 - Math.abs(lum - 255));
        // new amount of white color
        var m = (newLum - x * chroma) / 2;

        // Adjusted RGB
        short r = toShort((red - min) * x + m);
        short g = toShort((green - min) * x + m);
        short b = toShort((blue - min) * x + m);

        return new short[]{r, g, b};

    }

    private static double calculateLum(int lum, double tint) {
        if (tint < 0) {
            return lum * (1.0 + tint);
        } else {
            return (lum - 255) * (1.0 - tint) + 255;
        }
    }

    private static short toShort(double value) {
        if (value >= 255) {
            return 255;
        } else if (value <= 0) {
            return 0;
        } else {
            return (short) Math.round(value);
        }
    }

    private static short toShort(byte value) {
        return (short) (value & 0xFF);
    }

    public static short[] toRgb(short colorIndex, HSSFWorkbook workbook) {
        var cc = workbook.getCustomPalette().getColor(colorIndex);
        return toRgb(cc);
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
