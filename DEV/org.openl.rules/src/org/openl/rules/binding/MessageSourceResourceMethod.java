package org.openl.rules.binding;

import java.util.Locale;
import java.util.Optional;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * Virtual method for getting messages from OpenL Rules.<br>
 * <br>
 * Example:<br>
 * 
 * <pre>
 * {@code
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
public class MessageSourceResourceMethod implements IOpenMethod {

    private final OpenLMessageSource messageSource;

    public MessageSourceResourceMethod(ClassLoader classLoader) {
        this.messageSource = new OpenLMessageSource(classLoader);
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        Locale locale = Optional.ofNullable(((IRulesRuntimeContext) env.getContext()).getLocale()).orElse(Locale.US);

        var messageBundle = messageSource.getMessageBundle(locale);
        return messageBundle.msg((String) params[0],(Object[]) params[1]);
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
    public IMemberMetaInfo getInfo() {
        return null;
    }

    @Override
    public IOpenClass getDeclaringClass() {
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
