package org.openl.rules.webstudio.web;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.webstudio.util.WebTool;
import org.openl.rules.ui.ColorFilterHolder;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * Request scope managed bean for tableViewMenu page.
 */
@ManagedBean
@RequestScoped
public class TableViewMenuBean {

    private ColorFilterHolder filterHolder;

    private String requestParams;
    private String requestParamsView;

    public TableViewMenuBean() {
        WebStudio studio = WebStudioUtils.getWebStudio();

        filterHolder = studio.getModel().getFilterHolder();
        String filterType = FacesUtils.getRequestParameter("filterType");
        if (filterType != null) {
            int ftype = Integer.parseInt(filterType);
            filterHolder.setFilterType(ftype);
        }

        String transparency = FacesUtils.getRequestParameter("transparency");
        if ("up".equals(transparency)) {
            filterHolder.setTransparency(filterHolder.getTransparency() + 10);
        }
        if ("down".equals(transparency)) {
            filterHolder.setTransparency(filterHolder.getTransparency() - 10);
        }

        initRequestParams();
    }

    private void initRequestParams() {
        String[] menuParams = { "transparency", "filterType" };
        requestParams = WebTool.listRequestParams(FacesUtils.getRequest(), menuParams);

        String[] menuParamsView = { "transparency", "filterType", "view" };
        requestParamsView = WebTool.listRequestParams(FacesUtils.getRequest(), menuParamsView);
    }

    public ColorFilterHolder getFilterHolder() {
        return filterHolder;
    }

    public String getRequestParams() {
        return requestParams;
    }

    public String getRequestParamsView() {
        return requestParamsView;
    }

    public String[] getFilterImageNames() {
        return ColorFilterHolder.imageNames;
    }

    public String[] getFilterNames() {
        return ColorFilterHolder.filterNames;
    }

}
