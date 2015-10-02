package org.openl.rules.asm.invoker;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.util.factory.CachableFactory;
import org.openl.util.factory.Factory;

/**
 * Contains a set of invokers for SpreadsheetResult class.
 *
 * @author Yury Molchan
 */
public class SpreadsheetResultInvoker {
    private static final CachableFactory<String, Invoker> invokers = new CachableFactory<String, Invoker>(
        new InvokerFactory());

    public static Invoker getMethod(String method) {
        return invokers.create(method);
    }

    private static class InvokerFactory implements Factory<String, Invoker> {
        @Override
        public Invoker create(String method) {
            return VirtialInvoker.create(SpreadsheetResult.class, method, new Class<?>[] { String.class });
        }
    }
}
