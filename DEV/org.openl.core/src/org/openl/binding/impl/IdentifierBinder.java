package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;

/**
 *
 * @author Yury Molchan
 */
public class IdentifierBinder extends ANodeBinder {

    private static final Class<?> SPR_CLAZZ;

    static {
        //FIXME needs to figure out better solution. In wonder world, IdentifierBinder must know nothing of concrete
        // classes it must work in the same way for all types
        Class<?> sprType;
        try {
            sprType = IdentifierBinder.class.getClassLoader().loadClass("org.openl.rules.calc.SpreadsheetResult");
        } catch (ClassNotFoundException e) {
            sprType = null;
        }
        SPR_CLAZZ = sprType;
    }

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        boolean strictMatch = isStrictMatch(node);
        String fieldName = node.getText();

        // According to "6.4.2. Obscuring" of Java Language Specification:
        // A simple name may occur in contexts where it may potentially be interpreted as the name of a variable,
        // a type, or a package. In these situations, the rules of §6.5 specify that a variable will be chosen
        // in preference to a type, and that a type will be chosen in preference to a package.
        // See http://docs.oracle.com/javase/specs/jls/se8/html/jls-6.html#jls-6.4.2 for details
        // Implementation below tries to follow that specification.

        IOpenField field = bindingContext.findVar(ISyntaxConstants.THIS_NAMESPACE, fieldName, strictMatch);

        if (field != null) {
            return new FieldBoundNode(node, field);
        }

        IOpenClass type = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, fieldName);
        if (type == null) {
            throw new OpenlNotCheckedException(String.format("Identifier '%s' is not found.", fieldName));
        }

        BindHelper.checkOnDeprecation(node, bindingContext, type);
        return new TypeBoundNode(node, type.toStaticClass());
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
            if (!isSpreadsheetResult(type)) {
                // disable strict match for non SPR types to save backward compatibility with old client projects
                strictMatch = false;
            }
            field = type.getField(fieldName, strictMatch);
        }

        if (field == null) {
            throw new OpenlNotCheckedException(String.format("%s '%s' is not found in type '%s'.", type.isStatic() ? "Static field" : "Field", fieldName, type));
        }

        if (target.isStaticTarget() != field.isStatic()) {

            if (field.isStatic()) {
                if (!(field instanceof JavaOpenClass.JavaClassClassField)) {
                    BindHelper.processWarn(String.format("Accessing to static field '%s' from non-static object of type '%s'.",
                            field.getName(), target.getType().getName()), node, bindingContext);
                }
            } else {
                return makeErrorNode(String.format("Accessing to non-static field '%s' of static type '%s'.",
                        field.getName(), target.getType().getName()), node, bindingContext);
            }
        }

        BindHelper.checkOnDeprecation(node, bindingContext, field);
        return new FieldBoundNode(node, field, target, dims);
    }

    private static boolean isSpreadsheetResult(IOpenClass type) {
        return SPR_CLAZZ != null && ClassUtils.isAssignable(type.getInstanceClass(), SPR_CLAZZ);
    }

    private boolean isStrictMatch(ISyntaxNode node) {
        return !node.getType().contains(".nostrict");
    }

}
