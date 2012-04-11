package org.openl.rules.runtime;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.exception.OpenLRuntimeException;
import org.openl.message.OpenLMessages;
import org.openl.runtime.ASourceCodeEngineFactory;
import org.openl.runtime.IEngineWrapper;
import org.openl.runtime.OpenLInvocationHandler;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.vm.IRuntimeEnv;

public class ApiBasedRulesEngineFactory extends ASourceCodeEngineFactory {

    public static final String RULE_OPENL_NAME = "org.openl.xls";

    private CompiledOpenClass compiledOpenClass;
    private Class<?> interfaceClass;

    public ApiBasedRulesEngineFactory(String sourceFile) {
        super(RULE_OPENL_NAME, sourceFile);
    }

    public ApiBasedRulesEngineFactory(File file) {
        super(RULE_OPENL_NAME, file);
    }
    
    public void reset(boolean resetInterface) {
        compiledOpenClass = null;
        if (resetInterface) {
            interfaceClass = null;
        }
    }

    public Class<?> getInterfaceClass() {
        if(interfaceClass == null){
            IOpenClass openClass = getCompiledOpenClass().getOpenClass();
            String className = openClass.getName();
            try {
                interfaceClass = RulesFactory.generateInterface(className, openClass, getDefaultUserClassLoader());
            } catch (Exception e) {
                throw new OpenLRuntimeException("Failed to create interface : " + className, e);
            }
        }
        return interfaceClass;
    }

    @Override
    protected Class<?>[] getInstanceInterfaces() {
        return new Class[] { interfaceClass, IEngineWrapper.class };
    }

    @Override
    public Object makeInstance() {
        try {
            compiledOpenClass = getCompiledOpenClass();
            IOpenClass openClass = compiledOpenClass.getOpenClassWithErrors();

            IRuntimeEnv runtimeEnv = getOpenL().getVm().getRuntimeEnv();
            Object openClassInstance = openClass.newInstance(runtimeEnv);
            Map<Method, IOpenMember> methodMap = makeMethodMap(getInterfaceClass(), openClass);

            return makeEngineInstance(openClassInstance, methodMap, runtimeEnv, getDefaultUserClassLoader());

        } catch (Exception ex) {
            throw new OpenLRuntimeException("Cannot instantiate engine instance", ex);
        }
    }

    @Override
    protected InvocationHandler makeInvocationHandler(Object openClassInstance,
            Map<Method, IOpenMember> methodMap,
            IRuntimeEnv runtimeEnv) {

        return new OpenLInvocationHandler(openClassInstance, this, runtimeEnv, methodMap);
    }

    public CompiledOpenClass getCompiledOpenClass() {
        if(compiledOpenClass == null){
            OpenLMessages.getCurrentInstance().clear();
            compiledOpenClass = initializeOpenClass();
        }
        return compiledOpenClass;
    }
}
