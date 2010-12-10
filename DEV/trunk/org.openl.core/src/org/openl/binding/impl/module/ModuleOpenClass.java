/*
 * Created on Jul 25, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.exception.OpenLCompilationException;

import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenSchema;
import org.openl.types.impl.MethodKey;
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
    
    /**
     * Set of dependencies for current module.
     */
    private Set<CompiledOpenClass> usingModules = new HashSet<CompiledOpenClass>();
    
    public ModuleOpenClass(IOpenSchema schema, String name, OpenL openl) {
        super(schema, name, openl);
    } 
    
    /**
     * Constructor for module with dependent modules
     *
     */
    public ModuleOpenClass(IOpenSchema schema, String name, OpenL openl, Set<CompiledOpenClass> usingModules) {
        super(schema, name, openl);
        if (usingModules != null) {
            this.usingModules = new HashSet<CompiledOpenClass>(usingModules);
            try {
                initDependencies();
            } catch (OpenLCompilationException e) {
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError("Can`t add datatype", e, (ISyntaxNode) this);
                BindHelper.processError(error);
            }
        }
    }
    
    /**
     * Populate current module fields with data from dependent modules. 
     */
    private void initDependencies() throws OpenLCompilationException {    
        for (CompiledOpenClass dependency : usingModules) {
//            addTypes(dependency);
            addMethods(dependency);
        }
    }
    
    /**
     * Add datatypes from dependent modules to this one. 
     * Only one domain model is supported by a set of rules.
     * 
     * @param dependency compiled dependency module
     * @throws OpenLCompilationException if such datatype already presents.
     */
    private void addTypes(CompiledOpenClass dependency) throws OpenLCompilationException {
        Map<String, IOpenClass> dependentModuleTypes = dependency.getOpenClass().getTypes(); 
        for (String typeNamespace : dependentModuleTypes.keySet()) {
            add(typeNamespace, dependentModuleTypes.get(typeNamespace));
        }
    }
    
    /**
     * Add methods form dependent modules to current one.
     * 
     * @param dependency compiled dependency module
     */
    private void addMethods(CompiledOpenClass dependency) {
        for (IOpenMethod depMethod : dependency.getOpenClass().getMethods()) {
            // filter constructor and getOpenClass methods of dependency modules
            //
            if (!(depMethod instanceof OpenConstructor) && !(depMethod instanceof GetOpenClass)) {
                addMethod(depMethod);
            }
        }
    }

    /**
     * Overriden to add the possibility for overriding fields from dependent modules.<br>
     * At first tries to get the field from current module, if can`t search in dependencies.
     */
    @Override
    public IOpenField getField(String fname, boolean strictMatch) {
        // try to get field from own field map
        //
        IOpenField field = super.getField(fname, strictMatch);
        if (field != null) {
            return field;
        } else {
            // if can`t find, search in dependencies.
            //
            for (CompiledOpenClass dependency : usingModules) {
                field = dependency.getOpenClass().getField(fname, strictMatch);
                if (field != null) {
                    return field;
                }
            }
        }
        return null;
    }
        
    @Override
    public Map<String, IOpenField> getFields() {
        Map<String, IOpenField> fields = new HashMap<String, IOpenField>();

        // get fields from dependencies
        //
        for (CompiledOpenClass dependency : usingModules) {
            fields.putAll(dependency.getOpenClass().getFields());
        }

        // get own fields. if current module has duplicated fields they will
        // override the same from dependencies.
        //
        fields.putAll(super.getFields());

        return fields;
    }
    
    @Override
    public IOpenMethod getMethod(String name, IOpenClass[] classes) {
        
        IOpenMethod method = super.getMethod(name, classes);
        if (method != null) {
            return method;
        } else {
            // if can`t find, search in dependencies.
            //
            for (CompiledOpenClass dependency : usingModules) {
                method = dependency.getOpenClass().getMethod(name, classes);
                if (method != null) {
                    return method;
                }
            }
        }
        
        return null;
    }

    @Override
    public List<IOpenMethod> getMethods() {

        Map<MethodKey, IOpenMethod> methods = new HashMap<MethodKey, IOpenMethod>();

        // get methods from dependencies
        //
        for (CompiledOpenClass dependency : usingModules) {
            for (IOpenMethod method : dependency.getOpenClass().getMethods()) {
                if (!(method instanceof OpenConstructor) && !(method instanceof GetOpenClass)) {
                    methods.put(new MethodKey(method), method);
                }
            }
        }

        for (IOpenMethod method : super.getMethods()) {
            methods.put(new MethodKey(method), method);
        }

        return new ArrayList<IOpenMethod>(methods.values());
    }

    /**
     * Set compiled module dependencies for current module.
     * 
     * @param moduleDependencies
     */
    public void setDependencies(Set<CompiledOpenClass> moduleDependencies){
        if (moduleDependencies != null) {
            this.usingModules = new HashSet<CompiledOpenClass>(moduleDependencies);
        }
    }
    
    /**
     * Gets compiled module dependencies for current module.
     * @return compiled module dependencies for current module.
     */
    public Set<CompiledOpenClass> getDependencies() {
        return new HashSet<CompiledOpenClass>(usingModules);
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
        // it will contain all types from current module and dependent ones.
        //
        if (internalTypes.containsKey(name)) {
            return internalTypes.get(name);
        }
        
        // try to find type which is declared in dependency module
        //
        for (CompiledOpenClass dependency : usingModules) {
            IOpenClass type = dependency.getOpenClass().findType(namespace, typeName);
            if (type != null) {
                return type;
            }
        }
        
        return null;
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

    public IBindingContext makeBindingContext(IBindingContext topLevelContext) {        
        return new ModuleBindingContext(topLevelContext, this);
    }

}
