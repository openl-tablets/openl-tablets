/*
 * Created on Jul 25, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.module;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenSchema;

/**
 * {@link IOpenClass} implementation for full module.<br>
 * It is a common class for different sources module implementations.
 * 
 * @author snshor
 *
 */
public class ModuleOpenClass extends ComponentOpenClass {    
    
    /**
     * Map of internal types. XLS document can have internal types defined using
     * <code>Datatype</code> tables, e.g. domain model.
     */
    private Map<String, IOpenClass> internalTypes = new HashMap<String, IOpenClass>();
    
    /**
     * Set of dependencies for current module.
     */
    private Set<CompiledOpenClass> moduleDependencies = new HashSet<CompiledOpenClass>();

    public ModuleOpenClass(IOpenSchema schema, String name, OpenL openl) {
        super(schema, name, openl);
    } 

    public IBindingContext makeBindingContext(IBindingContext parentContext) {
        return new ModuleBindingContext(parentContext, this);
    }
    
    /**
     * Set compiled module dependencies for current module.
     * 
     * @param moduleDependencies
     */
    public void setDependencies(Set<CompiledOpenClass> moduleDependencies){
        this.moduleDependencies = moduleDependencies;
    }
    
    /**
     * Gets compiled module dependencies for current module.
     * @return compiled module dependencies for current module.
     */
    public Set<CompiledOpenClass> getDependencies() {
        return moduleDependencies;
    }
    
    /**
     * Return the whole map of internal types. Where the key is namespace of the type, 
     * the value is {@link IOpenClass}.
     * 
     * @return map of internal types 
     */
    @Override
    public Map<String, IOpenClass> getTypes() {
        return new HashMap<String, IOpenClass>(internalTypes);
    }
    
    /**
     * Finds type with given name in internal type list. If type with given name
     * exists in list it will be returned; <code>null</code> - otherwise.
     * 
     * @param typeName
     *            name of type to search
     * @return {@link IOpenClass} instance or <code>null</code>
     */
    @Override
    public IOpenClass findType(String namespace, String typeName) {
        
        String name = buildFullTypeName(namespace, typeName);
        
        return internalTypes.get(name);
    }
    
    /**
     * Builds full type name using namespace and type names.
     * 
     * @param namespace
     *            type namespace
     * @param type
     *            type name
     * @return full name string
     */
    private String buildFullTypeName(String namespace, String type) {
        
        return String.format("%s.%s", namespace, type);
    }
    
    /**
     * Add new type to internal types list. If the type with the same name
     * already exists exception will be thrown.
     * 
     * @param type
     *            IOpenClass instance
     * @throws Exception
     *             if an error had occurred.
     */
    @Override
    public void addType(String namespace, IOpenClass type) throws Exception {
        
        String typeName = buildFullTypeName(namespace, type.getName());
        
        if (internalTypes.containsKey(typeName)) {
            throw new Exception("The type " + typeName + " has been defined already");
        }
        
        internalTypes.put(typeName, type);
    }
}
