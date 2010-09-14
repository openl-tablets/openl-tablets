/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.table.ui.filters;

/**
 * @author snshor
 *
 */
public interface IColorFilter {
    
    public static final short[] BLACK = { 0, 0, 0 };
    public static final short[] WHITE = { 0xff, 0xff, 0xff };
    public static final short[] RED = { 0xff, 0, 0 };
    public static final short[] GREEN = { 0, 0xff, 0 };
    public static final short[] BLUE = { 0, 0, 0xff };
    public static final short[] YELLOW = { 0xff, 0xff, 0 };
    public static final short[] MAGENTA = { 0xff, 0, 0xff };
    public static final short[] CYAN = { 0, 0xff, 0xff };

    public static final String[] COLOR_NAMES = { "black", "white", "red", "green", "blue", "yellow", "cyan", "magenta", };
    public static final short[][] COLORS = { BLACK, WHITE, RED, GREEN, BLUE, YELLOW, CYAN, MAGENTA };

    short[] filterColor(short[] color);

}
