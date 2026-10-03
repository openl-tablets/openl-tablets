/**
 * Created Apr 1, 2007
 */
package org.openl.rules.tableeditor.model.ui;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * The line a side of a cell is drawn with: its width, its style and its colour. A line does not change once made, so
 * cells may share one.
 */
@Slf4j
@Getter
@AllArgsConstructor
public class BorderStyle {

    private static final String SOLID = "solid";

    /**
     * The line of the grid: the side of a cell the workbook draws no border on.
     *
     * <p>Every such side holds this one line, which tells it apart from a border the workbook draws.
     */
    public static final BorderStyle NONE = new BorderStyle(1, SOLID, new short[]{0xBB, 0xBB, 0xDD});

    private final int width;
    private final String style;
    private final short[] rgb;

    /**
     * The line a border of the workbook is drawn with.
     *
     * @param xlsStyle the border as the workbook writes it
     * @param rgb      the colour of the border
     * @return the line, or {@link BorderStyle#NONE} for no border
     */
    public static BorderStyle of(org.apache.poi.ss.usermodel.BorderStyle xlsStyle, short[] rgb) {
        return switch (xlsStyle) {
            case NONE -> NONE;
            case THIN -> new BorderStyle(1, SOLID, rgb);
            case MEDIUM, THICK -> new BorderStyle(2, SOLID, rgb);
            case DASHED, DASH_DOT, DASH_DOT_DOT -> new BorderStyle(1, "dashed", rgb);
            case MEDIUM_DASHED, MEDIUM_DASH_DOT, MEDIUM_DASH_DOT_DOT -> new BorderStyle(2, "dashed", rgb);
            case DOTTED, HAIR -> new BorderStyle(1, "dotted", rgb);
            case DOUBLE -> new BorderStyle(1, "double", rgb);
            default -> {
                log.warn("Unknown border style: {}", xlsStyle);
                yield new BorderStyle(1, SOLID, rgb);
            }
        };
    }

}
