
package org.openl.rules.webstudio.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.def.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.tableeditor.renderkit.TableProperty;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.CollectionUtils;
import org.openl.util.ISelector;
import org.openl.util.StringUtils;

@ManagedBean
@RequestScoped
public class SearchBean {

    public static final String[] SEARCH_PARAMS = {
        "query", "types", "header"
    };

    // TODO Move table names to Rules Core
    public static final String[] TABLE_NAMES = {
        "Decision", "Spreadsheet",
        "TBasic", "Column Match",
        "Datatype", "Data",
        "Method", "Test", "Run",
        "Constants", "Conditions", "Returns",
        "Environment", "Properties",
        "Other"
    };

    public static final String[] TABLE_TYPES = {
        XlsNodeTypes.XLS_DT.toString(), XlsNodeTypes.XLS_SPREADSHEET.toString(),
        XlsNodeTypes.XLS_TBASIC.toString(), XlsNodeTypes.XLS_COLUMN_MATCH.toString(),
        XlsNodeTypes.XLS_DATATYPE.toString(), XlsNodeTypes.XLS_DATA.toString(),
        XlsNodeTypes.XLS_METHOD.toString(), XlsNodeTypes.XLS_TEST_METHOD.toString(), XlsNodeTypes.XLS_RUN_METHOD.toString(),
        XlsNodeTypes.XLS_CONSTANTS.toString(), XlsNodeTypes.XLS_CONDITIONS.toString(), XlsNodeTypes.XLS_RETURNS.toString(), 
        XlsNodeTypes.XLS_ENVIRONMENT.toString(), XlsNodeTypes.XLS_PROPERTIES.toString(),
        XlsNodeTypes.XLS_OTHER.toString()
    };

    private static final SelectItem[] tableTypeItems;
    static {
        tableTypeItems = FacesUtils.createSelectItems(TABLE_TYPES, TABLE_NAMES);
    }

    private String query;
    private String[] tableTypes;
    private String tableHeader;
    private List<TableProperty> properties = new ArrayList<>();

    private List<IOpenLTable> searchResults;

    public SearchBean() {
        initProperties();

        if (((HttpServletRequest) FacesUtils.getRequest())
                .getRequestURI().contains("search.xhtml"))  {
            initSearchQuery();
            search();
        }
    }

    public String getQuery() {
        return query;
    }

    public String[] getTableTypes() {
        return tableTypes;
    }

    public String getTableHeader() {
        return tableHeader;
    }

    public List<TableProperty> getProperties() {
        return properties;
    }

    public SelectItem[] getTableTypeItems() {
        return tableTypeItems;
    }

    public List<IOpenLTable> getSearchResults() {
        return searchResults;
    }

    private void initProperties() {
        TablePropertyDefinition[] propDefinitions = DefaultPropertyDefinitions.getDefaultDefinitions();

        for (TablePropertyDefinition propDefinition : propDefinitions) {
            if (propDefinition.getDeprecation() == null) {
                TableProperty prop = new TableProperty(propDefinition);
                properties.add(prop);
            }
        }
    }

    private void initSearchQuery() {
        String query = FacesUtils.getRequestParameter(SEARCH_PARAMS[0]);
        String tableTypes = FacesUtils.getRequestParameter(SEARCH_PARAMS[1]);
        String tableHeader = FacesUtils.getRequestParameter(SEARCH_PARAMS[2]);

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

        // Init properties query
        Map<String, String> requestParams = FacesUtils.getRequestParameterMap();
        for (Map.Entry<String, String> entry: requestParams.entrySet()) {
            String paramName = entry.getKey();
            if (!ArrayUtils.contains(SEARCH_PARAMS, paramName)
                    && TablePropertyDefinitionUtils.isPropertyExist(paramName)) {
                TableProperty property = getPropertyByName(paramName);
                String propertyValue = entry.getValue();
                property.setStringValue(propertyValue);
            }
        }
    }

    private TableProperty getPropertyByName(String name) {
        for (TableProperty property : properties) {
            if (property.getName().equals(name)) {
                return property;
            }
        }
        throw new IllegalArgumentException("Incorrect property '" + name + "'");
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

        ISelector<TableSyntaxNode> selectors = new CellValueSelector(query);

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

        searchResults = projectModel.search(selectors);
    }

}
