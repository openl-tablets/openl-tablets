package org.openl.rules.webstudio.util;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellStyle;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellStyles;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class XSSFOptimizer {
    public static void removeUnusedStyles(XSSFWorkbook workbook) {
        try {
            StylesTable stylesSource = workbook.getStylesSource();
            @SuppressWarnings("unchecked")
            List<CTXf> xfs = (List<CTXf>) FieldUtils.readDeclaredField(stylesSource, "xfs", true);
            @SuppressWarnings("unchecked")
            List<CTXf> styleXfs = (List<CTXf>) FieldUtils.readDeclaredField(stylesSource, "styleXfs", true);
            CTCellStyles cellStyles = stylesSource.getCTStylesheet().getCellStyles();
            if (cellStyles == null) {
                cellStyles = CTCellStyles.Factory.newInstance();
            }

            List<CTXf> newStyleXfs = new ArrayList<CTXf>();
            List<CTCellStyle> newCellStyles = new ArrayList<CTCellStyle>();

            TreeSet<Integer> usedStyleXfs = new TreeSet<Integer>();
            for (CTXf xf : xfs) {
                usedStyleXfs.add((int) xf.getXfId());
            }

            // Change XfId references to the new ones
            CTCellStyle[] cellStyleArray = cellStyles.getCellStyleArray();
            long newXfId = 0;
            for (Integer usedStyleXf : usedStyleXfs) {
                CTXf styleXf = styleXfs.get(usedStyleXf);
                styleXf.setXfId(newXfId);
                newStyleXfs.add(styleXf);

                CTCellStyle cellStyle = cellStyleArray[usedStyleXf];
                cellStyle.setXfId(newXfId);
                newCellStyles.add(cellStyle);

                for (CTXf xf : xfs) {
                    if (xf.getXfId() == usedStyleXf) {
                        xf.setXfId(newXfId);
                    }
                }

                newXfId++;
            }

            // Remove unused styles.
            styleXfs.clear();
            styleXfs.addAll(newStyleXfs);

            cellStyles.setCount(newCellStyles.size());
            cellStyles.setCellStyleArray(newCellStyles.toArray(new CTCellStyle[newCellStyles.size()]));
        } catch (IllegalAccessException e) {
            // Something is changed in POI implementation. Don't modify workbook, just quit.
            Logger log = LoggerFactory.getLogger(XSSFOptimizer.class);
            log.error(e.getMessage(), e);
        }
    }

    private XSSFOptimizer() {
    }
}
