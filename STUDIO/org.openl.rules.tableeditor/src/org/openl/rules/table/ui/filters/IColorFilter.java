/*
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.table.ui.filters;

/**
 * @author snshor
 *
 */
public interface IColorFilter {

    short[] BLACK = { 0, 0, 0 };
    short[] WHITE = { 0xff, 0xff, 0xff };
    short[] RED = { 0xff, 0, 0 };
    short[] GREEN = { 0, 0xff, 0 };
    short[] BLUE = { 0, 0, 0xff };
    short[] YELLOW = { 0xff, 0xff, 0 };
    short[] MAGENTA = { 0xff, 0, 0xff };
    short[] CYAN = { 0, 0xff, 0xff };

    String[] COLOR_NAMES = { "black", "white", "red", "green", "blue", "yellow", "cyan", "magenta", };
    short[][] COLORS = { BLACK, WHITE, RED, GREEN, BLUE, YELLOW, CYAN, MAGENTA };

    short[] filterColor(short[] color);

}
