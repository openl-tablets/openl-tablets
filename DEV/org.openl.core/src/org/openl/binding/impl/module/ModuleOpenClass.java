/*
 * Created on Jul 25, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.exception.DuplicatedTypeException;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;

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
    private final ConcurrentHashMap<String, IOpenClass> internalTypes = new ConcurrentHashMap<>();
    private final Collection<IOpenClass> types = Collections.unmodifiableCollection(internalTypes.values());

    /**
     * Set of dependencies for current module.
     *
     * NOTE!!! Be careful when calling {@link CompiledOpenClass#getOpenClass()} as it throws errors when there are any
     * ones in {@link CompiledOpenClass}. Check if there are errors: {@link CompiledOpenClass#hasErrors()}
     *
     */
    private Set<CompiledDependency> usingModules = new LinkedHashSet<>();

    private volatile Collection<IOpenField> dependencyFields = null;

    // This field is used to refer to correct module name that is used in the system, the name of XlsModuleOpenClass can
    // be different if the module name is not matched to the java naming restrictions.
    private final String moduleName;

    public ModuleOpenClass(String moduleName, OpenL openl) {
        super(makeJavaIdentifier(moduleName), openl);
        this.moduleName = moduleName;
    }

    private static String makeJavaIdentifier(String src) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (i == 0) {
                buf.append(Character.isJavaIdentifierStart(c) ? c : '_');
            } else {
                buf.append(Character.isJavaIdentifierPart(c) ? c : '_');
            }
        }

        return buf.toString();
    }

    public String getModuleName() {
        return moduleName;
    }

    protected boolean isDependencyMethodInheritable(IOpenMethod method) {
        return true;
    }

    protected boolean isDependencyFieldInheritable(IOpenField openField) {
        return false;
    }


    /**
     * Overriden to add the possibility for overriding fields from dependent modules.<br>
     * At first tries to get the field from current module, if can`t search in dependencies.
     */
    @Override
    public IOpenField getField(String fname, boolean strictMatch) throws AmbiguousFieldException {
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
                field = compiledOpenClass.getOpenClassWithErrors().getField(fname, strictMatch);
                if (field != null) {
                    return field;
                }
            }
        }
        return null;
    }

    private Collection<IOpenField> createDependencyFields() {
        Collection<IOpenField> fields = new ArrayList<>();
        for (CompiledDependency dependency : usingModules) {
            CompiledOpenClass compiledOpenClass = dependency.getCompiledOpenClass();
            fields.addAll(compiledOpenClass.getOpenClassWithErrors().getFields());
        }
        return fields;
    }

    @Override
    public Collection<IOpenField> getFields() {
        Collection<IOpenField> fields = new ArrayList<>();
        // get fields from dependencies
        //
        if (this.dependencyFields == null) {
            synchronized (this) {
                this.dependencyFields = createDependencyFields();
            }
        }
        fields.addAll(dependencyFields);

        // get own fields. if current module has duplicated fields they will
        // override the same from dependencies.
        //
        fields.addAll(super.getFields());
        return fields;
    }

    /**
     * Set compiled module dependencies for current module.
     */
    public void setDependencies(Set<CompiledDependency> moduleDependencies) {
        if (moduleDependencies != null) {
            this.usingModules = new LinkedHashSet<>(moduleDependencies);
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

    protected IOpenClass processDependencyTypeBeforeAdding(IOpenClass type) {
        return type;
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
     */
    @Override
    public void addType(IOpenClass type) {
        IOpenClass openClass = internalTypes.put(type.getName(), type);
        if (openClass != null && !openClass.equals(type) && openClass.getPackageName().equals(type.getPackageName())) {
            throw new DuplicatedTypeException(null, type.getName());
        }
    }


    @Override
    public IOpenClass findType(String name) {
        return internalTypes.get(name);
    }
}
