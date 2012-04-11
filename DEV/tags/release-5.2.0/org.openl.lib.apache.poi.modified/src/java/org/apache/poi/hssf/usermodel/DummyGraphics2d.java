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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.awt.font.GlyphVector;
import java.awt.font.FontRenderContext;
import java.util.Map;
import java.text.AttributedCharacterIterator;

public class DummyGraphics2d extends Graphics2D {
    BufferedImage img;
    private Graphics2D g2D;

    public DummyGraphics2d() {
        img = new BufferedImage(1000, 1000, 2);
        g2D = (Graphics2D) img.getGraphics();
    }

    @Override
    public void addRenderingHints(Map hints) {
        System.out.println("addRenderingHinds(Map):");
        System.out.println("  hints = " + hints);
        g2D.addRenderingHints(hints);
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
        System.out.println("clearRect(int,int,int,int):");
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        g2D.clearRect(x, y, width, height);
    }

    @Override
    public void clip(Shape s) {
        System.out.println("clip(Shape):");
        System.out.println("  s = " + s);
        g2D.clip(s);
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        System.out.println("clipRect(int, int, int, int):");
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        g2D.clipRect(x, y, width, height);
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        System.out.println("copyArea(int,int,int,int):");
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        g2D.copyArea(x, y, width, height, dx, dy);
    }

    @Override
    public Graphics create() {
        System.out.println("create():");
        return g2D.create();
    }

    @Override
    public Graphics create(int x, int y, int width, int height) {
        System.out.println("create(int,int,int,int):");
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        return g2D.create(x, y, width, height);
    }

    @Override
    public void dispose() {
        System.out.println("dispose():");
        g2D.dispose();
    }

    @Override
    public void draw(Shape s) {
        System.out.println("draw(Shape):");
        System.out.println("s = " + s);
        g2D.draw(s);
    }

    // public void drawString(AttributedCharacterIterator iterator,
    // int x, int y)
    // {
    // g2D.drawString( iterator, x, y );
    // }

    @Override
    public void draw3DRect(int x, int y, int width, int height, boolean raised) {
        System.out.println("draw3DRect(int,int,int,int,boolean):");
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        System.out.println("raised = " + raised);
        g2D.draw3DRect(x, y, width, height, raised);
    }

    // public void drawString(String str, int x, int y)
    // {
    // g2D.drawString( str, x, y );
    // }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        System.out.println("drawArc(int,int,int,int,int,int):");
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        System.out.println("startAngle = " + startAngle);
        System.out.println("arcAngle = " + arcAngle);
        g2D.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    // public void fill3DRect(int x, int y, int width, int height,
    // boolean raised) {
    // g2D.fill3DRect( x, y, width, height, raised );
    // }

    @Override
    public void drawBytes(byte data[], int offset, int length, int x, int y) {
        System.out.println("drawBytes(byte[],int,int,int,int):");
        System.out.println("data = " + data);
        System.out.println("offset = " + offset);
        System.out.println("length = " + length);
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        g2D.drawBytes(data, offset, length, x, y);
    }

    @Override
    public void drawChars(char data[], int offset, int length, int x, int y) {
        System.out.println("drawChars(data,int,int,int,int):");
        System.out.println("data = " + data);
        System.out.println("offset = " + offset);
        System.out.println("length = " + length);
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        g2D.drawChars(data, offset, length, x, y);
    }

    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {
        System.out.println("drawGlyphVector(GlyphVector, float, float):");
        System.out.println("g = " + g);
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        g2D.drawGlyphVector(g, x, y);
    }

    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
        System.out.println("drawImage(BufferedImage, BufferedImageOp, x, y):");
        System.out.println("img = " + img);
        System.out.println("op = " + op);
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        g2D.drawImage(img, op, x, y);
    }

    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        System.out.println("drawImage(Image,AfflineTransform,ImageObserver):");
        System.out.println("img = " + img);
        System.out.println("xform = " + xform);
        System.out.println("obs = " + obs);
        return g2D.drawImage(img, xform, obs);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        System.out.println("drawImage(Image,int,int,Color,ImageObserver):");
        System.out.println("img = " + img);
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("bgcolor = " + bgcolor);
        System.out.println("observer = " + observer);
        return g2D.drawImage(img, x, y, bgcolor, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        System.out.println("drawImage(Image,int,int,observer):");
        System.out.println("img = " + img);
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("observer = " + observer);
        return g2D.drawImage(img, x, y, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        System.out.println("drawImage(Image,int,int,int,int,Color,ImageObserver):");
        System.out.println("img = " + img);
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        System.out.println("bgcolor = " + bgcolor);
        System.out.println("observer = " + observer);
        return g2D.drawImage(img, x, y, width, height, bgcolor, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        System.out.println("drawImage(Image,int,int,width,height,observer):");
        System.out.println("img = " + img);
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        System.out.println("observer = " + observer);
        return g2D.drawImage(img, x, y, width, height, observer);
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
            Color bgcolor, ImageObserver observer) {
        System.out.println("drawImage(Image,int,int,int,int,int,int,int,int,Color,ImageObserver):");
        System.out.println("img = " + img);
        System.out.println("dx1 = " + dx1);
        System.out.println("dy1 = " + dy1);
        System.out.println("dx2 = " + dx2);
        System.out.println("dy2 = " + dy2);
        System.out.println("sx1 = " + sx1);
        System.out.println("sy1 = " + sy1);
        System.out.println("sx2 = " + sx2);
        System.out.println("sy2 = " + sy2);
        System.out.println("bgcolor = " + bgcolor);
        System.out.println("observer = " + observer);
        return g2D.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
            ImageObserver observer) {
        System.out.println("drawImage(Image,int,int,int,int,int,int,int,int,ImageObserver):");
        System.out.println("img = " + img);
        System.out.println("dx1 = " + dx1);
        System.out.println("dy1 = " + dy1);
        System.out.println("dx2 = " + dx2);
        System.out.println("dy2 = " + dy2);
        System.out.println("sx1 = " + sx1);
        System.out.println("sy1 = " + sy1);
        System.out.println("sx2 = " + sx2);
        System.out.println("sy2 = " + sy2);
        System.out.println("observer = " + observer);
        return g2D.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        System.out.println("drawLine(int,int,int,int):");
        System.out.println("x1 = " + x1);
        System.out.println("y1 = " + y1);
        System.out.println("x2 = " + x2);
        System.out.println("y2 = " + y2);
        g2D.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        System.out.println("drawOval(int,int,int,int):");
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        g2D.drawOval(x, y, width, height);
    }

    @Override
    public void drawPolygon(int xPoints[], int yPoints[], int nPoints) {
        System.out.println("drawPolygon(int[],int[],int):");
        System.out.println("xPoints = " + xPoints);
        System.out.println("yPoints = " + yPoints);
        System.out.println("nPoints = " + nPoints);
        g2D.drawPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public void drawPolygon(Polygon p) {
        System.out.println("drawPolygon(Polygon):");
        System.out.println("p = " + p);
        g2D.drawPolygon(p);
    }

    @Override
    public void drawPolyline(int xPoints[], int yPoints[], int nPoints) {
        System.out.println("drawPolyline(int[],int[],int):");
        System.out.println("xPoints = " + xPoints);
        System.out.println("yPoints = " + yPoints);
        System.out.println("nPoints = " + nPoints);
        g2D.drawPolyline(xPoints, yPoints, nPoints);
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        System.out.println("drawRect(int,int,int,int):");
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        g2D.drawRect(x, y, width, height);
    }

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
        System.out.println("drawRenderableImage(RenderableImage, AfflineTransform):");
        System.out.println("img = " + img);
        System.out.println("xform = " + xform);
        g2D.drawRenderableImage(img, xform);
    }

    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
        System.out.println("drawRenderedImage(RenderedImage, AffineTransform):");
        System.out.println("img = " + img);
        System.out.println("xform = " + xform);
        g2D.drawRenderedImage(img, xform);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        System.out.println("drawRoundRect(int,int,int,int,int,int):");
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        System.out.println("arcWidth = " + arcWidth);
        System.out.println("arcHeight = " + arcHeight);
        g2D.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
        System.out.println("drawString(AttributedCharacterIterator):");
        System.out.println("iterator = " + iterator);
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        g2D.drawString(iterator, x, y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        System.out.println("drawString(AttributedCharacterIterator,int,int):");
        System.out.println("iterator = " + iterator);
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        g2D.drawString(iterator, x, y);
    }

    @Override
    public void drawString(String s, float x, float y) {
        System.out.println("drawString(s,x,y):");
        System.out.println("s = " + s);
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        g2D.drawString(s, x, y);
    }

    // public void translate(int x, int y)
    // {
    // g2D.translate( x, y );
    // }

    @Override
    public void drawString(String str, int x, int y) {
        System.out.println("drawString(str,int,int):");
        System.out.println("str = " + str);
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        g2D.drawString(str, x, y);
    }

    @Override
    public void fill(Shape s) {
        System.out.println("fill(Shape):");
        System.out.println("s = " + s);
        g2D.fill(s);
    }

    @Override
    public void fill3DRect(int x, int y, int width, int height, boolean raised) {
        System.out.println("fill3DRect(int,int,int,int,boolean):");
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        System.out.println("raised = " + raised);
        g2D.fill3DRect(x, y, width, height, raised);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        System.out.println("fillArc(int,int,int,int,int,int):");
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        System.out.println("startAngle = " + startAngle);
        System.out.println("arcAngle = " + arcAngle);
        g2D.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        System.out.println("fillOval(int,int,int,int):");
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        g2D.fillOval(x, y, width, height);
    }

    @Override
    public void fillPolygon(int xPoints[], int yPoints[], int nPoints) {
        System.out.println("fillPolygon(int[],int[],int):");
        System.out.println("xPoints = " + xPoints);
        System.out.println("yPoints = " + yPoints);
        System.out.println("nPoints = " + nPoints);
        g2D.fillPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public void fillPolygon(Polygon p) {
        System.out.println("fillPolygon(Polygon):");
        System.out.println("p = " + p);
        g2D.fillPolygon(p);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        System.out.println("fillRect(int,int,int,int):");
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        g2D.fillRect(x, y, width, height);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        System.out.println("fillRoundRect(int,int,int,int,int,int):");
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        g2D.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void finalize() {
        System.out.println("finalize():");
        g2D.finalize();
    }

    @Override
    public Color getBackground() {
        System.out.println("getBackground():");
        return g2D.getBackground();
    }

    @Override
    public Shape getClip() {
        System.out.println("getClip():");
        return g2D.getClip();
    }

    @Override
    public Rectangle getClipBounds() {
        System.out.println("getClipBounds():");
        return g2D.getClipBounds();
    }

    @Override
    public Rectangle getClipBounds(Rectangle r) {
        System.out.println("getClipBounds(Rectangle):");
        System.out.println("r = " + r);
        return g2D.getClipBounds(r);
    }

    @Override
    public Rectangle getClipRect() {
        System.out.println("getClipRect():");
        return g2D.getClipRect();
    }

    @Override
    public Color getColor() {
        System.out.println("getColor():");
        return g2D.getColor();
    }

    @Override
    public Composite getComposite() {
        System.out.println("getComposite():");
        return g2D.getComposite();
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        System.out.println("getDeviceConfiguration():");
        return g2D.getDeviceConfiguration();
    }

    @Override
    public Font getFont() {
        System.out.println("getFont():");
        return g2D.getFont();
    }

    @Override
    public FontMetrics getFontMetrics() {
        System.out.println("getFontMetrics():");
        return g2D.getFontMetrics();
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        System.out.println("getFontMetrics():");
        return g2D.getFontMetrics(f);
    }

    @Override
    public FontRenderContext getFontRenderContext() {
        System.out.println("getFontRenderContext():");
        return g2D.getFontRenderContext();
    }

    @Override
    public Paint getPaint() {
        System.out.println("getPaint():");
        return g2D.getPaint();
    }

    @Override
    public Object getRenderingHint(RenderingHints.Key hintKey) {
        System.out.println("getRenderingHint(RenderingHints.Key):");
        System.out.println("hintKey = " + hintKey);
        return g2D.getRenderingHint(hintKey);
    }

    @Override
    public RenderingHints getRenderingHints() {
        System.out.println("getRenderingHints():");
        return g2D.getRenderingHints();
    }

    @Override
    public Stroke getStroke() {
        System.out.println("getStroke():");
        return g2D.getStroke();
    }

    @Override
    public AffineTransform getTransform() {
        System.out.println("getTransform():");
        return g2D.getTransform();
    }

    @Override
    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        System.out.println("hit(Rectangle, Shape, onStroke):");
        System.out.println("rect = " + rect);
        System.out.println("s = " + s);
        System.out.println("onStroke = " + onStroke);
        return g2D.hit(rect, s, onStroke);
    }

    @Override
    public boolean hitClip(int x, int y, int width, int height) {
        System.out.println("hitClip(int,int,int,int):");
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        return g2D.hitClip(x, y, width, height);
    }

    @Override
    public void rotate(double theta) {
        System.out.println("rotate(theta):");
        System.out.println("theta = " + theta);
        g2D.rotate(theta);
    }

    @Override
    public void rotate(double theta, double x, double y) {
        System.out.println("rotate(double,double,double):");
        System.out.println("theta = " + theta);
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        g2D.rotate(theta, x, y);
    }

    @Override
    public void scale(double sx, double sy) {
        System.out.println("scale(double,double):");
        System.out.println("sx = " + sx);
        System.out.println("sy");
        g2D.scale(sx, sy);
    }

    @Override
    public void setBackground(Color color) {
        System.out.println("setBackground(Color):");
        System.out.println("color = " + color);
        g2D.setBackground(color);
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        System.out.println("setClip(int,int,int,int):");
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
        g2D.setClip(x, y, width, height);
    }

    @Override
    public void setClip(Shape clip) {
        System.out.println("setClip(Shape):");
        System.out.println("clip = " + clip);
        g2D.setClip(clip);
    }

    @Override
    public void setColor(Color c) {
        System.out.println("setColor():");
        System.out.println("c = " + c);
        g2D.setColor(c);
    }

    @Override
    public void setComposite(Composite comp) {
        System.out.println("setComposite(Composite):");
        System.out.println("comp = " + comp);
        g2D.setComposite(comp);
    }

    @Override
    public void setFont(Font font) {
        System.out.println("setFont(Font):");
        System.out.println("font = " + font);
        g2D.setFont(font);
    }

    @Override
    public void setPaint(Paint paint) {
        System.out.println("setPain(Paint):");
        System.out.println("paint = " + paint);
        g2D.setPaint(paint);
    }

    @Override
    public void setPaintMode() {
        System.out.println("setPaintMode():");
        g2D.setPaintMode();
    }

    @Override
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
        System.out.println("setRenderingHint(RenderingHints.Key, Object):");
        System.out.println("hintKey = " + hintKey);
        System.out.println("hintValue = " + hintValue);
        g2D.setRenderingHint(hintKey, hintValue);
    }

    @Override
    public void setRenderingHints(Map hints) {
        System.out.println("setRenderingHints(Map):");
        System.out.println("hints = " + hints);
        g2D.setRenderingHints(hints);
    }

    @Override
    public void setStroke(Stroke s) {
        System.out.println("setStroke(Stoke):");
        System.out.println("s = " + s);
        g2D.setStroke(s);
    }

    @Override
    public void setTransform(AffineTransform Tx) {
        System.out.println("setTransform():");
        System.out.println("Tx = " + Tx);
        g2D.setTransform(Tx);
    }

    @Override
    public void setXORMode(Color c1) {
        System.out.println("setXORMode(Color):");
        System.out.println("c1 = " + c1);
        g2D.setXORMode(c1);
    }

    @Override
    public void shear(double shx, double shy) {
        System.out.println("shear(shx, dhy):");
        System.out.println("shx = " + shx);
        System.out.println("shy = " + shy);
        g2D.shear(shx, shy);
    }

    @Override
    public String toString() {
        System.out.println("toString():");
        return g2D.toString();
    }

    @Override
    public void transform(AffineTransform Tx) {
        System.out.println("transform(AffineTransform):");
        System.out.println("Tx = " + Tx);
        g2D.transform(Tx);
    }

    @Override
    public void translate(double tx, double ty) {
        System.out.println("translate(double, double):");
        System.out.println("tx = " + tx);
        System.out.println("ty = " + ty);
        g2D.translate(tx, ty);
    }

    @Override
    public void translate(int x, int y) {
        System.out.println("translate(int,int):");
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        g2D.translate(x, y);
    }
}
