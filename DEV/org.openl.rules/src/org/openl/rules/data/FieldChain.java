package org.openl.rules.data;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

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

        Object result = null;

        for (IOpenField field : fields) {
            if (target == null) {
                result = getType().nullObject();
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
