package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

/**
 *
 * @author Yury Molchan
 */
public class IdentifierBinder extends ANodeBinder {

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

        assertNotNull(type, "Field '", fieldName, "' is not found");
        BindHelper.checkOnDeprecation(node, bindingContext, type);
        return new TypeBoundNode(node, type);
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
        IOpenField field = bindingContext.findFieldFor(type, fieldName, false);

        assertNotNull(field, "Field '", fieldName, "' is not found inside '", type, "' type");

        if (target.isStaticTarget() != field.isStatic()) {

            if (field.isStatic()) {
                BindHelper.processWarn("Access of a static field from non-static object", node, bindingContext);
            } else {
                return makeErrorNode("Access non-static field from a static object", node, bindingContext);
            }
        }

        BindHelper.checkOnDeprecation(node, bindingContext, field);
        return new FieldBoundNode(node, field, target, dims);
    }

    private boolean isStrictMatch(ISyntaxNode node) {
        return !node.getType().contains(".nostrict");
    }

}
