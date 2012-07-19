/*
 * Created on Jul 25, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.module;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.exception.OpenLCompilationException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenSchema;
import org.openl.util.StringTool;

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
     * <code>Datatype</code> tables, e.g. domain model.<br>
     * 
     * Key: type name with namespace see {@link StringTool#buildTypeName(String, String)}.<br>
     * Value: {@link IOpenClass} for datatype.
     */
    private Map<String, IOpenClass> internalTypes = new HashMap<String, IOpenClass>();
    
    
    public ModuleOpenClass(IOpenSchema schema, String name, OpenL openl) {
        this(schema, name, openl, null);
    } 
    
    /**
     * Constructor for module with dependent modules
     *
     */
    public ModuleOpenClass(IOpenSchema schema, String name, OpenL openl, Set<CompiledOpenClass> usingModules) {
        super(schema, name, openl);
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
    public void addType(String namespace, IOpenClass type) throws OpenLCompilationException {        
        String typeNameWithNamespace = StringTool.buildTypeName(namespace, type.getName());
        
        add(typeNameWithNamespace, type);
    }
    
    /**
     * Adds type to map of internal types.
     * 
     * @param typeNameWithNamespace type name with namespace, e.g. see {@link StringTool#buildTypeName(String, String)}.
     * @param type {@link IOpenClass} for this type
     * @throws OpenLCompilationException if such type already exists.
     */
    private void add(String typeNameWithNamespace, IOpenClass type) throws OpenLCompilationException {
        if (internalTypes.containsKey(typeNameWithNamespace)) {
            throw new OpenLCompilationException("The type " + typeNameWithNamespace + " has been already defined.");
        }
        
        internalTypes.put(typeNameWithNamespace, type);
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
        
        String name = StringTool.buildTypeName(namespace, typeName);
        // it will contain types from current module.
        //
        if (internalTypes.containsKey(name)) {
            return internalTypes.get(name);
        }
        return null;
    }

    public IBindingContext makeBindingContext(IBindingContext topLevelContext) {        
        return new ModuleBindingContext(topLevelContext, this);
    }

}
