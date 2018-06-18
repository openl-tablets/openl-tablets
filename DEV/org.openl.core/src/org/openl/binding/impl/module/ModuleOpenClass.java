/*
 * Created on Jul 25, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.AMethod;
import org.openl.util.Log;

/**
 * {@link IOpenClass} implementation for full module.<br>
 * It is a common class for different sources module implementations.
 * 
 * @author snshor
 *
 */
public class ModuleOpenClass extends ComponentOpenClass {

    /**
     * Map of internal types. XLS document can have internal types defined using <code>Datatype</code> tables, e.g.
     * domain model.<br>
     * 
     * Key: type name.<br>
     * Value: {@link IOpenClass} for datatype.
     */
    private ConcurrentHashMap<String, IOpenClass> internalTypes = new ConcurrentHashMap<String, IOpenClass>();
    private Collection<IOpenClass> types = Collections.unmodifiableCollection(internalTypes.values());

    /**
     * Set of dependencies for current module.
     * 
     * NOTE!!! Be careful when calling {@link CompiledOpenClass#getOpenClass()} as it throws errors when there are any
     * ones in {@link CompiledOpenClass}. Check if there are errors: {@link CompiledOpenClass#hasErrors()}
     * 
     */
    private Set<CompiledDependency> usingModules = new HashSet<CompiledDependency>();

    private List<Exception> errors = new ArrayList<Exception>();

    public ModuleOpenClass(String name, OpenL openl) {
        super(name, openl);
    }

    /**
     * Populate current module fields with data from dependent modules.
     */
    protected void initDependencies() throws OpenLCompilationException {
        for (CompiledDependency dependency : usingModules) {
            // commented as there is no need to add each datatype to upper module.
            // as now it`s will be impossible to validate from which module the datatype is.
            //
            // addTypes(dependency);
            addDependencyTypes(dependency);
            addMethods(dependency);
            addFields(dependency);
        }
    }

    protected boolean isDependencyMethodIgnorable(IOpenMethod method) {
        return false;
    }

    /**
     * Add methods form dependent modules to current one.
     * 
     * @param dependency compiled dependency module
     */
    protected void addMethods(CompiledDependency dependency) {
        CompiledOpenClass compiledOpenClass = dependency.getCompiledOpenClass();
        for (IOpenMethod depMethod : compiledOpenClass.getOpenClassWithErrors().getMethods()) {
            // filter constructor and getOpenClass methods of dependency modules
            //
            if (!(depMethod.isConstructor()) && !(depMethod instanceof GetOpenClass)) {
                try {
                    // Workaround for set dependency names in method while compile
                    if (depMethod instanceof AMethod) {
                        AMethod methodDependencyInfo = (AMethod) depMethod;
                        if (methodDependencyInfo.getModuleName() == null) {
                            methodDependencyInfo.setModuleName(dependency.getDependencyName());
                        }
                    }
                    if (!isDependencyMethodIgnorable(depMethod)) {
                        addMethod(depMethod);
                    }
                } catch (OpenlNotCheckedException e) {
                    if (Log.isDebugEnabled()) {
                        Log.debug(e.getMessage(), e);
                    }
                    addError(e);
                }
            }
        }
    }

    protected boolean isDependencyFieldIgnorable(IOpenField openField) {
        return true;
    }

    protected void addFields(CompiledDependency dependency) {
        CompiledOpenClass compiledOpenClass = dependency.getCompiledOpenClass();
        for (IOpenField depField : compiledOpenClass.getOpenClassWithErrors().getFields().values()) {
            try {
                if (!isDependencyFieldIgnorable(depField)) {
                    addField(depField);
                }
            } catch (OpenlNotCheckedException e) {
                if (Log.isDebugEnabled()) {
                    Log.debug(e.getMessage(), e);
                }
                addError(e);
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

    private Map<String, IOpenField> dependencyFields = null;

    @Override
    public Map<String, IOpenField> getFields() {
        Map<String, IOpenField> fields = new HashMap<String, IOpenField>();

        // get fields from dependencies
        //
        if (dependencyFields == null) {
            synchronized (this) {
                if (dependencyFields == null) {
                    dependencyFields = new HashMap<String, IOpenField>();
                    for (CompiledDependency dependency : usingModules) {
                        CompiledOpenClass compiledOpenClass = dependency.getCompiledOpenClass();
                        if (!compiledOpenClass.hasErrors()) {
                            dependencyFields.putAll(compiledOpenClass.getOpenClass().getFields());
                        }
                    }
                }
            }
        }
        fields.putAll(dependencyFields);

        // get own fields. if current module has duplicated fields they will
        // override the same from dependencies.
        //
        fields.putAll(super.getFields());

        return fields;
    }

    /**
     * Set compiled module dependencies for current module.
     */
    public void setDependencies(Set<CompiledDependency> moduleDependencies) {
        if (moduleDependencies != null) {
            this.usingModules = new HashSet<CompiledDependency>(moduleDependencies);
        }
    }

    /**
     * Gets compiled module dependencies for current module.
     * 
     * @return compiled module dependencies for current module.
     */
    public Set<CompiledDependency> getDependencies() {
        if (usingModules == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(usingModules);
    }

    protected void addDependencyTypes(CompiledDependency dependency) {
        CompiledOpenClass compiledOpenClass = dependency.getCompiledOpenClass();
        for (IOpenClass type : compiledOpenClass.getTypes()) {
            try {
                addType(type);
            } catch (OpenLCompilationException e) {
                addError(e);
            }
        }
    }

    /**
     * Return the whole map of internal types. Where the key is namespace of the type, the value is {@link IOpenClass}.
     * 
     * @return map of internal types
     */
    @Override
    public Collection<IOpenClass> getTypes() {
        return types;
    }

    /**
     * Add new type to internal types list. If the type with the same name already exists exception will be thrown.
     * 
     * @param type IOpenClass instance
     * @throws OpenLCompilationException if an error had occurred.
     */
    @Override
    public void addType(IOpenClass type) throws OpenLCompilationException {
        IOpenClass openClass = internalTypes.putIfAbsent(type.getName(), type);
        if (openClass != null && !openClass.equals(type)) {
            throw new OpenLCompilationException("The type " + type.getName() + " has been already defined.");
        }
    }

    @Override
    public IOpenClass findType(String name) {
        return internalTypes.get(name);
    }

    public void addError(Exception error) {
        errors.add(error);
    }

    public List<Exception> getErrors() {
        return errors;
    }
}
