package org.openl.rules.project.instantiation.variation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.reflect.MethodUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.binding.MethodUtil;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenLRuntimeException;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.main.OpenLWrapper;
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

/**
 * InvocationHandler for proxy that injects variations into service class.
 * 
 * Handles both original methods and enhanced with variations.
 * 
 * @author PUdalau, Marat Kamalov
 */
class VariationsInvocationHandler implements InvocationHandler {

    private final Log log = LogFactory.getLog(VariationsInvocationHandler.class);

    private Map<Method, Method> methodsMap;
    private Map<Method, Method> variationsFromRules;
    private Object serviceClassInstance;

    public VariationsInvocationHandler(Map<Method, Method> methodsMap, Object serviceClassInstance)
            throws OpenLCompilationException {
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
                    throw new OpenLCompilationException("Can not find variation from rules getter for method "
                            + MethodUtil.printMethod(method.getName(), method.getParameterTypes())
                            + ". Make sure you have method " + MethodUtil.printMethod(ruleName, parameterTypes)
                            + " in service class.");
                }
            }
        }
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method member = methodsMap.get(method);
        if (VariationsEnhancerHelper.isEnhancedMethod(method)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Invoking service class method with variations: %s -> %s", method.toString(),
                        member.toString()));
            }
            return calculateWithVariations(method, args, member);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Invoking service class method without variations: %s -> %s",
                        method.toString(), member.toString()));
            }
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
                runtimeEnv = (IRuntimeEnv) serviceClassInstance.getClass().getMethod("getRuntimeEnvironment")
                        .invoke(serviceClassInstance);
            }
            
            if (runtimeEnv instanceof SimpleRulesRuntimeEnv) {
                ((SimpleRulesRuntimeEnv) runtimeEnv)
                        .changeMethodArgumentsCache(org.openl.rules.vm.CacheMode.READ_WRITE);
                ((SimpleRulesRuntimeEnv) runtimeEnv).setMethodArgumentsCacheEnable(true);
                ((SimpleRulesRuntimeEnv) runtimeEnv).resetOriginalCalculationSteps();
                ((SimpleRulesRuntimeEnv) runtimeEnv).setOriginalCalculation(true);
                ((SimpleRulesRuntimeEnv) runtimeEnv).setIgnoreRecalculation(false);
            }
            calculateSingleVariation(member, variationsResults, arguments, new NoVariation());
            if (runtimeEnv instanceof SimpleRulesRuntimeEnv) {
                ((SimpleRulesRuntimeEnv) runtimeEnv).changeMethodArgumentsCache(org.openl.rules.vm.CacheMode.READ_ONLY);
                ((SimpleRulesRuntimeEnv) runtimeEnv).setOriginalCalculation(false);
            }
            if (variationsPack != null) {
                for (Variation variation : variationsPack.getVariations()) {
                    if (runtimeEnv instanceof SimpleRulesRuntimeEnv) {
                        ((SimpleRulesRuntimeEnv) runtimeEnv).initCurrentStep();
                    }
                    calculateSingleVariation(member, variationsResults, arguments, variation);
                }
            }
            if (runtimeEnv instanceof SimpleRulesRuntimeEnv) {
                ((SimpleRulesRuntimeEnv) runtimeEnv).setIgnoreRecalculation(true);
                ((SimpleRulesRuntimeEnv) runtimeEnv).setOriginalCalculation(true);
                ((SimpleRulesRuntimeEnv) runtimeEnv).resetOriginalCalculationSteps();
                ((SimpleRulesRuntimeEnv) runtimeEnv).setMethodArgumentsCacheEnable(false);
                ((SimpleRulesRuntimeEnv) runtimeEnv).resetMethodArgumentsCache();
            }
            return variationsResults;
        } else {
            if (log.isErrorEnabled()) {
                log.error("Service instance class should be implement IEngineWrapper or OpenLWrapper interface");
            }
            throw new OpenLRuntimeException(
                    "Service instance class should be implement IEngineWrapper or OpenLWrapper interface");
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
                    if (log.isErrorEnabled()) {
                        log.error(
                                "Failed to create variation defined in rules with id: " + description.getVariationID(),
                                e);
                    }
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
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
                if (log.isWarnEnabled()) {
                    log.warn("Failed to calculate \"" + variation.getVariationID() + "\"", e);
                }
                variationsResults.registerFailure(variation.getVariationID(), e.getMessage());
            }
        } finally {
            if (modifiedArguments != null) {
                try {
                    variation.revertModifications(modifiedArguments, currentValue);
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Failed to revert modifications in variation \"" + variation.getVariationID() + "\"");
                    }
                }
            }
        }
    }
}
