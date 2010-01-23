package org.openl.rules.ui.tablewizard;

import static org.openl.rules.ui.tablewizard.WizardUtils.getMetaInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;

public class PropertyTableCreationWizard extends WizardBase {

    private String selectedScopeType;    
    private String selectedCategoryName;
    private String categoryNameSelector;
    private String newCategoryName;
    private String tableName;
    private List<SelectItem> scopeTypes = new ArrayList<SelectItem>
                    (Arrays.asList(new SelectItem("Module"), new SelectItem("Category")));    
    List<SelectItem> categoryNamesList = new ArrayList<SelectItem>();
    
    private List<TypeNamePair> parameters = new ArrayList<TypeNamePair>();
    
    public String getSelectedCategoryName() {
        return selectedCategoryName;
    }

    public void setSelectedCategoryName(String selectedCategoryName) {
        this.selectedCategoryName = selectedCategoryName;
    }
    
    public List<SelectItem> getScopeTypes() {
        return scopeTypes;
    }

    public void setScopeTypes(List<SelectItem> scopeTypes) {
        this.scopeTypes = scopeTypes;
    }
    
    public String getSelectedScopeType() {
        return selectedScopeType;
    }

    public void setSelectedScopeType(String selectedScopeType) {
        this.selectedScopeType = selectedScopeType;
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
        this.newCategoryName = newCategoryName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public List<TypeNamePair> getParameters() {
        return parameters;
    }

    public void setParameters(List<TypeNamePair> parameters) {
        this.parameters = parameters;
    }

    public void setCategoryNamesList(List<SelectItem> categoryNamesList) {
        this.categoryNamesList = categoryNamesList;
    }

    @Override
    protected void onFinish(boolean cancelled) {
        // TODO Auto-generated method stub
    }

    @Override
    protected void onStart() {
        reset();
        initWorkbooks();
    }
    
    @Override
    public String getName() {
        return "newTableProperty";
    }

    public List<SelectItem> getCategoryNamesList() {
        List<SelectItem> result = Collections.EMPTY_LIST;
        if (categoryNameSelector != null && StringUtils.isNotEmpty(categoryNameSelector)) {            
        
            if (categoryNameSelector.equals("LIST")) {
                List<SelectItem> categoryNames = getAllCategoryNames();
                if (categoryNames.size() > 0) {
                    result = categoryNames;
                } else {
                    result = Collections.EMPTY_LIST;
                }                
            }        
        }
        return result;        
    }

    private List<SelectItem> getAllCategoryNames() {
        List<SelectItem> categoryNames = new ArrayList<SelectItem>();
        Set<String> names = new HashSet<String>();                
        TableSyntaxNode[] syntaxNodes = getMetaInfo().getXlsModuleNode().getXlsTableSyntaxNodes();
        for (TableSyntaxNode node : syntaxNodes) {
            ITableProperties tableProperties = node.getTableProperties();
            if (tableProperties != null) {
                String categoryName = tableProperties.getPropertyValueAsString("category");
                if (categoryName != null && StringUtils.isNotEmpty(categoryName)) {
                    names.add(categoryName);                            
                }
            }                    
        }                 
        for (String name : names) {
            categoryNames.add(new SelectItem(name));
        }
        return categoryNames;
    }

}
