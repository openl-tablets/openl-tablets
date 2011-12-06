/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.binding;

import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextDelegator;
import org.openl.rules.data.DataBase;
import org.openl.rules.data.IDataBase;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenSchema;
import org.openl.types.impl.MethodDelegator;
import org.openl.types.impl.MethodKey;
import org.openl.types.impl.MethodSignature;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.IRuntimeEnvWithContextManagingSupport;

/**
 * @author snshor
 * 
 */
public class XlsModuleOpenClass extends ModuleOpenClass {

    private static final Log LOG = LogFactory.getLog(XlsModuleOpenClass.class);

    private IDataBase dataBase = new DataBase();
	
	public XlsModuleOpenClass(IOpenSchema schema, String name, XlsMetaInfo metaInfo, OpenL openl) {
        this(schema, name, metaInfo, openl, null);
	}
	
	/**
	 * Constructor for module with dependent modules
	 *
	 */
	public XlsModuleOpenClass(IOpenSchema schema, String name, XlsMetaInfo metaInfo, OpenL openl, 
          Set<CompiledOpenClass> usingModules) {
	    super(schema, name, openl, usingModules);
	    this.metaInfo = metaInfo;
        addMethod(new ModifyRuntimeContextMethod(this));
        addMethod(new SetRuntimeContextMethod(this));
        addMethod(new RestoreRuntimeContextMethod(this));
        addMethod(new EmptyRuntimeContextMethod(this));
        addMethod(new CurrentRuntimeContextMethod(this));
	}
	
	
    // TODO: should be placed to ModuleOpenClass
	public IDataBase getDataBase() {
		return dataBase;
	}
	
	public XlsMetaInfo getXlsMetaInfo() {
		return (XlsMetaInfo) metaInfo;
	}
	
	/**
	 * Adds method to <code>XlsModuleOpenClass</code>.
	 * 
	 * @param method
	 *            method object
	 */
	@Override
	public void addMethod(IOpenMethod method) {
		
		// Get method key.
		//
		MethodKey key = new MethodKey(method);
		
		Map<MethodKey, IOpenMethod> methods = methodMap();
		
		// Checks that method aleready exists in method map. If it already
		// exists then "overload" it using decorator; otherwise - just add to
		// method map.
		//
		if (methods.containsKey(key)) {
			
			// Gets the existed method from map.
			// 
			IOpenMethod existedMethod = methods.get(key);
			
            if (!existedMethod.getType().equals(method.getType())) {
                throw new DuplicatedMethodException(
                    String.format("Method \"%s\" with return type \"%s\" has already been defined with another return type (\"%s\")",
                        method.getName(), method.getType().getDisplayName(0), existedMethod.getType().getDisplayName(0)), method);
            }
			
			// Checks the instance of existed method. If it's the
			// OpenMethodDecorator then just add the method-candidate to
			// decorator; otherwise - replace existed method with new instance
			// of OpenMethodDecorator for existed method and add new one.
			//
			if (existedMethod instanceof OpenMethodDispatcher) {
				OpenMethodDispatcher decorator = (OpenMethodDispatcher) existedMethod;
				decorator.addMethod(method);
				generateAuxiliaryMethod(method, decorator.getCandidates().size()-1);
			} else {
				
				// Create decorator for existed method.
				//
				OpenMethodDispatcher decorator = new MatchingOpenMethodDispatcher(existedMethod, this);
				
                generateAuxiliaryMethod(existedMethod, 0);
				// Add new method to decorator as candidate.
				//
				decorator.addMethod(method);
				
                generateAuxiliaryMethod(method, decorator.getCandidates().size()-1);
				// Replace existed method with decorator using the same key.
				//
				methodMap().put(key, decorator);
			}
		} else {
			
			// Just add original method.
			//
			methodMap().put(key, method);
		}
	}
	
    public static final String AUXILIARY_METHOD_DELIMETER = "$";

    private static final String AUXILIARY_METHOD_NAME_PATTERN = ".*\\$\\d*";

    public static boolean isAuxiliaryMethod(IOpenMethod method) {
        return method instanceof MethodDelegator && method.getName().matches(AUXILIARY_METHOD_NAME_PATTERN);
    }

    /**
     * Adds an auxiliary method for all overloaded methods. Such method can be
     * used internally and has name: <original method name>+prefix:&<index of
     * method in overloaded methods group>
     * 
     * @param originalMethod Method in overloaded group.
     * @param index Index in overloaded methods group(according to the sequence
     *            of binding).
     */
    private void generateAuxiliaryMethod(final IOpenMethod originalMethod, int index) {
        final String auxiliaryMethodName = originalMethod.getName() + AUXILIARY_METHOD_DELIMETER + index;
        IOpenMethod auxiliaryMethod = new MethodDelegator(originalMethod) {
            @Override
            public String getName() {
                return auxiliaryMethodName;
            }
        };
        addMethod(auxiliaryMethod);
    }

    @Override
	public void clearOddDataForExecutionMode() {
	    super.clearOddDataForExecutionMode();
	    dataBase = null;
    }

    public final class CurrentRuntimeContextMethod implements IOpenMethod {
        private final XlsModuleOpenClass moduleOpenClass;
        public final static  String CURRENT_CONTEXT_METHOD_NAME = "getContext";

        private CurrentRuntimeContextMethod(XlsModuleOpenClass moduleOpenClass) {
            this.moduleOpenClass = moduleOpenClass;
        }

        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            IRulesRuntimeContext context = (IRulesRuntimeContext)env.getContext();
            if (context == null) {
                return null;
            }
            try {
                return context.clone();
            } catch (CloneNotSupportedException e) {
                LOG.warn("Failed to clone runtime context. Runtime context managing may work incorrectly.", e);
                return context;
            }
        }

        @Override
        public IOpenMethod getMethod() {
            return this;
        }

        @Override
        public String getName() {
            return CURRENT_CONTEXT_METHOD_NAME;
        }

        @Override
        public String getDisplayName(int mode) {
            return CURRENT_CONTEXT_METHOD_NAME;
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public IOpenClass getType() {
            return JavaOpenClass.getOpenClass(IRulesRuntimeContext.class);
        }

        @Override
        public IMemberMetaInfo getInfo() {
            return null;
        }

        @Override
        public IOpenClass getDeclaringClass() {
            return moduleOpenClass;
        }

        @Override
        public IMethodSignature getSignature() {
            return new MethodSignature(new IOpenClass[] {}, new String[] {});
        }
    }

    public final class EmptyRuntimeContextMethod implements IOpenMethod {
        private final XlsModuleOpenClass moduleOpenClass;
        public final static  String EMPTY_CONTEXT_METHOD_NAME = "emptyContext";

        private EmptyRuntimeContextMethod(XlsModuleOpenClass moduleOpenClass) {
            this.moduleOpenClass = moduleOpenClass;
        }

        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            return new DefaultRulesRuntimeContext();
        }

        @Override
        public IOpenMethod getMethod() {
            return this;
        }

        @Override
        public String getName() {
            return EMPTY_CONTEXT_METHOD_NAME;
        }

        @Override
        public String getDisplayName(int mode) {
            return EMPTY_CONTEXT_METHOD_NAME;
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public IOpenClass getType() {
            return JavaOpenClass.getOpenClass(IRulesRuntimeContext.class);
        }

        @Override
        public IMemberMetaInfo getInfo() {
            return null;
        }

        @Override
        public IOpenClass getDeclaringClass() {
            return moduleOpenClass;
        }

        @Override
        public IMethodSignature getSignature() {
            return new MethodSignature(new IOpenClass[] {}, new String[] {});
        }
    }

    public final class RestoreRuntimeContextMethod implements IOpenMethod {
        private final XlsModuleOpenClass moduleOpenClass;
        public final static  String RESTORE_CONTEXT_METHOD_NAME = "restoreContext";

        private RestoreRuntimeContextMethod(XlsModuleOpenClass moduleOpenClass) {
            this.moduleOpenClass = moduleOpenClass;
        }

        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            if(env instanceof IRuntimeEnvWithContextManagingSupport){
                ((IRuntimeEnvWithContextManagingSupport)env).popContext();
            }
            LOG.warn("Failed to restore runtime context. Runtime context does not support context modifications.");
            return null;
        }

        @Override
        public IOpenMethod getMethod() {
            return this;
        }

        @Override
        public String getName() {
            return RESTORE_CONTEXT_METHOD_NAME;
        }

        @Override
        public String getDisplayName(int mode) {
            return RESTORE_CONTEXT_METHOD_NAME;
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public IOpenClass getType() {
            return JavaOpenClass.VOID;
        }

        @Override
        public IMemberMetaInfo getInfo() {
            return null;
        }

        @Override
        public IOpenClass getDeclaringClass() {
            return moduleOpenClass;
        }

        @Override
        public IMethodSignature getSignature() {
            return new MethodSignature(new IOpenClass[] {}, new String[] {});
        }
    }

    public final class SetRuntimeContextMethod implements IOpenMethod {
        private final XlsModuleOpenClass moduleOpenClass;
        public final static  String SET_CONTEXT_METHOD_NAME = "setContext";

        private SetRuntimeContextMethod(XlsModuleOpenClass moduleOpenClass) {
            this.moduleOpenClass = moduleOpenClass;
        }

        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            if(env instanceof IRuntimeEnvWithContextManagingSupport){
                IRulesRuntimeContext runtimeContext =  (IRulesRuntimeContext)params[0];
                ((IRuntimeEnvWithContextManagingSupport)env).pushContext(runtimeContext);
            }
            LOG.warn("Failed to set runtime context. Runtime context does not support context modifications.");
            return null;
        }

        @Override
        public IOpenMethod getMethod() {
            return this;
        }

        @Override
        public String getName() {
            return SET_CONTEXT_METHOD_NAME;
        }

        @Override
        public String getDisplayName(int mode) {
            return SET_CONTEXT_METHOD_NAME;
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public IOpenClass getType() {
            return JavaOpenClass.VOID;
        }

        @Override
        public IMemberMetaInfo getInfo() {
            return null;
        }

        @Override
        public IOpenClass getDeclaringClass() {
            return moduleOpenClass;
        }

        @Override
        public IMethodSignature getSignature() {
            return new MethodSignature(new IOpenClass[] { JavaOpenClass.getOpenClass(IRulesRuntimeContext.class) },
                new String[] { "context"});
        }
    }

    public final class ModifyRuntimeContextMethod implements IOpenMethod {
        private final XlsModuleOpenClass moduleOpenClass;
        public final static  String MODIFY_CONTEXT_METHOD_NAME = "modifyContext";

        private ModifyRuntimeContextMethod(XlsModuleOpenClass moduleOpenClass) {
            this.moduleOpenClass = moduleOpenClass;
        }

        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            if(env instanceof IRuntimeEnvWithContextManagingSupport){
                IRulesRuntimeContext runtimeContext = (IRulesRuntimeContext)env.getContext();
                if(runtimeContext == null){
                    runtimeContext = new DefaultRulesRuntimeContext();
                }else{
                    runtimeContext = new RulesRuntimeContextDelegator(runtimeContext);
                }
                runtimeContext.setValue((String)params[0], params[1]);
                ((IRuntimeEnvWithContextManagingSupport) env).pushContext(runtimeContext);
            }
            LOG.warn("Failed to modify runtime context. Runtime context does not support context modifications.");
            return null;
        }

        @Override
        public IOpenMethod getMethod() {
            return this;
        }

        @Override
        public String getName() {
            return MODIFY_CONTEXT_METHOD_NAME;
        }

        @Override
        public String getDisplayName(int mode) {
            return MODIFY_CONTEXT_METHOD_NAME;
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public IOpenClass getType() {
            return JavaOpenClass.VOID;
        }

        @Override
        public IMemberMetaInfo getInfo() {
            return null;
        }

        @Override
        public IOpenClass getDeclaringClass() {
            return moduleOpenClass;
        }

        @Override
        public IMethodSignature getSignature() {
            return new MethodSignature(new IOpenClass[] { JavaOpenClass.STRING, JavaOpenClass.OBJECT },
                new String[] { "property", "value" });
        }
    }

}
