/*
 * Created on Jul 25, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.AMethod;
import org.openl.types.impl.MethodKey;
import org.openl.util.Log;
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
     * 
     * NOTE!!!
     * Be careful when calling {@link CompiledOpenClass#getOpenClass()} as it
     * throws errors when there are any ones in {@link CompiledOpenClass}.
     * Check if there are errors: {@link CompiledOpenClass#hasErrors()} 
     * 
     */
    private Set<CompiledDependency> usingModules = new HashSet<CompiledDependency>();

    private List<Throwable> errors = new ArrayList<Throwable>();

    public ModuleOpenClass(String name, OpenL openl) {
        super(name, openl);
    }

    /**
     * Populate current module fields with data from dependent modules. 
     */
    protected void initDependencies() {
        for (CompiledDependency dependency : usingModules) {
            // commented as there is no need to add each datatype to upper module.
            // as now it`s will be impossible to validate from which module the datatype is.
            //
            //addTypes(dependency);
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
//    private void addTypes(CompiledOpenClass dependency) throws OpenLCompilationException {
//        Map<String, IOpenClass> dependentModuleTypes = dependency.getOpenClass().getTypes(); 
//        for (String typeNamespace : dependentModuleTypes.keySet()) {
//            add(typeNamespace, dependentModuleTypes.get(typeNamespace));
//        }
//    }
    
    /**
     * Add methods form dependent modules to current one.
     * 
     * @param dependency compiled dependency module
     */
    private void addMethods(CompiledDependency dependency) {
        CompiledOpenClass compiledOpenClass = dependency.getCompiledOpenClass();
        for (IOpenMethod depMethod : compiledOpenClass.getOpenClassWithErrors().getMethods()) {
            // filter constructor and getOpenClass methods of dependency modules
            //
            if (!(depMethod instanceof OpenConstructor) && !(depMethod instanceof GetOpenClass)) {
                try {
                    //Workaround for set dependency names in method while compile
                    if (depMethod instanceof AMethod){
                        AMethod methodDependencyInfo = (AMethod) depMethod;
                        if (methodDependencyInfo.getModuleName() == null){
                            methodDependencyInfo.setModuleName(dependency.getDependencyName());
                        }
                    }
                    addMethod(depMethod);
                } catch (OpenlNotCheckedException e) {
                    if (Log.isDebugEnabled()) {
                        Log.debug(e.getMessage(), e);
                    }
                    addError(e);
                }
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
            for (CompiledDependency dependency : usingModules) {
                CompiledOpenClass compiledOpenClass = dependency.getCompiledOpenClass();
                if (!compiledOpenClass.hasErrors()) {
                    field = compiledOpenClass.getOpenClass().getField(fname, strictMatch);
                    if (field != null) {
                        return field;
                    }
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
        for (CompiledDependency dependency : usingModules) {
            CompiledOpenClass compiledOpenClass = dependency.getCompiledOpenClass(); 
            if (!compiledOpenClass.hasErrors()) {
                fields.putAll(compiledOpenClass.getOpenClass().getFields());
            }            
        }

        // get own fields. if current module has duplicated fields they will
        // override the same from dependencies.
        //
        fields.putAll(super.getFields());

        return fields;
    }

    /**
     * Set compiled module dependencies for current module.
     */
    public void setDependencies(Set<CompiledDependency> moduleDependencies){
        if (moduleDependencies != null) {
            this.usingModules = new HashSet<CompiledDependency>(moduleDependencies);
        }
    }
    
    /**
     * Gets compiled module dependencies for current module.
     * @return compiled module dependencies for current module.
     */
    public Set<CompiledDependency> getDependencies() {
        if (usingModules == null){
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(usingModules);
    }
    
    /**
     * Return the whole map of internal types. Where the key is namespace of the type, 
     * the value is {@link IOpenClass}.
     * 
     * @return map of internal types 
     */
    @Override
    public Map<String, IOpenClass> getTypes() {
        Map<String, IOpenClass> currentModuleDatatypes = new HashMap<String, IOpenClass>(internalTypes);
        for (CompiledDependency dependency : usingModules) {
            CompiledOpenClass compiledOpenClass = dependency.getCompiledOpenClass();
            currentModuleDatatypes.putAll(compiledOpenClass.getTypes());
        }
        
        return currentModuleDatatypes;
    }

    /**
     * Add new type to internal types list. If the type with the same name
     * already exists exception will be thrown.
     * 
     * @param type
     *            IOpenClass instance
     * @throws OpenLCompilationException
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

    public void addError(Throwable error) {
        errors.add(error);
    }

    public List<Throwable> getErrors() {
        return errors;
    }
}
