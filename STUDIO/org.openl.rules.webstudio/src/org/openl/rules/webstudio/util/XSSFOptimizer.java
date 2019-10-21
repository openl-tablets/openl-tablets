package org.openl.rules.webstudio.util;

import java.util.ArrayList;
import java.util.Iterator;
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
    private static Logger LOG = LoggerFactory.getLogger(XSSFOptimizer.class);

    public static void removeUnusedStyles(XSSFWorkbook workbook) {
        LOG.info("Starting style optimization...");
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
            List<CTCellStyle> cellStyleArray = new ArrayList<>(cellStyles.getCellStyleList());
            LOG.info("Exists : xfs={}, styleXfs={}, styles={}", xfs.size(), styleXfs.size(), cellStyleArray.size());

            List<CTXf> newStyleXfs = new ArrayList<>();
            List<CTCellStyle> newCellStyles = new ArrayList<>();

            // TODO: Consider removing <xf> styles in <cellXfs> that is not referenced from any cell's "s" attribute (<c
            // s="33">)

            TreeSet<Integer> usedStyleXfs = new TreeSet<>();
            for (CTXf xf : xfs) {
                usedStyleXfs.add((int) xf.getXfId());
            }

            // Change XfId references to the new ones
            long newXfId = 0;
            for (Integer usedStyleXf : usedStyleXfs) {
                CTXf styleXf = styleXfs.get(usedStyleXf);
                styleXf.setXfId(newXfId);
                newStyleXfs.add(styleXf);

                // Change xfId in <cellStyle xfId=""> if exists such named style
                for (Iterator<CTCellStyle> iterator = cellStyleArray.iterator(); iterator.hasNext();) {
                    CTCellStyle style = iterator.next();
                    if (style.getXfId() == usedStyleXf) {
                        style.setXfId(newXfId);
                        newCellStyles.add(style);
                        iterator.remove();
                    }
                }

                for (CTXf xf : xfs) {
                    if (xf.getXfId() == usedStyleXf) {
                        xf.setXfId(newXfId);
                    }
                }

                newXfId++;
            }
            LOG.info("Used : styleXfs={}, styles={}", newStyleXfs.size(), newCellStyles.size());

            // Remove unused styles.
            styleXfs.clear();
            styleXfs.addAll(newStyleXfs);

            cellStyles.setCount(newCellStyles.size());
            cellStyles.setCellStyleArray(newCellStyles.toArray(new CTCellStyle[0]));
            LOG.info("Style optimization has been finished.");
        } catch (IllegalAccessException e) {
            // Something is changed in POI implementation. Don't modify workbook, just quit.
            LOG.error(e.getMessage(), e);
        }
    }

    private XSSFOptimizer() {
    }
}
