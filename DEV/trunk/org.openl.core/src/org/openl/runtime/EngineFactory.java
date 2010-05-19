package org.openl.runtime;

import java.io.File;
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
 * Class EngineFactory creates {@link Proxy} based wrappers around OpenL
 * classes. Each wrapper implements interface T and interface
 * {@link IEngineWrapper}. If OpenL IOpenClass does not have methods matching
 * interface T it will produce an error. <br/>
 * 
 * NOTE: OpenL fieldValues will be exposed as get<Field> methods
 * 
 * @param <T>
 * 
 * @author snshor
 */
public class EngineFactory<T> {

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
        this.sourceCode = new FileSourceCodeModule(file, null);
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
        this.sourceCode = new URLSourceCodeModule(url);
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
        this.sourceCode = new URLSourceCodeModule(url);
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

    public Class<T> getEngineInterface() {
        return engineInterface;
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
     * instance.
     */
    public void reset() {
        openClass = null;
        methodMap = null;
    }

    /**
     * Make new instance of rule engine
     * 
     * @return new instance
     */
    @SuppressWarnings("unchecked")
    private T makeEngineInstance() {

        IRuntimeEnv env = getOpenL().getVm().getRuntimeEnv();
        Object openlInstObject = getOpenClass().newInstance(env);

        // methodMap has been initialized with current Open Class
        OpenLInvocationHandler handler = makeInvocationHandler(openlInstObject, env, methodMap);

        return (T) Proxy.newProxyInstance(engineInterface.getClassLoader(), makeInstanceInterfaces(), handler);
    }

    protected OpenLInvocationHandler<T> makeInvocationHandler(Object openlInstance,
            IRuntimeEnv env,
            Map<Method, IOpenMember> methodMap) {
        
        return new OpenLInvocationHandler<T>(openlInstance, this, env, methodMap);
    }

    protected Class<?>[] makeInstanceInterfaces() {
        return new Class<?>[] { engineInterface, IEngineWrapper.class };
    }

    protected IOpenClass initializeOpenClass() {
        CompiledOpenClass compiledOpenClass = OpenLManager.compileModuleWithErrors(getOpenL(), getSourceCode());

        return compiledOpenClass.getOpenClass();
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

            IOpenMethod rulesMethod = module.getMatchingMethod(interfaceMethodName,
                JavaOpenClass.getOpenClasses(interfaceMethod.getParameterTypes()));

            if (rulesMethod != null) {
                methodMap.put(interfaceMethod, rulesMethod);
            } else {
                String fieldMethodPrefix = "get";

                if (interfaceMethodName.startsWith(fieldMethodPrefix)) {
                    String fieldName = "" + Character.toLowerCase(interfaceMethodName.charAt(fieldMethodPrefix.length())) + interfaceMethodName.substring(fieldMethodPrefix.length() + 1);

                    IOpenField rulesField = module.getField(fieldName, true);

                    if (rulesField != null) {
                        if (JavaOpenClass.getOpenClass(interfaceMethod.getReturnType()) == rulesField.getType()) {
                            methodMap.put(interfaceMethod, rulesField);

                            continue;
                        } else {
                            String message = String.format("Return type of method \"%s\" should be %s",
                                interfaceMethodName,
                                rulesField.getType());
                            throw new RuntimeException(message);
                        }
                    }
                }

                String message = String.format("There is no implementation in rules for interface method \"%s\"",
                    interfaceMethod);
                throw new RuntimeException(message);
            }
        }

        return methodMap;
    }

}
