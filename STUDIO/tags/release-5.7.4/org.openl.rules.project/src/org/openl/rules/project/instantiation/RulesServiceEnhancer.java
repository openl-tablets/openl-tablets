package org.openl.rules.project.instantiation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContextProvider;
import org.openl.rules.runtime.RuleInfo;
import org.openl.rules.runtime.RulesFactory;

/**
 * Auxiliary class which enhances rule service with ability to use rule service
 * method with rules runtime context during method invocation. The class is used
 * by engine to expose rules services as web service.
 * 
 * Enhancer class decorates methods of original service class and exposes new
 * methods signatures instead of original methods. New method signature has one
 * more parameter - rules runtime context. While service method invocation
 * engine do the following steps:
 * <ul>
 * <li>recognize context parameter;</li>
 * <li>recognize original service method to invoke using method signature;</li>
 * <li>set rules runtime context;</li>
 * <li>invoke appropriate service method.</li>
 * </ul>
 */
public class RulesServiceEnhancer {

    private static final Log LOG = LogFactory.getLog(RulesServiceEnhancer.class);

    /**
     * Suffix of enhanced class name.
     */
    private static final String CLASS_NAME_SUFFIX = "$RulesEnhanced";

    /**
     * Instantiation strategy delegate.
     */
    private RulesInstantiationStrategy instantiationStrategy;

    /**
     * Internal generated class at runtime which used as service class.
     */
    private Class<?> serviceClass;
    
    /**
     * Internal class loader.
     */
    private OpenLBundleClassLoader classLoader;

    /**
     * Constructs new instance of enhancer.
     * 
     * @param instantiationStrategy instantiation strategy which used to
     *            instantiate original service
     */
    public RulesServiceEnhancer(RulesInstantiationStrategy instantiationStrategy) {
        this.instantiationStrategy = instantiationStrategy;
    }

    /**
     * Gets enhanced service class.
     * 
     * @return service class
     * @throws ClassNotFoundException
     * @throws InstantiationException
     */
    public Class<?> getServiceClass() throws ClassNotFoundException, InstantiationException {

        if (serviceClass == null) {
            Class<?> originalServiceClass = instantiationStrategy.getServiceClass();

            try {
                serviceClass = decorateMethods(originalServiceClass);
            } catch (Exception e) {
                throw new InstantiationException(e.getMessage());
            }
        }

        return serviceClass;
    }

    /**
     * Creates new instance of service class.
     * 
     * @return instance of service class
     * @throws InstantiationException
     */
    public Object instantiate(ReloadType reloadType) throws InstantiationException {

        try {
            InvocationHandler handler = makeInvocationHandler(reloadType);
            return Proxy.newProxyInstance(getInternalClassLoader(), getProxyInterfaces(), handler);
        } catch (Exception e) {
            throw new InstantiationException(e.getMessage());
        }
    }

    /**
     * Makes invocation handler.
     * 
     * @return {@link InvocationHandler} instance
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    private InvocationHandler makeInvocationHandler(ReloadType reloadType) throws Exception {

        Map<Method, Method> methodsMap = makeMethodMap(getServiceClass(), instantiationStrategy.getServiceClass());
        return new RulesServiceEnhancerInvocationHandler(methodsMap, instantiationStrategy.instantiate(reloadType));
    }

    /**
     * Gets interface classes what used for proxy construction.
     * 
     * @return proxy interfaces
     * @throws Exception
     */
    private Class<?>[] getProxyInterfaces() throws Exception {
        return new Class<?>[] { getServiceClass() };
    }

    /**
     * Decorates methods signatures of given clazz.
     * 
     * @param clazz class to decorate
     * @return new class with decorated methods signatures
     * @throws Exception
     */
    private Class<?> decorateMethods(Class<?> clazz) throws Exception {

        Method[] methods = clazz.getMethods();
        List<RuleInfo> rules = getRules(methods);

        String className = clazz.getName() + CLASS_NAME_SUFFIX;
        RuleInfo[] rulesArray = rules.toArray(new RuleInfo[rules.size()]);
        
        LOG.debug(String.format("Generating proxy interface for '%s' class", clazz.getName()));
        
        return RulesFactory.generateInterface(className, rulesArray, getInternalClassLoader());
    }
    
    private ClassLoader getInternalClassLoader() throws ClassNotFoundException {
        if (classLoader == null) {
            ClassLoader originalClassLoader = instantiationStrategy.getClassLoader();
            classLoader = new SimpleBundleClassLoader(originalClassLoader);
            classLoader.addClassLoader(instantiationStrategy.getServiceClass().getClassLoader());
        }
        
        return classLoader;
    }

    /**
     * Gets list of rules.
     * 
     * @param methods array of methods what represents rule methods
     * @return list of rules meta-info
     */
    private List<RuleInfo> getRules(Method[] methods) {

        List<RuleInfo> rules = new ArrayList<RuleInfo>(methods.length);

        for (Method method : methods) {

            if (ArrayUtils.contains(IRulesRuntimeContextProvider.class.getMethods(), method)) {
                // Ignore methods what declared by IRulesRuntimeContextProvider
                // class. Service user shouldn't use they directly.
                //
                continue;
            }

            String methodName = method.getName();

            Class<?>[] paramTypes = method.getParameterTypes();
            Class<?> returnType = method.getReturnType();
            Class<?>[] newParams = new Class<?>[] { IRulesRuntimeContext.class };
            Class<?>[] extendedParamTypes = (Class<?>[]) ArrayUtils.addAll(newParams, paramTypes);

            RuleInfo ruleInfo = RulesFactory.createRuleInfo(methodName, extendedParamTypes, returnType);

            rules.add(ruleInfo);
        }

        return rules;
    }

    /**
     * Gets methods map where keys are interface class methods and values -
     * original service class methods.
     * 
     * @param interfaceClass class to expose as service class
     * @param serviceClass original service class
     * @return methods map
     */
    private Map<Method, Method> makeMethodMap(Class<?> interfaceClass, Class<?> serviceClass) {

        LOG.debug(String.format("Creating methods map for classes: %s <-> %s", interfaceClass, serviceClass));

        Map<Method, Method> methodMap = new HashMap<Method, Method>();
        Method[] serviceMethods = serviceClass.getDeclaredMethods();

        for (Method serviceMethod : serviceMethods) {

            String interfaceMethodName = serviceMethod.getName();
            Class<?>[] serviceMethodParameterTypes = serviceMethod.getParameterTypes();

            Class<?>[] newParams = new Class<?>[] { IRulesRuntimeContext.class };
            Class<?>[] extendedParamTypes = (Class<?>[]) ArrayUtils.addAll(newParams, serviceMethodParameterTypes);

            Method interfaceMethod;
            try {
                interfaceMethod = interfaceClass.getMethod(interfaceMethodName, extendedParamTypes);
                methodMap.put(interfaceMethod, serviceMethod);
            } catch (NoSuchMethodException e) {
                // Ignore an exception. Interface class can ignore several
                // methods what declared in service class.
                //
            }
        }

        LOG.debug(methodMap.toString());
        
        return methodMap;
    }

}
