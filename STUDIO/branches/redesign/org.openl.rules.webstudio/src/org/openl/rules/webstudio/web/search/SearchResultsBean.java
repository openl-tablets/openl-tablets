package org.openl.rules.webstudio.web.search;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.search.FileIndexer;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.richfaces.component.UIRepeat;

/**
 * Request scope managed bean providing logic for Search Results include page of OpenL Studio.
 */
@ManagedBean
@RequestScoped
public class SearchResultsBean {

    private UIRepeat searchResults;

    public SearchResultsBean() {
    }

    public UIRepeat getSearchResults() {
        return searchResults;
    }

    public void setSearchResults(UIRepeat searchResults) {
        this.searchResults = searchResults;
    }

    public String getUri() {
        String[] searchResult = (String[]) searchResults.getRowData();
        return searchResult[0];
    }

    public boolean isCanViewTable() {
        ProjectModel model = WebStudioUtils.getProjectModel();

        String uri = getUri();

        return model.getNode(uri) == null;
    }

    public String getFileHeader() {
        String uri = getUri();
        return FileIndexer.showElementHeader(uri);
    }

    public boolean isXlsFile() {
        String uri = getUri();
        if (uri.indexOf(".xls") >= 0) {
            return true;
        }
        return false;
    }

    public boolean isDocFile() {
        String uri = getUri();
        if (uri.indexOf(".doc") >= 0) {
            return true;
        }
        return false;
    }

}
