package org.openl.binding.impl.module;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.cast.EnumToStringCast;
import org.openl.binding.impl.cast.INestedCastOpenCast;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.cast.StringToEnumCast;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

public final class ContextPropertyBinderUtils {

    private ContextPropertyBinderUtils() {
    }

    public static String validateContextProperty(String contextProperty,
                                                 IOpenClass expectedType,
                                                 IBindingContext bindingContext) {
        String errorMessage = null;
        Class<?> contextPropertyClass = DefaultRulesRuntimeContext.CONTEXT_PROPERTIES.get(contextProperty);
        if (contextPropertyClass == null) {
            errorMessage = String.format("Property '%s' is not found in the context. Available properties: [%s].",
                    contextProperty,
                    String.join(", ", DefaultRulesRuntimeContext.CONTEXT_PROPERTIES.keySet()));
        } else {
            IOpenClass contextPropertyType = JavaOpenClass.getOpenClass(contextPropertyClass);
            IOpenCast openCast = null;
            try {
                openCast = bindingContext.getCast(expectedType, contextPropertyType);
            } catch (NullPointerException ignored) {
            }
            if (isNonValidCastForContextProperty(openCast)) {
                errorMessage = String.format(
                        "Type mismatch for context property '%s'. Cannot convert from '%s' to '%s'.",
                        contextProperty,
                        expectedType.getName(),
                        contextPropertyType.getName());
            }
        }
        return errorMessage;
    }

    public static boolean isNonValidCastForContextProperty(IOpenCast openCast) {
        return openCast == null || !openCast.isImplicit() && !(extractIfNestedOpenCast(
                openCast) instanceof EnumToStringCast) && !(extractIfNestedOpenCast(openCast) instanceof StringToEnumCast);
    }

    private static IOpenCast extractIfNestedOpenCast(IOpenCast openCast) {
        if (openCast instanceof INestedCastOpenCast) {
            INestedCastOpenCast nestedCastOpenCast = (INestedCastOpenCast) openCast;
            if (nestedCastOpenCast.hasNestedOpenCast()) {
                return nestedCastOpenCast.getNestedOpenCast();
            }
        }
        return openCast;
    }

}
