package org.openl.rules.webstudio.web.search;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.commons.lang.StringUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.ui.ProjectIndexer;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.search.FileIndexer;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.richfaces.component.UIRepeat;

/**
 * Request scope managed bean providing logic for Simple Search page.
 */
@ManagedBean
@RequestScoped
public class SimpleSearchBean {

    private String searchQuery;
    private String[][] searchResults;

    private UIRepeat searchResultsData;

    public SimpleSearchBean() {
        initSearchQuery();

        if (StringUtils.isNotBlank(searchQuery)) {
            search();
        }
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public String[][] getSearchResults() {
        return searchResults;
    }

    public void setSearchResults(String[][] searchResults) {
        this.searchResults = searchResults;
    }

    public UIRepeat getSearchResultsData() {
        return searchResultsData;
    }

    public void setSearchResultsData(UIRepeat searchResultsData) {
        this.searchResultsData = searchResultsData;
    }

    private void initSearchQuery() {
        String searchQuery = FacesUtils.getRequestParameter("searchQuery");

        if (StringUtils.isNotBlank(searchQuery)) {
            // Replace all non-breaking spaces by breaking spaces 
            String spaceToRemove = Character.toString((char) 160);
            searchQuery = searchQuery.replaceAll(spaceToRemove, " ");

            setSearchQuery(searchQuery);
        }
    }

    public String search() {
        String[][] searchResults = {};

        if (StringUtils.isNotBlank(searchQuery)) {
            ProjectModel projectModel = WebStudioUtils.getProjectModel();
            if (projectModel != null) {
                ProjectIndexer projectIndexer = projectModel.getIndexer();
                if (projectIndexer != null) {
                    searchResults = projectModel.getIndexer().getResultsForQuery(searchQuery, 200, null);
                }
            }
        }

        setSearchResults(searchResults);

        return null;
    }

    public String getUri() {
        String[] searchResult = (String[]) searchResultsData.getRowData();
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
