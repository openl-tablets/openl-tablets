package org.openl.rules.ui.search;

import java.util.Collections;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * Request scope bean, holding flag if search run is required.
 */
@ManagedBean
@RequestScoped
public class BussinessSearchRequest {
    private boolean needSearch;

    @ManagedProperty(value="#{bussinessSearch}")
    private BussinesSearchPropertyBean bussinessSearchBean;

    private List<IOpenLTable> tableSearchList;

    public BussinesSearchPropertyBean getBussinessSearchBean() {
        return bussinessSearchBean;
    }

    public void setBussinessSearchBean(BussinesSearchPropertyBean bussinessSearchBean) {
        this.bussinessSearchBean = bussinessSearchBean;
    }

    public boolean isSearching() {
        return needSearch;
    }

    /**
     * Start working on pressing the search button on UI
     * @return
     */
    public String search() {            
        needSearch = true;            
        bussinessSearchBean.initBusSearchCond();            
        return null;
    }

    public List<IOpenLTable> getSearchResults() {
        if (!isSearching() || !bussinessSearchBean.isReady() || !bussinessSearchBean.isAnyPropertyFilled()) {
            return Collections.emptyList();
        }
        if (tableSearchList == null) {
            ProjectModel model = WebStudioUtils.getWebStudio().getModel();
            model.runSearch(bussinessSearchBean.getSearch());
            tableSearchList = model.getBussinessSearchResults(
                    model.runSearch(bussinessSearchBean.getSearch()));
        }
        return tableSearchList;
    }

}
