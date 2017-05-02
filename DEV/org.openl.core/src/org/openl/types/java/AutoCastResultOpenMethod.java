package org.openl.types.java;

import java.lang.reflect.Array;

import org.openl.binding.IBindingContext;
import org.openl.binding.MethodUtil;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.DomainOpenClass;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.vm.IRuntimeEnv;

public final class AutoCastResultOpenMethod implements IOpenMethod, IMethodSignature {
    private IMethodCaller methodCaller;
    
    private IOpenCast cast;

    private IOpenClass returnType;

    public AutoCastResultOpenMethod(IMethodCaller methodCaller, IOpenClass returnType, IOpenCast cast) {
        if (methodCaller == null){
            throw new IllegalArgumentException();
        }
        if (returnType == null){
            throw new IllegalArgumentException();
        }
        if (cast == null){
            throw new IllegalArgumentException();
        }
        this.methodCaller = methodCaller;
        this.returnType = returnType;
        this.cast = cast;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getDeclaringClass()
     */
    public IOpenClass getDeclaringClass() {
        return methodCaller.getMethod().getDeclaringClass();
    }

    public String getDisplayName(int mode) {
        return MethodUtil.printSignature(this, mode);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getInfo()
     */
    public IMemberMetaInfo getInfo() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IMethodCaller#getMethod()
     */
    public IOpenMethod getMethod() {
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.base.INamedThing#getName()
     */
    public String getName() {
        return methodCaller.getMethod().getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IMethodSignature#getNumberOfParameters()
     */
    public int getNumberOfParameters() {
        return methodCaller.getMethod().getSignature().getNumberOfParameters();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IMethodSignature#getParameterName(int)
     */
    public String getParameterName(int i) {
        return methodCaller.getMethod().getSignature().getParameterName(i);
    }
    
    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IMethodSignature#getParameterType(int)
     */
    public IOpenClass getParameterType(int i) {
        return methodCaller.getMethod().getSignature().getParameterType(i);
    }

    public IOpenClass[] getParameterTypes() {
        return methodCaller.getMethod().getSignature().getParameterTypes();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMethodHeader#getSignature()
     */
    public IMethodSignature getSignature() {
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getType()
     */
    public IOpenClass getType() {
        return returnType;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMethod#invoke(java.lang.Object,
     *      java.lang.Object[])
     */
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        try {
            return cast.convert(methodCaller.invoke(target, params, env));
        } catch (Throwable t) {
            throw RuntimeExceptionWrapper.wrap(t);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#isStatic()
     */
    public boolean isStatic() {
        return methodCaller.getMethod().isStatic();
    }
    
    @Override
    public boolean isConstructor() {
        return false;
    }

    @Override
    public String toString() {
        return getName();
    }
    
    public static IMethodCaller buildAutoCastResultOpenMethod(IBindingContext bindingContext, IMethodCaller methodCaller,
			JavaOpenMethod method, IOpenClass type, Integer arrayDim) {
		IOpenClass simpleType = type;
		int d = 0;
		while (simpleType.isArray()) {
			if (simpleType.getAggregateInfo() != null) {
				simpleType = simpleType.getAggregateInfo().getComponentType(simpleType);
			} else {
				simpleType = simpleType.getComponentClass();
			}
			d++;
		}
		
		if (arrayDim != null && d > arrayDim) {
			simpleType = JavaOpenClass.OBJECT;
		}
		
		if (!method.getType().isArray()) {
			IOpenCast cast = bindingContext.getCast(method.getType(), simpleType);
			if (cast != null) {
				return new AutoCastResultOpenMethod(methodCaller, simpleType, cast);
			}
		} else {
			IOpenClass v = method.getType();
			int dimensions = 0;
			while (v.isArray()) {
				v = v.getComponentClass();
				dimensions++;
			}
			IOpenClass arrayType = JavaOpenClass
					.getOpenClass(Array.newInstance(simpleType.getInstanceClass(), dimensions).getClass());
			if (simpleType.getDomain() != null) {
				StringBuilder domainOpenClassName = new StringBuilder(simpleType.getName());
				for (int j = 0; j < dimensions; j++) {
					domainOpenClassName.append("[]");
				}
				DomainOpenClass domainArrayType = new DomainOpenClass(domainOpenClassName.toString(), arrayType,
						simpleType.getDomain(), null);
				IOpenCast cast = bindingContext.getCast(method.getType(), domainArrayType);
				if (cast != null) {
					return new AutoCastResultOpenMethod(methodCaller, domainArrayType, cast);
				}
			} else {
				IOpenCast cast = bindingContext.getCast(method.getType(), arrayType);
				if (cast != null) {
					return new AutoCastResultOpenMethod(methodCaller, arrayType, cast);
				}
			}
		}
		return methodCaller;
	}
}