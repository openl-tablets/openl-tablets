package org.openl.binding.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.ILocalVar;
import org.openl.binding.INodeBinder;
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

    public IOpenField findRange(String namespace,
            String rangeStartName,
            String rangeEndName) throws OpenLCompilationException {
        return delegate.findRange(namespace, rangeStartName, rangeEndName);
    }

    public void addError(SyntaxNodeException error) {
        delegate.addError(error);
    }

    public void addType(String namespace, IOpenClass type) throws OpenLCompilationException {
        throw new UnsupportedOperationException();
    }

    public ILocalVar addVar(String namespace, String name, IOpenClass type) {
        return delegate.addVar(namespace, name, type);
    }

    public INodeBinder findBinder(ISyntaxNode node) {
        return delegate.findBinder(node);
    }

    public IOpenField findFieldFor(IOpenClass type, String fieldName, boolean strictMatch) {
        return delegate.findFieldFor(type, fieldName, strictMatch);
    }

    public IMethodCaller findMethodCaller(String namespace,
            String name,
            IOpenClass[] parTypes) {
        return delegate.findMethodCaller(namespace, name, parTypes);
    }

    public IOpenClass findType(String namespace, String typeName) {
        return delegate.findType(namespace, typeName);
    }

    public IOpenField findVar(String namespace, String name, boolean strictMatch) {
        return delegate.findVar(namespace, name, strictMatch);
    }

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

    public SyntaxNodeException[] getErrors() {
        return delegate.getErrors();
    }

    public int getLocalVarFrameSize() {
        return delegate.getLocalVarFrameSize();
    }

    public OpenL getOpenL() {
        return delegate.getOpenL();
    }

    public IOpenClass getReturnType() {
        return delegate.getReturnType();
    }

    public List<SyntaxNodeException> popErrors() {
        return delegate.popErrors();
    }

    public void popLocalVarContext() {
        delegate.popLocalVarContext();
    }

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

    public void pushLocalVarContext() {
        delegate.pushLocalVarContext();
    }

    public void setDelegate(IBindingContext delegate) {
        this.delegate = delegate;
    }

    public void setReturnType(IOpenClass type) {
        delegate.setReturnType(type);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.binding.IBindingContextDelegator#setTopDelegate(org.openl. binding .IBindingContext)
     */
    public void setTopDelegate(IBindingContext delegate) {
        if (this.delegate == null) {
            this.delegate = delegate;
            return;
        }

        if (this.delegate instanceof IBindingContextDelegator) {
            ((IBindingContextDelegator) this.delegate).setTopDelegate(delegate);
        }
    }

    public boolean isExecutionMode() {
        return delegate.isExecutionMode();
    }

    @Override
    public void setExecutionMode(boolean exectionMode) {
        delegate.setExecutionMode(exectionMode);
    }

    public Map<String, Object> getExternalParams() {
        return delegate.getExternalParams();
    }

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
