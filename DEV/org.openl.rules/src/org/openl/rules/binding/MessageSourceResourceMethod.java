package org.openl.rules.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.CastNode;
import org.openl.binding.impl.IfNode;
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

    /**
     * Whether the module keeps the calls of the method bound in it. A table bound alone against a module compiled
     * before leaves the module as it was, so its calls are not kept.
     */
    private final boolean keepsCalls;

    /** The messages of the module the method is bound in, which a hint over a key of a call of it shows. */
    public OpenLMessageSource getMessageSource() {
        return module.getMessageSource();
    }

    /**
     * Keeps a call of the built-in {@code msg} that writes its key as text in the module the call is bound in, to
     * be checked once the compilation knows which messages the call reads at runtime. Any other call is left alone.
     *
     * <p>A compilation in the execution mode, such as the one of OpenL Rule Services, keeps nothing: the check only
     * warns the author, and a kept call would hold the source of its table in memory for as long as the service runs.
     *
     * @param node           the call
     * @param caller         the method the call binds to
     * @param arguments      the bound arguments of the call, as written
     * @param bindingContext the context the call is bound in
     */
    public static void keepCall(ISyntaxNode node,
                                IMethodCaller caller,
                                IBoundNode[] arguments,
                                IBindingContext bindingContext) {
        if (bindingContext.isExecutionMode()) {
            return;
        }
        var method = calledBy(caller);
        if (method == null || !method.keepsCalls) {
            return;
        }
        var values = valuesOf(arguments);
        keysOf(arguments).forEach(literal -> method.module.addMessageCall(
                new MessageCall((String) literal.getValue(), literal.getSyntaxNode(), node, values)));
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
     * The keys a call of {@code msg} writes as text: the key itself, or each key a choice of the call may give, as
     * both keys of {@code msg(isNew ? "order.new" : "order.old")}.
     *
     * @param arguments the bound arguments of the call, as written
     * @return the texts of the keys, none when the key is built, passed through a variable or returned by another
     *         rule, and so is known at runtime alone
     */
    public static List<LiteralBoundNode> keysOf(IBoundNode @Nullable [] arguments) {
        var keys = new ArrayList<LiteralBoundNode>();
        if (arguments != null && arguments.length > 0) {
            collectKeys(arguments[0], keys);
        }
        return keys;
    }

    /** Adds the texts a key may be: the text itself, or those of both branches of a choice, a cast left aside. */
    private static void collectKeys(@Nullable IBoundNode node, List<LiteralBoundNode> keys) {
        switch (node) {
            case LiteralBoundNode literal when literal.getValue() instanceof String -> keys.add(literal);
            case IfNode choice -> {
                collectKeys(choice.getThenNode(), keys);
                collectKeys(choice.getElseNode(), keys);
            }
            case CastNode cast -> collectKeys(cast.getChildren()[0], keys);
            case null, default -> {
                // A key built at runtime is known at runtime alone.
            }
        }
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
