package org.openl.types.java;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.function.BiFunction;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * Calls a Java method as {@link Method#invoke(Object, Object...)} does, and faster when the method is called often.
 * <p>
 * The first calls go through reflection. A method called {@value #HOT_CALLS} times gets a generated class that calls it
 * directly, and later calls with a target and arguments of the parameter types go through that class.
 * <p>
 * The results and the failures are those of reflection. A primitive result comes boxed, and a {@code void} one as
 * {@code null}. Whatever the method throws comes as the cause of an {@link InvocationTargetException}. Arguments that
 * reflection would convert or reject, such as an {@code Integer} for a {@code long} parameter or {@code null} for a
 * primitive one, still go through reflection. A method no class can be generated for is always called by reflection:
 * see {@link DirectCalls}.
 * <p>
 * An invoker is safe to share between threads.
 */
public final class MethodInvoker {

    static final int HOT_CALLS = 16;

    private static final Object[] NO_ARGS = {};

    // Marks a method that is called by reflection for good.
    private static final BiFunction<Object, Object[], Object> REFLECTION = (target, args) -> {
        throw new IllegalStateException("A method called by reflection has no direct call.");
    };

    @Getter
    private final Method method;

    // Unset until the method is hot. A stateless function, so a racy read is safe.
    private @Nullable BiFunction<Object, Object[], Object> direct;
    // Counts calls up to HOT_CALLS. Lost increments only delay the generation.
    private int calls;

    public MethodInvoker(Method method) {
        this.method = Objects.requireNonNull(method, "method cannot be null");
        if (Modifier.isPublic(method.getModifiers()) && Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
            // Accessible anyway: skip the access check that every reflective call repeats otherwise.
            method.trySetAccessible();
        }
    }

    /**
     * An invoker that is hot already and calls the method through {@code direct}.
     */
    MethodInvoker(Method method, BiFunction<Object, Object[], Object> direct) {
        this(method);
        this.direct = direct;
    }

    /**
     * Calls the method on {@code target}, ignored for a static method, with {@code args}.
     */
    // Throwable: reflection reports whatever the method throws, an Error too, as an InvocationTargetException
    @SuppressWarnings("java:S1181")
    public @Nullable Object invoke(@Nullable Object target, @Nullable Object[] args)
            throws IllegalAccessException, InvocationTargetException {
        var call = direct;
        if (call == null && ++calls >= HOT_CALLS) {
            call = directCall();
        }
        if (call != null && call != REFLECTION) {
            // The direct call answers these very arguments when they do not fit it
            var arguments = Objects.requireNonNullElse(args, NO_ARGS);
            Object result;
            try {
                result = call.apply(target, arguments);
            } catch (LinkageError e) {
                if (!failedToLink(e, call)) {
                    throw new InvocationTargetException(e);
                }
                // The method was not reached: call it by reflection from now on
                direct = REFLECTION;
                result = arguments;
            } catch (Throwable e) {
                // Reflection reports whatever the method throws the same way
                throw new InvocationTargetException(e);
            }
            if (result != arguments) {
                return result;
            }
        }
        return method.invoke(target, args);
    }

    /**
     * Generates the direct call once, however many threads find the method hot at the same time.
     */
    private synchronized BiFunction<Object, Object[], Object> directCall() {
        var call = direct;
        if (call == null) {
            call = Objects.requireNonNullElse(DirectCalls.of(method), REFLECTION);
            direct = call;
        }
        return call;
    }

    /**
     * Whether the direct call failed to link its own call of the method, so the method never ran. The error then comes
     * from the frame of the call class itself, or from this invoker when the frames of the hidden class are not shown.
     */
    private static boolean failedToLink(LinkageError e, BiFunction<Object, Object[], Object> call) {
        var trace = e.getStackTrace();
        if (trace.length == 0) {
            return false;
        }
        var origin = trace[0].getClassName();
        return origin.equals(call.getClass().getName()) || origin.equals(MethodInvoker.class.getName());
    }

    /**
     * Whether the method is called directly, not by reflection.
     */
    boolean isDirect() {
        var call = direct;
        return call != null && call != REFLECTION;
    }
}
