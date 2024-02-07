
package org.openl.rules.webstudio.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.faces.model.SelectItem;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;
import org.yaml.snakeyaml.util.UriEncoder;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.def.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.tableeditor.renderkit.TableProperty;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.rules.webstudio.web.search.AISearch;
import org.openl.rules.webstudio.web.search.SearchResult;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

@Service
@RequestScope
public class SearchBean {

    // TODO Move table names to Rules Core
    private static final SelectItem[] tableTypeItems = new SelectItem[]{
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
            new SelectItem(XlsNodeTypes.XLS_OTHER.toString(), "Other")};

    private static final int MAX_TABLES_FOR_AI_SEARCH_TO_IGNORE_XLS_OTHER = 1000;

    private boolean active;
    private String query;
    private String[] tableTypes;
    private String tableHeader;
    private SearchScope searchScope = SearchScope.CURRENT_MODULE;
    private final List<TableProperty> properties = new ArrayList<>();

    private List<IOpenLTable> searchResults;

    private final AISearch aiSearch;

    private int expectedIndexingDuration;
    private int tableCountForIndexing;

    public SearchBean(AISearch aiSearch) {
        this.aiSearch = aiSearch;
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

    public int getExpectedIndexingDuration() {
        return expectedIndexingDuration;
    }

    public int getTableCountForIndexing() {
        return tableCountForIndexing;
    }

    public SelectItem[] getSearchScopeItems() {
        List<SelectItem> selectItems = new ArrayList<>();
        if (WebStudioUtils.getWebStudio().getCurrentModule() != null) {
            selectItems.add(new SelectItem(SearchScope.CURRENT_MODULE, SearchScope.CURRENT_MODULE.getLabel()));
        }
        selectItems.add(new SelectItem(SearchScope.CURRENT_PROJECT, SearchScope.CURRENT_PROJECT.getLabel()));
        selectItems.add(new SelectItem(SearchScope.ALL, SearchScope.ALL.getLabel()));
        return selectItems.toArray(new SelectItem[0]);
    }

    public List<IOpenLTable> getSearchResults() {
        return searchResults;
    }

    public String getQuery() {
        return query;
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
        this.active = false;
        if (WebStudioUtils.getExternalContext().getRequestPathInfo().endsWith("search.xhtml")) {
            this.active = true;
            String query = WebStudioUtils.getRequestParameter("query");
            String tableTypes = WebStudioUtils.getRequestParameter("types");
            String tableHeader = WebStudioUtils.getRequestParameter("header");
            String searchScope = WebStudioUtils.getRequestParameter("searchScope");
            String useAiSearch = WebStudioUtils.getRequestParameter("useAiSearch");
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
            this.searchScope = WebStudioUtils.getWebStudio().getCurrentModule() == null ? SearchScope.CURRENT_PROJECT
                    : SearchScope.CURRENT_MODULE;
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
        if (this.active) {
            ProjectModel projectModel = WebStudioUtils.getProjectModel();

            Predicate<TableSyntaxNode> selectors = (e) -> true;

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

            projectModel.compileProject(true, true);

            List<TableSyntaxNode> tnses = projectModel.getSearchScopeData(searchScope)
                    .stream()
                    .filter(tableSyntaxNode -> !XlsNodeTypes.XLS_TABLEPART.toString().equals(tableSyntaxNode.getType()))
                    .filter(tsn -> !projectModel.isGapOverlap(tsn))
                    .filter(selectors)
                    .collect(Collectors.toList());
            String q = query != null ? UriEncoder.decode(query) : null;
            Predicate<TableSyntaxNode> cellValueSelector = new CellValueSelector(q);

            LinkedHashMap<TableSyntaxNode, Integer> result = new LinkedHashMap<>();
            List<TableSyntaxNode> foundTsnes = tnses.stream().filter(cellValueSelector).collect(Collectors.toList());
            foundTsnes.forEach(e -> result.put(e, 1));
            if (StringUtils.isNotBlank(q)) {
                // AI search is expensive operation. It is better to use only actual tables for it.
                if (tnses.size() > MAX_TABLES_FOR_AI_SEARCH_TO_IGNORE_XLS_OTHER) {
                    tnses = tnses.stream()
                            .filter(e -> !e.getType().equals(XlsNodeTypes.XLS_OTHER.toString()))
                            .collect(Collectors.toList());
                }
                SearchResult searchResult = aiSearch.filter(q, tnses);
                List<TableSyntaxNode> tnsesByAiSearch = searchResult.getTableSyntaxNodes();
                expectedIndexingDuration = searchResult.getExpectedIndexingDuration();
                tableCountForIndexing = searchResult.getTableCountForIndexing();
                tnsesByAiSearch.forEach(e -> result.compute(e, (key, value) -> (value == null) ? 1 : value + 1));
            } else {
                expectedIndexingDuration = 0;
                tableCountForIndexing = 0;
            }

            List<Map.Entry<TableSyntaxNode, Integer>> entryList = new ArrayList<>(result.entrySet());
            entryList.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));
            searchResults = entryList.stream()
                    .map(Map.Entry::getKey)
                    .map(TableSyntaxNodeAdapter::new)
                    .collect(Collectors.toList());
        }
    }

}
