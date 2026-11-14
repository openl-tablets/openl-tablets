package org.openl.rules.data;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.AOpenField;
import org.openl.vm.IRuntimeEnv;

public class FieldChain extends AOpenField {

    private final IOpenField[] fields;

    public FieldChain(IOpenClass type, IOpenField[] fields) {
        super(makeNames(fields), type);
        this.fields = fields;
    }

    public FieldChain(IOpenClass type,
                      IOpenField[] fields,
                      IdentifierNode[] fieldAccessorChainTokens,
                      boolean hasAccessByArrayId) {
        super(getFieldName(fields, fieldAccessorChainTokens, hasAccessByArrayId), type);
        this.fields = fields;
    }

    private static String getFieldName(IOpenField[] fields,
                                       IdentifierNode[] fieldAccessorChainTokens,
                                       boolean hasAccessByArrayId) {
        String name;

        if (hasAccessByArrayId) {
            name = makeNamesByTokens(fieldAccessorChainTokens);
        } else {
            name = makeNames(fields);
        }

        return name;
    }

    public static String makeNames(IOpenField[] fields) {
        return Arrays.stream(fields).filter(Objects::nonNull).map(IOpenField::getName).collect(Collectors.joining("."));
    }

    private static String makeNamesByTokens(IdentifierNode[] fieldAccessorChainTokens) {
        return Arrays.stream(fieldAccessorChainTokens)
                .map(IdentifierNode::getIdentifier)
                .collect(Collectors.joining("."));
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return fields[0].getDeclaringClass();
    }

    @Override
    public IOpenClass getType() {
        return fields[fields.length - 1].getType();
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return walk(target, env, getType().nullObject());
    }

    /**
     * The value at the end of the chain, or {@code null} when a link of the chain has no value, even for a field of a
     * primitive type. A test tells this way a missing value from a zero, such as a field of an element of an empty
     * array.
     */
    public @Nullable Object getOrNull(Object target, IRuntimeEnv env) {
        return walk(target, env, null);
    }

    /** The value at the end of the chain, or the given one when a link of the chain has no value. */
    private @Nullable Object walk(@Nullable Object target, IRuntimeEnv env, @Nullable Object missing) {
        for (IOpenField field : fields) {
            if (target == null) {
                return missing;
            }
            target = field.get(target, env);
        }
        return target;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {

        // find last target, make if necessary
        for (var i = 0; i < fields.length - 1; i++) {
            var newTarget = fields[i].get(target, env);

            if (newTarget == null) {
                newTarget = fields[i].getType().newInstance(env);
                fields[i].set(target, newTarget, env);
            }

            target = newTarget;
        }

        fields[fields.length - 1].set(target, value, env);
    }

    public IOpenField[] getFields() {
        return fields.clone();
    }
}
