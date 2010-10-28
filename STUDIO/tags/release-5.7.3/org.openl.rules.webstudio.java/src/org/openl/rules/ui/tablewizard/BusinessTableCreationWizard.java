package org.openl.rules.ui.tablewizard;

import static org.openl.rules.ui.tablewizard.WizardUtils.getMetaInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.faces.model.SelectItem;
import javax.validation.constraints.Pattern;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.def.TablePropertyDefinition.SystemValuePolicy;
import org.openl.rules.webstudio.properties.SystemValuesManager;

public abstract class BusinessTableCreationWizard extends WizardBase {

    @Pattern(regexp="([a-zA-Z_][a-zA-Z_0-9\\- ]*)?", message="Invalid business name")
    private String businessName;

    private String categoryName;
    private String newCategoryName;
    private String categoryNameSelector = "existing";

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.newCategoryName = null;
        this.categoryName = categoryName;
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

    protected String buildCategoryName() {
        String categoryName;
        if (StringUtils.isNotBlank(this.categoryName)) {
            categoryName = this.categoryName;
        } else if (StringUtils.isNotBlank(newCategoryName)) {
            categoryName = this.newCategoryName;
        } else {
            // This for the case when 'Sheet name' selected for category name.
            categoryName = null;
        }
        return categoryName;
    }

    protected Map<String, Object> buildSystemProperties() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();

        List<TablePropertyDefinition> systemPropDefinitions = TablePropertyDefinitionUtils.getSystemProperties();
        for (TablePropertyDefinition systemPropDef : systemPropDefinitions) {
            if (systemPropDef.getSystemValuePolicy().equals(SystemValuePolicy.IF_BLANK_ONLY)) {
                Object systemValue = SystemValuesManager.getInstance().
                    getSystemValue(systemPropDef.getSystemValueDescriptor());
                if (systemValue != null) {
                    result.put(systemPropDef.getName(), systemValue);
                }
            }
        }

        return result;
    }

    protected Map<String, Object> buildProperties() {
        Map<String, Object> properties = new LinkedHashMap<String, Object>();

        // Put business properties.
        if (StringUtils.isNotBlank(this.businessName)) {
            properties.put("name", this.businessName);
        }
        String category = buildCategoryName();
        if (category != null) {
            properties.put("category", category);
        }

        // Put system properties.
        Map<String, Object> systemProperties = buildSystemProperties();
        properties.putAll(systemProperties);

        return properties;
    }

}
