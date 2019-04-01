package org.openl.rules.table.ui;

/**
 *
 * @author DLiauchuk
 *
 */
public interface ICellFont {

    short[] getFontColor();

    String getName();

    int getSize();

    boolean isBold();

    boolean isItalic();

    boolean isStrikeout();

    boolean isUnderlined();

}
