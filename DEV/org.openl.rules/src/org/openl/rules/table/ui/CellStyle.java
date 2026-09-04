package org.openl.rules.table.ui;

import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;

/**
 * @author snshor
 */
public class CellStyle implements ICellStyle {

    @Getter
    private HorizontalAlignment horizontalAlignment = HorizontalAlignment.GENERAL;

    @Getter
    private VerticalAlignment verticalAlignment = VerticalAlignment.TOP;

    @Getter
    @Setter
    private short[] fillBackgroundColor;

    @Getter
    @Setter
    private short[] fillForegroundColor;

    @Getter
    private short fillBackgroundColorIndex;

    @Getter
    private short fillForegroundColorIndex;

    @Getter
    private FillPatternType fillPattern;

    @Getter
    @Setter
    private BorderStyle[] borderStyle;

    @Getter
    @Setter
    private short[][] borderRGB;
    @Getter
    private int indent;

    @Getter
    private boolean wrappedText;

    @Getter
    private int rotation;
    @Getter
    private short formatIndex;
    @Getter
    private String formatString;

    public CellStyle(ICellStyle cellStyle) {
        if (cellStyle == null) {
            return;
        }

        horizontalAlignment = cellStyle.getHorizontalAlignment();

        verticalAlignment = cellStyle.getVerticalAlignment();

        fillBackgroundColor = cellStyle.getFillBackgroundColor();
        fillForegroundColor = cellStyle.getFillForegroundColor();

        fillBackgroundColorIndex = cellStyle.getFillBackgroundColorIndex();
        fillForegroundColorIndex = cellStyle.getFillForegroundColorIndex();
        fillPattern = cellStyle.getFillPattern();

        borderStyle = cellStyle.getBorderStyle();
        borderRGB = cellStyle.getBorderRGB();

        indent = cellStyle.getIndent();

        wrappedText = cellStyle.isWrappedText();

        rotation = cellStyle.getRotation();

        formatIndex = cellStyle.getFormatIndex();
        formatString = cellStyle.getFormatString();
    }
}
