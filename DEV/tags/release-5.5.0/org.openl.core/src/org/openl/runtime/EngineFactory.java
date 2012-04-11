package org.openl.runtime;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.conf.IUserContext;
import org.openl.conf.UserContext;
import org.openl.engine.OpenLManager;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * 
 * @author snshor
 * 
 *         Class EngineFactory creates {@link Proxy} based wrappers around OpenL
 *         classes. Each wrapper implements interface T and interface
 *         {@link IEngineWrapper}. If OpenL IOpenClass does not have methods
 *         matching interface T it will produce an error. <br/>
 * 
 *         NOTE: OpenL fieldValues will be exposed as get<Field> methods
 * 
 * @param <T>
 */

public class EngineFactory<T> {

    private class OpenLHandler implements InvocationHandler, IEngineWrapper<T> {

        private Object openlInstance;
        private IRuntimeEnv openlEnv;
        private Map<Method, IOpenMember> methodMap;

        public OpenLHandler(Object openlInstance, IRuntimeEnv openlEnv, Map<Method, IOpenMember> methodMap) {
            this.openlInstance = openlInstance;
            this.openlEnv = openlEnv;
            this.methodMap = methodMap;
        }

        @Override
        public boolean equals(Object obj) {

            if (obj == null) {
                return false;
            }

            if (obj instanceof Proxy) {
                return Proxy.getInvocationHandler(obj) == this;
            }

            return super.equals(obj);
        }

        @SuppressWarnings("unchecked")
        public EngineFactory<T> getFactory() {
            return EngineFactory.this;
        }

        public Object getInstance() {
            return openlInstance;
        }

        public IRuntimeEnv getRuntimeEnv() {
            return openlEnv;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object executionResult;

            if (method.getDeclaringClass() == engineInterface) {
                IOpenMember m = methodMap.get(method);

                if (m instanceof IOpenMethod) {
                    IOpenMethod mx = (IOpenMethod) m;
                    executionResult = mx.invoke(openlInstance, args, openlEnv);
                } else {
                    IOpenField f = (IOpenField) m;
                    executionResult = f.get(openlInstance, openlEnv);
                }
            } else {
                Class<?>[] cargs = {};

                // TODO: What does this code mean?
                if (args != null && args.length == 1) {
                    cargs = new Class<?>[] { Object.class };
                }

                if (method.getDeclaringClass() == IEngineWrapper.class) {
                    Method myMethod = OpenLHandler.class.getDeclaredMethod(method.getName(), cargs);
                    executionResult = myMethod.invoke(this, args);
                } else {
                    Method objectMethod = Object.class.getDeclaredMethod(method.getName(), cargs);
                    executionResult = objectMethod.invoke(this, args);
                }
            }

            return executionResult;

        }

        @Override
        public String toString() {
            return String.format("Rule Engine(%s)", getOpenClass().getName());
        }

    }

    // This field should be always passed as constructor parameter
    private Class<T> engineInterface;

    // These fieldValues may be derived from other fieldValues, or set by
    // constructor directly
    private IOpenSourceCodeModule sourceCode;
    private OpenL openl;
    private IUserContext userContext;

    private String openlName;
    private String userHome = ".";
    private String sourceFile;

    // These fields are initialized internally and can't be passed as a
    // parameter of constructor
    private IOpenClass openClass;
    private Map<Method, IOpenMember> methodMap;

    /**
     * 
     * @param openlName Name of OpenL configuration {@link OpenL}.
     * @param factoryDef Engine factory definition
     *            {@link EngineFactoryDefinition}.
     * @param engineInterface User interface of rule.
     */
    public EngineFactory(String openlName, EngineFactoryDefinition factoryDef, Class<T> engineInterface) {
        this.openlName = openlName;
        this.userContext = factoryDef.ucxt;
        this.sourceCode = factoryDef.sourceCode;

        this.engineInterface = engineInterface;
    }

    /**
     * 
     * @param openlName Name of OpenL configuration {@link OpenL}
     * @param file Rule file
     * @param engineInterface User interface of rule
     */
    public EngineFactory(String openlName, File file, Class<T> engineInterface) {
        this.openlName = openlName;
        sourceCode = new FileSourceCodeModule(file, null);
        this.engineInterface = engineInterface;
    }

    /**
     * 
     * @param openlName Name of OpenL configuration {@link OpenL}
     * @param sourceFile A pathname of rule file string
     * @param engineInterface User interface of a rule
     */
    public EngineFactory(String openlName, String sourceFile, Class<T> engineInterface) {
        this.openlName = openlName;
        this.sourceFile = sourceFile;
        this.engineInterface = engineInterface;
    }

    /**
     * 
     * @param openlName Name of OpenL configuration {@link OpenL}
     * @param sourceFile A pathname of rule file string
     * @param engineInterface User interface of a rule
     * @param userContext User context {@link IUserContext}
     */
    public EngineFactory(String openlName, String sourceFile, Class<T> engineInterface, IUserContext userContext) {
        this.openlName = openlName;
        this.sourceFile = sourceFile;
        this.engineInterface = engineInterface;
        this.userContext = userContext;
    }

    /**
     * 
     * @param openlName Name of OpenL configuration {@link OpenL}
     * @param userHome Current path of Openl userHome
     * @param sourceFile A pathname of rule file string
     * @param engineInterface User interface of a rule
     */
    public EngineFactory(String openlName, String userHome, String sourceFile, Class<T> engineInterface) {
        this.openlName = openlName;
        this.userHome = userHome;
        this.sourceFile = sourceFile;
        this.engineInterface = engineInterface;
    }

    /**
     * 
     * @param openlName Name of OpenL configuration {@link OpenL}
     * @param url Url to rule file
     * @param engineInterface User interface of a rule
     */
    public EngineFactory(String openlName, URL url, Class<T> engineInterface) {
        this.openlName = openlName;
        sourceCode = new URLSourceCodeModule(url);
        this.engineInterface = engineInterface;
    }

    /**
     * 
     * @param openlName Name of OpenL configuration {@link OpenL}
     * @param url Url to rule file
     * @param engineInterface User interface of a rule
     * @param userContext User context {@link IUserContext}
     */
    public EngineFactory(String openlName, URL url, Class<T> engineInterface, IUserContext userContext) {
        this.openlName = openlName;
        sourceCode = new URLSourceCodeModule(url);
        this.engineInterface = engineInterface;
        this.userContext = userContext;
    }

    /**
     * @return an abstraction of a "class".
     */
    public synchronized IOpenClass getOpenClass() {
        if (openClass == null) {
            openClass = initializeOpenClass();
            // methodMap must be initialized with OpenClass it relates to
            methodMap = initializeMethodMap(openClass);
        }
        return openClass;
    }

    /**
     * @return Openl instance.
     */
    public synchronized OpenL getOpenL() {
        if (openl == null) {
            openl = OpenL.getInstance(openlName, getUserContext());
        }
        return openl;
    }

    /**
     * @return source code of a file.
     */
    public synchronized IOpenSourceCodeModule getSourceCode() {
        if (sourceCode == null) {
            sourceCode = new FileSourceCodeModule(sourceFile, null);
        }
        return sourceCode;
    }

    /**
     * @return user context.
     */
    public synchronized IUserContext getUserContext() {
        if (userContext == null) {
            userContext = new UserContext(getDefaultUserClassLoader(), userHome);
        }
        return userContext;
    }

    protected ClassLoader getDefaultUserClassLoader() {
        ClassLoader userClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            // checking if classloader has openl, sometimes it does not
            userClassLoader.loadClass(this.getClass().getName());
        } catch (ClassNotFoundException cnfe) {
            userClassLoader = this.getClass().getClassLoader();
        }
        return userClassLoader;
    }

    private Map<Method, IOpenMember> initializeMethodMap(IOpenClass module) {
        Map<Method, IOpenMember> methodMap = new HashMap<Method, IOpenMember>();

        Method[] interfaceMethods = engineInterface.getDeclaredMethods();

        for (Method interfaceMethod : interfaceMethods) {
            String interfaceMethodName = interfaceMethod.getName();

            IOpenMethod rulesMethod = module.getMatchingMethod(interfaceMethodName, JavaOpenClass
                    .getOpenClasses(interfaceMethod.getParameterTypes()));

            if (rulesMethod != null) {
                methodMap.put(interfaceMethod, rulesMethod);
            } else {
                final String fieldMethodPrefix = "get";
                if (interfaceMethodName.startsWith(fieldMethodPrefix)) {
                    String fieldName = ""
                            + Character.toLowerCase(interfaceMethodName.charAt(fieldMethodPrefix.length()))
                            + interfaceMethodName.substring(fieldMethodPrefix.length() + 1);

                    IOpenField rulesField = module.getField(fieldName, true);

                    if (rulesField != null) {
                        if (JavaOpenClass.getOpenClass(interfaceMethod.getReturnType()) == rulesField.getType()) {
                            methodMap.put(interfaceMethod, rulesField);
                            continue;
                        } else {
                            throw new RuntimeException(String.format("Return type of method \"%s\" should be %s",
                                    interfaceMethodName, rulesField.getType()));
                        }
                    }
                }
                throw new RuntimeException(String.format(
                        "There is no implementation in rules for interface method \"%s\"", interfaceMethod));
            }
        }

        return methodMap;
    }

    private Class<?>[] makeInstanceInterfaces() {
        return new Class<?>[] { engineInterface, IEngineWrapper.class };
    }

    protected IOpenClass initializeOpenClass() {
        CompiledOpenClass compiledOpenClass = OpenLManager.compileModuleWithErrors(getOpenL(), getSourceCode());
        return compiledOpenClass.getOpenClass();
    }

    /**
     * Make new instance of rule engine
     * 
     * @return new instance
     */
    @SuppressWarnings("unchecked")
    public T makeEngineInstance() {
        IRuntimeEnv env = getOpenL().getVm().getRuntimeEnv();

        Object openlInstObject = getOpenClass().newInstance(env);

        // methodMap has been initialized with current Open Class
        OpenLHandler handler = new OpenLHandler(openlInstObject, env, methodMap);

        return (T) Proxy.newProxyInstance(engineInterface.getClassLoader(), makeInstanceInterfaces(), handler);
    }

    /**
     * Create new instance of rule engine
     * 
     * @return new instance
     */
    public T newInstance() {
        return makeEngineInstance();
    }

    /**
     * Force EngineFactory to recompile the rules when creating new rules
     * instance
     */
    public void reset() {
        openClass = null;
        methodMap = null;
    }

}
