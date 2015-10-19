package org.openl.rules.asm.invoker;

import org.openl.util.factory.CacheableFactory;
import org.openl.util.factory.Factory;

/**
 * Contains a set of invokers for StringBuilder class.
 *
 * @author Yury Molchan
 */
public class StringBuilderInvoker {
    private static final Invoker TO_STRING = VirtialInvoker.create(StringBuilder.class, "toString");

    private static final CacheableFactory<Class<?>, Invoker> appendInvokers = new CacheableFactory<Class<?>, Invoker>(
        new InvokerFactory());

    public static Invoker getToString() {
        return TO_STRING;
    }

    public static Invoker getAppend(Class<?> type) {
        return appendInvokers.create(type);
    }

    private static class InvokerFactory implements Factory<Class<?>, Invoker> {
        @Override
        public Invoker create(Class<?> type) {
            return VirtialInvoker.create(StringBuilder.class, "append", new Class<?>[] { type });
        }
    }
}
