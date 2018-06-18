package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.ILocalVar;
import org.openl.binding.INodeBinder;
import org.openl.binding.exception.AmbiguousVarException;
import org.openl.binding.exception.DuplicatedVarException;
import org.openl.binding.exception.FieldNotFoundException;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.message.OpenLMessage;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.MethodKey;

/**
 * @author snshor
 * 
 */
public class BindingContext implements IBindingContext {

    private IOpenBinder binder;
    private IOpenClass returnType;
    private OpenL openl;

    private LocalFrameBuilder localFrame = new LocalFrameBuilder();
    private List<SyntaxNodeException> errors = new ArrayList<>();
    private Stack<List<SyntaxNodeException>> errorStack = new Stack<>();
    
    private Map<String, Object> externalParams;

    private Collection<OpenLMessage> messages = new LinkedHashSet<>();
    private Stack<Collection<OpenLMessage>> messagesStack = new Stack<>();

    private boolean executionMode = false;

    /*
     * // NOTE: A temporary implementation of multi-module feature.
     * 
     * private Set<IOpenClass> imports = new LinkedHashSet<IOpenClass>();
     */

    public BindingContext(IOpenBinder binder, IOpenClass returnType, OpenL openl) {
        this.binder = binder;
        this.returnType = returnType;
        this.openl = openl;
    }

    public void addError(SyntaxNodeException error) {
        errors.add(error);
    }

    public ILocalVar addParameter(String namespace, String name, IOpenClass type) throws DuplicatedVarException {
        throw new UnsupportedOperationException();
    }

    public void addType(String namespace, IOpenClass type) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.binding.IBindingContext#addVar(java.lang.String, java.lang.String)
     */
    public ILocalVar addVar(String namespace, String name, IOpenClass type) throws DuplicatedVarException {
        return localFrame.addVar(namespace, name, type);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.binding.IBindingContext#findBinder(org.openl.syntax.ISyntaxNode )
     */
    public INodeBinder findBinder(ISyntaxNode node) {
        return binder.getNodeBinderFactory().getNodeBinder(node);
    }

    public IOpenField findFieldFor(IOpenClass type, String fieldName, boolean strictMatch) {
        return type.getField(fieldName, strictMatch);
    }

    private static final Object NOT_FOUND = "NOT_FOUND";

    public IMethodCaller findMethodCaller(String namespace, String name, IOpenClass[] parTypes) {
        MethodKey key = new MethodKey(namespace + ':' + name, parTypes, false, true);
        Map<MethodKey, Object> methodCache = ((Binder) binder).methodCache;

        synchronized (methodCache) {
            Object res = methodCache.get(key);
            if (res == null) {
                IMethodCaller found = binder.getMethodFactory()
                    .getMethodCaller(namespace, name, parTypes, binder.getCastFactory());
                methodCache.put(key, found == null ? NOT_FOUND : found);
                return found;
            }
            if (res == NOT_FOUND)
                return null;
            return (IMethodCaller) res;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.binding.IBindingContext#findType(java.lang.String, java.lang.String)
     */
    public IOpenClass findType(String namespace, String typeName) {
        return binder.getTypeFactory().getType(namespace, typeName);
    }

    public IOpenField findVar(String namespace, String name, boolean strictMatch) // throws
    // Exception
    {
        ILocalVar var = localFrame.findLocalVar(namespace, name);
        if (var != null) {
            return var;
        }

        return binder.getVarFactory().getVar(namespace, name, strictMatch);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.binding.IBindingContext#getBinder()
     */
    public IOpenBinder getBinder() {
        return binder;
    }

    public IOpenCast getCast(IOpenClass from, IOpenClass to) {

        return binder.getCastFactory().getCast(from, to);
    }

    @Override
    public IOpenClass findClosestClass(IOpenClass openClass1, IOpenClass openClass2) {

        return binder.getCastFactory().findClosestClass(openClass1, openClass2);

    }

    private static final SyntaxNodeException[] NO_ERRORS = {};

    public SyntaxNodeException[] getErrors() {
        return errors.size() == 0 ? NO_ERRORS : errors.toArray(new SyntaxNodeException[0]);
    }

    public int getLocalVarFrameSize() {
        return localFrame.getLocalVarFrameSize();
    }

    public OpenL getOpenL() {
        return openl;
    }

    public IOpenClass getReturnType() {
        return returnType;
    }

    public List<SyntaxNodeException> popErrors() {
        List<SyntaxNodeException> tmp = errors;
        errors = errorStack.pop();
        return tmp;
    }

    public Collection<OpenLMessage> popMessages() {
        Collection<OpenLMessage> tmp = messages;
        messages = messagesStack.pop();
        return tmp;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.openl.binding.IBindingContext#popLocalVarcontext()
     */
    public void popLocalVarContext() {
        localFrame.popLocalVarcontext();
    }

    public void pushErrors() {
        errorStack.push(errors);
        errors = new ArrayList<>();
    }

    public void pushMessages() {
        messagesStack.push(messages);
        messages = new LinkedHashSet<>();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.openl.binding.IBindingContext#pushLocalVarContext(org.openl.binding .ILocalVarContext)
     */
    public void pushLocalVarContext() {
        localFrame.pushLocalVarContext();
    }

    public void setReturnType(IOpenClass type) {
        if (returnType != NullOpenClass.the) {
            throw new RuntimeException("Can not override return type " + returnType.getName());
        }
        returnType = type;
    }

    public boolean isExecutionMode() {
        return executionMode;
    }
    
    @Override
    public void setExecutionMode(boolean exectionMode) {
        this.executionMode = exectionMode;
    }

    public Map<String, Object> getExternalParams() {
        return externalParams;
    }

    public void setExternalParams(Map<String, Object> externalParams) {
        this.externalParams = externalParams;
    }

    public IOpenField findRange(String namespace,
            String rangeStartName,
            String rangeEndName) throws AmbiguousVarException, FieldNotFoundException {
        throw new FieldNotFoundException("Range:", rangeStartName + ":" + rangeEndName, null);
    }

    public Collection<OpenLMessage> getMessages() {
        return Collections.unmodifiableCollection(messages);
    }
    
    @Override
    public void addMessage(OpenLMessage message) {
        messages.add(message);
    }
    
    @Override
    public void addMessages(Collection<OpenLMessage> messages) {
        for (OpenLMessage message : messages) {
            addMessage(message);
        }
    }
    
    public void setOpenl(OpenL openl) {
        this.openl = openl;
    }
    
    public void setBinder(IOpenBinder binder) {
        this.binder = binder;
    }
}
