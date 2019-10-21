package org.openl.rules.types;

import java.util.*;

import org.openl.binding.MethodUtil;
import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.context.IRulesRuntimeContextOptimizationForOpenMethodDispatcher;
import org.openl.rules.lang.xls.binding.TableVersionComparator;
import org.openl.rules.lang.xls.binding.wrapper.IOpenMethodWrapper;
import org.openl.rules.lang.xls.prebind.LazyMethodWrapper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ITablePropertiesMethod;
import org.openl.rules.table.properties.DimensionPropertiesMethodKey;
import org.openl.runtime.IRuntimeContext;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.*;
import org.openl.types.impl.MethodDelegator;
import org.openl.types.impl.MethodKey;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.Tracer;

/**
 * Class that decorates the <code>IOpenMehtod</code> interface for method overload support.
 *
 * @author Alexey Gamanovich
 *
 */
public abstract class OpenMethodDispatcher implements IOpenMethod {

    /**
     * Delegate method. Used as a descriptor of method for all overloaded version to delegate requests about method info
     * such as signature, name, etc.
     */
    private IOpenMethod delegate;

    /**
     * Method key. Used for method signatures comparison.
     */
    private MethodKey delegateKey;

    /**
     * List of method candidates.
     */
    private List<IOpenMethod> candidates = new ArrayList<>();
    private Map<Integer, DimensionPropertiesMethodKey> candidatesToDimensionKey = new HashMap<>();

    private final Invokable invokeInner = new Invokable() {
        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            return invokeInner(target, params, env);
        }
    };

    /**
     * Creates new instance of decorator.
     *
     * @param delegate method to decorate
     */
    protected void decorate(IOpenMethod delegate) {

        // Check that IOpenMethod object is not null.
        //
        Objects.requireNonNull(delegate, "Method cannot be null");

        // Save method as delegate. It used by decorator to delegate requests
        // about method info such as signature, name, etc.
        //
        this.delegate = delegate;

        // Evaluate method key.
        //
        this.delegateKey = new MethodKey(delegate);

        // First method candidate is himself.
        //
        this.candidates.add(delegate);
        if (delegate instanceof ITablePropertiesMethod) {
            int idx = this.candidates.size() - 1;
            this.candidatesToDimensionKey.put(idx, new DimensionPropertiesMethodKey(delegate));
        }
    }

    /**
     * Gets the signature of method.
     */
    @Override
    public IMethodSignature getSignature() {
        return delegate.getSignature();
    }

    /**
     * Gets the declaring class.
     */
    @Override
    public IOpenClass getDeclaringClass() {
        return delegate.getDeclaringClass();
    }

    /**
     * Gets <code>null</code>. The decorator hasn't info about overloaded methods.
     */
    @Override
    public IMemberMetaInfo getInfo() {
        return null;
    }

    /**
     * Gets the type of method.
     */
    @Override
    public IOpenClass getType() {
        return delegate.getType();
    }

    @Override
    public boolean isStatic() {
        return delegate.isStatic();
    }

    @Override
    public boolean isConstructor() {
        return false;
    }

    /**
     * Gets the user-friendly name.
     */
    @Override
    public String getDisplayName(int mode) {
        return delegate.getDisplayName(mode);
    }

    /**
     * Gets the method name.
     */
    @Override
    public String getName() {
        return delegate.getName();
    }

    /**
     * Gets <code>this</code>. The decorator cannot resolve which overloaded method should be returned.
     */
    @Override
    public IOpenMethod getMethod() {
        return this;
    }

    public List<IOpenMethod> getCandidates() {
        return Collections.unmodifiableList(candidates);
    }

    /**
     * Invokes appropriate method using runtime context.
     */
    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return Tracer.invoke(invokeInner, target, params, env, this);
    }

    /**
     * Finds appropriate method using runtime context. This method used to optimize runtime where the same method is
     * used more that one time.
     */
    public IOpenMethod findMatchingMethod(IRuntimeEnv env) {
        // Gets the runtime context.
        //
        IRuntimeContext context = env.getContext();

        // Get matching method.
        //
        IOpenMethod method = null;

        if (context instanceof IRulesRuntimeContextOptimizationForOpenMethodDispatcher) {
            IRulesRuntimeContextOptimizationForOpenMethodDispatcher rulesRuntimeContextOptimizationForOpenMethodDispatcher = (IRulesRuntimeContextOptimizationForOpenMethodDispatcher) context;
            method = rulesRuntimeContextOptimizationForOpenMethodDispatcher.getMethodForOpenMethodDispatcher(this);
            if (method == null) {
                method = findMatchingMethod(candidates, context);
                rulesRuntimeContextOptimizationForOpenMethodDispatcher.putMethodForOpenMethodDispatcher(this, method);
            }
        } else {
            method = findMatchingMethod(candidates, context);
        }

        // Check that founded required method.
        //
        if (method == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Method signature: ");
            MethodUtil.printMethod(this, sb);
            sb.append("\n");
            sb.append("Context: ");
            sb.append(context.toString());

            String message = String.format("Appropriate overloaded method for '%1$s' is not found. Details: \n%2$s",
                getName(),
                sb.toString());

            throw new OpenLRuntimeException(message);
        }

        while (method instanceof LazyMethodWrapper || method instanceof MethodDelegator) {
            if (method instanceof LazyMethodWrapper) {
                method = ((LazyMethodWrapper) method).getCompiledMethod(env);
            }
            if (method instanceof MethodDelegator) {
                MethodDelegator methodDelegator = (MethodDelegator) method;
                method = methodDelegator.getMethod();
            }
        }

        if (method instanceof IOpenMethodWrapper) {
            method = ((IOpenMethodWrapper) method).getDelegate();
        }

        return method;
    }

    /**
     * Invokes appropriate method using runtime context.
     */
    protected Object invokeInner(Object target, Object[] params, IRuntimeEnv env) {
        IOpenMethod method = findMatchingMethod(env);
        Tracer.put(this, "rule", method);
        return method.invoke(target, params, env);
    }

    /**
     * In case we have several versions of one table we should add only the newest or active version of table.
     *
     * @param newMethod The methods that we are trying to add.
     * @param key Method key of these methods based on signature.
     * @param existedMethod The existing method.
     */
    protected IOpenMethod useActiveOrNewerVersion(IOpenMethod existedMethod, IOpenMethod newMethod, MethodKey key) {
        int compareResult = TableVersionComparator.getInstance().compare(existedMethod, newMethod);
        if (compareResult > 0) {
            return newMethod;
        } else if (compareResult == 0) {
            /**
             * Throw the error with the right message for the case when the methods are equal
             */
            if (newMethod instanceof IUriMember && existedMethod instanceof IUriMember) {
                if (!UriMemberHelper.isTheSame((IUriMember) newMethod, (IUriMember) existedMethod)) {
                    String message = ValidationMessages.getDuplicatedMethodMessage(existedMethod, newMethod);
                    throw new DuplicatedMethodException(message, existedMethod, newMethod);
                }
            } else {
                throw new IllegalStateException("Implementation supports only IUriMember!");
            }
        }
        return existedMethod;
    }

    private int searchTheSameMethod(DimensionPropertiesMethodKey newMethodPropertiesKey) {
        for (Map.Entry<Integer, DimensionPropertiesMethodKey> it : candidatesToDimensionKey.entrySet()) {
            DimensionPropertiesMethodKey existedMethodPropertiesKey = it.getValue();
            if (existedMethodPropertiesKey.hashCode() == newMethodPropertiesKey.hashCode() && newMethodPropertiesKey
                .equals(existedMethodPropertiesKey)) {
                return it.getKey();
            }
        }
        return -1;
    }

    private Set<MethodKey> candidateKeys = new HashSet<>();

    /**
     * Try to add method as overloaded version of decorated method.
     *
     * @param candidate method to add
     */
    public void addMethod(IOpenMethod candidate) {

        // Evaluate the candidate method key.
        //

        MethodKey candidateKey = new MethodKey(candidate);

        // Check that candidate has the same method signature and list of
        // parameters as a delegate. If they different then is two different
        // methods and delegate cannot be overloaded by candidate.
        //
        if (delegateKey.equals(candidateKey)) {
            int i = -1;
            DimensionPropertiesMethodKey dimensionMethodKey = null;
            if (candidate instanceof ITablePropertiesMethod) {
                dimensionMethodKey = new DimensionPropertiesMethodKey(candidate);
                i = searchTheSameMethod(dimensionMethodKey);
            }
            if (i < 0) {
                candidates.add(candidate);
                if (dimensionMethodKey != null) {
                    int idx = candidates.size() - 1;
                    candidatesToDimensionKey.put(idx, dimensionMethodKey);
                }
            } else {
                IOpenMethod existedMethod = candidates.get(i);
                try {
                    candidate = useActiveOrNewerVersion(existedMethod, candidate, candidateKey);
                    candidates.set(i, candidate);
                    candidatesToDimensionKey.put(i, new DimensionPropertiesMethodKey(candidate));
                } catch (DuplicatedMethodException e) {
                    if (!candidateKeys.contains(candidateKey)) {
                        if (candidate instanceof IMemberMetaInfo) {
                            IMemberMetaInfo memberMetaInfo = (IMemberMetaInfo) candidate;
                            if (memberMetaInfo.getSyntaxNode() instanceof TableSyntaxNode) {
                                SyntaxNodeException error = SyntaxNodeExceptionUtils
                                    .createError(e.getMessage(), e, memberMetaInfo.getSyntaxNode());
                                ((TableSyntaxNode) memberMetaInfo.getSyntaxNode()).addError(error);
                            }
                        }
                        candidateKeys.add(candidateKey);
                    }
                    throw e;
                }
            }
        } else {
            // Throw appropriate exception.
            //
            StringBuilder sb = new StringBuilder();
            MethodUtil.printMethod(this, sb);

            throw new OpenLRuntimeException("Invalid method signature to overload: " + sb.toString());
        }
    }

    /**
     * Resolve best matching method to invoke.
     *
     * @param candidates list of candidates
     * @param context runtime context
     * @return method to invoke
     */
    protected abstract IOpenMethod findMatchingMethod(List<IOpenMethod> candidates, IRuntimeContext context);

    public IOpenMethod getTargetMethod() {
        return this.delegate;
    }

    public abstract TableSyntaxNode getDispatcherTable();
}