package org.openl.rules.ui.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.search.GroupOperator;
import org.openl.rules.search.OpenLAdvancedSearch;
import org.openl.rules.search.OpenLAdvancedSearchResult;
import org.openl.rules.search.OpenLSavedSearch;
import org.openl.rules.search.SearchElement;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.web.jsf.util.FacesUtils;
import static org.openl.rules.web.jsf.util.FacesUtils.createSelectItems;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.AStringBoolOperator;

/**
 * JSF managed bean, session scope.
 */
public class OpenLAdvancedSearchBean {
    private final static Log log = LogFactory.getLog(OpenLAdvancedSearchBean.class);

    private static final SelectItem[] tableTypes;
    private static final SelectItem[] columnTypeValues;
    private static final SelectItem[] groupOperationValues;
    private static final SelectItem[] notFlagValues;
    private static final SelectItem[] typeValues;
    private static final SelectItem[] opTypeValues;
    private static final Map<String, Integer> tableType2Index;

    private String[] selectedTableTypes;
    private String newSearchName;
    private SearchElementBean[] tableElements;
    private SearchElementBean[] columnElements;
    private final OpenLAdvancedSearch search = new OpenLAdvancedSearch();

    static {
        tableTypes = createSelectItems(OpenLAdvancedSearch.typeButtons);
        columnTypeValues = createSelectItems(OpenLAdvancedSearch.colTypeValues);
        groupOperationValues = createSelectItems(GroupOperator.names);
        notFlagValues = createSelectItems(OpenLAdvancedSearch.nfValues);
        typeValues = createSelectItems(OpenLAdvancedSearch.typeValues);
        opTypeValues = createSelectItems(AStringBoolOperator.allNames());

        tableType2Index = new HashMap<String, Integer>();
        for (int i = 0; i < OpenLAdvancedSearch.typeButtons.length; i++) {
            tableType2Index.put(OpenLAdvancedSearch.typeButtons[i], i);
        }
    }

    public class SearchElementBean {
        protected int index;

        SearchElementBean(int index) {
            this.index = index;
        }

        protected SearchElement getSearchElement() {
            return search.getTableElements()[index];
        }

        public GroupOperator getOperator() {
            return getSearchElement().getOperator();
        }

        public String getGroupOperatorName() {
            return getSearchElement().getOperator().getName();
        }

        public void setGroupOperatorName(String groupOperatorName) {
            getSearchElement().setOperator(GroupOperator.find(groupOperatorName));
        }

        public String getNotFlag() {
            return OpenLAdvancedSearch.nfValues[getSearchElement().isNotFlag() ? 1 : 0];
        }

        public void setNotFlag(String flag) {
            getSearchElement().setNotFlag(OpenLAdvancedSearch.nfValues[1].equals(flag));
        }

        public String getType() {
            return getSearchElement().getType();
        }

        public void setType(String type) {
            getSearchElement().setType(type);
        }

        public String getValue1() {
            return getSearchElement().getValue1();
        }

        public void setValue1(String value1) {
            getSearchElement().setValue1(value1);
        }

        public boolean isRequiredValue1() {
            return search.showValue1(getType());
        }

        public String getValue2() {
            return getSearchElement().getValue2();
        }

        public void setValue2(String value2) {
            getSearchElement().setValue2(value2);
        }

        public String getOpType1() {
            return getSearchElement().getOpType1();
        }

        public void setOpType1(String type) {
            getSearchElement().setOpType1(type);
        }

        public String getOpType2() {
            return getSearchElement().getOpType2();
        }

        public void setOpType2(String type) {
            getSearchElement().setOpType2(type);
        }
    }

    public class ColumnSearchElementBean extends SearchElementBean {
        public ColumnSearchElementBean(int index) {
            super(index);
        }

        @Override
        protected SearchElement getSearchElement() {
            return search.getColumnElements()[index];
        }
    }

    /**
     * Request scope bean, holding flag if search run is required.
     */
    public static class SearchRequest {
        private boolean needSearch;
        private OpenLAdvancedSearchBean advancedSearchBean;
        private List<TableSearch> tableSearchList;

        public OpenLAdvancedSearchBean getAdvancedSearchBean() {
            return advancedSearchBean;
        }

        public void setAdvancedSearchBean(OpenLAdvancedSearchBean advancedSearchBean) {
            this.advancedSearchBean = advancedSearchBean;
        }

        public String search() {
            needSearch = true;
            return null;
        }

        public boolean isSearching() {
            return needSearch;
        }

        public String getSearchResult() {
            if (!isSearching() || !advancedSearchBean.isReady()) {
                return "";
            }

            ProjectModel model = WebStudioUtils.getWebStudio().getModel();
            return model.displayResult(model.runSearch(advancedSearchBean.search));
        }

        public List<TableSearch> getSearchResults() {
            if (!isSearching() || !advancedSearchBean.isReady()) {
                return Collections.emptyList();
            }
            if (tableSearchList == null) {
                ProjectModel model = WebStudioUtils.getWebStudio().getModel();
                model.runSearch(advancedSearchBean.search);
                tableSearchList = model.getSearchList(model.runSearch(advancedSearchBean.search));
            }
            return tableSearchList;
        }
    }

    public OpenLAdvancedSearchBean() {
        updateTableElements();
        updateColumnElements();
    }

    private void updateTableElements() {
        tableElements = new SearchElementBean[search.getTableElements().length];
        for (int i = 0; i < tableElements.length; ++i) {
            tableElements[i] = new SearchElementBean(i);
        }
    }

    private void updateColumnElements() {
        columnElements = new SearchElementBean[search.getColumnElements().length];
        for (int i = 0; i < columnElements.length; ++i) {
            columnElements[i] = new ColumnSearchElementBean(i);
        }
    }

    public String[] getSelectedTableTypes() {
        return selectedTableTypes;
    }

    public SelectItem[] getColumnTypeValues() {
        return columnTypeValues;
    }

    public void setSelectedTableTypes(String[] selectedTableTypes) {
        this.selectedTableTypes = selectedTableTypes;

        boolean[] selected = new boolean[tableTypes.length];
        for (String selectedTableType : selectedTableTypes) {
            Integer index = tableType2Index.get(selectedTableType);
            if (index != null) {
                selected[index] = true;
            }
        }

        for (int i = 0; i < selected.length; i++) {
            search.selectType(i, selected[i]);            
        }
    }

    public String save() {
        WebStudio webStudio = WebStudioUtils.getWebStudio();
        if (webStudio != null) {
            try {
                OpenLSavedSearch savedSearch = new OpenLSavedSearch(search.getColumnElements(), search.getTableElements(), selectedTableTypes);
                savedSearch.setName(getNewSearchName());
                webStudio.getModel().saveSearch(savedSearch);
            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Failed to save search", e.getMessage()));
                log.error("failed to save search", e);
            }
        }
        return null;
    }

    public OpenLSavedSearch[] getSavedSearches() {
        WebStudio webStudio = WebStudioUtils.getWebStudio();
        if (webStudio != null) {
            return webStudio.getModel().getSavedSearches();
        }
        return null;
    }

    public boolean isShowSearches() {
        OpenLSavedSearch[] savedSearches = getSavedSearches();
        return savedSearches != null && savedSearches.length > 0;
    }

    public String applySearch() {
        WebStudio webStudio = WebStudioUtils.getWebStudio();
        if (webStudio != null) {
            int index = -1;
            try {
                index = Integer.parseInt(FacesUtils.getRequestParameter("index"));
            } catch (NumberFormatException e) {}

            OpenLSavedSearch[] savedSearches = webStudio.getModel().getSavedSearches();
            if (savedSearches != null && index >= 0 && index < savedSearches.length) {
                applySearch(savedSearches[index]);
            }
        }

        return null;
    }

    private void applySearch(OpenLSavedSearch savedSearch) {
        search.setTableElements(savedSearch.getTableElements());
        search.setColumnElements(savedSearch.getColumnElements());
        updateColumnElements();
        updateTableElements();
        String types = savedSearch.getTableTypes();
        setSelectedTableTypes(types.trim().split(", *"));
    }

    public SelectItem[] getTableTypes() {
        return tableTypes;
    }

    public SelectItem[] getGroupOperationValues() {
        return groupOperationValues;
    }

    public SelectItem[] getNotFlagValues() {
        return notFlagValues;
    }

    public SelectItem[] getTypeValues() {
        return typeValues;
    }

    public synchronized SearchElementBean[] getTableElements() {
        return tableElements;
    }

    public synchronized SearchElementBean[] getColumnElements() {
        return columnElements;
    }

    public SelectItem[] getOpTypeValues() {
        return opTypeValues;
    }

    public synchronized String addCondition() {
        search.editAction(OpenLAdvancedSearch.ADD_ACTION + FacesUtils.getRequestParameter("index"));
        updateTableElements();
        return null;
    }

    public synchronized String deleteCondition() {
        search.editAction(OpenLAdvancedSearch.DELETE_ACTION + FacesUtils.getRequestParameter("index"));
        updateTableElements();
        return null;
    }

    public synchronized String addColCondition() {
        search.editAction(OpenLAdvancedSearch.COL_ADD_ACTION + FacesUtils.getRequestParameter("index"));
        updateColumnElements();
        return null;
    }

    public synchronized String deleteColCondition() {
        search.editAction(OpenLAdvancedSearch.COL_DELETE_ACTION + FacesUtils.getRequestParameter("index"));
        updateColumnElements();
        return null;
    }

    public boolean isReady() {
        return WebStudioUtils.isStudioReady();
    }

    public boolean isStudioReadOnly() {
        WebStudio webStudio = WebStudioUtils.getWebStudio();
        return webStudio == null || webStudio.getModel().isReadOnly();
    }

    public String getStudioView() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        return studio == null ? null : studio.getModel().getTableView(FacesUtils.getRequestParameter("view"));
    }

    public String getNewSearchName() {
        return newSearchName;
    }

    public void setNewSearchName(String newSearchName) {
        this.newSearchName = newSearchName;
    }
}
