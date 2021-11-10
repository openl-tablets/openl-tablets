package org.openl.binding.impl;

import java.util.Collection;
import java.util.Optional;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.impl.module.ArrayOpenField;
import org.openl.binding.impl.module.ModuleSpecificOpenField;
import org.openl.binding.impl.module.ModuleSpecificType;
import org.openl.binding.impl.module.WrapModuleSpecificTypes;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.message.OpenLMessagesUtils;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.StaticOpenClass;
import org.openl.types.impl.OpenFieldDelegator;
import org.openl.types.java.JavaOpenClass;

/**
 *
 * @author Yury Molchan
 */
public class IdentifierBinder extends ANodeBinder {

    protected IBoundNode bindAsOpenField(ISyntaxNode node, boolean strictMatch, IBindingContext bindingContext) {
        String fieldName = node.getText();

        // According to "6.4.2. Obscuring" of Java Language Specification:
        // A simple name may occur in contexts where it may potentially be interpreted as the name of a variable,
        // a type, or a package. In these situations, the rules of §6.5 specify that a variable will be chosen
        // in preference to a type, and that a type will be chosen in preference to a package.
        // See http://docs.oracle.com/javase/specs/jls/se8/html/jls-6.html#jls-6.4.2 for details
        // Implementation below tries to follow that specification.

        IOpenField field;
        try {
            field = bindingContext.findVar(ISyntaxConstants.THIS_NAMESPACE, fieldName, strictMatch);
        } catch (AmbiguousFieldException ex) {
            field = selectFieldFromAmbiguous(ex, node, bindingContext);
        }
        if (field != null) {
            return new FieldBoundNode(node, field);
        }
        return null;
    }

    protected IBoundNode bindAsType(ISyntaxNode node, IBindingContext bindingContext) {
        String typeName = node.getText();
        IOpenClass type = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, typeName);
        if (type != null) {
            type = type.toStaticClass();
            BindHelper.checkOnDeprecation(node, bindingContext, type);
            return new TypeBoundNode(node, type);
        }
        return null;
    }

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        IBoundNode fieldBoundNode = bindAsOpenField(node, true, bindingContext);
        if (fieldBoundNode != null) {
            return fieldBoundNode;
        }

        IBoundNode typeBoundNode = bindAsType(node, bindingContext);
        if (typeBoundNode != null) {
            return typeBoundNode;
        }
        fieldBoundNode = bindAsOpenField(node, false, bindingContext);
        if (fieldBoundNode != null) {
            return fieldBoundNode;
        }
        throw new OpenlNotCheckedException(String.format("Identifier '%s' is not found.", node.getText()));
    }

    @Override
    public IBoundNode bindTarget(ISyntaxNode node, IBindingContext bindingContext, IBoundNode target) {
        String fieldName = node.getText();
        IOpenClass type = target.getType();
        int dims = 0;
        while (type.isArray() && !"length".equals(fieldName)) { // special case for arr[].length
            dims++;
            type = type.getComponentClass();
        }
        IOpenField field;
        boolean strictMatch = isStrictMatch(node);
        if (target.isStaticTarget()) {
            field = type.getStaticField(fieldName, strictMatch);
        } else {
            if (!isAllowOnlyStrictFieldMatch(type)) {
                // disable strict match for all types to save backward compatibility with old client projects, except
                // new types annotated with @AllowOnlyStrictFieldMatchType
                strictMatch = false;
            }
            field = type.getField(fieldName, strictMatch);
        }

        if (field == null) {
            throw new OpenlNotCheckedException(String.format("%s '%s' is not found in type '%s'.",
                type.isStatic() ? "Static field" : "Field",
                fieldName,
                type instanceof StaticOpenClass ? ((StaticOpenClass) type).getDelegate().getName() : type.getName()));
        }

        if (type instanceof WrapModuleSpecificTypes && field.getType() instanceof ModuleSpecificType) {
            IOpenClass t = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, field.getType().getName());
            if (t != null) {
                field = new ModuleSpecificOpenField(field, t);
            }
        }

        if (type instanceof WrapModuleSpecificTypes && field.getType() instanceof ModuleSpecificType) {
            IOpenClass t = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, field.getType().getName());
            if (t != null) {
                field = new ModuleSpecificOpenField(field, t);
            }
        }

        if (target.isStaticTarget() != field.isStatic()) {

            if (field.isStatic()) {
                if (!(field instanceof JavaOpenClass.JavaClassClassField)) {
                    BindHelper.processWarn(
                        String.format("Accessing to static field '%s' from non-static object of type '%s'.",
                            field.getName(),
                            target.getType().getName()),
                        node,
                        bindingContext);
                }
            } else {
                return makeErrorNode(String.format("Accessing to non-static field '%s' of static type '%s'.",
                    field.getName(),
                    target.getType().getName()), node, bindingContext);
            }
        }

        BindHelper.checkOnDeprecation(node, bindingContext, field);
        return new FieldBoundNode(node, field, target, dims);
    }

    private IOpenField selectFieldFromAmbiguous(AmbiguousFieldException ex,
            ISyntaxNode node,
            IBindingContext bindingContext) {
        Collection<IOpenField> matchingFields = ex.getMatchingFields();
        if (matchingFields.stream().allMatch(e -> e instanceof OpenFieldDelegator)) {
            long arraysCount = matchingFields.stream()
                .filter(e -> ((OpenFieldDelegator) e).getDelegate() instanceof ArrayOpenField)
                .count();
            if (matchingFields.size() - arraysCount == 1) {
                Optional<IOpenField> f = matchingFields.stream()
                    .filter(e -> !(((OpenFieldDelegator) e).getDelegate() instanceof ArrayOpenField))
                    .findFirst();
                if (f.isPresent()) {
                    bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(ex.getMessage(), node));
                    return f.get();
                }
            }
        }
        throw ex;
    }

    private static boolean isAllowOnlyStrictFieldMatch(IOpenClass type) {
        return type != null && type.getInstanceClass() != null && type.getInstanceClass()
            .isAnnotationPresent(AllowOnlyStrictFieldMatchType.class);
    }

    private boolean isStrictMatch(ISyntaxNode node) {
        return !node.getType().contains(".nostrict");
    }

}
