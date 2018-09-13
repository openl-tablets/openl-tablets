package org.openl.binding.impl.component;

import java.util.HashMap;
import java.util.Map;

import org.openl.binding.IBindingContext;
import org.openl.binding.ILocalVar;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.AmbiguousVarException;
import org.openl.binding.exception.DuplicatedVarException;
import org.openl.binding.impl.BindingContextDelegator;
import org.openl.binding.impl.method.MethodSearch;
import org.openl.binding.impl.module.ModuleBindingContext;
import org.openl.exception.OpenLCompilationException;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

/**
 * Binding context for different Openl components.<br>
 * Handles {@link ComponentOpenClass} for which binding is performed.<br> 
 * And a map of internal types that are found during binding.<br>
 * 
 * Was created by extracting functionality from {@link ModuleBindingContext} of 20192 revision.
 * 
 * @author DLiauchuk
 *
 */
public class ComponentBindingContext extends BindingContextDelegator {
	
	//private final Log log = LogFactory.getLog(ComponentBindingContext.class); 
    
    private ComponentOpenClass componentOpenClass;
    
    private Map<String, IOpenClass> internalTypes = null;
    
    public ComponentBindingContext(IBindingContext delegate, ComponentOpenClass componentOpenClass) {
        super(delegate);
        this.componentOpenClass = componentOpenClass;
    }

    /**
     * Builds the type name with namespace.
     *
     * @param namespace for typeName
     * @param typeName
     * @return namespace::typeName
     */
    private String buildTypeName(String namespace, String typeName) {
        final StringBuilder builder = new StringBuilder(64);
        return builder.append(namespace).append("::").append(typeName).toString();
    }

    public ComponentOpenClass getComponentOpenClass() {
        return componentOpenClass;
    }
    
    @Override
    public synchronized void addType(String namespace, IOpenClass type) throws OpenLCompilationException {
        add(namespace, type.getName(), type);
    }

    protected synchronized void add(String namespace, String typeName, IOpenClass type) throws OpenLCompilationException {
        if (internalTypes == null) {
            internalTypes = new HashMap<>();
        }
        String nameWithNamespace = buildTypeName(namespace, typeName);
        if (internalTypes.containsKey(nameWithNamespace)) {
            IOpenClass openClass = internalTypes.get(nameWithNamespace);
            if (openClass == type){
                return;
            }
            if (openClass.getPackageName().equals(type.getPackageName())) {
                throw new OpenLCompilationException("Type " + nameWithNamespace + " has been defined already");
            }
        }

        internalTypes.put(nameWithNamespace, type);
    }

    @Override
    public ILocalVar addVar(String namespace, String name, IOpenClass type)
            throws DuplicatedVarException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IMethodCaller findMethodCaller(String namespace, String methodName,
            IOpenClass[] parTypes) throws AmbiguousMethodException {

        IMethodCaller imc = null;
        if (ISyntaxConstants.THIS_NAMESPACE.equals(namespace)) {
            imc = MethodSearch.findMethod(methodName, parTypes, this,
                componentOpenClass);
        }

        return imc != null ? imc : super.findMethodCaller(namespace,
                methodName, parTypes);
    }
    
    @Override
    public IOpenClass findType(String namespace, String typeName) {
        String key = buildTypeName(namespace, typeName);
        if (internalTypes != null) {
            IOpenClass ioc = internalTypes.get(key);
            if (ioc != null) {
                return ioc;
            }
        }
        
        IOpenClass type = componentOpenClass.findType(typeName);
        if (type != null){
            return type;
        }
        
        return super.findType(namespace, typeName);
    }
    
    @Override
    public IOpenField findVar(String namespace, String name, boolean strictMatch)
            throws AmbiguousVarException {
        IOpenField res = null;
        if (namespace.equals(ISyntaxConstants.THIS_NAMESPACE)) {
            res = componentOpenClass.getField(name, strictMatch);
        }

        return res != null ? res : super.findVar(namespace, name, strictMatch);
    }
}
