package org.openl.rules.binding;

import java.util.regex.PatternSyntaxException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.LiteralBoundNode;
import org.openl.binding.impl.method.AOpenMethodDelegator;
import org.openl.rules.util.LikePattern;
import org.openl.rules.util.Strings;
import org.openl.types.IMethodCaller;
import org.openl.types.java.JavaOpenMethod;

/**
 * Checks the pattern of a call of {@code like} when the pattern is written as text.
 *
 * <p>A pattern that is not valid, such as {@code [a-A]} with a reversed range, fails every call. It is reported as a
 * compilation error on the pattern, rather than as a failure when the call runs.
 *
 * <p>A pattern passed through a variable, built from parts or returned by another rule is known at runtime alone, so
 * it is not checked.
 *
 * @author Yury Molchan
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LikePatternCheck {

    private static final String LIKE = "like";

    /**
     * Reports the pattern of the call when the call is {@code like} and its pattern is invalid text.
     *
     * @param caller    the method the call binds to
     * @param arguments the bound arguments of the call
     */
    public static void check(IMethodCaller caller, IBoundNode[] arguments, IBindingContext bindingContext) {
        if (arguments.length == 2 && isLike(caller)
                && arguments[1] instanceof LiteralBoundNode literal
                && literal.getValue() instanceof String pattern && !pattern.isEmpty()) {
            try {
                LikePattern.compile(pattern);
            } catch (PatternSyntaxException e) {
                var message = "Pattern '%s' of the function 'like' is not valid: %s."
                        .formatted(pattern, e.getDescription());
                BindHelper.processError(message, literal.getSyntaxNode(), bindingContext);
            }
        }
    }

    /** Whether the call runs {@code like}, also for each text of an array or through a module of the project. */
    private static boolean isLike(IMethodCaller caller) {
        var method = caller.getMethod();
        while (method instanceof AOpenMethodDelegator delegator) {
            method = delegator.getDelegate();
        }
        return method instanceof JavaOpenMethod javaMethod
                && javaMethod.getJavaMethod().getDeclaringClass() == Strings.class
                && LIKE.equals(javaMethod.getName());
    }
}
