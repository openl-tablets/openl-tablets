package org.openl.rules.ui.tablewizard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.table.xls.builder.PropertiesTableBuilder;
import org.openl.rules.table.xls.builder.TableBuilder;
import org.openl.rules.tableeditor.renderkit.TableProperty;

public class PropertyTableCreationWizard extends BusinessTableCreationWizard {

    private PropertiesBean propertiesManager;
    private String scopeType;

    private String tableName;
    private List<SelectItem> scopeTypes = new ArrayList<SelectItem>(Arrays.asList(new SelectItem("Module"),
            new SelectItem("Category")));

    public PropertiesBean getPropertiesManager() {
        return propertiesManager;
    }

    public List<SelectItem> getScopeTypes() {
        return scopeTypes;
    }

    public void setScopeTypes(List<SelectItem> scopeTypes) {
        this.scopeTypes = scopeTypes;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public String next() {
        if (getStep() == 1){
            // After step two we create PropertiesBean according to specified scope
            propertiesManager = new PropertiesBean(getPropertyNamesList());
        }
        return super.next();
    }

    @Override
    protected void onStart() {
        reset();
        initWorkbooks();
    }

    @Override
    protected void onCancel() {
        reset();
    }

    @Override
    protected void reset() {
        super.reset();
    }

    @Override
    public String getName() {
        return "newTableProperty";
    }

    public List<String> getPropertyNamesList() {
        List<String> propertyNames = new ArrayList<String>();
        TablePropertyDefinition[] propDefinitions = TablePropertyDefinitionUtils
                .getDefaultDefinitionsByInheritanceLevel(InheritanceLevel.valueOf(scopeType.toUpperCase()));
        for (TablePropertyDefinition propDefinition : propDefinitions) {
            String propName = propDefinition.getName();
            List<String> exceptProperties = getExceptProperties();
            if (!exceptProperties.contains(propName)) {
                propertyNames.add(propName);
            }
        }
        return propertyNames;
    }

    private List<String> getExceptProperties() {
        List<String> exceptProps = new ArrayList<String>();
        exceptProps.add("scope");
        exceptProps.add("category");
        return exceptProps;
    }

    @Override
    protected void onFinish() throws Exception {
        XlsSheetSourceCodeModule sheetSourceModule = getDestinationSheet();
        String newTableUri = buildTable(sheetSourceModule);
        setNewTableUri(newTableUri);
        getModifiedWorkbooks().add(sheetSourceModule.getWorkbookSource());
        super.onFinish();
    }

    protected String buildTable(XlsSheetSourceCodeModule sourceCodeModule) throws CreateTableException {
        XlsSheetGridModel gridModel = new XlsSheetGridModel(sourceCodeModule);
        PropertiesTableBuilder builder = new PropertiesTableBuilder(gridModel);
        Map<String, Object> properties = buildProperties();
        builder.beginTable(TableBuilder.HEADER_HEIGHT + properties.size());
        builder.writeHeader(tableName);
        builder.writeBody(properties);

        String uri = gridModel.getRangeUri(builder.getTableRegion());

        builder.endTable();
        return uri;
    }

    @Override
    protected Map<String, Object> buildProperties() {
        Map<String, Object> resultProperties = new LinkedHashMap<String, Object>();
        resultProperties.put("scope", scopeType);
        if (InheritanceLevel.CATEGORY.getDisplayName().equals(scopeType)) {
            String categoryName = buildCategoryName();
            if (categoryName != null) {
                resultProperties.put("category", categoryName);
            }
        }
        for (TableProperty property : propertiesManager.getProperties()) {
            String name = property.getName();
            Object value = property.getValue();
            if (value == null || (value != null && (value instanceof String && StringUtils.isEmpty((String) value)))) {
                continue;
            } else {
                resultProperties.put(name.trim(), value);
            }
        }
        return resultProperties;
    }

}
