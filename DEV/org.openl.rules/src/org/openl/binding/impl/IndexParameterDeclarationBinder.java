package org.openl.binding.impl;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.binding.exception.DuplicatedVarException;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * Binds the declaration of a name in an array index operator, as {@code x} or {@code Type x} in {@code (x, i)}, and
 * declares the local variable of the name.
 * <p>
 * Without a declared type, the variable takes the type the operator gives: the type of the elements for the element,
 * Integer for the index.
 * <p>
 * A declared type that is not found is reported once, by the binding of the type. A name that is already defined is
 * reported on the declaration.
 */
public class IndexParameterDeclarationBinder extends ANodeBinder {

    /**
     * Binds a declaration left outside its operator, which only a code with a syntax error has. The syntax error is
     * already reported, so the declaration binds to an error with no message of its own.
     */
    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) {
        return new ErrorBoundNode(node);
    }

    /**
     * Declares the variable of a name of the declared type, or of the given one.
     *
     * @param origin where the value of the variable comes from, for the hint of the name
     * @return the declaration, or {@code null} when it has an error, which is already reported
     */
    static @Nullable IndexParameterNode declare(ISyntaxNode node,
                                                IOpenClass defaultType,
                                                String origin,
                                                IBindingContext bindingContext) {
        IBoundNode typeNode = null;
        var type = defaultType;
        if (node.getNumberOfChildren() == 2) {
            typeNode = bindChildNode(node.getChild(0), bindingContext);
            if (typeNode instanceof ErrorBoundNode) {
                return null;
            }
            type = typeNode.getType();
        }
        try {
            var variable = bindingContext.addVar(ISyntaxConstants.THIS_NAMESPACE, nameOf(node).getIdentifier(), type);
            return new IndexParameterNode(node, typeNode, variable, origin);
        } catch (DuplicatedVarException e) {
            BindHelper.processError(e, node, bindingContext);
            return null;
        }
    }

    private static IdentifierNode nameOf(ISyntaxNode declaration) {
        return (IdentifierNode) declaration.getChild(declaration.getNumberOfChildren() - 1);
    }

    /**
     * The declaration of an element or of an index, with the variable it declares.
     */
    static class IndexParameterNode extends ABoundNode {
        @Getter
        private final ILocalVar variable;

        /**
         * Where the value of the variable comes from, as {@code Element of Member[]}, for the hint of the name.
         */
        @Getter
        private final String origin;

        private IndexParameterNode(ISyntaxNode syntaxNode,
                                   @Nullable IBoundNode typeNode,
                                   ILocalVar variable,
                                   String origin) {
            super(syntaxNode, typeNode);
            this.variable = variable;
            this.origin = origin;
        }

        /**
         * The name the declaration gives, as {@code m} in {@code Member m}.
         */
        IdentifierNode getNameNode() {
            return nameOf(getSyntaxNode());
        }

        @Override
        protected Object evaluateRuntime(IRuntimeEnv env) {
            return null;
        }

        @Override
        public IOpenClass getType() {
            return variable.getType();
        }
    }
}
