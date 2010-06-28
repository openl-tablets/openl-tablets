package org.openl.rules.ruleservice.factory;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import org.openl.exception.OpenLRuntimeException;
import org.openl.runtime.ASourceCodeEngineFactory;
import org.openl.runtime.IEngineWrapper;
import org.openl.runtime.OpenLInvocationHandler;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.vm.IRuntimeEnv;

public class WebServiceRulesEngineFactory extends ASourceCodeEngineFactory {

    public static final String RULE_OPENL_NAME = "org.openl.xls";

    private IOpenClass openClass;
    private Class<?> interfaceClass;

    public WebServiceRulesEngineFactory(String sourceFile) {
        super(RULE_OPENL_NAME, sourceFile);
    }

    public WebServiceRulesEngineFactory(File file) {
        super(RULE_OPENL_NAME, file);
    }

    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }

    @Override
    protected Class<?>[] getInstanceInterfaces() {
        return new Class[] { interfaceClass, IEngineWrapper.class };
    }

    @Override
    public Object makeInstance() {

        try {
            openClass = initializeOpenClass().getOpenClass();
            String className = openClass.getName();
            interfaceClass = RulesFactory.generateInterface(className, openClass, getDefaultUserClassLoader());

            IRuntimeEnv runtimeEnv = getOpenL().getVm().getRuntimeEnv();
            Object openClassInstance = openClass.newInstance(runtimeEnv);
            Map<Method, IOpenMember> methodMap = makeMethodMap(interfaceClass, openClass);

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

}
