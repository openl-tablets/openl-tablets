package org.openl.rules.lang.xls.binding.wrapper;

import java.util.LinkedHashMap;
import java.util.Map;

import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.EnumToStringCast;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.cast.StringToEnumCast;
import org.openl.binding.impl.module.ContextPropertyBinderUtils;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.MethodSignature;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ContextPropertiesInjector {
    private static final Logger LOG = LoggerFactory.getLogger(ContextPropertiesInjector.class);
    private static final IContextPropertyInjection[] PROPERTY_INJECTIONS = new IContextPropertyInjection[0];
    private final IContextPropertyInjection[] contextPropertyInjections;

    public ContextPropertiesInjector(IMethodSignature methodSignature, ICastFactory castFactory) {
        IOpenClass[] paramTypes = methodSignature.getParameterTypes();
        int i = 0;
        Map<String, IContextPropertyInjection> contextInjections = new LinkedHashMap<>();
        for (IOpenClass paramType : paramTypes) {
            int paramIndex = i;
            try {
                paramType.getFields()
                    .stream()
                    .filter(IOpenField::isContextProperty)
                    .forEach(field -> contextInjections.put(field.getContextProperty(),
                        createFieldContextPropertyInjection(paramIndex, field, castFactory)));
            } catch (Exception | LinkageError e) {
                LOG.debug("Ignored error: ", e);
            }
            if (methodSignature instanceof MethodSignature) {
                String contextParameter = ((MethodSignature) methodSignature).getParameterDeclaration(i)
                    .getContextProperty();
                if (contextParameter != null) {
                    contextInjections.put(contextParameter,
                        createParameterContextPropertyInjection(paramIndex,
                            methodSignature.getParameterType(i),
                            contextParameter,
                            castFactory));
                }
            }
            i++;
        }
        this.contextPropertyInjections = !contextInjections.isEmpty()
                                                                      ? contextInjections.values()
                                                                          .toArray(PROPERTY_INJECTIONS)
                                                                      : null;
    }

    private static IContextPropertyInjection createParameterContextPropertyInjection(int paramIndex,
            IOpenClass type,
            String contextProperty,
            ICastFactory castFactory) {
        Class<?> contextType = DefaultRulesRuntimeContext.CONTEXT_PROPERTIES.get(contextProperty);
        if (contextType == null) {
            throw new IllegalStateException(String.format("Context property '%s' is not found.", contextProperty));
        }
        IOpenClass contextTypeOpenClass = JavaOpenClass.getOpenClass(contextType);
        IOpenCast openCast = castFactory.getCast(type, contextTypeOpenClass);
        if (openCast == null || !openCast
            .isImplicit() && !(openCast instanceof EnumToStringCast) && !(openCast instanceof StringToEnumCast)) {
            throw new IllegalStateException(
                String.format("Type mismatch for context property '%s'. Cannot convert from '%s' to '%s'.",
                    contextProperty,
                    type.getName(),
                    contextTypeOpenClass.getName()));
        } else {
            return new ParameterContextPropertyInjection(paramIndex, contextProperty, openCast);
        }
    }

    private static IContextPropertyInjection createFieldContextPropertyInjection(int paramIndex,
            IOpenField field,
            ICastFactory castFactory) {
        Class<?> contextType = DefaultRulesRuntimeContext.CONTEXT_PROPERTIES.get(field.getContextProperty());
        if (contextType == null) {
            throw new IllegalStateException(
                String.format("Context property '%s' is not found.", field.getContextProperty()));
        }
        IOpenClass contextTypeOpenClass = JavaOpenClass.getOpenClass(contextType);
        IOpenCast openCast = castFactory.getCast(field.getType(), contextTypeOpenClass);
        if (ContextPropertyBinderUtils.isNonValidCastForContextProperty(openCast)) {
            throw new IllegalStateException(String.format(
                "Type mismatch for context property '%s' for field '%s' in class '%s'. " + "Cannot convert from '%s' to '%s'.",
                field.getContextProperty(),
                field.getName(),
                field.getDeclaringClass().getName(),
                field.getType().getName(),
                contextTypeOpenClass.getName()));
        } else {
            return new FieldContextPropertyInjection(paramIndex, field, openCast);
        }
    }

    public boolean push(Object[] params, IRuntimeEnv env, SimpleRulesRuntimeEnv simpleRulesRuntimeEnv) {
        if (contextPropertyInjections != null) {
            IRulesRuntimeContext rulesRuntimeContext = null;
            for (IContextPropertyInjection contextPropertiesInjector : contextPropertyInjections) {
                rulesRuntimeContext = contextPropertiesInjector
                    .inject(params, env, simpleRulesRuntimeEnv, rulesRuntimeContext);
            }
            if (rulesRuntimeContext != null) {
                env.pushContext(rulesRuntimeContext);
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public void pop(SimpleRulesRuntimeEnv env) {
        env.popContext();
    }
}
