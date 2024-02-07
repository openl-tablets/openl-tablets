package org.openl.rules.xls.merge;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.poi.hssf.record.PaletteRecord;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;

import org.openl.rules.xls.merge.diff.XlsMatch;

/**
 * HSSF Palette Matcher
 *
 * @author Vladyslav Pikus
 */
public class HSSFPaletteMatcher {

    public static final int FIRST_COLOR_INDEX = PaletteRecord.FIRST_COLOR_INDEX;
    public static final int LAST_COLOR_INDEX = PaletteRecord.STANDARD_PALETTE_SIZE + FIRST_COLOR_INDEX;

    public static Map<Short, XlsMatch> matchPalette(HSSFWorkbook baseWorkbook, HSSFWorkbook workbook) {
        HSSFPalette basePalette = baseWorkbook.getCustomPalette();
        HSSFPalette palette = workbook.getCustomPalette();

        Map<Short, XlsMatch> matchRes = new HashMap<>();

        for (short i = FIRST_COLOR_INDEX; i < LAST_COLOR_INDEX; i++) {
            HSSFColor baseColor = basePalette.getColor(i);
            HSSFColor color = palette.getColor(i);
            if (baseColor == null) {
                if (color == null) {
                    matchRes.put(i, XlsMatch.EQUAL);
                } else {
                    matchRes.put(i, XlsMatch.CREATED);
                }
            } else if (color == null) {
                matchRes.put(i, XlsMatch.REMOVED);
            } else if (equalColor(baseColor, color)) {
                matchRes.put(i, XlsMatch.EQUAL);
            } else {
                matchRes.put(i, XlsMatch.UPDATED);
            }
        }
        return matchRes;
    }

    static boolean equalColor(HSSFColor color1, HSSFColor color2) {
        if (Objects.equals(color1, color2)) {
            if (color1 == null) {
                // both null
                return true;
            }
            // Apache POI doesn't compare RGB triplet using regular equal method
            return Arrays.equals(color1.getTriplet(), color2.getTriplet());
        } else {
            return false;
        }
    }

}