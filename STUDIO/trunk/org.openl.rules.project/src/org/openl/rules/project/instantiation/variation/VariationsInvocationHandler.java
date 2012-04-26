package org.openl.rules.project.instantiation.variation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * InvocationHandler for proxy that injects variations into service class.
 * 
 * Handles both original methods and enhanced with variations.
 * 
 * @author PUdalau
 */
class VariationsInvocationHandler implements InvocationHandler {

    private final Log log = LogFactory.getLog(VariationsInvocationHandler.class);

    private Map<Method, Method> methodsMap;
    private Object serviceClassInstance;

    public VariationsInvocationHandler(Map<Method, Method> methodsMap, Object serviceClassInstance) {
        this.methodsMap = methodsMap;
        this.serviceClassInstance = serviceClassInstance;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Method member = methodsMap.get(method);
        if (VariationsEnhancerHelper.isEnhancedMethod(method)) {
            log.debug(String.format("Invoking service class method with variations: %s -> %s",
                method.toString(),
                member.toString()));
            return calculateWithVariations(method, args, member);
        } else {
            log.debug(String.format("Invoking service class method without variations: %s -> %s",
                method.toString(),
                member.toString()));
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
    public Object calculateWithVariations(Method method, Object[] args, Method member) {
        VariationsPack variationsPack = (VariationsPack) args[args.length - 1];
        VariationsResult variationsResults = new VariationsResult();

        Object[] arguments = Arrays.copyOf(args, args.length - 1);

        // invoke without variations
        calculateSingleVariation(member, variationsResults, arguments, new NoVariation());

        if (variationsPack != null) {
            for (Variation variation : variationsPack.getVariations()) {
                calculateSingleVariation(member, variationsResults, arguments, variation);
            }
        }

        return variationsResults;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void calculateSingleVariation(Method member,
            VariationsResult variationsResults,
            Object[] arguments,
            Variation variation) {
        Stack<Object> stack = new Stack<Object>();
        Object[] modifiedArguments = null;
        try {
            try {
                modifiedArguments = variation.applyModification(arguments, stack);
                Object result = member.invoke(serviceClassInstance, modifiedArguments);
                variationsResults.registerResults(variation.getVariationID(), result);
            } catch (Exception e) {
				log.debug("Failed to calculate \"" + variation.getVariationID()
						+ "\"", e);
                variationsResults.registerFailure(variation.getVariationID(), e);
            }
        } finally {
            if (modifiedArguments != null) {
                try {
                    variation.revertModifications(modifiedArguments, stack);
                } catch (Exception e) {
                    log.error("Failed to revert modifications in variation \"" + variation.getVariationID() + "\"");
                }
            }
        }
    }
}
