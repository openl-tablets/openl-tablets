package org.openl.runtime;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Map;
import java.util.Objects;

import org.openl.CompiledOpenClass;
import org.openl.conf.IUserContext;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.vm.IRuntimeEnv;

/**
 * Class EngineFactory creates {@link Proxy} based wrappers around OpenL classes. Each wrapper implements interface T
 * and interface {@link IEngineWrapper}. If OpenL IOpenClass does not have methods matching interface T it will produce
 * an error. <br/>
 * <p>
 * NOTE: OpenL fieldValues will be exposed as get<Field> methods
 *
 * @param <T>
 * @author Marat Kamalov
 */
public class EngineFactory<T> extends ASourceCodeEngineFactory {

    private Class<T> interfaceClass;
    private CompiledOpenClass compiledOpenClass;

    protected Map<Method, IOpenMember> methodMap;

    protected EngineFactory(String openlName, IOpenSourceCodeModule sourceCode, IUserContext userContext) {
        super(openlName, sourceCode, userContext);
    }

    protected EngineFactory(String openlName, IOpenSourceCodeModule sourceCode, String userHome) {
        super(openlName, sourceCode, userHome);
    }

    protected EngineFactory(String openlName, IOpenSourceCodeModule sourceCode) {
        super(openlName, sourceCode);
    }

    protected EngineFactory(String openlName, String sourceFile, String userHome) {
        super(openlName, sourceFile, userHome);
    }

    protected EngineFactory(String openlName, String sourceFile) {
        super(openlName, sourceFile);
    }

    protected EngineFactory(String openlName, URL source) {
        super(openlName, source);
    }

    public EngineFactory(String openlName,
                         IOpenSourceCodeModule sourceCode,
                         IUserContext userContext,
                         Class<T> interfaceClass) {
        super(openlName, sourceCode, userContext);
        this.interfaceClass = Objects.requireNonNull(interfaceClass, "interfaceClass cannot be null");
    }

    public EngineFactory(String openlName, IOpenSourceCodeModule sourceCode, String userHome, Class<T> interfaceClass) {
        super(openlName, sourceCode, userHome);
        this.interfaceClass = Objects.requireNonNull(interfaceClass, "interfaceClass cannot be null");
    }

    public EngineFactory(String openlName, IOpenSourceCodeModule sourceCode, Class<T> interfaceClass) {
        super(openlName, sourceCode);
        this.interfaceClass = Objects.requireNonNull(interfaceClass, "interfaceClass cannot be null");
    }

    public EngineFactory(String openlName, String sourceFile, String userHome, Class<T> interfaceClass) {
        super(openlName, sourceFile, userHome);
        this.interfaceClass = Objects.requireNonNull(interfaceClass, "interfaceClass cannot be null");
    }

    public EngineFactory(String openlName, String sourceFile, Class<T> interfaceClass) {
        super(openlName, sourceFile);
        this.interfaceClass = Objects.requireNonNull(interfaceClass, "interfaceClass cannot be null");
    }

    public EngineFactory(String openlName, URL source, Class<T> interfaceClass) {
        super(openlName, source);
        this.interfaceClass = Objects.requireNonNull(interfaceClass, "interfaceClass cannot be null");
    }

    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public T newEngineInstance() {
        return newEngineInstance(false);
    }

    public T newEngineInstance(IRuntimeEnv runtimeEnv) {
        return newEngineInstance(runtimeEnv, false);
    }

    @SuppressWarnings("unchecked")
    public T newEngineInstance(IRuntimeEnv runtimeEnv, boolean ignoreCompilationErrors) {
        return (T) newInstance(runtimeEnv, ignoreCompilationErrors);
    }

    @SuppressWarnings("unchecked")
    public T newEngineInstance(boolean ignoreCompilationErrors) {
        return (T) newInstance(ignoreCompilationErrors);
    }

    public void reset() {
        compiledOpenClass = null;
    }

    protected Class<?>[] prepareInstanceInterfaces() {
        return new Class[]{IEngineWrapper.class};
    }

    @Override
    public Object prepareInstance(IRuntimeEnv runtimeEnv, boolean ignoreCompilationErrors) {
        try {
            compiledOpenClass = getCompiledOpenClass();
            IOpenClass openClass = ignoreCompilationErrors ? compiledOpenClass.getOpenClassWithErrors()
                    : compiledOpenClass.getOpenClass();
            Class<T> interfaceClass = getInterfaceClass();
            Map<Method, IOpenMember> methodMap = prepareMethodMap(interfaceClass, openClass);
            Object openClassInstance = openClass
                    .newInstance(runtimeEnv == null ? getRuntimeEnvBuilder().buildRuntimeEnv() : runtimeEnv);
            ClassLoader classLoader = interfaceClass.getClassLoader();

            Class<?>[] interfaces = prepareInstanceInterfaces();

            var proxyInterfaces = new Class[interfaces.length + 1];
            proxyInterfaces[0] = interfaceClass;
            System.arraycopy(interfaces, 0, proxyInterfaces, 1, interfaces.length);

            return ASMProxyFactory.newProxyInstance(classLoader,
                    prepareMethodHandler(openClassInstance, methodMap, runtimeEnv),
                    proxyInterfaces);
        } catch (OpenlNotCheckedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OpenlNotCheckedException("Failed to instantiate engine instance.", ex);
        }
    }


    @Override
    public CompiledOpenClass getCompiledOpenClass() {
        if (compiledOpenClass == null) {
            compiledOpenClass = initializeOpenClass();
        }
        return compiledOpenClass;
    }
}
