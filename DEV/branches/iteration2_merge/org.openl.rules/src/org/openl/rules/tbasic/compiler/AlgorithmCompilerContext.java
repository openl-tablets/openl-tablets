package org.openl.rules.tbasic.compiler;

import org.openl.IOpenRunner;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.meta.StringValue;
import org.openl.rules.tbasic.runtime.TBasicContext;
import org.openl.rules.tbasic.runtime.TBasicVM;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.AMethod;
import org.openl.types.impl.DynamicObjectField;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;


/**
 * 
 * @author snshor
 *
 * This class is responsible for OpenL binding context	
 *
 */
public class AlgorithmCompilerContext 
{

	ModuleOpenClass openClass;
	
	public void addSub(StringValue labelName)
	{
		IOpenMethodHeader header = new OpenMethodHeader(labelName.getValue(), JavaOpenClass.VOID,  IMethodSignature.VOID, openClass);
		SubMethod smethod = new SubMethod(header);
		openClass.addMethod(smethod);
		
		openClass.addField(new NoParamMethodField(labelName, smethod));
		
	}

	
	public void addFunction(StringValue labelName)
	{
		IOpenMethodHeader header = new OpenMethodHeader(labelName.getValue(), JavaOpenClass.VOID,  IMethodSignature.VOID, openClass);
		FunctionMethod smethod = new FunctionMethod(header);
		openClass.addMethod(smethod);
		
		openClass.addField(new NoParamMethodField(labelName, smethod));
		
	}
	
	public void addGlobalVar(StringValue varName)
	{
		openClass.addField(new DynamicObjectField(varName.getValue(), JavaOpenClass.VOID));
		
	}
	
	
	
	static class NoParamMethodField implements IOpenField
	{
		
		StringValue labelName; 
		SubMethod smethod;

		public NoParamMethodField(StringValue labelName, SubMethod smethod) 
		{
			this.labelName = labelName;
			this.smethod = smethod;
		}

		public Object get(Object target, IRuntimeEnv env) {
			return smethod.invoke(target, new Object[]{}, env);
		}

		public boolean isConst() {
			return false;
		}

		public boolean isReadable() {
			return true;
		}

		public boolean isWritable() {
			return false;
		}

		public void set(Object target, Object value, IRuntimeEnv env) {
			
		}

		public IOpenClass getDeclaringClass() {
			return smethod.getDeclaringClass();
		}

		public IMemberMetaInfo getInfo() {
			return null;
		}

		public IOpenClass getType() {
			return smethod.getType();
		}

		public boolean isStatic() {
			return false;
		}

		public String getDisplayName(int mode) {
			return smethod.getDisplayName(mode);
		}

		public String getName() {
			return labelName.getValue();
		}
		
	}
	
	static class SubMethod extends AMethod
	{

		public SubMethod(IOpenMethodHeader header) {
			super(header);
		}

		public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
//TODO fix
			
			
//			TBasicVM vm = new TBasicVM(operations, labels);
//			
//			vm.run(target, target, params, env);
			
			return null;
		}
		
	}
	
	
	
	static class FunctionMethod extends SubMethod
	{

		public FunctionMethod(IOpenMethodHeader header) {
			super(header);
		}
		
	}
	
	
	
	static class TBasicEnv implements IRuntimeEnv
	{
		IRuntimeEnv env;
		TBasicVM tbasicVm;
		TBasicContext tbasicContext;
		

		public IRuntimeEnv getEnv() {
			return env;
		}

		public TBasicVM getTbasicVm() {
			return tbasicVm;
		}

		public TBasicContext getTbasicContext() {
			return tbasicContext;
		}

		public Object[] getLocalFrame() {
			return env.getLocalFrame();
		}

		public IOpenRunner getRunner() {
			return env.getRunner();
		}

		public Object getThis() {
			return env.getThis();
		}

		public Object[] popLocalFrame() {
			return env.popLocalFrame();
		}

		public Object popThis() {
			return env.popThis();
		}

		public void pushLocalFrame(Object[] frame) {
			env.pushLocalFrame(frame);
		}

		public void pushThis(Object thisObject) {
			env.pushThis(thisObject);
		}

		public TBasicEnv(IRuntimeEnv env, TBasicVM tbasicVm,
				TBasicContext tbasicContext) {
			super();
			this.env = env;
			this.tbasicVm = tbasicVm;
			this.tbasicContext = tbasicContext;
		}
	}
	
}
