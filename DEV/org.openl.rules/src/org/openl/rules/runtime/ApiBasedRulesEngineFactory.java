package org.openl.rules.runtime;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.exception.OpenLRuntimeException;
import org.openl.message.OpenLMessages;
import org.openl.runtime.IEngineWrapper;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.vm.IRuntimeEnv;

/**
 * @deprecated use {@link RulesEngineFactory}
 */
@Deprecated
public class ApiBasedRulesEngineFactory extends ASourceCodeRulesEngineFactory {

    private static final String RULES_XLS_OPENL_NAME = OpenL.OPENL_JAVA_RULE_NAME;
    
    private CompiledOpenClass compiledOpenClass;
    private Class<?> interfaceClass;

    public ApiBasedRulesEngineFactory(String sourceFile) {
        super(RULES_XLS_OPENL_NAME, sourceFile);
    }

    public ApiBasedRulesEngineFactory(File file) {
        super(RULES_XLS_OPENL_NAME, file);
    }

    public ApiBasedRulesEngineFactory(IOpenSourceCodeModule source) {
        super(RULES_XLS_OPENL_NAME, source);
    }

    public ApiBasedRulesEngineFactory(String openlName, IOpenSourceCodeModule source) {
        super(openlName, source);
    }

    public void reset(boolean resetInterface) {
        compiledOpenClass = null;
        if (resetInterface) {
            interfaceClass = null;
        }
    }

    /**
     * Creates java interface for rules project.
     * 
     * @return interface for rules project.
     */
    public Class<?> getInterfaceClass() {
        if (interfaceClass == null) {
            IOpenClass openClass = getCompiledOpenClass().getOpenClass();
            String className = openClass.getName();
            try {
                interfaceClass = InterfaceGenerator.generateInterface(className, openClass, getCompiledOpenClass()
                        .getClassLoader());
            } catch (Exception e) {
                throw new OpenLRuntimeException("Failed to create interface : " + className, e);
            }
        }
        return interfaceClass;
    }

    @Override
    protected Class<?>[] prepareInstanceInterfaces() {
        return new Class[] { interfaceClass, IEngineWrapper.class };
    }

    @Override
    protected Object prepareInstance(IRuntimeEnv runtimeEnv) {
        try {
            compiledOpenClass = getCompiledOpenClass();
            IOpenClass openClass = compiledOpenClass.getOpenClassWithErrors();
            Object openClassInstance = openClass.newInstance(runtimeEnv);
            Map<Method, IOpenMember> methodMap = prepareMethodMap(getInterfaceClass(), openClass);

            return prepareProxyInstance(openClassInstance, methodMap, runtimeEnv, getCompiledOpenClass().getClassLoader());

        } catch (Exception ex) {
            throw new OpenLRuntimeException("Cannot instantiate engine instance", ex);
        }
    }

    public CompiledOpenClass getCompiledOpenClass() {
        if (compiledOpenClass == null) {
            OpenLMessages.getCurrentInstance().clear();
            compiledOpenClass = initializeOpenClass();
        }
        return compiledOpenClass;
    }
}
