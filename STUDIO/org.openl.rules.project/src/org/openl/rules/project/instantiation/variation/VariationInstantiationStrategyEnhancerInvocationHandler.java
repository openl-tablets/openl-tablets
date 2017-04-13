package org.openl.rules.project.instantiation.variation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.openl.binding.MethodUtil;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenLRuntimeException;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.main.OpenLWrapper;
import org.openl.rules.project.SafeCloner;
import org.openl.rules.runtime.OpenLRulesInvocationHandler;
import org.openl.rules.variation.NoVariation;
import org.openl.rules.variation.Variation;
import org.openl.rules.variation.VariationDescription;
import org.openl.rules.variation.VariationsFactory;
import org.openl.rules.variation.VariationsFromRules;
import org.openl.rules.variation.VariationsPack;
import org.openl.rules.variation.VariationsResult;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.runtime.IEngineWrapper;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * InvocationHandler for proxy that injects variations into service class.
 * <p/>
 * Handles both original methods and enhanced with variations.
 *
 * @author PUdalau, Marat Kamalov
 */
class VariationInstantiationStrategyEnhancerInvocationHandler implements InvocationHandler {

    private static final String GET_RUNTIME_ENVIRONMENT_METHOD = "getRuntimeEnvironment";

    private final static String VARIATION_CORE_POOL_SIZE = "variationCorePoolSize";
    private final static String VARIATION_MAX_POOL_SIZE = "variationMaximumPoolSize";

    static final ExecutorService executorService = new ThreadPoolExecutor(getSystemParam(VARIATION_CORE_POOL_SIZE, 8),
        getSystemParam(VARIATION_MAX_POOL_SIZE, 16),
        60L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>());

    SafeCloner cloner = new SafeCloner(); 
    
    private static int getSystemParam(String name, int defaultValue) {
        try {
            return Integer.parseInt(System.getProperty(name));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private final Logger log = LoggerFactory.getLogger(VariationInstantiationStrategyEnhancerInvocationHandler.class);

    private Map<Method, Method> methodsMap;
    private Map<Method, Method> variationsFromRules;
    private Object serviceClassInstance;

    public VariationInstantiationStrategyEnhancerInvocationHandler(Map<Method, Method> methodsMap,
            Object serviceClassInstance) throws OpenLCompilationException {
        this.methodsMap = methodsMap;
        this.serviceClassInstance = serviceClassInstance;
        initVariationFromRules(methodsMap, serviceClassInstance);
    }

    private void initVariationFromRules(Map<Method, Method> methodsMap, Object serviceClassInstance) throws OpenLCompilationException {
        variationsFromRules = new HashMap<Method, Method>();
        for (Method method : methodsMap.keySet()) {
            VariationsFromRules annotation = method.getAnnotation(VariationsFromRules.class);
            if (annotation != null) {
                String ruleName = annotation.ruleName();
                Class<?>[] parameterTypes = Arrays.copyOf(method.getParameterTypes(),
                    method.getParameterTypes().length - 1);
                Method variationsGetter = MethodUtils.getMatchingAccessibleMethod(serviceClassInstance.getClass(),
                    ruleName,
                    parameterTypes);
                if (variationsGetter != null) {
                    variationsFromRules.put(method, variationsGetter);
                } else {
                    throw new OpenLCompilationException("Failed to find variation from rules getter for method " + MethodUtil.printMethod(method.getName(),
                        method.getParameterTypes()) + ". Make sure you have method " + MethodUtil.printMethod(ruleName,
                        parameterTypes) + " in the service class.");
                }
            }
        }
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method member = methodsMap.get(method);
        if (member == null) {
            return method.invoke(serviceClassInstance, args);
        }
        if (VariationInstantiationStrategyEnhancerHelper.isDecoratedMethod(method)) {
            log.debug("Invoking service class method with variations: {} -> {}", method, member);
            return calculateWithVariations(method, args, member);
        } else {
            log.debug("Invoking service class method without variations: {} -> {}", method, member);
            return calculateWithoutVariations(args, member);
        }
    }

    /**
     * Simple invocation.
     */
    public Object calculateWithoutVariations(Object[] args, Method member) throws Exception {
        return member.invoke(serviceClassInstance, args);
    }

    /**
     * Calculate with variations.
     */
    public Object calculateWithVariations(Method method, Object[] args, Method member) throws Exception {
        VariationsPack variationsPack = getVariationsPack(method, args);
        VariationsResult<Object> variationsResults = new VariationsResult<Object>();

        Object[] arguments = Arrays.copyOf(args, args.length - 1);

        if (serviceClassInstance instanceof IEngineWrapper || serviceClassInstance instanceof OpenLWrapper) {
            IRuntimeEnv runtimeEnv = null;
            if (serviceClassInstance instanceof IEngineWrapper) {
                runtimeEnv = ((IEngineWrapper) serviceClassInstance).getRuntimeEnv();
            } else {
                runtimeEnv = (IRuntimeEnv) serviceClassInstance.getClass()
                    .getMethod(GET_RUNTIME_ENVIRONMENT_METHOD)
                    .invoke(serviceClassInstance);
            }

            if (runtimeEnv instanceof SimpleRulesRuntimeEnv) {
                SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = ((SimpleRulesRuntimeEnv) runtimeEnv);
                simpleRulesRuntimeEnv.changeMethodArgumentsCache(org.openl.rules.vm.CacheMode.READ_WRITE);
                simpleRulesRuntimeEnv.setMethodArgumentsCacheEnable(true);
                simpleRulesRuntimeEnv.resetOriginalCalculationSteps();
                simpleRulesRuntimeEnv.resetMethodArgumentsCache();
                simpleRulesRuntimeEnv.setOriginalCalculation(true);
                simpleRulesRuntimeEnv.setIgnoreRecalculate(false);
            } else {
                log.error("Runtime env must be SimpleRulesRuntimeEnv.class");
            }
            try {
                final Collection<VariationsResult<Object>> results = new ArrayList<VariationsResult<Object>>();
                results.add(calculateSingleVariation(member, arguments, new NoVariation()));
                if (variationsPack != null) {
                    final Collection<VariationCalculationTask> tasks = createTasks(member,
                        variationsPack,
                        arguments,
                        runtimeEnv);
                    if (!tasks.isEmpty()) {
                        final List<Future<VariationsResult<Object>>> futures = executorService.invokeAll(tasks);
                        for (Future<VariationsResult<Object>> item : futures) {
                            results.add(item.get());
                        }

                    }
                }
                mergeResults(variationsResults, results);
                return variationsResults;
            } finally {
                if (runtimeEnv instanceof SimpleRulesRuntimeEnv) {
                    SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = ((SimpleRulesRuntimeEnv) runtimeEnv);
                    simpleRulesRuntimeEnv.setIgnoreRecalculate(true);
                    simpleRulesRuntimeEnv.setOriginalCalculation(true);
                    simpleRulesRuntimeEnv.resetOriginalCalculationSteps();
                    simpleRulesRuntimeEnv.setMethodArgumentsCacheEnable(false);
                    simpleRulesRuntimeEnv.resetMethodArgumentsCache();
                }
            }
        } else {
            throw new OpenLRuntimeException("Service instance class must to implement IEngineWrapper or OpenLWrapper interface.");
        }
    }
    
    private void mergeResults(VariationsResult<Object> variationsResults, Collection<VariationsResult<Object>> results) {
        for (VariationsResult<Object> item : results) {
            for (Map.Entry<String, Object> entry : item.getVariationResults().entrySet()) {
                variationsResults.registerResult(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, String> entry : item.getVariationFailures().entrySet()) {
                variationsResults.registerFailure(entry.getKey(), entry.getValue());
            }
        }
    }

    private VariationsPack getVariationsPack(Method method, Object[] args) throws Exception {
        VariationsPack variationsPack = (VariationsPack) args[args.length - 1];
        Method variationsGetter = variationsFromRules.get(method);
        if (variationsGetter != null) {
            if (variationsPack == null) {
                variationsPack = new VariationsPack();
            }
            VariationDescription[] variationDescriptions = getVariationsFromRules(args, variationsGetter);
            for (VariationDescription description : variationDescriptions) {
                try {
                    variationsPack.addVariation(VariationsFactory.getVariation(description));
                } catch (Exception e) {
                    log.error("Failed to create variation defined in rules with id: {}",
                        description.getVariationID(),
                        e);
                }
            }
        }
        return variationsPack;
    }

    private VariationDescription[] getVariationsFromRules(Object[] args, Method variationsGetter) {
        try {
            Object[] argumentsForVariationsGetter = Arrays.copyOf(args, args.length - 1);
            return (VariationDescription[]) variationsGetter.invoke(serviceClassInstance, argumentsForVariationsGetter);
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to retrieve vairiations from rules", e);
        }
    }

    private VariationsResult<Object> calculateSingleVariation(Method member,
            Object[] arguments,
            Variation variation) {
        VariationsResult<Object> variationsResults = new VariationsResult<Object>();
        Object[] modifiedArguments = null;
        Object currentValue = null;
        try {
            try {
                currentValue = variation.currentValue(arguments);
                modifiedArguments = variation.applyModification(arguments);
                Object result = member.invoke(serviceClassInstance, modifiedArguments);
                variationsResults.registerResult(variation.getVariationID(), result);
            } catch (Exception e) {
                log.warn("Failed to calculate \"{}\"", variation.getVariationID(), e);
                Throwable e1 = e;
                if (e instanceof InvocationTargetException && e.getCause() != null) {
                    e1 = e.getCause();
                }
                variationsResults.registerFailure(variation.getVariationID(), e1.getMessage());
            }
        } finally {
            if (modifiedArguments != null) {
                try {
                    variation.revertModifications(modifiedArguments, currentValue);
                } catch (Exception e) {
                    log.error("Failed to revert modifications in variation \"{}\"", variation.getVariationID());
                }
            }
        }
        return variationsResults;
    }
    
    private Collection<VariationCalculationTask> createTasks(Method member,
            VariationsPack variationsPack,
            Object[] arguments,
            IRuntimeEnv parentRuntimeEnv) {
        final Collection<VariationCalculationTask> tasks = new ArrayList<VariationCalculationTask>(variationsPack.getVariations()
            .size());
        boolean f = false;
        for (Variation variation : variationsPack.getVariations()) {
            final IRuntimeEnv runtimeEnv = parentRuntimeEnv.cloneEnvForMT();

            if (parentRuntimeEnv instanceof SimpleRulesRuntimeEnv) {
                if (Proxy.isProxyClass(serviceClassInstance.getClass())){
                    final OpenLRulesInvocationHandler handler = (OpenLRulesInvocationHandler) Proxy.getInvocationHandler(serviceClassInstance);
                    handler.setRuntimeEnv(runtimeEnv);
                    SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = ((SimpleRulesRuntimeEnv) runtimeEnv);
                    simpleRulesRuntimeEnv.changeMethodArgumentsCache(org.openl.rules.vm.CacheMode.READ_ONLY);
                    simpleRulesRuntimeEnv.setOriginalCalculation(false);
                    simpleRulesRuntimeEnv.initCurrentStep();
                }else{
                    if (!f){
                        log.warn("Variation features aren't supported for Wrapper classses. This functionality was depricated!");
                        f = true;
                    }
                }
            }

            final VariationCalculationTask item = new VariationCalculationTask(
                member,
                cloner.deepClone(arguments),
                variation,
                runtimeEnv);
            tasks.add(item);

        }
        return tasks;
    }

    private class VariationCalculationTask implements Callable<VariationsResult<Object>> {
        private final Method member;
        private final Object[] arguments;
        private final Variation variation;
        private final IRuntimeEnv runtimeEnv;
        
        private VariationCalculationTask(
                Method member,
                Object[] arguments,
                Variation variation,
                IRuntimeEnv runtimeEnv) {
            this.member = member;
            this.arguments = arguments;
            this.variation = variation;
            this.runtimeEnv = runtimeEnv;
        }

        @Override
        public VariationsResult<Object> call() throws Exception {
            OpenLRulesInvocationHandler handler = null;
            try {
                if (runtimeEnv instanceof SimpleRulesRuntimeEnv) {
                    if (Proxy.isProxyClass(serviceClassInstance.getClass())){
                        handler = (OpenLRulesInvocationHandler) Proxy.getInvocationHandler(serviceClassInstance);
                        handler.setRuntimeEnv(runtimeEnv);
                    }
                    SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = ((SimpleRulesRuntimeEnv) runtimeEnv);
                    simpleRulesRuntimeEnv.changeMethodArgumentsCache(org.openl.rules.vm.CacheMode.READ_ONLY);
                    simpleRulesRuntimeEnv.setOriginalCalculation(false);
                    simpleRulesRuntimeEnv.initCurrentStep();
                }

                return calculateSingleVariation(member, arguments, variation);
            } catch (Exception e) {
                log.error("Failed to calculate variation!", e);
                throw e;
            } finally {
                if (handler != null)
                    handler.release();
            }
        }
    }
}
