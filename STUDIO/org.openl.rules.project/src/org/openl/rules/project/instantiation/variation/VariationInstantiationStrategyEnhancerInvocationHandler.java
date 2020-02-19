package org.openl.rules.project.instantiation.variation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.RecursiveTask;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.core.ce.ServiceMT;
import org.openl.rules.project.SafeCloner;
import org.openl.rules.runtime.OpenLRulesMethodHandler;
import org.openl.rules.variation.NoVariation;
import org.openl.rules.variation.Variation;
import org.openl.rules.variation.VariationsPack;
import org.openl.rules.variation.VariationsResult;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.runtime.IEngineWrapper;
import org.openl.runtime.IOpenLMethodHandler;
import org.openl.runtime.OpenLASMProxy;
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
class VariationInstantiationStrategyEnhancerInvocationHandler implements IOpenLMethodHandler<Method, Method> {

    private final SafeCloner cloner = new SafeCloner();

    private final Logger log = LoggerFactory.getLogger(VariationInstantiationStrategyEnhancerInvocationHandler.class);

    private final Map<Method, Method> methodsMap;
    private final Object serviceClassInstance;

    VariationInstantiationStrategyEnhancerInvocationHandler(Map<Method, Method> methodsMap,
            Object serviceClassInstance) {
        this.methodsMap = Objects.requireNonNull(methodsMap, "methodMap can not be null");
        this.serviceClassInstance = Objects.requireNonNull(serviceClassInstance,
            "serviceClassInstance can not be null");
    }

    @Override
    public Object getTarget() {
        return serviceClassInstance;
    }

    @Override
    public Method getTargetMember(Method key) {
        return methodsMap.get(key);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method member = methodsMap.get(method);
        if (member == null) {
            return method.invoke(serviceClassInstance, args);
        }
        if (VariationInstantiationStrategyEnhancerHelper.isDecoratedMethod(method)) {
            log.debug("Invoking service class method with variations: {} -> {}", method, member);
            return calculateWithVariations(args, member);
        } else {
            log.debug("Invoking service class method without variations: {} -> {}", method, member);
            return member.invoke(serviceClassInstance, args);
        }
    }

    /**
     * Calculate with variations.
     */
    private Object calculateWithVariations(Object[] args, Method member) {
        VariationsPack variationsPack = (VariationsPack) args[args.length - 1];

        Object[] arguments = Arrays.copyOf(args, args.length - 1);

        if (serviceClassInstance instanceof IEngineWrapper) {
            SimpleRulesRuntimeEnv runtimeEnv;
            runtimeEnv = (SimpleRulesRuntimeEnv) ((IEngineWrapper) serviceClassInstance).getRuntimeEnv();

            runtimeEnv.changeMethodArgumentsCacheMode(org.openl.rules.vm.CacheMode.READ_WRITE);
            runtimeEnv.setMethodArgumentsCacheEnable(true);
            runtimeEnv.getArgumentCachingStorage().resetOriginalCalculationSteps();
            runtimeEnv.getArgumentCachingStorage().resetMethodArgumentsCache();
            runtimeEnv.setOriginalCalculation(true);
            runtimeEnv.setIgnoreRecalculate(false);

            try {
                VariationsResult<Object> variationsResults = new VariationsResult<>();
                VariationsResult<Object> singleVariation = calculateSingleVariation(member,
                    arguments,
                    new NoVariation());
                merge(variationsResults, singleVariation);
                if (variationsPack != null) {
                    final VariationCalculationTask[] tasks = createTasks(member, variationsPack, arguments, runtimeEnv);
                    if (tasks.length > 0) {
                        ServiceMT.getInstance().executeAll(tasks);
                        for (VariationCalculationTask task : tasks) {
                            VariationsResult<Object> joinedVariation = task.join();
                            merge(variationsResults, joinedVariation);
                        }
                    }
                }
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

    private void merge(VariationsResult<Object> results, VariationsResult<Object> item) {
        for (Map.Entry<String, Object> entry : item.getVariationResults().entrySet()) {
            results.registerResult(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : item.getVariationFailures().entrySet()) {
            results.registerFailure(entry.getKey(), entry.getValue());
        }
    }

    private VariationsResult<Object> calculateSingleVariation(Method member, Object[] arguments, Variation variation) {
        VariationsResult<Object> variationsResults = new VariationsResult<>();
        Object[] modifiedArguments = null;
        Object currentValue = null;
        try {
            try {
                currentValue = variation.currentValue(arguments);
                modifiedArguments = variation.applyModification(arguments);
                Object result = member.invoke(serviceClassInstance, modifiedArguments);
                variationsResults.registerResult(variation.getVariationID(), result);
            } catch (Exception e) {
                log.warn("Failed to calculate '{}'", variation.getVariationID(), e);
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
                    log.error("Failed to revert modifications in variation '{}'", variation.getVariationID());
                }
            }
        }
        return variationsResults;
    }

    private VariationCalculationTask[] createTasks(Method member,
            VariationsPack variationsPack,
            Object[] arguments,
            SimpleRulesRuntimeEnv parentRuntimeEnv) {
        final Collection<VariationCalculationTask> tasks = new ArrayList<>(variationsPack.getVariations().size());
        boolean f = false;
        for (Variation variation : variationsPack.getVariations()) {
            final IRuntimeEnv runtimeEnv = parentRuntimeEnv.clone();

            if (OpenLASMProxy.isProxy(serviceClassInstance)) {
                final OpenLRulesMethodHandler handler = (OpenLRulesMethodHandler) OpenLASMProxy
                    .getHandler(serviceClassInstance);
                handler.setRuntimeEnv(runtimeEnv);
                SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = (SimpleRulesRuntimeEnv) runtimeEnv;
                simpleRulesRuntimeEnv.changeMethodArgumentsCacheMode(org.openl.rules.vm.CacheMode.READ_ONLY);
                simpleRulesRuntimeEnv.setOriginalCalculation(false);
                simpleRulesRuntimeEnv.setIgnoreRecalculate(true);
                simpleRulesRuntimeEnv.getArgumentCachingStorage().initCurrentStep();
            } else {
                if (!f) {
                    log.warn(
                        "Variation features are not supported for Wrapper classes. This functionality is deprecated.");
                    f = true;
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
            OpenLRulesMethodHandler handler = null;
            try {
                if (runtimeEnv instanceof SimpleRulesRuntimeEnv) {
                    if (OpenLASMProxy.isProxy(serviceClassInstance)) {
                        handler = (OpenLRulesMethodHandler) OpenLASMProxy.getHandler(serviceClassInstance);
                        handler.setRuntimeEnv(runtimeEnv);
                    }
                    SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = (SimpleRulesRuntimeEnv) runtimeEnv;
                    simpleRulesRuntimeEnv.changeMethodArgumentsCacheMode(org.openl.rules.vm.CacheMode.READ_ONLY);
                    simpleRulesRuntimeEnv.setOriginalCalculation(false);
                    simpleRulesRuntimeEnv.setIgnoreRecalculate(true);
                    simpleRulesRuntimeEnv.getArgumentCachingStorage().initCurrentStep();
                }

                return calculateSingleVariation(member, arguments, variation);
            } catch (Exception e) {
                log.error("Failed to calculate a variation.", e);
                throw e;
            } finally {
                if (handler != null) {
                    handler.release();
                }
            }
        }
    }
}
