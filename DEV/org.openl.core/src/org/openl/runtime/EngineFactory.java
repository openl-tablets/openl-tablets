package org.openl.runtime;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.conf.IUserContext;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.vm.IRuntimeEnv;

/**
 * Class EngineFactory creates {@link Proxy} based wrappers around OpenL
 * classes. Each wrapper implements interface T and interface
 * {@link IEngineWrapper}. If OpenL IOpenClass does not have methods matching
 * interface T it will produce an error. <br/>
 * 
 * NOTE: OpenL fieldValues will be exposed as get<Field> methods
 * 
 * @param <T>
 * 
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

    public EngineFactory(String openlName, IOpenSourceCodeModule sourceCode, IUserContext userContext,
            Class<T> interfaceClass) {
        super(openlName, sourceCode, userContext);
        if (interfaceClass == null) {
            throw new IllegalArgumentException("Interface can't be null!");
        }
        this.interfaceClass = interfaceClass;
    }

    public EngineFactory(String openlName, IOpenSourceCodeModule sourceCode, String userHome, Class<T> interfaceClass) {
        super(openlName, sourceCode, userHome);
        if (interfaceClass == null) {
            throw new IllegalArgumentException("Interface can't be null!");
        }
        this.interfaceClass = interfaceClass;
    }

    public EngineFactory(String openlName, IOpenSourceCodeModule sourceCode, Class<T> interfaceClass) {
        super(openlName, sourceCode);
        if (interfaceClass == null) {
            throw new IllegalArgumentException("Interface can't be null!");
        }
        this.interfaceClass = interfaceClass;
    }

    public EngineFactory(String openlName, String sourceFile, String userHome, Class<T> interfaceClass) {
        super(openlName, sourceFile, userHome);
        if (interfaceClass == null) {
            throw new IllegalArgumentException("Interface can't be null!");
        }
        this.interfaceClass = interfaceClass;
    }

    public EngineFactory(String openlName, String sourceFile, Class<T> interfaceClass) {
        super(openlName, sourceFile);
        if (interfaceClass == null) {
            throw new IllegalArgumentException("Interface can't be null!");
        }
        this.interfaceClass = interfaceClass;
    }

    public EngineFactory(String openlName, URL source, Class<T> interfaceClass) {
        super(openlName, source);
        if (interfaceClass == null) {
            throw new IllegalArgumentException("Interface can't be null!");
        }
        this.interfaceClass = interfaceClass;
    }

    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    protected void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    @SuppressWarnings("unchecked")
    public T newEngineInstance() {
        return (T) newInstance();
    }

    @SuppressWarnings("unchecked")
    public T newEngineInstance(IRuntimeEnv runtimeEnv) {
        return (T) newInstance(runtimeEnv);
    }

    public void reset() {
        compiledOpenClass = null;
    }

    @Override
    protected Class<?>[] prepareInstanceInterfaces() {
        return new Class[] { getInterfaceClass(), IEngineWrapper.class };
    }

    @Override
    public Object prepareInstance(IRuntimeEnv runtimeEnv) {
        try {
            compiledOpenClass = getCompiledOpenClass();
            IOpenClass openClass = compiledOpenClass.getOpenClass();
            Map<Method, IOpenMember> methodMap = prepareMethodMap(getInterfaceClass(), openClass);
            Object openClassInstance = openClass.newInstance(runtimeEnv);
            return prepareProxyInstance(openClassInstance, methodMap, runtimeEnv, getInterfaceClass().getClassLoader());
        } catch (OpenlNotCheckedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OpenlNotCheckedException("Failed to instantiate engine instance!", ex);
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
