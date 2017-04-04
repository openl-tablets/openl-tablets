/*
 * This class has been generated. Do not change it, if you need to modify functionality - subclass it
 */

package org.openl.tablets;

import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.types.IOpenClass;
import org.openl.conf.IUserContext;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;

public class Algorithm1Wrapper implements org.openl.main.OpenLWrapper {
    public static org.openl.types.IOpenClass __class;

    public static org.openl.CompiledOpenClass __compiledClass;

    public static String __openlName = "org.openl.xls";

    public static String __src = "test/rules/Algorithm.xls";

    public static String __srcModuleClass = null;

    public static String __folder = "rules";

    public static String __project = "algorithm1";

    public static String __userHome = ".";

    static org.openl.types.IOpenField this_Field;

    static org.openl.types.IOpenMethod modification_Method;

    static boolean __initialized = false;

    Object __instance;

    private ThreadLocal<org.openl.vm.IRuntimeEnv> __env = new ThreadLocal<org.openl.vm.IRuntimeEnv>() {
        @Override
        protected org.openl.vm.IRuntimeEnv initialValue() {
            set(new org.openl.vm.SimpleVM().getRuntimeEnv());
            return get();
        }
    };

    static synchronized protected void __init() {
        if (__initialized) {
            return;
        }

        IUserContext ucxt = UserContext.getCurrentContextOrCreateNew(Thread.currentThread().getContextClassLoader(), __userHome);
        __compiledClass = OpenClassJavaWrapper.createWrapper(__openlName, ucxt, __src, null);
        __class = __compiledClass.getOpenClassWithErrors();

        this_Field = __class.getField("this");
        modification_Method = __class.getMatchingMethod("modification", new IOpenClass[] {});

        __initialized = true;
    }

    static public void reset() {
        __initialized = false;
    }

    public Algorithm1Wrapper() {
        this(false);
    }

    public Algorithm1Wrapper(boolean ignoreErrors) {
        __init();
        if (!ignoreErrors) {
            __compiledClass.throwErrorExceptionsIfAny();
        }
        __instance = __class.newInstance(__env.get());
    }

    public org.openl.CompiledOpenClass getCompiledOpenClass() {
        return __compiledClass;
    }

    public Object getInstance() {
        return __instance;
    }

    public IOpenClass getOpenClass() {
        return __class;
    }

    public org.openl.vm.IRuntimeEnv getRuntimeEnvironment() {
        return __env.get();
    }

    public org.openl.types.impl.DynamicObject getThis() {
        Object __res = this_Field.get(__instance, __env.get());
        return (org.openl.types.impl.DynamicObject) __res;
    }

    public void modification() {
        Object[] __params = new Object[0];
        try {
            Object __myInstance = __instance;
            modification_Method.invoke(__myInstance, __params, __env.get());
        } catch (Throwable t) {
            Log.error("Java Wrapper execution error:", t);
            throw RuntimeExceptionWrapper.wrap(t);
        }

    }

    public synchronized void reload() {
        reset();
        __init();
        __instance = __class.newInstance(__env.get());
    }

    public void setRuntimeEnvironment(org.openl.vm.IRuntimeEnv __env) {
        this.__env.set(__env);
    }

    public void setThis(org.openl.types.impl.DynamicObject __var) {
        this_Field.set(__instance, __var, __env.get());
    }
}