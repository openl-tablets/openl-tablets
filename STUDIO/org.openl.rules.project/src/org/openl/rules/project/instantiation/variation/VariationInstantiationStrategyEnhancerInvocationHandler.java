package org.openl.rules.project.instantiation.variation;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.openl.binding.MethodUtil;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenLRuntimeException;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.main.OpenLWrapper;
import org.openl.rules.variation.*;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.runtime.IEngineWrapper;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * InvocationHandler for proxy that injects variations into service class.
 * <p/>
 * Handles both original methods and enhanced with variations.
 *
 * @author PUdalau, Marat Kamalov
 */
class VariationInstantiationStrategyEnhancerInvocationHandler implements InvocationHandler {

    private static final String GET_RUNTIME_ENVIRONMENT_METHOD = "getRuntimeEnvironment";

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

    private void initVariationFromRules(Map<Method, Method> methodsMap, Object serviceClassInstance)
            throws OpenLCompilationException {
        variationsFromRules = new HashMap<Method, Method>();
        for (Method method : methodsMap.keySet()) {
            VariationsFromRules annotation = method.getAnnotation(VariationsFromRules.class);
            if (annotation != null) {
                String ruleName = annotation.ruleName();
                Class<?>[] parameterTypes = Arrays.copyOf(method.getParameterTypes(),
                        method.getParameterTypes().length - 1);
                Method variationsGetter = MethodUtils.getMatchingAccessibleMethod(serviceClassInstance.getClass(),
                        ruleName, parameterTypes);
                if (variationsGetter != null) {
                    variationsFromRules.put(method, variationsGetter);
                } else {
                    throw new OpenLCompilationException("Can't find variation from rules getter for method "
                            + MethodUtil.printMethod(method.getName(), method.getParameterTypes())
                            + ". Make sure you have method " + MethodUtil.printMethod(ruleName, parameterTypes)
                            + " in service class.");
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
    @SuppressWarnings("rawtypes")
    public Object calculateWithVariations(Method method, Object[] args, Method member) throws Exception {
        VariationsPack variationsPack = getVariationsPack(method, args);
        VariationsResult variationsResults = new VariationsResult();

        Object[] arguments = Arrays.copyOf(args, args.length - 1);

        if (serviceClassInstance instanceof IEngineWrapper || serviceClassInstance instanceof OpenLWrapper) {
            IRuntimeEnv runtimeEnv = null;
            if (serviceClassInstance instanceof IEngineWrapper) {
                runtimeEnv = ((IEngineWrapper) serviceClassInstance).getRuntimeEnv();
            } else {
                runtimeEnv = (IRuntimeEnv) serviceClassInstance.getClass().getMethod(GET_RUNTIME_ENVIRONMENT_METHOD)
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
                log.error("Runtime env should be SimpleRulesRuntimeEnv.class");
            }
            calculateSingleVariation(member, variationsResults, arguments, new NoVariation());
            try {
                if (runtimeEnv instanceof SimpleRulesRuntimeEnv) {
                    SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = ((SimpleRulesRuntimeEnv) runtimeEnv);
                    simpleRulesRuntimeEnv.changeMethodArgumentsCache(org.openl.rules.vm.CacheMode.READ_ONLY);
                    simpleRulesRuntimeEnv.setOriginalCalculation(false);
                }
                if (variationsPack != null) {
                    for (Variation variation : variationsPack.getVariations()) {
                        if (runtimeEnv instanceof SimpleRulesRuntimeEnv) {
                            ((SimpleRulesRuntimeEnv) runtimeEnv).initCurrentStep();
                        }
                        calculateSingleVariation(member, variationsResults, arguments, variation);
                    }
                }
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
            log.error("Service instance class should be implement IEngineWrapper or OpenLWrapper interface");
            throw new OpenLRuntimeException("Service instance class should be implement IEngineWrapper or OpenLWrapper interface");
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
                    log.error("Failed to create variation defined in rules with id: {}", description.getVariationID(), e);
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void calculateSingleVariation(Method member, VariationsResult variationsResults, Object[] arguments,
                                          Variation variation) {
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
    }
}
