package org.openl.binding.impl.module;

import org.openl.binding.IBindingContext;
import org.openl.binding.ILocalVar;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.exception.DuplicatedVarException;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.BindingContextDelegator;
import org.openl.binding.impl.LocalFrameBuilder;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.IParameterDeclaration;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.MethodSignature;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * @author snshor
 */
public class MethodBindingContext extends BindingContextDelegator {

    public static final boolean DEFAULT_SEARCH_IN_CONTEXT = true;
    public static final int DEFAULT_CONTEXT_LEVEL = 1;

    private final LocalFrameBuilder localFrame = new LocalFrameBuilder();
    private IOpenClass returnType;
    private final IOpenMethodHeader header;
    private final boolean searchInParameterContext;
    private final int parameterContextDepthLevel;
    private final ILocalVar[] paramVars;
    private VariableInContextFinder rootContext;

    public MethodBindingContext(IOpenMethodHeader header, IBindingContext delegate) {
        super(delegate);
        this.header = header;
        this.searchInParameterContext = DEFAULT_SEARCH_IN_CONTEXT;
        this.parameterContextDepthLevel = DEFAULT_CONTEXT_LEVEL;

        pushLocalVarContext();
        IMethodSignature signature = header.getSignature();
        IOpenClass[] params = signature.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            try {
                String pName = signature.getParameterName(i);
                if (pName != null) {
                    localFrame.addVar(ISyntaxConstants.THIS_NAMESPACE, pName, params[i]);
                }
            } catch (DuplicatedVarException e) {
                if (signature instanceof MethodSignature) {
                    IParameterDeclaration paramDeclaration = ((MethodSignature) signature).getParameterDeclaration(i);
                    if (paramDeclaration != null && paramDeclaration.getModule() != null) {
                        BindHelper.processError(e, paramDeclaration.getModule(), delegate);
                        continue;
                    }
                }
                throw RuntimeExceptionWrapper.wrap(e.getMessage(), e);
            }
        }

        paramVars = localFrame.getTopFrame().toArray(new ILocalVar[0]);

    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBindingContext#addVar(java.lang.String, java.lang.String, org.openl.types.IOpenClass)
     */
    @Override
    public ILocalVar addVar(String namespace, String name, IOpenClass type) throws DuplicatedVarException {
        return localFrame.addVar(namespace, name, type);
    }

    @Override
    public IOpenField findVar(String namespace, String name, boolean strictMatch) throws AmbiguousFieldException {
        IOpenField var = localFrame.findLocalVar(namespace, name, strictMatch);
        if (var != null) {
            return var;
        }
        var = delegate.findVar(namespace, name, strictMatch);
        if (var != null) {
            return var;
        }
        if (searchInParameterContext) {
            VariableInContextFinder cxt = getRootContext(parameterContextDepthLevel);
            return cxt.findVariable(name, strictMatch);
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

    @Override
    public IOpenClass getReturnType() {
        return returnType == null ? header.getType() : returnType;
    }

    private VariableInContextFinder getRootContext(int depthLevel) {
        if (rootContext == null) {
            rootContext = new RootDictionaryContext(paramVars, depthLevel);
        }
        return rootContext;
    }

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
            throw new RuntimeException("Cannot override return type " + getReturnType());
        }
        returnType = type;
    }

}
