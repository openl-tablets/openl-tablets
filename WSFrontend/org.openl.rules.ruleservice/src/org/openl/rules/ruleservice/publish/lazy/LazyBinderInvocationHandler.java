package org.openl.rules.ruleservice.publish.lazy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.openl.IOpenBinder;
import org.openl.conf.IUserContext;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
import org.openl.rules.lang.xls.prebind.XlsPreBinder;

/**
 * IOpenBinder invocation handler that uses XlsPreBinder on prebind step and XlsBinder on compile step (to compile
 * necessary modules on demand). On prebind step use {@link #setPrebindHandler(IPrebindHandler)} , when prebinding is
 * finished, invoke {@link #removePrebindHandler()}
 *
 * @author NSamatov, Marat Kamalov
 */
public class LazyBinderInvocationHandler implements InvocationHandler {
    private static final ThreadLocal<IPrebindHandler> prebindHandlerHolder = new ThreadLocal<>();

    private final IOpenBinder originalBinder;
    private final IUserContext ucxt;

    /**
     * Set a prebind handler for current thread
     *
     * @param prebindHandler prebind handler for current thread
     */
    public static void setPrebindHandler(IPrebindHandler prebindHandler) {
        prebindHandlerHolder.set(prebindHandler);
    }

    /**
     * Remove prebind handler for current thread. Necessary modules will be compiled on demand after that.
     */
    public static void removePrebindHandler() {
        prebindHandlerHolder.remove();
    }

    public static IPrebindHandler getPrebindHandler() {
        return prebindHandlerHolder.get();
    }

    /**
     * Create an IOpenBinder invocation handler.
     *
     * @param originalBinder original binder that will be used to compile necessary modules on demand
     * @param ucxt user context for module
     */
    public LazyBinderInvocationHandler(IOpenBinder originalBinder, IUserContext ucxt) {
        this.originalBinder = originalBinder;
        this.ucxt = ucxt;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        IOpenBinder binder = originalBinder;
        IPrebindHandler prebindHandler = prebindHandlerHolder.get();
        if (prebindHandler != null) {
            binder = new XlsPreBinder(ucxt, prebindHandler);
        }
        try {
            return method.invoke(binder, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}