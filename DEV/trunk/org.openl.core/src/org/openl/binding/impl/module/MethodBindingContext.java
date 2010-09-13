/*
 * Created on Jul 25, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.module;

import org.openl.binding.IBindingContext;
import org.openl.binding.ILocalVar;
import org.openl.binding.exception.AmbiguousVarException;
import org.openl.binding.exception.DuplicatedVarException;
import org.openl.binding.impl.BindingContextDelegator;
import org.openl.binding.impl.LocalFrameBuilder;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.NullOpenClass;

/**
 * @author snshor
 *
 */
public class MethodBindingContext extends BindingContextDelegator {

    static final int STATUS_ADDING_PARAMS = 0, STATUS_ADDING_LOCAL_VARS = 1;

    public static final boolean DEFAULT_SEARCH_IN_CONTEXT = true;

    public static final int DEFAULT_CONTEXT_LEVEL = 1;

    LocalFrameBuilder localFrame = new LocalFrameBuilder();

    int paramFrameSize = 0;

    int status = STATUS_ADDING_PARAMS;

    IOpenClass returnType;
    IOpenMethodHeader header;

    boolean searchInParameterContext;
    int parameterContextDepthLevel;

    ILocalVar[] paramVars;

    RootDictionaryContext rootContext;

    /**
     * @param delegate
     */
    public MethodBindingContext(IOpenMethodHeader header, IBindingContext delegate) {
        this(header, delegate, DEFAULT_SEARCH_IN_CONTEXT, DEFAULT_CONTEXT_LEVEL);
    }

    public MethodBindingContext(IOpenMethodHeader header, IBindingContext delegate, boolean searchInParameterContext,
            int parameterContextDepthLevel) {
        super(delegate);
        this.header = header;
        this.searchInParameterContext = searchInParameterContext;
        this.parameterContextDepthLevel = parameterContextDepthLevel;

        pushLocalVarContext();
        IMethodSignature signature = header.getSignature();
        IOpenClass[] params = signature.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            try {
                addParameter(ISyntaxConstants.THIS_NAMESPACE, signature.getParameterName(i), params[i]);
            } catch (DuplicatedVarException e) {
                throw new OpenLRuntimeException(e);
            }
        }

        paramVars = localFrame.getTopFrame().toArray(new ILocalVar[0]);

    }

    @Override
    public ILocalVar addParameter(String namespace, String name, IOpenClass type) throws DuplicatedVarException {
        if (status != STATUS_ADDING_PARAMS) {
            throw new IllegalStateException();
        }
        paramFrameSize++;

        return localFrame.addVar(namespace, name, type);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBindingContext#addVar(java.lang.String,
     *      java.lang.String, org.openl.types.IOpenClass)
     */
    @Override
    public ILocalVar addVar(String namespace, String name, IOpenClass type) throws DuplicatedVarException {
        status = STATUS_ADDING_LOCAL_VARS;
        return localFrame.addVar(namespace, name, type);
    }

    @Override
    public IOpenField findVar(String namespace, String name, boolean strictMatch) throws AmbiguousVarException {
        IOpenField var = localFrame.findLocalVar(namespace, name);

        if (var != null) {
            return var;
        }

        var = delegate.findVar(namespace, name, strictMatch);
        if (var != null) {
            return var;
        }

        if (searchInParameterContext) {
            RootDictionaryContext cxt = getRootContext(parameterContextDepthLevel);
            return cxt.findField(name);
        }
        return null;

    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBindingContext#getLocalVarFrameSize()
     */
    @Override
    public int getLocalVarFrameSize() {
        return localFrame.getLocalVarFrameSize();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBindingContext#getParamFrameSize()
     */
    @Override
    public int getParamFrameSize() {
        return paramFrameSize;
    }

    @Override
    public IOpenClass getReturnType() {
        return returnType == null ? header.getType() : returnType;
    }

    private RootDictionaryContext getRootContext(int depthLevel) {
        if (rootContext == null) {
            rootContext = new RootDictionaryContext(paramVars, depthLevel);
        }
        return rootContext;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBindingContext#popLocalVarcontext()
     */
    @Override
    public void popLocalVarContext() {
        localFrame.popLocalVarcontext();
    }

    @Override
    public void pushLocalVarContext() {
        localFrame.pushLocalVarContext();
    }

    @Override
    public void setReturnType(IOpenClass type) {
        if (getReturnType() != NullOpenClass.the) {
            throw new RuntimeException("Can not override return type " + getReturnType());
        }
        returnType = type;
    }

}
