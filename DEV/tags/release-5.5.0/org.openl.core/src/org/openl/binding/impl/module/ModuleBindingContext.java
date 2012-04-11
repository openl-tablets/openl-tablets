/*
 * Created on Jul 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.module;

import java.util.HashMap;
import java.util.Map;

import org.openl.binding.IBindingContext;
import org.openl.binding.ILocalVar;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.AmbiguousVarException;
import org.openl.binding.exception.DuplicatedVarException;
import org.openl.binding.impl.BindingContextDelegator;
import org.openl.binding.impl.MethodSearch;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

/**
 * @author snshor
 *
 */
public class ModuleBindingContext extends BindingContextDelegator {

    // DeferredMethod[] method;

    private ModuleOpenClass module;

    private Map<String, IOpenClass> internalTypes = null;

    // public void addMethod(DeferredMethod dm)
    // {
    // module.addMethod(dm);
    // }

   public ModuleBindingContext(IBindingContext delegate, ModuleOpenClass module) {
        super(delegate);
        this.module = module;
    }

    @Override
    public synchronized void addType(String namespace, IOpenClass type) throws Exception {
        String key = typeKey(namespace, type.getName());
        Map<String, IOpenClass> map = initInternalTypes();
        if (map.containsKey(key)) {
            throw new Exception("Type " + key + " has been defined already");
        }

        map.put(key, type);
    }

    @Override
    public ILocalVar addVar(String namespace, String name, IOpenClass type) throws DuplicatedVarException {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBindingContext#findMethodCaller(java.lang.String,
     *      java.lang.String, org.openl.types.IOpenClass[])
     */
    @Override
    public IMethodCaller findMethodCaller(String namespace, String methodName, IOpenClass[] parTypes)
            throws AmbiguousMethodException {

        IMethodCaller imc = null;
        if (ISyntaxConstants.THIS_NAMESPACE.equals(namespace)) {
            imc = MethodSearch.getMethodCaller(methodName, parTypes, this, module);
        }

        return imc != null ? imc : super.findMethodCaller(namespace, methodName, parTypes);
    }

    /**
     *
     */

    @Override
    public IOpenClass findType(String namespace, String typeName) {

        if (internalTypes != null) {
            String key = typeKey(namespace, typeName);
            IOpenClass ioc = internalTypes.get(key);
            if (ioc != null) {
                return ioc;
            }
        }

        return super.findType(namespace, typeName);
    }

    @Override
    public IOpenField findVar(String namespace, String name, boolean strictMatch) throws AmbiguousVarException {
        IOpenField res = null;
        if (namespace.equals(ISyntaxConstants.THIS_NAMESPACE)) {
            res = module.getField(name, strictMatch);
        }

        return res != null ? res : super.findVar(namespace, name, strictMatch);
    }

    public ModuleOpenClass getModule() {
        return module;
    }

    private synchronized Map<String, IOpenClass> initInternalTypes() {
        if (internalTypes == null) {
            internalTypes = new HashMap<String, IOpenClass>();
        }
        return internalTypes;
    }

    final String typeKey(String namespace, String typeName) {
        return namespace + "::" + typeName;
    }

}
