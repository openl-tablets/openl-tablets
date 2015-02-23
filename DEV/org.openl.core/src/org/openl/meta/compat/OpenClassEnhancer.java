package org.openl.meta.compat;

import java.util.List;

import org.openl.meta.DoubleValueCompatibility;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.AOpenClass;
import org.openl.types.impl.MethodDelegator;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ArrayTool;
import org.openl.vm.IRuntimeEnv;

public class OpenClassEnhancer {

	public enum Options {
		ALL, OBJECT_METHODS, STATIC_METHODS, FIELDS, CONSTRUCTORS
	}

	static public IOpenClass enhance(AOpenClass target, IOpenClass src, Options op) {

		if (op == Options.ALL || op == Options.STATIC_METHODS)
			enhanceStaticMethods(target, src);

		if (op == Options.ALL || op == Options.OBJECT_METHODS)
			enhanceObjectMethods(target, src);

//		if (op == Options.ALL || op == Options.FIELDS)
//			enhanceFields(target, src);

		if (op == Options.ALL || op == Options.FIELDS)
			enhanceConstructors(target, src);
		
		return target;
	}

	private static void enhanceConstructors(AOpenClass target, IOpenClass src) {
		List<IOpenMethod> methods = src.getMethods();
		for (IOpenMethod method : methods) {

			if (!isConstructor(method))
				continue;

			target.addMethod(new EnhancedConstructor(method, target));

		}
		
	}

	private static void enhanceStaticMethods(AOpenClass target, IOpenClass src) {

		List<IOpenMethod> methods = src.getMethods();
		for (IOpenMethod method : methods) {

			if (!method.isStatic() || method.getDeclaringClass() != src
					|| isConstructor(method) 
					|| method.getName().equals(src.getName()) //src Constructor
					)
				continue;

			target.addMethod(new EnhancedStatictMethod(method, target));

		}
	}
	

	/**
	 * 
	 * For example double getValue(Double d){return d.doubleValue();} after
	 * enhancing class {@link Double} will become a new method
	 * Double.getValue();
	 * 
	 * 
	 * @param targetClass
	 * @param sourceClass
	 */
	private static void enhanceObjectMethods(AOpenClass targetClass, IOpenClass sourceClass) {

		List<IOpenMethod> methods = sourceClass.getMethods();
		for (IOpenMethod method : methods) {

			if (method.isStatic() || method.getDeclaringClass() != sourceClass
					|| isConstructor(method))
				continue;

			targetClass.addMethod(new EnhancedObjectMethod(method, targetClass));

		}

	}

	
	
	
	private static boolean isConstructor(IOpenMethod method) {
		return "Constructor".equals(method.getName()) && method.isStatic();
	}

	public static void main(String[] args) {
		IOpenClass src = JavaOpenClass
				.getOpenClass(DoubleValueCompatibility.class);

		List<IOpenMethod> methods = src.getMethods();
		for (IOpenMethod method : methods) {
			System.out.println(method.getName() + '\t' + method.isStatic()
					+ '\t' + method.getDeclaringClass());

		}

	}


	static class EnhancedStatictMethod extends MethodDelegator
	{
		IOpenClass declaringClass;

		public EnhancedStatictMethod(IMethodCaller methodCaller, IOpenClass declaringClass) {
			super(methodCaller);
			this.declaringClass = declaringClass;
		}

		@Override
		public IOpenClass getDeclaringClass() {
			return declaringClass;
		}
	}	

	
	
	static class EnhancedConstructor extends EnhancedStatictMethod
	{

		public EnhancedConstructor(IMethodCaller methodCaller, IOpenClass declaringClass) {
			super(methodCaller, declaringClass);
		}

		@Override
		public String getName() {
			return declaringClass.getName();
		}
	}	
	
	
	static class EnhancedObjectMethod extends EnhancedStatictMethod
	{

		IMethodSignature signature;
		Object target;
		
		
		
		public EnhancedObjectMethod(IOpenMethod method, IOpenClass declaringClass) {
			super(method, declaringClass);
			this.signature = makeSignature(method);
			this.target = method.getDeclaringClass().newInstance(null);
		}

		private IMethodSignature makeSignature(IOpenMethod method) {
			IMethodSignature ms = method.getSignature();
			IParameterDeclaration[] pd = new IParameterDeclaration[ms.getNumberOfParameters() - 1];
			for (int i = 0; i < pd.length; i++) {
				pd[i] = new ParameterDeclaration(ms.getParameterType(i + 1 ), ms.getParameterName(i + 1));
			}
			return new MethodSignature(pd);
		}

		@Override
		public IMethodSignature getSignature() {
			return signature;
		}


		@Override
		public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
			Object[] newParams = (Object[])ArrayTool.insertValue(0, params, target);
			return super.invoke(this.target, newParams, env);
		}

	}
	
	
}
