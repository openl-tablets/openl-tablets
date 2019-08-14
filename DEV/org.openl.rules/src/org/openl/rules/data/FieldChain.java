package org.openl.rules.data;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.AOpenField;
import org.openl.vm.IRuntimeEnv;

public class FieldChain extends AOpenField {

    private IOpenField[] fields;
    private NewInstanceBuilder[] newInstanceBuilders;

    public FieldChain(IOpenClass type, IOpenField[] fields) {
        this(type, fields, null);
    }

    public FieldChain(IOpenClass type, IOpenField[] fields, NewInstanceBuilder[] newInstanceBuilders) {
        super(makeNames(fields), type);
        this.fields = fields;
        if (newInstanceBuilders != null && fields.length != newInstanceBuilders.length) {
            throw new IllegalArgumentException(
                "Fields array length must be equal to newInstanceBuilders array length.");
        }
        this.newInstanceBuilders = newInstanceBuilders;
    }

    public FieldChain(IOpenClass type,
            IOpenField[] fields,
            IdentifierNode[] fieldAccessorChainTokens,
            boolean hasAccessByArrayId,
            NewInstanceBuilder[] newInstanceBuilders) {
        super(getFieldName(fields, fieldAccessorChainTokens, hasAccessByArrayId), type);
        this.fields = fields;
        if (newInstanceBuilders != null && fields.length != newInstanceBuilders.length) {
            throw new IllegalArgumentException(
                "Fields array length must be equal to newInstanceBuilders array length.");
        }
        this.newInstanceBuilders = newInstanceBuilders;
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

    private static String makeNames(IOpenField[] fields) {
        return Arrays.stream(fields).map(IOpenField::getName).collect(Collectors.joining("."));
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

        Object result = null;

        for (IOpenField field : fields) {
            if (target == null) {
                result = null;
                break;
            }
            result = field.get(target, env);
            target = result;
        }

        return result;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {

        // find last target, make if necessary
        for (int i = 0; i < fields.length - 1; i++) {
            Object newTarget = fields[i].get(target, env);

            if (newTarget == null) {
                if (newInstanceBuilders != null && newInstanceBuilders[i] != null) {
                    newTarget = newInstanceBuilders[i].newInstance(env);
                } else {
                    newTarget = fields[i].getType().newInstance(env);
                }
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
