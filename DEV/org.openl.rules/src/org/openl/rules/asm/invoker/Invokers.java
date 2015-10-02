package org.openl.rules.asm.invoker;

/**
 * Contains a set of invokers for Java lang classes.
 *
 * @author Yury Molchan
 */
public class Invokers {
    public static final Invoker GET_CLASS = VirtialInvoker.create(Object.class, "getClass");
    public static final Invoker GET_CLASS_NAME = VirtialInvoker.create(Class.class, "getSimpleName");
    public static final Invoker INT_VALUE = VirtialInvoker.create(Integer.class, "intValue");
}
