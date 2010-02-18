/**
 * 
 */
package org.openl.rules.binding;

import java.util.HashMap;
import java.util.Map;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.module.ModuleBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.table.properties.TableProperties;

/**
 * @author DLiauchuk
 *
 */
public class RulesModuleBindingContext extends ModuleBindingContext {

    private TableProperties moduleProperties;
    
    /**
     * Map of properties: key - category name, value - properties for this category. 
     */
    private Map<String, TableProperties> categoryProperties = new HashMap<String, TableProperties>();
    
    /**
     * @param delegate
     * @param module
     */
    public RulesModuleBindingContext(IBindingContext delegate,
            ModuleOpenClass module) {
        super(delegate, module);
    }

    public void addCategoryProperties(String categoryName, TableProperties categoryProperties) {        
        this.categoryProperties.put(categoryName, categoryProperties);
    }

    public boolean isExistCategoryProperties(String categoryName) {
        return this.categoryProperties.containsKey(categoryName);
    }

    public void setModuleProperties(TableProperties theModuleProperties) {
        moduleProperties = theModuleProperties;
    }

    public boolean isExistModuleProperties() {
        return moduleProperties != null;
    }
    
    public TableProperties getCategotyProperties(String category) {
        return categoryProperties.get(category);
    }
    
    public TableProperties getModuleProperties() {
        return moduleProperties;
    }
}
