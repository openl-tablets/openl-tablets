package org.openl.rules.runtime;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.exception.OpenLRuntimeException;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.message.OpenLMessages;
import org.openl.runtime.ASourceCodeEngineFactory;
import org.openl.runtime.IEngineWrapper;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;

/**
 * Simple engine factory requiring only source of rules and generates interface
 * for it.
 * 
 * @author PUdalau
 */
public class SimpleEngineFactory extends ASourceCodeEngineFactory {
    private static final String RULES_XLS_OPENL_NAME = "org.openl.xls";

    private CompiledOpenClass compiledOpenClass;
    private Class<?> interfaceClass;

    public SimpleEngineFactory(String sourceFile) {
        super(RULES_XLS_OPENL_NAME, sourceFile);
    }

    public SimpleEngineFactory(File file) {
        super(RULES_XLS_OPENL_NAME, file);
    }

    public SimpleEngineFactory(IOpenSourceCodeModule sourceCodeModule) {
        super(RULES_XLS_OPENL_NAME, sourceCodeModule);
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
                interfaceClass = RulesFactory.generateInterface(className,
                    openClass,
                    getCompiledOpenClass().getClassLoader());
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
    protected ThreadLocal<org.openl.vm.IRuntimeEnv> initRuntimeEnvironment() {
        return new ThreadLocal<org.openl.vm.IRuntimeEnv>() {
            @Override
            protected org.openl.vm.IRuntimeEnv initialValue() {
                return getOpenL().getVm().getRuntimeEnv();
            }
        };
    }

    @Override
    public Object makeInstance() {
        try {
            compiledOpenClass = getCompiledOpenClass();
            IOpenClass openClass = compiledOpenClass.getOpenClassWithErrors();

            Object openClassInstance = openClass.newInstance(getRuntimeEnv());
            Map<Method, IOpenMember> methodMap = makeMethodMap(getInterfaceClass(), openClass);

            return makeEngineInstance(openClassInstance,
                methodMap,
                getRuntimeEnv(),
                getCompiledOpenClass().getClassLoader());

        } catch (Exception ex) {
            throw new OpenlNotCheckedException("Cannot instantiate engine instance", ex);
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
