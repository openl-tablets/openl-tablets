package org.openl.rules.webstudio.web.search;

import org.ajax4jsf.component.UIRepeat;
import org.apache.commons.lang.StringUtils;
import org.openl.rules.indexer.Index.TokenBucket;
import org.openl.rules.ui.ProjectIndexer;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * Request scope managed bean providing logic for Index Search page of OpenL Studio.
 */
public class IndexSearchBean extends SimpleSearchBean {

    private UIRepeat searchLetters;

    public IndexSearchBean() {
    }

    public UIRepeat getSearchLetters() {
        return searchLetters;
    }

    public void setSearchLetters(UIRepeat searchLetters) {
        this.searchLetters = searchLetters;
    }

    public String[] getIndexLetters() {
        ProjectModel model = WebStudioUtils.getProjectModel();
        ProjectIndexer indexer = model.getIndexer();

        if (indexer != null) {
            return indexer.getLetters();
        }

        return new String[0];
    }

    public TokenBucket[] getIndexTokens() {
        String letter = (String) searchLetters.getRowData();

        ProjectModel model = WebStudioUtils.getProjectModel();
        ProjectIndexer indexer = model.getIndexer();

        if (indexer != null) {
            return indexer.getBuckets(letter);
        }

        return new TokenBucket[0];
    }

    public String search() {
        String[][] searchResults = {};

        if (StringUtils.isNotBlank(getSearchQuery())) {
            ProjectModel projectModel = WebStudioUtils.getProjectModel();
            if (projectModel != null) {
                ProjectIndexer projectIndexer = projectModel.getIndexer();
                if (projectIndexer != null) {
                    searchResults = projectIndexer.getResultsForIndex(getSearchQuery());
                }
            }
        }

        setSearchResults(searchResults);

        return null;
    }

}
