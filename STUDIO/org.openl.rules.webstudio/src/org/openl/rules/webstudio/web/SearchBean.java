
package org.openl.rules.webstudio.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.faces.model.SelectItem;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.def.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.tableeditor.renderkit.TableProperty;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

@Service
@RequestScope
public class SearchBean {

    // TODO Move table names to Rules Core
    private static final SelectItem[] tableTypeItems = new SelectItem[] {
            new SelectItem(XlsNodeTypes.XLS_DT.toString(), "Decision"),
            new SelectItem(XlsNodeTypes.XLS_SPREADSHEET.toString(), "Spreadsheet"),
            new SelectItem(XlsNodeTypes.XLS_TBASIC.toString(), "TBasic"),
            new SelectItem(XlsNodeTypes.XLS_COLUMN_MATCH.toString(), "Column Match"),
            new SelectItem(XlsNodeTypes.XLS_DATATYPE.toString(), "Datatype"),
            new SelectItem(XlsNodeTypes.XLS_DATA.toString(), "Data"),
            new SelectItem(XlsNodeTypes.XLS_METHOD.toString(), "Method"),
            new SelectItem(XlsNodeTypes.XLS_TEST_METHOD.toString(), "Test"),
            new SelectItem(XlsNodeTypes.XLS_RUN_METHOD.toString(), "Run"),
            new SelectItem(XlsNodeTypes.XLS_CONSTANTS.toString(), "Constants"),
            new SelectItem(XlsNodeTypes.XLS_CONDITIONS.toString(), "Conditions"),
            new SelectItem(XlsNodeTypes.XLS_ACTIONS.toString(), "Actions"),
            new SelectItem(XlsNodeTypes.XLS_RETURNS.toString(), "Returns"),
            new SelectItem(XlsNodeTypes.XLS_ENVIRONMENT.toString(), "Environment"),
            new SelectItem(XlsNodeTypes.XLS_PROPERTIES.toString(), "Properties"),
            new SelectItem(XlsNodeTypes.XLS_OTHER.toString(), "Other") };

    private static final SelectItem[] searchScopeItems = new SelectItem[] {
            new SelectItem(SearchScope.CURRENT_MODULE, SearchScope.CURRENT_MODULE.getLabel()),
            new SelectItem(SearchScope.CURRENT_PROJECT, SearchScope.CURRENT_PROJECT.getLabel()),
            new SelectItem(SearchScope.ALL, SearchScope.ALL.getLabel()) };

    private String query;
    private String[] tableTypes;
    private String tableHeader;
    private SearchScope searchScope = SearchScope.CURRENT_MODULE;
    private final List<TableProperty> properties = new ArrayList<>();

    private List<IOpenLTable> searchResults;

    public SearchBean() {
        initProperties();
        initSearchQuery();
        search();
    }

    public String[] getTableTypes() {
        return tableTypes;
    }

    public void setTableTypes(String[] tableTypes) {
        this.tableTypes = tableTypes;
    }

    public List<TableProperty> getProperties() {
        return properties;
    }

    public SelectItem[] getTableTypeItems() {
        return tableTypeItems;
    }

    public SelectItem[] getSearchScopeItems() {
        return searchScopeItems;
    }

    public List<IOpenLTable> getSearchResults() {
        return searchResults;
    }

    private void initProperties() {
        TablePropertyDefinition[] propDefinitions = DefaultPropertyDefinitions.getDefaultDefinitions();

        for (TablePropertyDefinition propDefinition : propDefinitions) {
            if (propDefinition.getDeprecation() == null) {
                TableProperty prop = new TableProperty(propDefinition, WebStudioFormats.getInstance());
                properties.add(prop);
            }
        }
    }

    private void initSearchQuery() {
        String query = WebStudioUtils.getRequestParameter("query");
        String tableTypes = WebStudioUtils.getRequestParameter("types");
        String tableHeader = WebStudioUtils.getRequestParameter("header");
        String searchScope = WebStudioUtils.getRequestParameter("searchScope");

        if (StringUtils.isNotBlank(query)) {
            // Replace all non-breaking spaces by breaking spaces
            String spaceToRemove = Character.toString((char) 160);
            query = query.replaceAll(spaceToRemove, " ");

            this.query = query;
        }

        if (StringUtils.isNotBlank(tableTypes)) {
            this.tableTypes = tableTypes.split("-");
        }

        this.tableHeader = tableHeader;

        if (StringUtils.isNotBlank(searchScope)) {
            this.searchScope = SearchScope.valueOf(searchScope);
        }

        // Init properties query
        for (TableProperty property : properties) {
            String propertyValue = WebStudioUtils.getRequestParameter(property.getName());
            if (propertyValue != null) {
                property.setStringValue(propertyValue);
            }
        }
    }

    private Map<String, Object> getSearchProperties() {
        Map<String, Object> properties = new HashMap<>();

        for (TableProperty prop : this.properties) {
            Object propValue = prop.getValue();
            if (propValue != null) {
                properties.put(prop.getName(), propValue);
            }
        }

        return properties;
    }

    private void search() {
        ProjectModel projectModel = WebStudioUtils.getProjectModel();

        Predicate<TableSyntaxNode> selectors = new CellValueSelector(query);

        if (CollectionUtils.isNotEmpty(tableTypes)) {
            selectors = selectors.and(new TableTypeSelector(tableTypes));
        }

        if (StringUtils.isNotBlank(tableHeader)) {
            selectors = selectors.and(new TableHeaderSelector(tableHeader));
        }

        Map<String, Object> properties = getSearchProperties();
        if (CollectionUtils.isNotEmpty(properties)) {
            selectors = selectors.and(new TablePropertiesSelector(properties));
        }

        searchResults = projectModel.search(selectors, searchScope);
    }

}
