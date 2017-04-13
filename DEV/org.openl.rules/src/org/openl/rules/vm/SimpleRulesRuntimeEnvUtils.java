package org.openl.rules.vm;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.main.OpenLWrapper;
import org.openl.runtime.IEngineWrapper;
import org.openl.vm.IRuntimeEnv;

public final class SimpleRulesRuntimeEnvUtils {
    private SimpleRulesRuntimeEnvUtils() {
    }

    public static IRuntimeEnv getRuntimeEnv(Object instance) {
        if (instance instanceof IEngineWrapper || instance instanceof OpenLWrapper) {
            IRuntimeEnv runtimeEnv;
            if (instance instanceof IEngineWrapper) {
                runtimeEnv = ((IEngineWrapper) instance).getRuntimeEnv();
            } else {
                try {
                    runtimeEnv = (IRuntimeEnv) instance.getClass().getMethod("getRuntimeEnvironment").invoke(instance);
                } catch (Exception e) {
                    throw new OpenlNotCheckedException(e);
                }
            }
            return runtimeEnv;
        }
        throw new OpenlNotCheckedException("Instance must implement " + IEngineWrapper.class
            .getCanonicalName() + " or " + OpenLWrapper.class.getCanonicalName() + "!");
    }

    public static boolean isMethodArgumentsCacheEnable(Object instance) {
        IRuntimeEnv runtimeEnv = getRuntimeEnv(instance);
        return isMethodArgumentsCacheEnableEnv(runtimeEnv);
    }

    public static CacheMode getMethodArgumentsCacheMode(Object instance) {
        IRuntimeEnv runtimeEnv = getRuntimeEnv(instance);
        return getMethodArgumentsCacheModeEnv(runtimeEnv);
    }

    public static boolean isCacheSupports(Object instance) {
        IRuntimeEnv runtimeEnv = getRuntimeEnv(instance);
        return isCacheSupportsEnv(runtimeEnv);
    }

    public static void setMethodArgumentsCacheMode(Object instance, CacheMode cacheMode) {
        IRuntimeEnv runtimeEnv = getRuntimeEnv(instance);
        changeMethodArgumentsCacheEnv(runtimeEnv, cacheMode);
    }

    public static void setMethodArgumentsCacheEnable(Object instance, boolean enable) {
        IRuntimeEnv runtimeEnv = getRuntimeEnv(instance);
        setMethodArgumentsCacheEnableEnv(runtimeEnv, enable);
    }

    private static boolean isCacheSupportsEnv(IRuntimeEnv env) {
        return env instanceof SimpleRulesRuntimeEnv;
    }

    private static void setMethodArgumentsCacheEnableEnv(IRuntimeEnv env, boolean enable) {
        if (isCacheSupportsEnv(env)) {
            ((SimpleRulesRuntimeEnv) env).setMethodArgumentsCacheEnable(enable);
            return;
        }
        throw new OpenlNotCheckedException("Runtime env doesn't support cache.");
    }

    private static boolean isMethodArgumentsCacheEnableEnv(IRuntimeEnv env) {
        if (isCacheSupportsEnv(env)) {
            return ((SimpleRulesRuntimeEnv) env).isMethodArgumentsCacheEnable();
        }
        throw new OpenlNotCheckedException("Runtime env doesn't support cache.");
    }

    private static CacheMode getMethodArgumentsCacheModeEnv(IRuntimeEnv env) {
        if (isCacheSupportsEnv(env)) {
            return ((SimpleRulesRuntimeEnv) env).getCacheMode();
        }
        throw new OpenlNotCheckedException("Runtime env doesn't support cache.");
    }

    private static void changeMethodArgumentsCacheEnv(IRuntimeEnv env, CacheMode cacheMode) {
        if (isCacheSupportsEnv(env)) {
            ((SimpleRulesRuntimeEnv) env).changeMethodArgumentsCache(cacheMode);
            return;
        }
        throw new OpenlNotCheckedException("Runtime env doesn't support cache.");
    }
}
