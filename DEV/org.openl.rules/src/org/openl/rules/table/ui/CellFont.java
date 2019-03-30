/**
 * Created Feb 27, 2007
 */
package org.openl.rules.table.ui;

/**
 * @author snshor
 *
 */
public class CellFont implements ICellFont {

    private short[] fontColor;

    private int size;

    private String name;

    private boolean italic;

    private boolean bold;
    
    private boolean underlined;
    
    private boolean strikeout;
    
    public CellFont(ICellFont cf) {
        if (cf == null) {
            name = "arial";
            size = 9;
            return;
        }

        fontColor = cf.getFontColor();

        size = cf.getSize();

        name = cf.getName();

        italic = cf.isItalic();
        bold = cf.isBold();
        underlined = cf.isUnderlined();
        strikeout = cf.isStrikeout();
    }

    @Override
    public short[] getFontColor() {
        return fontColor;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public boolean isBold() {
        return bold;
    }

    @Override
    public boolean isItalic() {
        return italic;
    }

    @Override
    public boolean isStrikeout() {
        return strikeout;
    }

    @Override
    public boolean isUnderlined() {
        return underlined;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public void setFontColor(short[] fontColor) {
        this.fontColor = fontColor;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setStrikeout(boolean strikeout) {
        this.strikeout = strikeout;
    }

    public void setUnderlined(boolean underlined) {
        this.underlined = underlined;
    }

}
