/*
 * Created on Jul 2, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 */
public class TypeBinder extends ANodeBinder {

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bind(org.openl.syntax.ISyntaxNode,
     * org.openl.binding.IBindingContext)
     */
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        ISyntaxNode typeNode = node.getChild(0);
        int dimension = 0;

        for (; !(typeNode instanceof IdentifierNode); ++dimension) {
            typeNode = typeNode.getChild(0);
        }

        String typeName = ((IdentifierNode) typeNode).getIdentifier();
        IOpenClass varType = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, typeName);

        if (varType == null) {
            String message = String
                .format("Can't bind node: '%s'. Can't find type: '%s'.", node.getModule().getCode(), typeName);
            BindHelper.processError(message, node, bindingContext, false);

            return new ErrorBoundNode(node);
        }

        if (varType instanceof JavaOpenClass) {
            String errorMessage = validateJavaType(bindingContext, varType, typeName, new HashSet<IOpenClass>());
            if (errorMessage != null) {
                BindHelper.processError(errorMessage, node, bindingContext, false);
                return new ErrorBoundNode(node);
            }
        }

        if (dimension > 0) {
            varType = varType.getAggregateInfo().getIndexedAggregateType(varType, dimension);
        }

        return new TypeBoundNode(node, varType);
    }

    private String validateJavaType(IBindingContext bindingContext,
            IOpenClass varType,
            String typeName,
            Set<IOpenClass> checkedTypes) {
        // Check that dependent classes can be loaded too

        checkedTypes.add(varType);
        try {
            Map<String, IOpenField> fields = varType.getFields();
            for (IOpenField field : fields.values()) {

                IOpenClass type = field.getType();
                if (checkedTypes.contains(type)) {
                    continue;
                }

                String message = validateJavaType(bindingContext, type, typeName, checkedTypes);
                if (message != null) {
                    return message;
                }
                checkedTypes.add(type);
            }
        } catch (NoClassDefFoundError error) {
            if (error.getCause() instanceof ClassNotFoundException) {
                String noClassMessage = error.getCause() != null ? error.getCause().getMessage() : error.getMessage();

                String simpleClassName = noClassMessage;
                if (simpleClassName.lastIndexOf(".") > 0) {
                    simpleClassName = simpleClassName.substring(simpleClassName.lastIndexOf(".") + 1);
                }
                IOpenClass openClass = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, simpleClassName);
                if (openClass != null && !(openClass instanceof JavaOpenClass)) {
                    return null;
                }
                return String.format("Can't load type '%s' because of absent type '%s' in the type '%s'.",
                    typeName,
                    noClassMessage,
                    varType.getInstanceClass().getName());
            }
        } catch (UnsupportedClassVersionError e) {
            // Type is found but it's compiled using newer version of JDK
            return String.format(
                "Can't load the class '%s' that was compiled using newer version of JDK than current JRE (%s)",
                typeName,
                System.getProperty("java.version"));
        } catch (Throwable e) {
            return String.format("Can't load type '%s': %s", typeName, e.getMessage());
        }
        return null;
    }

}
