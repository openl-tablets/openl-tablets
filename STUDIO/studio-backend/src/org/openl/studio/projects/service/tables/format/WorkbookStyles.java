package org.openl.studio.projects.service.tables.format;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.extensions.XSSFCellBorder;
import org.apache.poi.xssf.usermodel.extensions.XSSFCellFill;
import org.apache.xmlbeans.XmlException;
import org.jspecify.annotations.Nullable;
import org.openxmlformats.schemas.drawingml.x2006.main.CTFontScheme;
import org.openxmlformats.schemas.drawingml.x2006.main.ThemeDocument;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBorder;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBorderPr;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellStyle;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellStyles;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFill;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFont;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STBorderStyle;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STFontScheme;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STHorizontalAlignment;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STPatternType;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STVerticalAlignment;

import org.openl.rules.table.xls.PoiExcelHelper.ThemedColor;
import org.openl.studio.projects.service.tables.format.TableStyle.Line;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * The OpenL table style in one workbook: the built-in cell styles of the roles, and the cell formats that link them.
 *
 * <p>The nine built-in styles of the roles are written into the workbook, redefined with the theme colours and the
 * theme fonts. A style the workbook already names is redefined in place; the others are added. The Normal style is
 * never changed, and every role takes the font size it has.
 *
 * <p>A cell format links the style of a role, draws the lines at the edges of its region, and keeps what the cell had:
 * its number format, its indent, its text wrapping and its text rotation. A cell format, a font, a fill or a border
 * the workbook already holds is reused, so formatting a table again adds nothing to the workbook.
 *
 * <p>A workbook with no theme gets the Office theme, so the theme colours and the theme fonts resolve as Excel
 * resolves them.
 *
 * @author Yury Molchan
 */
final class WorkbookStyles {

    /** The Office theme of Excel 2013–2022, which a workbook with no theme gets. */
    private static final String OFFICE_THEME = "office-theme.xml";

    /** The font size of a workbook whose Normal style names none, as Excel writes a new workbook. */
    private static final double OFFICE_SIZE = 11;

    /** The id of the built-in Normal style. */
    private static final long NORMAL = 0;

    /** The indexed colour SpreadsheetML names automatic, which a solid fill takes as its background colour. */
    private static final long AUTOMATIC_COLOUR = 64;

    private final StylesTable styles;
    private final String majorFace;
    private final String minorFace;
    private final double size;

    /** The cell style of each role, as an index of the cell styles of the workbook. */
    private final Map<TableStyle, Integer> roleStyles = new EnumMap<>(TableStyle.class);
    private final Map<TableStyle, Integer> roleFonts = new EnumMap<>(TableStyle.class);
    private final Map<TableStyle, Integer> roleFills = new EnumMap<>(TableStyle.class);

    private final Map<FontKey, Integer> fonts = new HashMap<>();
    private final Map<FontKey, XSSFFont> pieceFonts = new HashMap<>();
    private final Map<FillKey, Integer> fills = new HashMap<>();
    private final Map<BorderKey, Integer> borders = new HashMap<>();
    private final Map<Format, Integer> formats = new HashMap<>();
    private final Map<Integer, Kept> kept = new HashMap<>();
    private @Nullable Map<XfKey, Integer> cellXfs;

    /** The last cell format asked for, which the cells next to a cell mostly ask for again. */
    private @Nullable Asked last;

    WorkbookStyles(XSSFWorkbook workbook) {
        styles = workbook.getStylesSource();
        var fontScheme = fontSchemeOf(styles);
        majorFace = fontScheme.getMajorFont().getLatin().getTypeface();
        minorFace = fontScheme.getMinorFont().getLatin().getTypeface();
        size = normalSize();
        indexFonts();
        indexFills();
        indexBorders();
        writeRoleStyles();
    }

    /**
     * The cell format a cell takes: the style of its role, the lines at the edges of its region, and what is kept from
     * the cell format it has.
     *
     * @param style  the style of the role of the cell
     * @param edges  the lines at the edges of its region
     * @param source the cell format the cell has
     * @return the cell format, as an index of the cell formats of the workbook
     */
    int format(TableStyle style, Edges edges, int source) {
        var asked = last;
        if (asked != null && asked.style == style && asked.source == source && asked.edges.equals(edges)) {
            return asked.format;
        }
        var from = kept.computeIfAbsent(source, this::keptOf);
        var format = formats.computeIfAbsent(new Format(style, edges, from), this::write);
        last = new Asked(style, edges, source, format);
        return format;
    }

    /** A cell format asked for: the style of a role and the lines at its edges over a cell format a cell had. */
    private record Asked(TableStyle style, Edges edges, int source, int format) {
    }

    /**
     * A font of a piece of the signature: the heading font of the theme, in the size of the Normal style. It is not
     * registered in the workbook: a piece of rich text holds its font itself.
     *
     * @param bold   whether the piece is bold
     * @param colour the colour of the piece
     * @return the font
     */
    XSSFFont pieceFont(boolean bold, ThemedColor colour) {
        return pieceFonts.computeIfAbsent(new FontKey(majorFace, size, bold, false, STFontScheme.INT_MAJOR, colour),
                key -> new XSSFFont(fontOf(key)));
    }

    /** What is kept from a cell format: its number format, indent, text wrapping and text rotation. */
    private Kept keptOf(int source) {
        var xf = styles.getCellXfAt(source < styles.getNumCellStyles() ? source : 0);
        if (!xf.isSetAlignment()) {
            return new Kept(xf.getNumFmtId(), 0, false, 0);
        }
        var alignment = xf.getAlignment();
        var rotation = alignment.isSetTextRotation() ? alignment.getTextRotation().longValue() : 0;
        return new Kept(xf.getNumFmtId(), alignment.getIndent(), alignment.getWrapText(), rotation);
    }

    /** Writes a cell format, or finds it in the workbook. */
    private int write(Format format) {
        var style = format.style();
        var from = format.kept();
        var xf = CTXf.Factory.newInstance();
        xf.setNumFmtId(from.numberFormat());
        xf.setFontId(roleFonts.get(style));
        xf.setFillId(roleFills.get(style));
        xf.setBorderId(border(format.edges()));
        xf.setXfId(roleStyles.get(style));
        if (from.numberFormat() != 0) {
            xf.setApplyNumberFormat(true);
        }
        xf.setApplyFont(true);
        xf.setApplyFill(true);
        xf.setApplyBorder(true);
        xf.setApplyAlignment(true);
        var alignment = xf.addNewAlignment();
        // An indent shows only in a text aligned to the left, as Excel aligns an indented text.
        var align = from.indent() > 0 ? HorizontalAlignment.LEFT : style.getLook().align();
        if (align != HorizontalAlignment.GENERAL) {
            alignment.setHorizontal(STHorizontalAlignment.Enum.forInt(align.ordinal() + 1));
        }
        alignment.setVertical(STVerticalAlignment.CENTER);
        if (from.indent() > 0) {
            alignment.setIndent(from.indent());
        }
        if (from.wrap()) {
            alignment.setWrapText(true);
        }
        if (from.rotation() != 0) {
            alignment.setTextRotation(BigInteger.valueOf(from.rotation()));
        }
        var known = cellXfs().get(XfKey.of(xf));
        if (known != null) {
            return known;
        }
        var index = styles.putCellXf(xf) - 1;
        cellXfs().put(XfKey.of(xf), index);
        return index;
    }

    /** The cell formats of the workbook, by what they hold, read once the first cell format is written. */
    private Map<XfKey, Integer> cellXfs() {
        if (cellXfs == null) {
            cellXfs = new HashMap<>();
            for (var index = 0; index < styles.getNumCellStyles(); index++) {
                cellXfs.putIfAbsent(XfKey.of(styles.getCellXfAt(index)), index);
            }
        }
        return cellXfs;
    }

    /**
     * Writes the built-in style of each role, redefined with the theme colours and the theme fonts. A style the
     * workbook names already is redefined in place, so an edit of it in Excel is replaced.
     */
    private void writeRoleStyles() {
        var named = namedStyles();
        for (var style : TableStyle.values()) {
            var look = style.getLook();
            var font = fontOf(look);
            var fill = fill(look.fill());
            roleFonts.put(style, font);
            roleFills.put(style, fill);
            var definition = CTXf.Factory.newInstance();
            definition.setNumFmtId(0);
            definition.setFontId(font);
            definition.setFillId(fill);
            definition.setBorderId(border(new Edges(look.top(), look.bottom(), null)));
            definition.setApplyNumberFormat(false);
            definition.setApplyProtection(false);
            var alignment = definition.addNewAlignment();
            if (look.align() != HorizontalAlignment.GENERAL) {
                alignment.setHorizontal(STHorizontalAlignment.Enum.forInt(look.align().ordinal() + 1));
            }
            alignment.setVertical(STVerticalAlignment.CENTER);
            var entry = builtIn(named, style.getBuiltinId());
            if (entry != null && entry.getXfId() < styles._getStyleXfsSize()) {
                styles.getCellStyleXfAt((int) entry.getXfId()).set(definition);
            } else {
                entry = entry != null ? entry : named.addNewCellStyle();
                entry.setName(style.getStyleName());
                entry.setBuiltinId(style.getBuiltinId());
                entry.setXfId(styles.putCellStyleXf(definition) - 1L);
            }
            // The style is not the one Office defines: Excel keeps the definition when cells are copied elsewhere.
            entry.setCustomBuiltin(true);
            roleStyles.put(style, (int) entry.getXfId());
        }
        named.setCount(named.sizeOfCellStyleArray());
    }

    /**
     * The named styles of the workbook. A workbook that names none gets the list, naming its Normal style, the first
     * cell style of every workbook.
     */
    private CTCellStyles namedStyles() {
        var sheet = styles.getCTStylesheet();
        if (sheet.isSetCellStyles()) {
            return sheet.getCellStyles();
        }
        var named = sheet.addNewCellStyles();
        var normal = named.addNewCellStyle();
        normal.setName("Normal");
        normal.setXfId(0);
        normal.setBuiltinId(NORMAL);
        return named;
    }

    /** The named style of a built-in style, or {@code null} when the workbook names none. */
    private static @Nullable CTCellStyle builtIn(CTCellStyles named, long builtinId) {
        for (var entry : named.getCellStyleList()) {
            if (entry.isSetBuiltinId() && entry.getBuiltinId() == builtinId) {
                return entry;
            }
        }
        return null;
    }

    /** The font size of the Normal style of the workbook. */
    private double normalSize() {
        var sheet = styles.getCTStylesheet();
        var normalXf = 0;
        if (sheet.isSetCellStyles()) {
            var normal = builtIn(sheet.getCellStyles(), NORMAL);
            normalXf = normal == null ? 0 : (int) normal.getXfId();
        }
        var xf = styles.getCellStyleXfAt(normalXf);
        if (xf == null || xf.getFontId() >= styles.getFonts().size()) {
            return OFFICE_SIZE;
        }
        var font = styles.getFontAt((int) xf.getFontId()).getCTFont();
        return font.sizeOfSzArray() > 0 ? font.getSzArray(0).getVal() : OFFICE_SIZE;
    }

    /**
     * The fonts of the theme of the workbook. A workbook with no theme, or with a theme naming no fonts, gets the
     * Office theme.
     */
    private static CTFontScheme fontSchemeOf(StylesTable styles) {
        var theme = styles.getTheme();
        if (theme != null) {
            try (var part = theme.getPackagePart().getInputStream()) {
                var elements = ThemeDocument.Factory.parse(part).getTheme().getThemeElements();
                if (elements != null && elements.getFontScheme() != null) {
                    return elements.getFontScheme();
                }
            } catch (IOException | XmlException unreadable) {
                // A theme written in this session and not yet saved has no part to read: it is written again.
            }
        }
        styles.ensureThemesTable();
        try (var office = officeTheme(); var copy = officeTheme()) {
            styles.getTheme().readFrom(office);
            return ThemeDocument.Factory.parse(copy).getTheme().getThemeElements().getFontScheme();
        } catch (IOException | XmlException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    private static InputStream officeTheme() {
        return Objects.requireNonNull(WorkbookStyles.class.getResourceAsStream(OFFICE_THEME), OFFICE_THEME);
    }

    /** The font of a look: the heading or the body font of the theme, in the size of the Normal style. */
    private int fontOf(TableStyle.Look look) {
        var scheme = look.major() ? STFontScheme.INT_MAJOR : STFontScheme.INT_MINOR;
        var key = new FontKey(look.major() ? majorFace : minorFace, size, look.bold(), look.italic(), scheme,
                look.colour());
        return fonts.computeIfAbsent(key, font -> styles.putFont(new XSSFFont(fontOf(font)), true));
    }

    /** A font as the workbook writes it, its elements in the order Excel writes them. */
    private CTFont fontOf(FontKey key) {
        var font = CTFont.Factory.newInstance();
        if (key.bold()) {
            font.addNewB();
        }
        if (key.italic()) {
            font.addNewI();
        }
        font.addNewSz().setVal(key.size());
        var colour = font.addNewColor();
        colour.setTheme(key.colour().index());
        if (key.colour().tint() != 0) {
            colour.setTint(key.colour().writtenTint());
        }
        font.addNewName().setVal(key.name());
        font.addNewScheme().setVal(STFontScheme.Enum.forInt(key.scheme()));
        return font;
    }

    /** A fill of a theme colour, or no fill. */
    private int fill(@Nullable ThemedColor colour) {
        return fills.computeIfAbsent(new FillKey(colour), key -> {
            var fill = CTFill.Factory.newInstance();
            var pattern = fill.addNewPatternFill();
            if (colour == null) {
                pattern.setPatternType(STPatternType.NONE);
            } else {
                pattern.setPatternType(STPatternType.SOLID);
                var foreground = pattern.addNewFgColor();
                foreground.setTheme(colour.index());
                if (colour.tint() != 0) {
                    foreground.setTint(colour.writtenTint());
                }
                pattern.addNewBgColor().setIndexed(AUTOMATIC_COLOUR);
            }
            return styles.putFill(new XSSFCellFill(fill, styles.getIndexedColors()));
        });
    }

    /** A border of the lines at the edges of a region. No line is drawn on the right. */
    private int border(Edges edges) {
        return borders.computeIfAbsent(new BorderKey(edges.left(), null, edges.top(), edges.bottom()), key -> {
            var border = CTBorder.Factory.newInstance();
            side(border.addNewLeft(), key.left());
            border.addNewRight();
            side(border.addNewTop(), key.top());
            side(border.addNewBottom(), key.bottom());
            border.addNewDiagonal();
            return add(new XSSFCellBorder(border, styles.getIndexedColors()));
        });
    }

    /**
     * Adds a border, or finds an equal one. POI compares a border with the others by their colours read through the
     * theme, which writes the RGB of the theme colour beside the theme colour of every line it reads: the theme is set
     * aside while it compares, so the lines keep their theme colours alone.
     */
    private int add(XSSFCellBorder border) {
        var theme = styles.getTheme();
        styles.setTheme(null);
        try {
            return styles.putBorder(border);
        } finally {
            styles.setTheme(theme);
        }
    }

    private static void side(CTBorderPr side, @Nullable Line line) {
        if (line != null) {
            side.setStyle(STBorderStyle.Enum.forInt(line.style().getCode() + 1));
            var colour = side.addNewColor();
            colour.setTheme(line.colour().index());
            if (line.colour().tint() != 0) {
                colour.setTint(line.colour().writtenTint());
            }
        }
    }

    /** Indexes the fonts of the workbook that a role could take, by what they hold. */
    private void indexFonts() {
        var all = styles.getFonts();
        for (var index = 0; index < all.size(); index++) {
            var key = FontKey.of(all.get(index).getCTFont());
            if (key != null) {
                fonts.putIfAbsent(key, index);
            }
        }
    }

    /** Indexes the fills of the workbook that a role could take: no fill, and the solid fills of a theme colour. */
    private void indexFills() {
        var all = styles.getFills();
        for (var index = 0; index < all.size(); index++) {
            var key = FillKey.of(all.get(index).getCTFill());
            if (key != null) {
                fills.putIfAbsent(key, index);
            }
        }
    }

    /** Indexes the borders of the workbook that a region could take, by their lines. */
    private void indexBorders() {
        var all = styles.getBorders();
        for (var index = 0; index < all.size(); index++) {
            var key = BorderKey.of(all.get(index));
            if (key != null) {
                borders.putIfAbsent(key, index);
            }
        }
    }

    /**
     * The lines at the edges of a region a cell stands at.
     *
     * @param top    the line above the cell, or {@code null} for none
     * @param bottom the line below the cell, or {@code null} for none
     * @param left   the line on the left of the cell, or {@code null} for none
     */
    record Edges(@Nullable Line top, @Nullable Line bottom, @Nullable Line left) {
    }

    /**
     * What is kept from the cell format a cell had.
     *
     * @param numberFormat the number format
     * @param indent       the indent
     * @param wrap         whether the text wraps
     * @param rotation     the text rotation
     */
    private record Kept(long numberFormat, long indent, boolean wrap, long rotation) {
    }

    /** A cell format of a role: its style, the lines at its edges and what is kept from the cell. */
    private record Format(TableStyle style, Edges edges, Kept kept) {
    }

    /**
     * A font a role could take: a theme font in a theme colour, neither underlined nor struck out.
     *
     * @param scheme the font of the theme, {@link STFontScheme#INT_MAJOR} or {@link STFontScheme#INT_MINOR}
     */
    private record FontKey(String name, double size, boolean bold, boolean italic, int scheme, ThemedColor colour) {

        /** The font, or {@code null} for a font no role takes. */
        static @Nullable FontKey of(CTFont font) {
            var plain = font.sizeOfUArray() == 0 && font.sizeOfStrikeArray() == 0 && font.sizeOfVertAlignArray() == 0
                    && font.sizeOfOutlineArray() == 0 && font.sizeOfShadowArray() == 0;
            var colour = font.sizeOfColorArray() > 0
                    ? ThemedColor.of(XSSFColor.from(font.getColorArray(0), null))
                    : null;
            if (!plain || colour == null || font.sizeOfNameArray() == 0 || font.sizeOfSzArray() == 0
                    || font.sizeOfSchemeArray() == 0) {
                return null;
            }
            return new FontKey(font.getNameArray(0).getVal(), font.getSzArray(0).getVal(),
                    font.sizeOfBArray() > 0 && font.getBArray(0).getVal(),
                    font.sizeOfIArray() > 0 && font.getIArray(0).getVal(),
                    font.getSchemeArray(0).getVal().intValue(), colour);
        }
    }

    /**
     * A fill a role could take.
     *
     * @param colour the theme colour of a solid fill, or {@code null} for no fill
     */
    private record FillKey(@Nullable ThemedColor colour) {

        /** The fill, or {@code null} for a fill no role takes. */
        static @Nullable FillKey of(CTFill fill) {
            if (!fill.isSetPatternFill()) {
                return null;
            }
            var pattern = fill.getPatternFill();
            if (!pattern.isSetPatternType() || pattern.getPatternType() == STPatternType.NONE) {
                return new FillKey(null);
            }
            if (pattern.getPatternType() != STPatternType.SOLID || !pattern.isSetFgColor()) {
                return null;
            }
            var colour = ThemedColor.of(XSSFColor.from(pattern.getFgColor(), null));
            return colour == null ? null : new FillKey(colour);
        }
    }

    /** A border a region could take: a line of a theme colour, or none, on each side. */
    private record BorderKey(@Nullable Line left, @Nullable Line right, @Nullable Line top, @Nullable Line bottom) {

        /**
         * The border, or {@code null} for a border no region takes. The colours are read as written: read through
         * the theme, POI would write the RGB of each theme colour beside it.
         */
        static @Nullable BorderKey of(XSSFCellBorder border) {
            var ct = border.getCTBorder();
            var sides = new CTBorderPr[]{ct.getLeft(), ct.getRight(), ct.getTop(), ct.getBottom()};
            var lines = new Line[sides.length];
            for (var at = 0; at < sides.length; at++) {
                if (isLine(sides[at])) {
                    lines[at] = lineOf(sides[at]);
                    if (lines[at] == null) {
                        return null;
                    }
                }
            }
            return isLine(ct.getDiagonal()) ? null : new BorderKey(lines[0], lines[1], lines[2], lines[3]);
        }

        private static boolean isLine(@Nullable CTBorderPr side) {
            return side != null && side.isSetStyle() && side.getStyle() != STBorderStyle.NONE;
        }

        /** The line of a side, or {@code null} for a line of a colour no role takes. */
        private static @Nullable Line lineOf(CTBorderPr side) {
            var colour = side.isSetColor() ? ThemedColor.of(XSSFColor.from(side.getColor(), null)) : null;
            return colour == null
                    ? null
                    : new Line(BorderStyle.valueOf((short) (side.getStyle().intValue() - 1)), colour);
        }
    }

    /** A cell format, by what it holds. */
    private record XfKey(long numberFormat, long font, long fill, long border, long style, boolean applyNumberFormat,
                         boolean applyFont, boolean applyFill, boolean applyBorder, boolean applyAlignment,
                         int horizontal, int vertical, long indent, boolean wrap, long rotation) {

        static XfKey of(CTXf xf) {
            var alignment = xf.isSetAlignment() ? xf.getAlignment() : null;
            var aligned = alignment != null;
            return new XfKey(xf.getNumFmtId(), xf.getFontId(), xf.getFillId(), xf.getBorderId(), xf.getXfId(),
                    xf.getApplyNumberFormat(), xf.getApplyFont(), xf.getApplyFill(), xf.getApplyBorder(),
                    xf.getApplyAlignment(),
                    aligned && alignment.isSetHorizontal() ? alignment.getHorizontal().intValue() : 0,
                    aligned && alignment.isSetVertical() ? alignment.getVertical().intValue() : 0,
                    aligned ? alignment.getIndent() : 0,
                    aligned && alignment.getWrapText(),
                    aligned && alignment.isSetTextRotation() ? alignment.getTextRotation().longValue() : 0);
        }
    }
}
