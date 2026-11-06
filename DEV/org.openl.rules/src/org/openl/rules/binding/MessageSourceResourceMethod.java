package org.openl.rules.binding;

import java.util.Locale;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.binding.IBoundNode;
import org.openl.binding.impl.LiteralBoundNode;
import org.openl.binding.impl.method.AOpenMethodDelegator;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.CastingMethodCaller;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleRuntimeEnv;

/**
 * Virtual method for getting messages from OpenL Rules.<br>
 * <br>
 * Example:<br>
 *
 * <pre>
 * {
 *     &#64;code
 *     // foo.bar = Hello, {0} {1}!
 *     // Get message without formatting by code
 *     String msg = msg("foo.bar"); // Hello, {0} {1}!
 *     // Get message formatter by code
 *     String formattedMsg = msg("foo.bar", "Mr.", "John"); // Hello, Mr. John!
 * }
 * </pre>
 *
 * @author Vladyslav Pikus
 */
@RequiredArgsConstructor
public class MessageSourceResourceMethod implements IOpenMethod {

    /**
     * The module the method is bound in, which keeps the calls of the method its tables write a key in. At runtime
     * the messages of the module the call entered through are read.
     */
    private final XlsModuleOpenClass module;

    /** The messages of the module the method is bound in, which a hint over a key of a call of it shows. */
    public OpenLMessageSource getMessageSource() {
        return module.getMessageSource();
    }

    /**
     * Keeps a call of the built-in {@code msg} that writes its key as text in the module the call is bound in, to
     * be checked once the compilation knows which messages the call reads at runtime. Any other call is left alone.
     *
     * @param node      the call
     * @param caller    the method the call binds to
     * @param arguments the bound arguments of the call, as written
     */
    public static void keepCall(ISyntaxNode node, IMethodCaller caller, IBoundNode[] arguments) {
        var method = calledBy(caller);
        var literal = keyOf(arguments);
        if (method != null && literal != null) {
            method.module.addMessageCall(new MessageCall((String) literal.getValue(), literal.getSyntaxNode(), node,
                    valuesOf(arguments)));
        }
    }

    /**
     * How many values a call passes, or 0 where it cannot be told: a single array of values, or a single null,
     * which stands for no values.
     */
    private static int valuesOf(IBoundNode[] arguments) {
        if (arguments.length == 2) {
            var single = arguments[1];
            var none = single instanceof LiteralBoundNode literal && literal.getValue() == null;
            if (none || single.getType().isArray()) {
                return 0;
            }
        }
        return arguments.length - 1;
    }

    /**
     * The built-in {@code msg} a call binds to, through the wrappers of its varargs and casts.
     *
     * @param caller the method a call binds to
     * @return the built-in {@code msg}, or {@code null} for any other method, a user rule called {@code msg} too
     */
    public static @Nullable MessageSourceResourceMethod calledBy(IMethodCaller caller) {
        return switch (caller) {
            case MessageSourceResourceMethod method -> method;
            case AOpenMethodDelegator delegator -> calledBy(delegator.getDelegate());
            case CastingMethodCaller casting -> casting.getMethod() == caller ? null : calledBy(casting.getMethod());
            default -> null;
        };
    }

    /**
     * The key a call of {@code msg} writes as text.
     *
     * @param arguments the bound arguments of the call, as written
     * @return the text of the key, or {@code null} when the key is built, passed through a variable or returned
     *         by another rule, and so is known at runtime alone
     */
    public static @Nullable LiteralBoundNode keyOf(IBoundNode @Nullable [] arguments) {
        return arguments != null && arguments.length > 0 && arguments[0] instanceof LiteralBoundNode literal
                && literal.getValue() instanceof String ? literal : null;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        var locale = Optional.ofNullable(((IRulesRuntimeContext) env.getContext()).getLocale()).orElse(Locale.US);
        var messageBundle = ((XlsModuleOpenClass) ((SimpleRuntimeEnv) env).getTopClass()).getMessageSource()
                .getMessageBundle(locale);
        return messageBundle.msg((String) params[0], (Object[]) params[1]);
    }

    @Override
    public String getDisplayName(int mode) {
        return getName();
    }

    @Override
    public String getName() {
        return "msg";
    }

    @Override
    public IOpenMethod getMethod() {
        return this;
    }

    @Override
    public IOpenClass getType() {
        return JavaOpenClass.STRING;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public @Nullable IMemberMetaInfo getInfo() {
        return null;
    }

    @Override
    public @Nullable IOpenClass getDeclaringClass() {
        return null;
    }

    @Override
    public boolean isConstructor() {
        return false;
    }

    @Override
    public IMethodSignature getSignature() {
        return new MethodSignature(new ParameterDeclaration(JavaOpenClass.STRING, "code"),
                new ParameterDeclaration(JavaOpenClass.getOpenClass(Object[].class), "args"));
    }
}
