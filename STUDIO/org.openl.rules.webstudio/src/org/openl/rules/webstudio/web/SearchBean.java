
package org.openl.rules.webstudio.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

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
import org.openl.util.ISelector;
import org.openl.util.StringUtils;

@ManagedBean
@RequestScoped
public class SearchBean {

    // TODO Move table names to Rules Core
    private static final SelectItem[] tableTypeItems = new SelectItem[] {
        new SelectItem ("Decision", XlsNodeTypes.XLS_DT.toString()),
        new SelectItem ("Spreadsheet", XlsNodeTypes.XLS_SPREADSHEET.toString()),
        new SelectItem ("TBasic", XlsNodeTypes.XLS_TBASIC.toString()),
        new SelectItem ("Column Match", XlsNodeTypes.XLS_COLUMN_MATCH.toString()),
        new SelectItem ("Datatype", XlsNodeTypes.XLS_DATATYPE.toString()),
        new SelectItem ("Data", XlsNodeTypes.XLS_DATA.toString()),
        new SelectItem ("Method", XlsNodeTypes.XLS_METHOD.toString()),
        new SelectItem ("Test", XlsNodeTypes.XLS_TEST_METHOD.toString()),
        new SelectItem ("Run", XlsNodeTypes.XLS_RUN_METHOD.toString()),
        new SelectItem ("Constants", XlsNodeTypes.XLS_CONSTANTS.toString()),
        new SelectItem ("Conditions", XlsNodeTypes.XLS_CONDITIONS.toString()),
        new SelectItem ("Actions", XlsNodeTypes.XLS_ACTIONS.toString()),
        new SelectItem ("Returns", XlsNodeTypes.XLS_RETURNS.toString()),
        new SelectItem ("Environment", XlsNodeTypes.XLS_ENVIRONMENT.toString()),
        new SelectItem ("Properties", XlsNodeTypes.XLS_PROPERTIES.toString()),
        new SelectItem ("Other", XlsNodeTypes.XLS_OTHER.toString())
    };

    private String query;
    private String[] tableTypes;
    private String tableHeader;
    private List<TableProperty> properties = new ArrayList<>();

    private List<IOpenLTable> searchResults;

    public SearchBean() {
        initProperties();

        if (((HttpServletRequest) WebStudioUtils.getExternalContext().getRequest()).getRequestURI().contains("search.xhtml")) {
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

    public void setTableTypes(String[] tableTypes) {
        this.tableTypes = tableTypes;
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
                TableProperty prop = new TableProperty(propDefinition, WebStudioFormats.getInstance());
                properties.add(prop);
            }
        }
    }

    private void initSearchQuery() {
        String query = WebStudioUtils.getRequestParameter("query");
        String tableTypes = WebStudioUtils.getRequestParameter("types");
        String tableHeader = WebStudioUtils.getRequestParameter("header");

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
        for (TableProperty property : properties) {
            String propertyValue = WebStudioUtils.getRequestParameter(property.getName());
            if (propertyValue!= null) {
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
