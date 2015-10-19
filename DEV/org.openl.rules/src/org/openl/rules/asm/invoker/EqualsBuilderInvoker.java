package org.openl.rules.asm.invoker;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.openl.util.factory.CacheableFactory;
import org.openl.util.factory.Factory;

/**
 * Contains a set of invokers for EqualsBuilder class.
 * 
 * @author Yury Molchan
 */
public class EqualsBuilderInvoker {
    private static final Invoker IS_EQUALS = VirtialInvoker.create(EqualsBuilder.class, "isEquals");

    private static final CacheableFactory<Class<?>, Invoker> appendInvokers = new CacheableFactory<Class<?>, Invoker>(
        new InvokerFactory());

    public static Invoker getIsEquals() {
        return IS_EQUALS;
    }

    public static Invoker getAppend(Class<?> type) {
        return appendInvokers.create(type);
    }

    private static class InvokerFactory implements Factory<Class<?>, Invoker> {
        @Override
        public Invoker create(Class<?> type) {
            return VirtialInvoker.create(EqualsBuilder.class, "append", new Class<?>[] { type, type });
        }
    }
}
