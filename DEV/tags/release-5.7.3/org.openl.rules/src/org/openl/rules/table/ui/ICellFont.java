package org.openl.rules.table.ui;

/**
 * 
 * @author DLiauchuk Temporary we copy some POI constants that we are using, we will provide more
 *         complicated mapping once we start using other libraries, if ever
 *
 */
public interface ICellFont {
    
    /**
     * not underlined
     */

    byte U_NONE = 0;
    
    /**
     * single (normal) underline
     */

    byte U_SINGLE = 1;

    /**
     * double underlined
     */

    byte U_DOUBLE = 2;
    
    /**
     * Normal boldness (not bold)
     */

    short BOLDWEIGHT_NORMAL = 0x190;
    
    /**
     * Bold boldness (bold)
     */

    short BOLDWEIGHT_BOLD = 0x2bc;

    short[] getFontColor();

    String getName();

    int getSize();

    boolean isBold();

    boolean isItalic();

    boolean isStrikeout();

    boolean isUnderlined();

}
