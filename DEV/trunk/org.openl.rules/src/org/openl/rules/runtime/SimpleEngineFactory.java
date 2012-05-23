package org.openl.rules.runtime;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.exception.OpenLRuntimeException;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.message.OpenLMessages;
import org.openl.rules.datatype.gen.BeanByteCodeGenerator;
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
    private final Log log = LogFactory.getLog(SimpleEngineFactory.class);
    private static final String RULES_XLS_OPENL_NAME = OpenL.OPENL_JAVA_RULE_NAME;

    private CompiledOpenClass compiledOpenClass;
    private Class<?> interfaceClass;

    public SimpleEngineFactory(String sourceFile) {
        this(new File(sourceFile));
    }

    public SimpleEngineFactory(String sourceFile, String userHome) {
        super(RULES_XLS_OPENL_NAME, sourceFile, userHome);
    }

    public SimpleEngineFactory(File file) {
        super(RULES_XLS_OPENL_NAME, file);
    }

    public SimpleEngineFactory(IOpenSourceCodeModule sourceCodeModule) {
        super(RULES_XLS_OPENL_NAME, sourceCodeModule);
    }

    public SimpleEngineFactory(IOpenSourceCodeModule source, String userHome) {
        super(RULES_XLS_OPENL_NAME, source, userHome);
    }

    public void reset(boolean resetInterface) {
        compiledOpenClass = null;
        if (resetInterface) {
            interfaceClass = null;
        }
    }

    public void setInterfaceClass(Class<?> interfaceClass) {
		this.interfaceClass = interfaceClass;
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
            ClassLoader classLoader = getCompiledOpenClass().getClassLoader();
            try {
                if (BeanByteCodeGenerator.isClassLoaderContainsClass(classLoader, className)) {
                    log.warn(String.format("Previously generated  interface '%s' will be used as service class.",
                        className));
                    interfaceClass = classLoader.loadClass(className);
                } else {
                    interfaceClass = RulesFactory.generateInterface(className, openClass, classLoader);
                }
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
