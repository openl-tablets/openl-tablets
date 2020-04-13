package org.openl.rules.lang.xls.binding.wrapper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

class ContextPropertiesInjector {

    private final Map<String, ContextPropertyInjection> contextPropertyInjections;

    public ContextPropertiesInjector(IOpenClass[] paramTypes, ICastFactory castFactory) {
        int i = 0;
        Map<String, ContextPropertyInjection> contextPropertyInjections = new LinkedHashMap<>();
        for (IOpenClass paramType : paramTypes) {
            try {
                int paramIndex = i;
                paramType.getFields()
                    .stream()
                    .filter(IOpenField::isContextProperty)
                    .forEach(field -> contextPropertyInjections.put(field.getContextProperty(),
                        createContextInjection(paramIndex, field, castFactory)));
            } catch (Exception | LinkageError ignored) {
            }
            i++;
        }
        this.contextPropertyInjections = Collections.unmodifiableMap(contextPropertyInjections);
    }

    private ContextPropertyInjection createContextInjection(int paramIndex,
            IOpenField field,
            ICastFactory castFactory) {
        Class<?> contextType = DefaultRulesRuntimeContext.CONTEXT_PROPERTIES.get(field.getContextProperty());
        if (contextType == null) {
            throw new IllegalStateException(
                String.format("Context property '%s' is not found.", field.getContextProperty()));
        }
        IOpenClass contextTypeOpenClass = JavaOpenClass.getOpenClass(contextType);
        IOpenCast openCast = castFactory.getCast(field.getType(), contextTypeOpenClass);
        if (openCast != null && (openCast
            .isImplicit() || contextTypeOpenClass.getInstanceClass() != null && contextTypeOpenClass.getInstanceClass()
                .isEnum())) {
            return new ContextPropertyInjection(paramIndex, field, openCast);
        } else {
            throw new ClassCastException(String.format(
                "Type mismatch for context property '%s' for field '%s' in class '%s'. Cannot convert from '%s' to '%s'.",
                field.getContextProperty(),
                field.getName(),
                field.getDeclaringClass().getName(),
                field.getType().getName(),
                contextTypeOpenClass.getName()));
        }
    }

    public boolean push(Object[] params, IRuntimeEnv env, SimpleRulesRuntimeEnv simpleRulesRuntimeEnv) {
        IRulesRuntimeContext rulesRuntimeContext = null;
        for (ContextPropertyInjection contextPropertiesInjector : contextPropertyInjections.values()) {
            rulesRuntimeContext = contextPropertiesInjector
                .inject(params, env, simpleRulesRuntimeEnv, rulesRuntimeContext);
        }
        if (rulesRuntimeContext != null) {
            env.pushContext(rulesRuntimeContext);
        }
        return rulesRuntimeContext != null;
    }

    public void pop(SimpleRulesRuntimeEnv env) {
        env.popContext();
    }

    private static class ContextPropertyInjection {
        private int paramIndex;
        private IOpenField field;
        private IOpenCast openCast;

        public ContextPropertyInjection(int paramIndex, IOpenField field, IOpenCast openCast) {
            super();
            this.paramIndex = paramIndex;
            this.field = field;
            this.openCast = openCast;
        }

        public IRulesRuntimeContext inject(Object[] params,
                IRuntimeEnv env,
                SimpleRulesRuntimeEnv simpleRulesRuntimeEnv,
                IRulesRuntimeContext rulesRuntimeContext) {
            if (params[paramIndex] != null) {
                Object value = field.get(params[paramIndex], env);
                value = openCast.convert(value);
                if (rulesRuntimeContext == null) {
                    IRulesRuntimeContext currentRuntimeContext = (IRulesRuntimeContext) simpleRulesRuntimeEnv
                        .getContext();
                    try {
                        rulesRuntimeContext = (IRulesRuntimeContext) currentRuntimeContext.clone();
                        rulesRuntimeContext.setValue(field.getContextProperty(), value);
                    } catch (CloneNotSupportedException e) {
                        throw new OpenlNotCheckedException(e);
                    }
                } else {
                    rulesRuntimeContext.setValue(field.getContextProperty(), value);
                }
            }
            return rulesRuntimeContext;
        }
    }
}
