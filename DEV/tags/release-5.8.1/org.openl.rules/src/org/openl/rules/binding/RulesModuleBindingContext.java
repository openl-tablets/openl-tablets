/**
 * 
 */
package org.openl.rules.binding;

import java.util.HashMap;
import java.util.Map;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.module.ModuleBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * Binding context for xls rules.
 * 
 * @author DLiauchuk
 *
 */
public class RulesModuleBindingContext extends ModuleBindingContext {
    public static String MODULE_PROPERTIES_KEY = "Properties:Module";
    public static String CATEGORY_PROPERTIES_KEY = "Properties:Category:";
    private Map<String, TableSyntaxNode> bindedTables = new HashMap<String, TableSyntaxNode>();
    
    public RulesModuleBindingContext(IBindingContext delegate,
            ModuleOpenClass module) {
        super(delegate, module);
    }

    /**
     * Registers the tsn by specified key.
     * 
     * @param key Key that have to be same for equivalent tables.
     * @param tsn TableSyntaxNode to register.
     */
    public void registerTableSyntaxNode(String key, TableSyntaxNode tsn) {
        this.bindedTables.put(key, tsn);
    }

    /**
     * @return <code>true</code> if key TableSyntaxNode with specified key has
     *         already been registered.
     */
    public boolean isTableSyntaxNodeExist(String key) {
        return this.bindedTables.containsKey(key);
    }

    public TableSyntaxNode getTableSyntaxNode(String key) {
        return bindedTables.get(key);
    }
}
