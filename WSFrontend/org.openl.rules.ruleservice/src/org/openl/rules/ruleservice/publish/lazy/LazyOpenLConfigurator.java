package org.openl.rules.ruleservice.publish.lazy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.conf.IOpenLBuilder;
import org.openl.conf.IUserContext;
import org.openl.conf.OpenLConfigurator;

/**
 * Allows using {@link org.openl.rules.lang.xls.prebind.XlsPreBinder XlsPreBinder} for dependent modules on
 * LazyMultiModule projects. Creates an IOpenBinder proxy that uses XlsPreBinder on prebind step and XlsBinder on
 * compile step.
 * 
 * @see LazyBinderInvocationHandler
 * @author NSamatov
 */
public class LazyOpenLConfigurator extends OpenLConfigurator {
    private static final String BUILD_METHOD_NAME = "build";

    @Override
    public synchronized IOpenLBuilder getBuilder(String openlName, IUserContext ucxt) {
        IOpenLBuilder builder = super.getBuilder(openlName, ucxt);

        if (!openlName.startsWith(OpenL.OPENL_JAVA_RULE_NAME)) {
            return builder;
        }

        InvocationHandler handler = makeBuilderInvocationHandler(builder, ucxt);

        return (IOpenLBuilder) Proxy
            .newProxyInstance(builder.getClass().getClassLoader(), new Class<?>[] { IOpenLBuilder.class }, handler);
    }

    private InvocationHandler makeBuilderInvocationHandler(final IOpenLBuilder builder, final IUserContext ucxt) {
        return new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals(BUILD_METHOD_NAME)) {
                    OpenL openl = (OpenL) method.invoke(builder, args);
                    LazyBinderInvocationHandler handler = new LazyBinderInvocationHandler(openl.getBinder(), ucxt);

                    IOpenBinder newBinder = (IOpenBinder) Proxy.newProxyInstance(
                        openl.getBinder().getClass().getClassLoader(),
                        new Class<?>[] { IOpenBinder.class },
                        handler);

                    openl.setBinder(newBinder);
                    return openl;
                }

                return method.invoke(builder, args);
            }

        };
    }
}
