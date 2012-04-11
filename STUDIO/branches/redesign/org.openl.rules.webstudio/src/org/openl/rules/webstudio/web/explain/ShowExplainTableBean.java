package org.openl.rules.webstudio.web.explain;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.commons.lang.StringUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.ui.IGridSelector;
import org.openl.rules.table.ui.RegionGridSelector;
import org.openl.rules.table.ui.filters.ColorGridFilter;
import org.openl.rules.table.ui.filters.IColorFilter;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

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

        uri = FacesUtils.getRequestParameter("uri");
        table = model.getTable(uri);
    }

    public IOpenLTable getTable() {
        return table;
    }

    public IGridFilter getFilter() {
        ProjectModel model = WebStudioUtils.getProjectModel();

        XlsUrlParser p1 = new XlsUrlParser();
        p1.parse(uri);
        IGridRegion region = IGridRegion.Tool.makeRegion(p1.range);

        IGridSelector regionSelector = new RegionGridSelector(region, true);
        IColorFilter colorFilter = model.getFilterHolder().makeFilter();
        IGridFilter filter = new ColorGridFilter(regionSelector, colorFilter);

        return filter;
    }

    public String getTableView() {
        ProjectModel model = WebStudioUtils.getProjectModel();
        return model.getTableView(FacesUtils.getRequestParameter("view"));
    }

    public String getHeader() {
        StringBuilder header = new StringBuilder();

        if (table != null) {
            header.append(table.getName());
            header.append(" : ");

            String text = FacesUtils.getRequestParameter("text");
            if (StringUtils.isNotBlank(text)) {
                header.append(text);
            }
        }

        return header.toString();
    }

}
