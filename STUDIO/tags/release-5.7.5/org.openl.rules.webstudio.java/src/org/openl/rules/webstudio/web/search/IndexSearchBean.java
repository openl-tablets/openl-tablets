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

    private static final int SEARCH_RESULT_COLUMNS_COUNT = 3;
    
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
        if (isReady()) {
            ProjectModel model = WebStudioUtils.getProjectModel();
            String letter = (String) searchLetters.getRowData();
            ProjectIndexer indexer = model.getIndexer();

            if (indexer != null) {
                return indexer.getBuckets(letter);
            }
        }
        return new TokenBucket[0];
    }
    
    public int getColumnsCount() {
        return SEARCH_RESULT_COLUMNS_COUNT;
    }
    
    public TokenBucket[][] getOrderedIndexTokens() {
        
        TokenBucket[] tokens = getIndexTokens();
        
        if (tokens.length == 12) {
            System.out.println();
        }
        
        int rows = roundUp(Double.valueOf(tokens.length)  / SEARCH_RESULT_COLUMNS_COUNT);
        TokenBucket[][] result = new TokenBucket[rows][SEARCH_RESULT_COLUMNS_COUNT];
        
        for (int i = 0; i < SEARCH_RESULT_COLUMNS_COUNT; i++) {
            for (int j = 0; j < rows; j++) {
                int index = i * rows + j;
                
                if (index < tokens.length) {
                    TokenBucket element = tokens[index];
                    result[j][i] = element;
                }
            }
        }

        return result;
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
    
    public boolean isReady() {
        ProjectModel model = WebStudioUtils.getProjectModel();        
        return model.isProjectCompiledSuccessfully(); 
    }

    private int roundUp(double value) {
        
        int result = Double.valueOf(value).intValue();
        
        if (result < value) {
            result += 1;
        }
        
        return result;
    }
}
