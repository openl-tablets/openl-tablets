package org.openl.rules.webstudio.web.explain;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.table.*;
import org.openl.rules.table.ui.IGridSelector;
import org.openl.rules.table.ui.RegionGridSelector;
import org.openl.rules.table.ui.filters.ColorGridFilter;
import org.openl.rules.table.ui.filters.IColorFilter;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.table.xls.XlsUrlUtils;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;

/**
 * Request scope managed bean for showExplainTable page.
 */
@ManagedBean
@RequestScoped
public class ShowExplainTableBean {

    private String uri;
    private IOpenLTable table;

    public ShowExplainTableBean() {
        ProjectModel model = WebStudioUtils.getProjectModel();

        uri = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_URI);
        table = model.getTable(uri);
    }

    public IOpenLTable getTable() {
        return table;
    }

    private IGridRegion findInCompositeGrid(CompositeGrid compositeGrid, XlsUrlParser p1, IGridRegion region) {
        int i = 0;
        for (IGridTable gridTable : compositeGrid.getGridTables()) {
            if (gridTable.getGrid() instanceof CompositeGrid) {
                IGridRegion region2 = findInCompositeGrid((CompositeGrid) gridTable.getGrid(), p1, region);
                if (region2 != null) {
                    IGridRegion region3 = compositeGrid.getMappedRegion(i);
                    int top = region.getTop() - region2.getTop() + region3.getTop();
                    int bottom = region.getBottom() - region2.getTop() + region3.getTop();
                    int left = region.getLeft() - region2.getLeft() + region3.getLeft();
                    int right = region.getRight() - region2.getLeft() + region3.getLeft();
                    return new GridRegion(top, left, bottom, right);
                }
            } else {
                if (XlsUrlUtils.intersects(p1, gridTable.getUriParser())) {
                    IGridRegion region2 = gridTable.getRegion();
                    IGridRegion region3 = compositeGrid.getMappedRegion(i);
                    IGridRegion tmp = region;
                    if (region.getBottom() - region.getTop() == 0 && region.getRight() - region.getLeft() == 0) {// if
                        // one
                        // cell
                        // find
                        // merged
                        // region
                        IGridRegion margedRegion = gridTable.getGrid()
                            .getRegionContaining(region.getLeft(), region.getTop());
                        if (margedRegion != null) {
                            tmp = margedRegion;
                        }
                    }
                    int top = tmp.getTop() - region2.getTop() + region3.getTop();
                    int bottom = tmp.getBottom() - region2.getTop() + region3.getTop();
                    int left = tmp.getLeft() - region2.getLeft() + region3.getLeft();
                    int right = tmp.getRight() - region2.getLeft() + region3.getLeft();
                    return new GridRegion(top, left, bottom, right);
                }
            }
            i++;
        }
        return null;
    }

    public IGridFilter getFilter() {
        ProjectModel model = WebStudioUtils.getProjectModel();

        XlsUrlParser p1 = new XlsUrlParser();
        p1.parse(uri);
        IGridRegion region = IGridRegion.Tool.makeRegion(p1.getRange());

        if (table.getGridTable().getGrid() instanceof CompositeGrid) {
            CompositeGrid compositeGrid = (CompositeGrid) table.getGridTable().getGrid();
            IGridRegion r = findInCompositeGrid(compositeGrid, p1, region);
            if (r != null) {
                region = r;
            }
        } else {
            if (region.getBottom() - region.getTop() == 0 && region.getRight() - region.getLeft() == 0) { // is one cell
                // find merged
                // region
                IGridRegion margedRegion = table.getGridTable()
                    .getGrid()
                    .getRegionContaining(region.getLeft(), region.getTop());
                if (margedRegion != null) {
                    region = margedRegion;
                }
            }
        }

        IGridSelector regionSelector = new RegionGridSelector(region, true);
        IColorFilter colorFilter = model.getFilterHolder().makeFilter();

        return new ColorGridFilter(regionSelector, colorFilter);
    }

    public String getHeader() {
        StringBuilder header = new StringBuilder();

        if (table != null) {
            header.append(table.getDisplayName());
            header.append(" : ");

            String text = FacesUtils.getRequestParameter("text");
            if (StringUtils.isNotBlank(text)) {
                header.append(text);
            }
        }

        return header.toString();
    }

}
