package openl;

import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.conf.UserContext;
import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.impl.OpenClassJavaWrapper;

public class Hello {
    static org.openl.vm.IRuntimeEnv __env;

    static org.openl.types.IOpenClass __class;

    static String __openlName = "org.openl.j";

    static String __src = "openl/Hello.j";

    static String __userHome = ".";

    static org.openl.types.IOpenMethod main_Method;

    static boolean __initialized = false;

    Object __instance;

    static synchronized protected void __init() {
        if (__initialized) {
            return;
        }

        UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(), __userHome);
        OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper(__openlName, ucxt, __src);
        __class = wrapper.getOpenClass();
        __env = wrapper.getEnv();

        main_Method = __class.getMatchingMethod("main", new IOpenClass[] { JavaOpenClass
                .getOpenClass(java.lang.String[].class) });

        __initialized = true;
    }

    public static void main(java.lang.String[] args) {
        Object[] __params = new Object[1];
        __params[0] = args;
        try {
            Object __myInstance = new Hello().__instance;
            main_Method.invoke(__myInstance, __params, __env);
        } catch (Throwable t) {
            Log.error("Java Wrapper execution error:", t);
            throw RuntimeExceptionWrapper.wrap(t);
        }

    }

    public Hello() {
        __init();
        __instance = __class.newInstance(__env);
    }
}