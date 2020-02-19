package org.openl.binding.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.ILocalVar;
import org.openl.binding.INodeBinder;
import org.openl.binding.exception.*;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessage;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

/**
 * @author snshor
 *
 */
public class BindingContextDelegator implements IBindingContextDelegator {

    protected IBindingContext delegate;

    public BindingContextDelegator(IBindingContext delegate) {
        this.delegate = delegate;
    }

    @Override
    public IOpenField findRange(String namespace,
            String rangeStartName,
            String rangeEndName) throws OpenLCompilationException {
        return delegate.findRange(namespace, rangeStartName, rangeEndName);
    }

    @Override
    public void addError(SyntaxNodeException error) {
        delegate.addError(error);
    }

    @Override
    public IOpenClass addType(String namespace, IOpenClass type) throws DuplicatedTypeException {
        return delegate.addType(namespace, type);
    }

    @Override
    public ILocalVar addVar(String namespace, String name, IOpenClass type) throws DuplicatedVarException {
        return delegate.addVar(namespace, name, type);
    }

    @Override
    public INodeBinder findBinder(ISyntaxNode node) {
        return delegate.findBinder(node);
    }

    @Override
    public IMethodCaller findMethodCaller(String namespace,
            String name,
            IOpenClass[] parTypes) throws AmbiguousMethodException {
        return delegate.findMethodCaller(namespace, name, parTypes);
    }

    @Override
    public IOpenClass findType(String namespace, String typeName) throws AmbiguousTypeException {
        return delegate.findType(namespace, typeName);
    }

    @Override
    public IOpenField findVar(String namespace, String name, boolean strictMatch) throws AmbiguousVarException {
        return delegate.findVar(namespace, name, strictMatch);
    }

    @Override
    public IOpenCast getCast(IOpenClass from, IOpenClass to) {
        return delegate.getCast(from, to);
    }

    @Override
    public IOpenClass findClosestClass(IOpenClass openClass1, IOpenClass openClass2) {
        return delegate.findClosestClass(openClass1, openClass2);
    }

    public IBindingContext getDelegate() {
        return delegate;
    }

    @Override
    public SyntaxNodeException[] getErrors() {
        return delegate.getErrors();
    }

    @Override
    public int getLocalVarFrameSize() {
        return delegate.getLocalVarFrameSize();
    }

    @Override
    public OpenL getOpenL() {
        return delegate.getOpenL();
    }

    @Override
    public IOpenClass getReturnType() {
        return delegate.getReturnType();
    }

    @Override
    public List<SyntaxNodeException> popErrors() {
        return delegate.popErrors();
    }

    @Override
    public void popLocalVarContext() {
        delegate.popLocalVarContext();
    }

    @Override
    public void pushErrors() {
        delegate.pushErrors();
    }

    @Override
    public Collection<OpenLMessage> popMessages() {
        return delegate.popMessages();
    }

    @Override
    public void pushMessages() {
        delegate.pushMessages();
    }

    @Override
    public void pushLocalVarContext() {
        delegate.pushLocalVarContext();
    }

    @Override
    public void setDelegate(IBindingContext delegate) {
        this.delegate = delegate;
    }

    @Override
    public void setReturnType(IOpenClass type) {
        delegate.setReturnType(type);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBindingContextDelegator#setTopDelegate(org.openl. binding .IBindingContext)
     */
    @Override
    public void setTopDelegate(IBindingContext delegate) {
        if (this.delegate == null) {
            this.delegate = delegate;
            return;
        }

        if (this.delegate instanceof IBindingContextDelegator) {
            ((IBindingContextDelegator) this.delegate).setTopDelegate(delegate);
        }
    }

    @Override
    public boolean isExecutionMode() {
        return delegate.isExecutionMode();
    }

    @Override
    public void setExecutionMode(boolean exectionMode) {
        delegate.setExecutionMode(exectionMode);
    }

    @Override
    public Map<String, Object> getExternalParams() {
        return delegate.getExternalParams();
    }

    @Override
    public void setExternalParams(Map<String, Object> params) {
        delegate.setExternalParams(params);
    }

    @Override
    public Collection<OpenLMessage> getMessages() {
        return delegate.getMessages();
    }

    @Override
    public void addMessage(OpenLMessage message) {
        delegate.addMessage(message);
    }

    @Override
    public void addMessages(Collection<OpenLMessage> messages) {
        delegate.addMessages(messages);
    }
}
