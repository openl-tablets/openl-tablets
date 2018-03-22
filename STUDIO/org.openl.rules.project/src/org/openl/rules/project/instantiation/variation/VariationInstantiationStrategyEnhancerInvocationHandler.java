package org.openl.rules.project.instantiation.variation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RecursiveTask;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.openl.binding.MethodUtil;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.core.ce.ServiceMT;
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
import org.openl.runtime.IOpenLInvocationHandler;
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
class VariationInstantiationStrategyEnhancerInvocationHandler implements IOpenLInvocationHandler {

    private static final String GET_RUNTIME_ENVIRONMENT_METHOD = "getRuntimeEnvironment";

    private SafeCloner cloner = new SafeCloner();

    private final Logger log = LoggerFactory.getLogger(VariationInstantiationStrategyEnhancerInvocationHandler.class);

    private Map<Method, Method> methodsMap;
    @Deprecated
    private Map<Method, Method> variationsFromRules;
    private Object serviceClassInstance;

    public VariationInstantiationStrategyEnhancerInvocationHandler(Map<Method, Method> methodsMap,
            Object serviceClassInstance) throws OpenLCompilationException {
        this.methodsMap = methodsMap;
        this.serviceClassInstance = serviceClassInstance;
        initVariationFromRules(methodsMap, serviceClassInstance);
    }

    @Deprecated
    private void initVariationFromRules(Map<Method, Method> methodsMap,
            Object serviceClassInstance) throws OpenLCompilationException {
        variationsFromRules = new HashMap<Method, Method>();
        for (Method method : methodsMap.keySet()) {
            VariationsFromRules annotation = method.getAnnotation(VariationsFromRules.class);
            if (annotation != null) {
                String ruleName = annotation.ruleName();
                Class<?>[] parameterTypes = Arrays.copyOf(method.getParameterTypes(),
                    method.getParameterTypes().length - 1);
                Method variationsGetter = MethodUtils
                    .getMatchingAccessibleMethod(serviceClassInstance.getClass(), ruleName, parameterTypes);
                if (variationsGetter != null) {
                    variationsFromRules.put(method, variationsGetter);
                } else {
                    throw new OpenLCompilationException(
                        "Failed to find variation from rules getter for method " + MethodUtil.printMethod(
                            method.getName(),
                            method.getParameterTypes()) + ". Make sure you have method " + MethodUtil
                                .printMethod(ruleName, parameterTypes) + " in the service class.");
                }
            }
        }
    }
    
    @Override
    public Object getTarget() {
        return serviceClassInstance;
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

        if (serviceClassInstance instanceof IEngineWrapper) {
            SimpleRulesRuntimeEnv runtimeEnv = null;
            if (serviceClassInstance instanceof IEngineWrapper) {
                runtimeEnv = (SimpleRulesRuntimeEnv) ((IEngineWrapper) serviceClassInstance).getRuntimeEnv();
            } else {
                runtimeEnv = (SimpleRulesRuntimeEnv) serviceClassInstance.getClass()
                    .getMethod(GET_RUNTIME_ENVIRONMENT_METHOD)
                    .invoke(serviceClassInstance);
            }

            runtimeEnv.changeMethodArgumentsCacheMode(org.openl.rules.vm.CacheMode.READ_WRITE);
            runtimeEnv.setMethodArgumentsCacheEnable(true);
            runtimeEnv.getArgumentCachingStorage().resetOriginalCalculationSteps();
            runtimeEnv.getArgumentCachingStorage().resetMethodArgumentsCache();
            runtimeEnv.setOriginalCalculation(true);
            runtimeEnv.setIgnoreRecalculate(false);

            try {
                final Collection<VariationsResult<Object>> results = new ArrayList<VariationsResult<Object>>();
                results.add(calculateSingleVariation(member, arguments, new NoVariation()));
                if (variationsPack != null) {
                    final VariationCalculationTask[] tasks = createTasks(member,
                        variationsPack,
                        arguments,
                        runtimeEnv);
                    if (tasks.length > 0) {
                        ServiceMT.getInstance().executeAll(tasks);
                        for (VariationCalculationTask task : tasks) {
                            results.add(task.join());
                        }
                    }
                }
                mergeResults(variationsResults, results);
                return variationsResults;
            } finally {
                runtimeEnv.setIgnoreRecalculate(true);
                runtimeEnv.setOriginalCalculation(true);
                runtimeEnv.getArgumentCachingStorage().resetOriginalCalculationSteps();
                runtimeEnv.setMethodArgumentsCacheEnable(false);
                runtimeEnv.setIgnoreRecalculate(true);
                runtimeEnv.getArgumentCachingStorage().resetMethodArgumentsCache();
            }
        } else {
            throw new OpenlNotCheckedException(
                "Service instance class must to implement IEngineWrapper or OpenLWrapper interface.");
        }
    }

    private void mergeResults(VariationsResult<Object> variationsResults,
            Collection<VariationsResult<Object>> results) {
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

    @Deprecated
    private VariationDescription[] getVariationsFromRules(Object[] args, Method variationsGetter) {
        try {
            Object[] argumentsForVariationsGetter = Arrays.copyOf(args, args.length - 1);
            return (VariationDescription[]) variationsGetter.invoke(serviceClassInstance, argumentsForVariationsGetter);
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to retrieve vairiations from rules", e);
        }
    }

    private VariationsResult<Object> calculateSingleVariation(Method member, Object[] arguments, Variation variation) {
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

    private VariationCalculationTask[] createTasks(Method member,
            VariationsPack variationsPack,
            Object[] arguments,
            SimpleRulesRuntimeEnv parentRuntimeEnv) {
        final Collection<VariationCalculationTask> tasks = new ArrayList<VariationCalculationTask>(
            variationsPack.getVariations().size());
        boolean f = false;
        for (Variation variation : variationsPack.getVariations()) {
            final IRuntimeEnv runtimeEnv = parentRuntimeEnv.clone();

            if (parentRuntimeEnv instanceof SimpleRulesRuntimeEnv) {
                if (Proxy.isProxyClass(serviceClassInstance.getClass())) {
                    final OpenLRulesInvocationHandler handler = (OpenLRulesInvocationHandler) Proxy
                        .getInvocationHandler(serviceClassInstance);
                    handler.setRuntimeEnv(runtimeEnv);
                    SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = ((SimpleRulesRuntimeEnv) runtimeEnv);
                    simpleRulesRuntimeEnv.changeMethodArgumentsCacheMode(org.openl.rules.vm.CacheMode.READ_ONLY);
                    simpleRulesRuntimeEnv.setOriginalCalculation(false);
                    simpleRulesRuntimeEnv.setIgnoreRecalculate(true);
                    simpleRulesRuntimeEnv.getArgumentCachingStorage().initCurrentStep();
                } else {
                    if (!f) {
                        log.warn(
                            "Variation features aren't supported for Wrapper classses. This functionality was depricated!");
                        f = true;
                    }
                }
            }

            final VariationCalculationTask item = new VariationCalculationTask(member,
                cloner.deepClone(arguments),
                variation,
                runtimeEnv);
            tasks.add(item);

        }
        return tasks.toArray(new VariationCalculationTask[] {});
    }

    private class VariationCalculationTask extends RecursiveTask<VariationsResult<Object>> {
        private static final long serialVersionUID = 1L;
        private final Method member;
        private final Object[] arguments;
        private final Variation variation;
        private final IRuntimeEnv runtimeEnv;

        private VariationCalculationTask(Method member,
                Object[] arguments,
                Variation variation,
                IRuntimeEnv runtimeEnv) {
            this.member = member;
            this.arguments = arguments;
            this.variation = variation;
            this.runtimeEnv = runtimeEnv;
        }
        
        @Override
        protected VariationsResult<Object> compute() {
            OpenLRulesInvocationHandler handler = null;
            try {
                if (runtimeEnv instanceof SimpleRulesRuntimeEnv) {
                    if (Proxy.isProxyClass(serviceClassInstance.getClass())) {
                        handler = (OpenLRulesInvocationHandler) Proxy.getInvocationHandler(serviceClassInstance);
                        handler.setRuntimeEnv(runtimeEnv);
                    }
                    SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = ((SimpleRulesRuntimeEnv) runtimeEnv);
                    simpleRulesRuntimeEnv.changeMethodArgumentsCacheMode(org.openl.rules.vm.CacheMode.READ_ONLY);
                    simpleRulesRuntimeEnv.setOriginalCalculation(false);
                    simpleRulesRuntimeEnv.setIgnoreRecalculate(true);
                    simpleRulesRuntimeEnv.getArgumentCachingStorage().initCurrentStep();
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
