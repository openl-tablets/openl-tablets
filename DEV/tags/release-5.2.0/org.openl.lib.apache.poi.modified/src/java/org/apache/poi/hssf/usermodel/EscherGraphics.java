/* ====================================================================
   Copyright 2004   Apache Software Foundation

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hssf.usermodel;

import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.hssf.util.HSSFColor;

import java.awt.*;
import java.awt.image.ImageObserver;
import java.text.AttributedCharacterIterator;

/**
 * Translates Graphics calls into escher calls. The translation is lossy so many
 * features are not supported and some just aren't implemented yet. If in doubt
 * test the specific calls you wish to make. Graphics calls are always performed
 * into an EscherGroup so one will need to be created.
 * <p>
 * <b>Important:</b> <blockquote> One important concept worth considering is
 * that of font size. One of the difficulties in converting Graphics calls into
 * escher drawing calls is that Excel does not have the concept of absolute
 * pixel positions. It measures it's cell widths in 'characters' and the cell
 * heights in points. Unfortunately it's not defined exactly what a type of
 * character it's measuring. Presumably this is due to the fact that the Excel
 * will be using different fonts on different platforms or even within the same
 * platform.
 * <p>
 * Because of this constraint we've had to calculate the verticalPointsPerPixel.
 * This the amount the font should be scaled by when you issue commands such as
 * drawString(). A good way to calculate this is to use the follow formula:
 * <p>
 *
 * <pre>
 * multipler = groupHeightInPoints / heightOfGroup
 * </pre>
 *
 * <p>
 * The height of the group is calculated fairly simply by calculating the
 * difference between the y coordinates of the bounding box of the shape. The
 * height of the group can be calculated by using a convenience called
 * <code>HSSFClientAnchor.getAnchorHeightInPoints()</code>. </blockquote>
 *
 * @author Glen Stampoultzis (glens at apache.org)
 */
public class EscherGraphics extends Graphics {
    private static POILogger logger = POILogFactory.getLogger(EscherGraphics.class);
    private HSSFShapeGroup escherGroup;
    private HSSFWorkbook workbook;
    private float verticalPointsPerPixel = 1.0f;
    private float verticalPixelsPerPoint;
    private Color foreground;
    private Color background = Color.white;
    private Font font;

    /**
     * Construct an escher graphics object.
     *
     * @param escherGroup The escher group to write the graphics calls into.
     * @param workbook The workbook we are using.
     * @param forecolor The foreground color to use as default.
     * @param verticalPointsPerPixel The font multiplier. (See class description
     *            for information on how this works.).
     */
    public EscherGraphics(HSSFShapeGroup escherGroup, HSSFWorkbook workbook, Color forecolor,
            float verticalPointsPerPixel) {
        this.escherGroup = escherGroup;
        this.workbook = workbook;
        this.verticalPointsPerPixel = verticalPointsPerPixel;
        verticalPixelsPerPoint = 1 / verticalPointsPerPixel;
        font = new Font("Arial", 0, 10);
        foreground = forecolor;
        // background = backcolor;
    }

    /**
     * Constructs an escher graphics object.
     *
     * @param escherGroup The escher group to write the graphics calls into.
     * @param workbook The workbook we are using.
     * @param foreground The foreground color to use as default.
     * @param verticalPointsPerPixel The font multiplier. (See class description
     *            for information on how this works.).
     * @param font The font to use.
     */
    EscherGraphics(HSSFShapeGroup escherGroup, HSSFWorkbook workbook, Color foreground, Font font,
            float verticalPointsPerPixel) {
        this.escherGroup = escherGroup;
        this.workbook = workbook;
        this.foreground = foreground;
        // this.background = background;
        this.font = font;
        this.verticalPointsPerPixel = verticalPointsPerPixel;
        verticalPixelsPerPoint = 1 / verticalPointsPerPixel;
    }

    private int[] addToAll(int[] values, int amount) {
        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i] + amount;
        }
        return result;
    }

    /**
     * Constructs an escher graphics object.
     *
     * @param escherGroup The escher group to write the graphics calls into.
     * @param workbook The workbook we are using.
     * @param forecolor The default foreground color.
     */
    // public EscherGraphics( HSSFShapeGroup escherGroup, HSSFWorkbook workbook,
    // Color forecolor)
    // {
    // this(escherGroup, workbook, forecolor, 1.0f);
    // }

    @Override
    public void clearRect(int x, int y, int width, int height) {
        Color color = foreground;
        setColor(background);
        fillRect(x, y, width, height);
        setColor(color);
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        if (logger.check(POILogger.WARN)) {
            logger.log(POILogger.WARN, "clipRect not supported");
        }
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        if (logger.check(POILogger.WARN)) {
            logger.log(POILogger.WARN, "copyArea not supported");
        }
    }

    @Override
    public Graphics create() {
        EscherGraphics g = new EscherGraphics(escherGroup, workbook, foreground, font, verticalPointsPerPixel);
        return g;
    }

    @Override
    public void dispose() {
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        if (logger.check(POILogger.WARN)) {
            logger.log(POILogger.WARN, "drawArc not supported");
        }
    }

    @Override
    public boolean drawImage(Image image, int i, int j, Color color, ImageObserver imageobserver) {
        return drawImage(image, i, j, image.getWidth(imageobserver), image.getHeight(imageobserver), color,
                imageobserver);
    }

    @Override
    public boolean drawImage(Image image, int i, int j, ImageObserver imageobserver) {
        return drawImage(image, i, j, image.getWidth(imageobserver), image.getHeight(imageobserver), imageobserver);
    }

    @Override
    public boolean drawImage(Image image, int i, int j, int k, int l, Color color, ImageObserver imageobserver) {
        return drawImage(image, i, j, i + k, j + l, 0, 0, image.getWidth(imageobserver),
                image.getHeight(imageobserver), color, imageobserver);
    }

    @Override
    public boolean drawImage(Image image, int i, int j, int k, int l, ImageObserver imageobserver) {
        return drawImage(image, i, j, i + k, j + l, 0, 0, image.getWidth(imageobserver),
                image.getHeight(imageobserver), imageobserver);
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
            Color bgcolor, ImageObserver observer) {
        if (logger.check(POILogger.WARN)) {
            logger.log(POILogger.WARN, "drawImage not supported");
        }

        return true;
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
            ImageObserver observer) {
        if (logger.check(POILogger.WARN)) {
            logger.log(POILogger.WARN, "drawImage not supported");
        }
        return true;
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        HSSFSimpleShape shape = escherGroup.createShape(new HSSFChildAnchor(x1, y1, x2, y2));
        shape.setShapeType(HSSFSimpleShape.OBJECT_TYPE_LINE);
        shape.setLineWidth(0);
        shape.setLineStyleColor(foreground.getRed(), foreground.getGreen(), foreground.getBlue());
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        HSSFSimpleShape shape = escherGroup.createShape(new HSSFChildAnchor(x, y, x + width, y + height));
        shape.setShapeType(HSSFSimpleShape.OBJECT_TYPE_OVAL);
        shape.setLineWidth(0);
        shape.setLineStyleColor(foreground.getRed(), foreground.getGreen(), foreground.getBlue());
        shape.setNoFill(true);
    }

    @Override
    public void drawPolygon(int xPoints[], int yPoints[], int nPoints) {
        int right = findBiggest(xPoints);
        int bottom = findBiggest(yPoints);
        int left = findSmallest(xPoints);
        int top = findSmallest(yPoints);
        HSSFPolygon shape = escherGroup.createPolygon(new HSSFChildAnchor(left, top, right, bottom));
        shape.setPolygonDrawArea(right - left, bottom - top);
        shape.setPoints(addToAll(xPoints, -left), addToAll(yPoints, -top));
        shape.setLineStyleColor(foreground.getRed(), foreground.getGreen(), foreground.getBlue());
        shape.setLineWidth(0);
        shape.setNoFill(true);
    }

    @Override
    public void drawPolyline(int xPoints[], int yPoints[], int nPoints) {
        if (logger.check(POILogger.WARN)) {
            logger.log(POILogger.WARN, "drawPolyline not supported");
        }
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        if (logger.check(POILogger.WARN)) {
            logger.log(POILogger.WARN, "drawRect not supported");
        }
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        if (logger.check(POILogger.WARN)) {
            logger.log(POILogger.WARN, "drawRoundRect not supported");
        }
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        if (logger.check(POILogger.WARN)) {
            logger.log(POILogger.WARN, "drawString not supported");
        }
    }

    @Override
    public void drawString(String str, int x, int y) {
        if (str == null || str.equals("")) {
            return;
        }

        Font excelFont = font;
        if (font.getName().equals("SansSerif")) {
            excelFont = new Font("Arial", font.getStyle(), (int) (font.getSize() / verticalPixelsPerPoint));
        } else {
            excelFont = new Font(font.getName(), font.getStyle(), (int) (font.getSize() / verticalPixelsPerPoint));
        }
        FontDetails d = StaticFontMetrics.getFontDetails(excelFont);
        int width = ((d.getStringWidth(str) * 8) + 12);
        int height = (int) ((font.getSize() / verticalPixelsPerPoint) + 6) * 2;
        y -= (font.getSize() / verticalPixelsPerPoint) + 2 * verticalPixelsPerPoint; // we
                                                                                        // want
                                                                                        // to
                                                                                        // draw
                                                                                        // the
                                                                                        // shape
                                                                                        // from
                                                                                        // the
                                                                                        // top-left
        HSSFTextbox textbox = escherGroup.createTextbox(new HSSFChildAnchor(x, y, x + width, y + height));
        textbox.setNoFill(true);
        textbox.setLineStyle(HSSFShape.LINESTYLE_NONE);
        HSSFRichTextString s = new HSSFRichTextString(str);
        HSSFFont hssfFont = matchFont(excelFont);
        s.applyFont(hssfFont);
        textbox.setString(s);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        if (logger.check(POILogger.WARN)) {
            logger.log(POILogger.WARN, "fillArc not supported");
        }
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        HSSFSimpleShape shape = escherGroup.createShape(new HSSFChildAnchor(x, y, x + width, y + height));
        shape.setShapeType(HSSFSimpleShape.OBJECT_TYPE_OVAL);
        shape.setLineStyle(HSSFShape.LINESTYLE_NONE);
        shape.setFillColor(foreground.getRed(), foreground.getGreen(), foreground.getBlue());
        shape.setLineStyleColor(foreground.getRed(), foreground.getGreen(), foreground.getBlue());
    }

    @Override
    public void fillPolygon(int xPoints[], int yPoints[], int nPoints) {
        int right = findBiggest(xPoints);
        int bottom = findBiggest(yPoints);
        int left = findSmallest(xPoints);
        int top = findSmallest(yPoints);
        HSSFPolygon shape = escherGroup.createPolygon(new HSSFChildAnchor(left, top, right, bottom));
        shape.setPolygonDrawArea(right - left, bottom - top);
        shape.setPoints(addToAll(xPoints, -left), addToAll(yPoints, -top));
        shape.setLineStyleColor(foreground.getRed(), foreground.getGreen(), foreground.getBlue());
        shape.setFillColor(foreground.getRed(), foreground.getGreen(), foreground.getBlue());
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        HSSFSimpleShape shape = escherGroup.createShape(new HSSFChildAnchor(x, y, x + width, y + height));
        shape.setShapeType(HSSFSimpleShape.OBJECT_TYPE_RECTANGLE);
        shape.setLineStyle(HSSFShape.LINESTYLE_NONE);
        shape.setFillColor(foreground.getRed(), foreground.getGreen(), foreground.getBlue());
        shape.setLineStyleColor(foreground.getRed(), foreground.getGreen(), foreground.getBlue());
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        if (logger.check(POILogger.WARN)) {
            logger.log(POILogger.WARN, "fillRoundRect not supported");
        }
    }

    private int findBiggest(int[] values) {
        int result = Integer.MIN_VALUE;
        for (int i = 0; i < values.length; i++) {
            if (values[i] > result) {
                result = values[i];
            }
        }
        return result;
    }

    private int findSmallest(int[] values) {
        int result = Integer.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            if (values[i] < result) {
                result = values[i];
            }
        }
        return result;
    }

    public Color getBackground() {
        return background;
    }

    @Override
    public Shape getClip() {
        return getClipBounds();
    }

    @Override
    public Rectangle getClipBounds() {
        return null;
    }

    @Override
    public Rectangle getClipRect() {
        return getClipBounds();
    }

    @Override
    public Color getColor() {
        return foreground;
    }

    @Override
    public Font getFont() {
        return font;
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        return Toolkit.getDefaultToolkit().getFontMetrics(f);
    }

    private HSSFFont matchFont(Font font) {
        HSSFColor hssfColor = workbook.getCustomPalette().findColor((byte) foreground.getRed(),
                (byte) foreground.getGreen(), (byte) foreground.getBlue());
        if (hssfColor == null) {
            hssfColor = workbook.getCustomPalette().findSimilarColor((byte) foreground.getRed(),
                    (byte) foreground.getGreen(), (byte) foreground.getBlue());
        }
        boolean bold = (font.getStyle() & Font.BOLD) != 0;
        boolean italic = (font.getStyle() & Font.ITALIC) != 0;
        HSSFFont hssfFont = workbook.findFont(bold ? HSSFFont.BOLDWEIGHT_BOLD : 0, hssfColor.getIndex(), (short) (font
                .getSize() * 20), font.getName(), italic, false, (short) 0, (byte) 0);
        if (hssfFont == null) {
            hssfFont = workbook.createFont();
            hssfFont.setBoldweight(bold ? HSSFFont.BOLDWEIGHT_BOLD : 0);
            hssfFont.setColor(hssfColor.getIndex());
            hssfFont.setFontHeight((short) (font.getSize() * 20));
            hssfFont.setFontName(font.getName());
            hssfFont.setItalic(italic);
            hssfFont.setStrikeout(false);
            hssfFont.setTypeOffset((short) 0);
            hssfFont.setUnderline((byte) 0);
        }

        return hssfFont;
    }

    public void setBackground(Color background) {
        this.background = background;
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        setClip(((new Rectangle(x, y, width, height))));
    }

    @Override
    public void setClip(Shape shape) {
        // ignore... not implemented
    }

    @Override
    public void setColor(Color color) {
        foreground = color;
    }

    @Override
    public void setFont(Font f) {
        font = f;
    }

    @Override
    public void setPaintMode() {
        if (logger.check(POILogger.WARN)) {
            logger.log(POILogger.WARN, "setPaintMode not supported");
        }
    }

    @Override
    public void setXORMode(Color color) {
        if (logger.check(POILogger.WARN)) {
            logger.log(POILogger.WARN, "setXORMode not supported");
        }
    }

    @Override
    public void translate(int x, int y) {
        if (logger.check(POILogger.WARN)) {
            logger.log(POILogger.WARN, "translate not supported");
        }
    }

}
