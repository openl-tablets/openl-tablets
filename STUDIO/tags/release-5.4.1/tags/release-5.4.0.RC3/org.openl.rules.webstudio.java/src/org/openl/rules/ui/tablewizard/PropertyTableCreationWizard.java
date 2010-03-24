package org.openl.rules.ui.tablewizard;

import static org.openl.rules.ui.tablewizard.WizardUtils.getMetaInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.table.xls.builder.PropertiesTableBuilder;
import org.openl.rules.tableeditor.renderkit.TableProperty;

public class PropertyTableCreationWizard extends WizardBase {

    private String scopeType;    
    private String categoryName;
    private String newCategoryName;
    private String categoryNameSelector = "existing";
    private String tableName;
    private String propNameToAdd;
    private TableProperty propToRemove;
    private List<SelectItem> scopeTypes = new ArrayList<SelectItem>
                    (Arrays.asList(new SelectItem("Module"), new SelectItem("Category")));
    private List<TableProperty> properties = new ArrayList<TableProperty>();

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.newCategoryName = null;
        this.categoryName = categoryName;
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

    public String getCategoryNameSelector() {
        return categoryNameSelector;
    }

    public void setCategoryNameSelector(String categoryNameSelector) {
        this.categoryNameSelector = categoryNameSelector;
    }

    public String getNewCategoryName() {
        return newCategoryName;
    }

    public void setNewCategoryName(String newCategoryName) {
        this.categoryName = null;
        this.newCategoryName = newCategoryName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getPropNameToAdd() {
        return propNameToAdd;
    }

    public void setPropNameToAdd(String propNameToAdd) {
        this.propNameToAdd = propNameToAdd;
    }

    public TableProperty getPropToRemove() {
        return propToRemove;
    }

    public void setPropToRemove(TableProperty propToRemove) {
        this.propToRemove = propToRemove;
    }

    @Override
    protected void onStart() {
        reset();
        initWorkbooks();
    }

    @Override
    protected void onFinish(boolean cancelled) {
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

    public List<SelectItem> getCategoryNamesList() {
        List<SelectItem> categoryList = new ArrayList<SelectItem>();
        Set<String> categories = getAllCategories();
        for (String categoryName : categories) {
            categoryList.add(new SelectItem(categoryName));
        }
        return categoryList;
    }

    private Set<String> getAllCategories() {
        Set<String> categories = new TreeSet<String>();
        TableSyntaxNode[] syntaxNodes = getMetaInfo().getXlsModuleNode().getXlsTableSyntaxNodes();
        for (TableSyntaxNode node : syntaxNodes) {
            ITableProperties tableProperties = node.getTableProperties();
            if (tableProperties != null) {
                String categoryName = tableProperties.getCategory();
                if (StringUtils.isNotBlank(categoryName)) {
                    categories.add(categoryName);
                }
            }
        }
        return categories;
    }

    public List<TableProperty> getProperties() {
        return properties;
    }

    public List<SelectItem> getPropertyNamesList() {
        List<SelectItem> propertyNames = new ArrayList<SelectItem>();
        TablePropertyDefinition[] propDefinitions = TablePropertyDefinitionUtils
            .getDefaultDefinitionsByInheritanceLevel(InheritanceLevel.valueOf(scopeType.toUpperCase()));
        for (TablePropertyDefinition propDefinition : propDefinitions) {
            String propName = propDefinition.getName();
            List<String> exceptProperties = getExceptProperties();
            if (!exceptProperties.contains(propName)) {
                propertyNames.add(new SelectItem(propName, propDefinition.getDisplayName()));
            }
        }
        return propertyNames;
    }

    private List<String> getExceptProperties() {
        List<String> exceptProps = new ArrayList<String>();
        exceptProps.add("scope");
        exceptProps.add("category");
        for (TableProperty property : properties) {
            String name = property.getName();
            exceptProps.add(name);
        }
        return exceptProps;
    }

    private TablePropertyDefinition getPropByName(String name) {
        TablePropertyDefinition[] propDefinitions = TablePropertyDefinitionUtils
            .getDefaultDefinitionsByInheritanceLevel(InheritanceLevel.valueOf(scopeType.toUpperCase()));
        for (TablePropertyDefinition propDefinition : propDefinitions) {
            if (propDefinition.getName().equals(name)) {
                return propDefinition;
            }
        }
        return null;
    }

    public void addProperty() {
        TablePropertyDefinition propDefinition = getPropByName(propNameToAdd);
        Class<?> propType = propDefinition.getType() == null ? String.class : propDefinition.getType()
                .getInstanceClass();
        properties.add(new TableProperty.TablePropertyBuilder(propDefinition.getName(),
                propDefinition.getDisplayName()).type(propType).format(propDefinition.getFormat()).build());
    }

    public void removeProperty() {
        properties.remove(propToRemove);
    }

    public String save() {
        try {
            doSave();
            return "done";
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Could not create table: ", e.getMessage()));
        }
        return null;
    }

    private void doSave() throws CreateTableException, IOException {
        PropertiesTableBuilder builder = new PropertiesTableBuilder(
                new XlsSheetGridModel(getDestinationSheet()));
        Map<String, Object> properties = buildProperties();
        builder.beginTable(properties.size() + 1);
        builder.writeHeader(tableName);
        builder.writeBody(properties);
        builder.endTable();
        builder.save();
    }

    private String buildCategoryName() {
        String categoryName;
        if (StringUtils.isNotBlank(this.categoryName)) {
            categoryName = this.categoryName;
        } else if (StringUtils.isNotBlank(newCategoryName)) {
            categoryName = this.newCategoryName;
        } else {
            // this for the case when sheet name selected for category name.
            categoryName = null;
        }
        return categoryName;
    }

    private Map<String, Object> buildProperties() {
        Map<String, Object> resultProperties = new LinkedHashMap<String, Object>();
        resultProperties.put("scope", scopeType);
        if (InheritanceLevel.CATEGORY.getDisplayName().equals(scopeType)) {
            String categoryName = buildCategoryName();
            if (categoryName != null) {
                resultProperties.put("category", categoryName);
            }            
        }
        for (int i = 0; i < properties.size(); i++) {
            String name = (properties.get(i)).getName();
            Object value = (properties.get(i)).getValue();
            if (value == null
                    || (value != null && (value instanceof String && StringUtils.isEmpty((String) value)))) {
                continue;
            } else {
                resultProperties.put(name.trim(), value);
            }
        }
        return resultProperties;
    }

}
