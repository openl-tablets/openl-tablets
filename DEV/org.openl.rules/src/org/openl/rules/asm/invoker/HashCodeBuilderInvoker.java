package org.openl.rules.asm.invoker;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.openl.util.factory.CachableFactory;
import org.openl.util.factory.Factory;

/**
 * Contains a set of invokers for HashCodeBuilder class.
 *
 * @author Yury Molchan
 */
public class HashCodeBuilderInvoker {
    private static final Invoker TO_HASH_CODE = VirtialInvoker.create(HashCodeBuilder.class, "toHashCode");

    private static final CachableFactory<Class<?>, Invoker> appendInvokers = new CachableFactory<Class<?>, Invoker>(
            new InvokerFactory());

    public static Invoker getToHashCode() {
        return TO_HASH_CODE;
    }

    public static Invoker getAppend(Class<?> type) {
        return appendInvokers.create(type);
    }

    private static class InvokerFactory implements Factory<Class<?>, Invoker> {
        @Override
        public Invoker create(Class<?> type) {
            return VirtialInvoker.create(HashCodeBuilder.class, "append", new Class<?>[] { type });
        }
    }
}
