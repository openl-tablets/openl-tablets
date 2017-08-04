package org.openl.rules.runtime;

import org.openl.OpenL;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.context.IRulesRuntimeContextProvider;
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.runtime.EngineFactory;
import org.openl.runtime.IEngineWrapper;
import org.openl.runtime.IRuntimeEnvBuilder;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;

/**
 * Simple engine factory Requiring only source of rules and generates interface
 * for it if service interface not defined.
 *
 * @author PUdalau, Marat Kamalov
 */
public class RulesEngineFactory<T> extends EngineFactory<T> {

    private final Logger log = LoggerFactory.getLogger(RulesEngineFactory.class);
    private static final String RULES_XLS_OPENL_NAME = OpenL.OPENL_JAVA_RULE_NAME;

    private InterfaceClassGenerator interfaceClassGenerator = new InterfaceClassGeneratorImpl();

    public void setInterfaceClassGenerator(InterfaceClassGenerator interfaceClassGenerator) {
        if (interfaceClassGenerator == null) {
            throw new IllegalArgumentException("interfaceClassGenerator argument can't be null");
        }
        if (super.getInterfaceClass() != null) {
            log.warn("Rules engine factory has already had interface class. Interface class generator will be ignored!");
        }
        this.interfaceClassGenerator = interfaceClassGenerator;
    }

    public InterfaceClassGenerator getInterfaceClassGenerator() {
        return interfaceClassGenerator;
    }

    /**
     * @deprecated use {@link #RulesEngineFactory(URL)},
     */
    @Deprecated
    public RulesEngineFactory(File file) {
        super(RULES_XLS_OPENL_NAME, URLSourceCodeModule.toUrl(file));
    }

    /**
     * @deprecated use {@link #RulesEngineFactory(URL, Class)}
     */
    @Deprecated
    public RulesEngineFactory(File file, Class<T> interfaceClass) {
        super(RULES_XLS_OPENL_NAME, URLSourceCodeModule.toUrl(file));
        super.setInterfaceClass(interfaceClass);
    }

    public RulesEngineFactory(String sourceFile) {
        super(RULES_XLS_OPENL_NAME, sourceFile);
    }

    public RulesEngineFactory(String sourceFile, Class<T> interfaceClass) {
        super(RULES_XLS_OPENL_NAME, sourceFile, interfaceClass);
    }

    public RulesEngineFactory(String sourceFile, String userHome) {
        super(RULES_XLS_OPENL_NAME, sourceFile, userHome);
    }

    public RulesEngineFactory(String sourceFile, String userHome, Class<T> interfaceClass) {
        super(RULES_XLS_OPENL_NAME, sourceFile, userHome);
        super.setInterfaceClass(interfaceClass);
    }

    public RulesEngineFactory(IOpenSourceCodeModule sourceCodeModule) {
        super(RULES_XLS_OPENL_NAME, sourceCodeModule);
    }

    public RulesEngineFactory(IOpenSourceCodeModule sourceCodeModule, Class<T> interfaceClass) {
        super(RULES_XLS_OPENL_NAME, sourceCodeModule);
        super.setInterfaceClass(interfaceClass);
    }

    public RulesEngineFactory(IOpenSourceCodeModule source, String userHome) {
        super(RULES_XLS_OPENL_NAME, source, userHome);
    }

    public RulesEngineFactory(IOpenSourceCodeModule source, String userHome, Class<T> interfaceClass) {
        super(RULES_XLS_OPENL_NAME, source, userHome);
        super.setInterfaceClass(interfaceClass);
    }
    
    public RulesEngineFactory(URL source) {
        super(RULES_XLS_OPENL_NAME, source);
    }
    
    public RulesEngineFactory(URL source, Class<T> interfaceClass) {
        super(RULES_XLS_OPENL_NAME, source);
        if (interfaceClass == null) {
            throw new IllegalArgumentException("Interface can't be null!");
        }
        super.setInterfaceClass(interfaceClass);
    }

    public RulesEngineFactory(String openlName, IOpenSourceCodeModule sourceCode, Class<T> interfaceClass) {
        super(openlName, sourceCode);
        super.setInterfaceClass(interfaceClass);
    }

    /**
     * Added to allow using other openl names, such as org.openl.xls.ce
     *
     * @param source
     * @param userHome
     * @param openlName
     */
    public RulesEngineFactory(IOpenSourceCodeModule source, String userHome, String openlName) {
        super(openlName, source, userHome);
    }

    public void reset(boolean resetInterface) {
        super.reset();
        if (resetInterface) {
            setInterfaceClass(null);
        }
    }

    /**
     * Creates java interface for rules project.
     *
     * @return interface for rules project.
     */
    @Override
    public Class<T> getInterfaceClass() {
        if (super.getInterfaceClass() == null) {
            IOpenClass openClass = getCompiledOpenClass().getOpenClass();
            String className = openClass.getName();
            ClassLoader classLoader = getCompiledOpenClass().getClassLoader();
            try {
                if (ByteCodeGeneratorHelper.isClassLoaderContainsClass(classLoader, className)) {
                    log.warn("Previously generated  interface '{}' will be used as service class.", className);
                    @SuppressWarnings("unchecked")
                    Class<T> interfaceClass = (Class<T>) classLoader.loadClass(className);
                    setInterfaceClass(interfaceClass);
                    return interfaceClass;
                } else {
                    @SuppressWarnings("unchecked")
                    Class<T> interfaceClass = (Class<T>) interfaceClassGenerator.generateInterface(className, openClass,
                            classLoader);
                    setInterfaceClass(interfaceClass);
                    return interfaceClass;
                }
            } catch (Exception e) {
                throw new OpenlNotCheckedException("Failed to generate interface : " + className, e);
            }
        } else {
            return super.getInterfaceClass();
        }
    }

    @Override
    protected Class<?>[] prepareInstanceInterfaces() {
        return new Class[]{getInterfaceClass(), IEngineWrapper.class, IRulesRuntimeContextProvider.class};
    }

    private IRuntimeEnvBuilder runtimeEnvBuilder = null;

    @Override
    protected IRuntimeEnvBuilder getRuntimeEnvBuilder() {
        if (runtimeEnvBuilder == null) {
            runtimeEnvBuilder = new IRuntimeEnvBuilder() {
                @Override
                public IRuntimeEnv buildRuntimeEnv() {
                    return new SimpleRulesVM().getRuntimeEnv();
                }
            };
        }
        return runtimeEnvBuilder;
    }

    @Override
    protected InvocationHandler prepareInvocationHandler(Object openClassInstance, Map<Method, IOpenMember> methodMap,
                                                         IRuntimeEnv runtimeEnv) {
        return new OpenLRulesInvocationHandler(openClassInstance, runtimeEnv, methodMap);
    }
}
