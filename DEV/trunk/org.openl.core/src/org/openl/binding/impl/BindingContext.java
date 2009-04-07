/*
 * Created on May 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.binding.DuplicatedVarException;
import org.openl.binding.IBindingContext;
import org.openl.binding.ILocalVar;
import org.openl.binding.INodeBinder;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenCast;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;

/**
 * @author snshor
 *
 */
public class BindingContext implements IBindingContext {

    IOpenBinder binder;

    List<ISyntaxError> errors = new ArrayList<ISyntaxError>();

    Map<String, String> aliases = new HashMap<String, String>();

    IOpenClass returnType;

    LocalFrameBuilder localFrame = new LocalFrameBuilder();

    OpenL openl;

    Stack<List<ISyntaxError>> errorStack = new Stack<List<ISyntaxError>>();

    public BindingContext(Binder binder, IOpenClass returnType, OpenL openl) {
        this.binder = binder;
        this.returnType = returnType;
        this.openl = openl;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBindingContext#addAlias(java.lang.String,
     *      java.lang.String)
     */
    public synchronized void addAlias(String name, String value) {
        aliases.put(name, value);

    }

    public void addError(ISyntaxError error) {
        // if (errors.size() > 100)
        // throw new TooManyErrorsError();
        errors.add(error);
    }

    public ILocalVar addParameter(String namespace, String name, IOpenClass type) throws DuplicatedVarException {
        throw new UnsupportedOperationException();
    }

    // public void addAllErrors(Vector err)
    // {
    // for (int i = 0; i < err.size(); i++)
    // {
    // errors.add(err.elementAt(i));
    // }
    // }

    public void addType(String namespace, IOpenClass type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBindingContext#addVar(java.lang.String,
     *      java.lang.String)
     */
    public ILocalVar addVar(String namespace, String name, IOpenClass type) throws DuplicatedVarException {
        return localFrame.addVar(namespace, name, type);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBindingContext#findBinder(org.openl.syntax.ISyntaxNode)
     */
    public INodeBinder findBinder(ISyntaxNode node) {
        return binder.getNodeBinderFactory().getNodeBinder(node);
    }

    public IOpenField findFieldFor(IOpenClass type, String fieldName, boolean strictMatch) {
        return type.getField(fieldName, strictMatch);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBindingContext#findMethod(java.lang.String,
     *      org.openl.types.IOpenClass[])
     */
    public IMethodCaller findMethodCaller(String namespace, String name, IOpenClass[] parTypes) {
        return binder.getMethodFactory().getMethodCaller(namespace, name, parTypes, binder.getCastFactory());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBindingContext#findType(java.lang.String,
     *      java.lang.String)
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

    public synchronized String getAlias(String name) {
        return aliases.get(name);
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

    public ISyntaxError[] getError() {
        return errors == null ? ISyntaxError.EMPTY : ((ISyntaxError[]) errors.toArray(ISyntaxError.EMPTY));
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBindingContext#getError(int)
     */
    public ISyntaxError getError(int i) {
        return errors.get(i);
    }

    /**
     * @return
     */
    public int getLocalVarFrameSize() {
        return localFrame.getLocalVarFrameSize();
    }

    public int getNumberOfErrors() {
        return errors == null ? 0 : errors.size();
    }

    public OpenL getOpenL() {
        return openl;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBindingContext#getParamFrameSize()
     */
    public int getParamFrameSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    public IOpenClass getReturnType() {
        return returnType;
    }

    public List<ISyntaxError> popErrors() {
        List<ISyntaxError> tmp = errors;
        errors = errorStack.pop();
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
        errors = new ArrayList<ISyntaxError>();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBindingContext#pushLocalVarContext(org.openl.binding.ILocalVarContext)
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

}
