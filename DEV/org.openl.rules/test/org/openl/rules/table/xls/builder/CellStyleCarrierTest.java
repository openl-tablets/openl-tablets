package org.openl.rules.table.xls.builder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.ReadingOrder;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.XlsCellStyle;

/**
 * A workbook can give the colour numbers a meaning of its own, through a palette it carries. A style
 * written in one workbook and read in another therefore shows different colours, so a style carried into
 * another workbook keeps the colours it is seen with rather than the numbers standing for them.
 */
class CellStyleCarrierTest {

    private static final short FILL = 21;
    private static final short BORDER = 12;
    private static final short FONT = 10;

    private static final short[] PALE_BLUE = {0xdc, 0xe6, 0xf2};
    private static final short[] KHAKI = {0xc4, 0xbd, 0x97};
    private static final short[] OLIVE = {0x4a, 0x45, 0x2a};

    /**
     * A workbook whose palette gives the three indices the colours above, holding one style that names them.
     *
     * <p>It is written out and read back, because a palette takes effect on the workbook that is read.
     */
    private static Workbook paletted() throws IOException {
        var workbook = new XSSFWorkbook();
        var palette = workbook.getStylesSource().getCTStylesheet().addNewColors().addNewIndexedColors();
        for (var i = 0; i < 64; i++) {
            palette.addNewRgbColor().setRgb(rgb(colourAt(i)));
        }

        var font = workbook.createFont();
        font.setFontName("Palatino Linotype");
        font.setColor(FONT);

        var style = workbook.createCellStyle();
        style.setFillForegroundColor(FILL);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(BORDER);
        style.setFont(font);

        var saved = new ByteArrayOutputStream();
        workbook.write(saved);
        return new XSSFWorkbook(new ByteArrayInputStream(saved.toByteArray()));
    }

    private static short[] colourAt(int index) {
        return switch (index) {
            case FILL -> PALE_BLUE;
            case BORDER -> KHAKI;
            case FONT -> OLIVE;
            default -> new short[]{0, 0, 0};
        };
    }

    private static byte[] rgb(short[] colour) {
        return new byte[]{(byte) colour[0], (byte) colour[1], (byte) colour[2]};
    }

    private static XlsCellStyle theStyleOf(Workbook workbook) {
        return new XlsCellStyle(workbook.getCellStyleAt(workbook.getNumCellStyles() - 1), workbook);
    }

    private static short[] fontColourOf(CellStyle style, Workbook workbook) {
        return PoiExcelHelper.getFontColor(workbook.getFontAt(style.getFontIndex()), workbook);
    }

    @Test
    void aCarriedStyleKeepsTheColoursItIsSeenWith() throws IOException {
        var source = paletted();
        var destination = new XSSFWorkbook();

        var carried = new CellStyleCarrier(destination).carry(theStyleOf(source));

        var seen = new XlsCellStyle(carried, destination);
        assertArrayEquals(PALE_BLUE, seen.getFillForegroundColor(), "the fill");
        assertArrayEquals(KHAKI, seen.getBorderRGB()[2], "the bottom border");
        assertArrayEquals(OLIVE, fontColourOf(carried, destination), "the font");
    }

    @Test
    void aCarriedStyleKeepsWhatItLooksLikeBesidesItsColours() throws IOException {
        var source = paletted();
        var destination = new XSSFWorkbook();

        var carried = new CellStyleCarrier(destination).carry(theStyleOf(source));

        assertEquals(FillPatternType.SOLID_FOREGROUND, carried.getFillPattern());
        assertEquals(BorderStyle.THIN, carried.getBorderBottom());
        assertEquals("Palatino Linotype", destination.getFontAt(carried.getFontIndex()).getFontName());
    }

    /**
     * A workbook of the older format keeps a palette of its own as well, and a style of one cannot be
     * cloned into a workbook of the newer format at all — so its colours are the only thing to go by.
     */
    @Test
    void aStyleOfAnOlderWorkbookKeepsItsColoursToo() {
        var source = new HSSFWorkbook();
        source.getCustomPalette().setColorAtIndex(FILL, (byte) PALE_BLUE[0], (byte) PALE_BLUE[1], (byte) PALE_BLUE[2]);
        var style = source.createCellStyle();
        style.setFillForegroundColor(FILL);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        var destination = new XSSFWorkbook();
        var carried = new CellStyleCarrier(destination).carry(new XlsCellStyle(style, source));

        assertArrayEquals(PALE_BLUE, new XlsCellStyle(carried, destination).getFillForegroundColor());
    }

    /**
     * The workbook a style was carried into stays readable.
     *
     * <p>Two things make it unreadable, and a spreadsheet application reads either as damaged content: a
     * style copied from another workbook is written back under the namespace of the workbook it came from,
     * and a colour is a value of four bytes, so three bytes of one is not a colour a workbook can hold.
     */
    @Test
    void theWorkbookAStyleIsCarriedIntoIsWrittenAsItsOwn() throws IOException {
        var source = paletted();
        var destination = new XSSFWorkbook();
        destination.createSheet("Module").createRow(0).createCell(0)
                .setCellStyle(new CellStyleCarrier(destination).carry(theStyleOf(source)));

        var styles = WrittenWorkbook.stylesOf(destination);
        assertEquals(List.of(), WrittenWorkbook.prefixedElements(styles), "elements under a namespace prefix");
        assertEquals(List.of(), WrittenWorkbook.coloursShortOfAValue(styles), "colours short of a value");
    }

    /**
     * A style is carried whatever number format it names.
     *
     * <p>A format is named by an index of its own workbook, and a workbook spells out only the ones it
     * defines: a style naming an index it does not spell out has no format to carry.
     */
    @Test
    void aStyleNamingAFormatItsWorkbookDoesNotSpellOutIsCarriedAllTheSame() {
        var source = new XSSFWorkbook();
        var style = source.createCellStyle();
        style.setDataFormat((short) 200);
        assertNull(style.getDataFormatString(), "a format the workbook does not spell out");

        var carried = new CellStyleCarrier(new HSSFWorkbook()).carry(new XlsCellStyle(style, source));

        assertEquals("General", carried.getDataFormatString());
    }

    /**
     * A style laying its text out right to left is carried laying it out the same way.
     */
    @Test
    void aCarriedStyleKeepsTheDirectionItLaysTextOutIn() {
        var source = new XSSFWorkbook();
        var style = source.createCellStyle();
        style.setReadingOrder(ReadingOrder.RIGHT_TO_LEFT);

        var destination = new XSSFWorkbook();
        var carried = (XSSFCellStyle) new CellStyleCarrier(destination).carry(new XlsCellStyle(style, source));

        assertEquals(ReadingOrder.RIGHT_TO_LEFT, carried.getReadingOrder());
    }

    /**
     * A font is carried at the size it is written in, which can be half a point.
     */
    @Test
    void aCarriedFontKeepsASizeOfHalfAPoint() {
        var source = new XSSFWorkbook();
        var font = source.createFont();
        font.setFontHeight((short) 230);
        var style = source.createCellStyle();
        style.setFont(font);

        var destination = new XSSFWorkbook();
        var carried = new CellStyleCarrier(destination).carry(new XlsCellStyle(style, source));

        assertEquals(230, destination.getFontAt(carried.getFontIndex()).getFontHeight());
    }

    @Test
    void carryingTheSameStyleAgainAddsNothingToTheWorkbook() throws IOException {
        var source = paletted();
        var destination = new XSSFWorkbook();

        new CellStyleCarrier(destination).carry(theStyleOf(source));
        var styles = destination.getNumCellStyles();
        var fonts = destination.getNumberOfFonts();
        var again = new CellStyleCarrier(destination).carry(theStyleOf(source));

        assertEquals(styles, destination.getNumCellStyles(), "the style already carried is the one used");
        assertEquals(fonts, destination.getNumberOfFonts(), "the font already carried is the one used");
        assertArrayEquals(PALE_BLUE, new XlsCellStyle(again, destination).getFillForegroundColor());
    }
}
