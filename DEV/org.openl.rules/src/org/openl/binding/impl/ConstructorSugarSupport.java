package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.base.INamedThing;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.binding.impl.method.MethodSearch;
import org.openl.message.OpenLMessage;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.util.MessageUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConstructorSugarSupport {

    /**
     * Checks whether every argument names a field of the new object, as in {@code new Customer(name = "Ann")}.
     *
     * <p>A call without arguments has no named arguments.
     */
    public static boolean isNamedArguments(ISyntaxNode[] arguments) {
        return arguments.length > 0 && Arrays.stream(arguments).allMatch(ConstructorSugarSupport::isAssignment);
    }

    /** Checks whether an argument names a field of the new object, as {@code name = "Ann"}. */
    static boolean isAssignment(ISyntaxNode argument) {
        return "op.assign".equals(argument.getType());
    }

    /**
     * Calls the constructor of the type that takes the given arguments in their order, as {@code Customer("Ann")}.
     *
     * @return the call, or {@code null} when the type has no such constructor
     */
    public static @Nullable IBoundNode makePositionalConstructor(ISyntaxNode node,
                                                                 IBoundNode[] arguments,
                                                                 IBindingContext bindingContext,
                                                                 IOpenClass type) {
        if (Date.class.getName().equals(type.getName())) {
            return null;
        }
        var methodCaller = MethodSearch.findConstructor(ANodeBinder.getTypes(arguments), bindingContext, type);
        if (methodCaller == null) {
            return null;
        }
        BindHelper.checkOnDeprecation(node, bindingContext, methodCaller);
        return new ConstructorParamsNode(new MethodBoundNode(node, methodCaller, arguments));
    }

    /**
     * Creates an object of a type with arguments bound already, as {@code Customer("Ann")}.
     *
     * <p>The arguments are passed as they are bound: they are not bound again. A call without arguments calls the
     * default constructor.
     *
     * @param typeNode the name of the type in the call
     * @return the call of the constructor, an error when the type cannot be created, or {@code null} when the type
     *         has no constructor for the arguments, or some of them are named
     */
    static @Nullable IBoundNode makeConstructor(ISyntaxNode node,
                                                ISyntaxNode[] argumentNodes,
                                                IBoundNode[] arguments,
                                                IBindingContext bindingContext,
                                                IOpenClass type,
                                                ISyntaxNode typeNode) {
        if (argumentNodes.length == 0) {
            return makeNamedConstructor(node, argumentNodes, bindingContext, type, typeNode);
        }
        if (type.getInstanceClass() == null) {
            return typeDefinedError(typeNode, bindingContext);
        }
        return Arrays.stream(argumentNodes).noneMatch(ConstructorSugarSupport::isAssignment)
                ? makePositionalConstructor(node, arguments, bindingContext, type)
                : null;
    }

    /**
     * Calls the default constructor of the type and assigns the named arguments to its fields, as
     * {@code Customer(name = "Ann")}. A call without arguments calls the default constructor alone.
     *
     * <p>The names are the fields of the type; the values are bound in the context of the call.
     *
     * @param namedArguments the arguments, each an assignment of a field
     * @param typeNode       the name of the type in the call
     * @return the call, or an error when the type cannot be created, or a field is named twice, is missing, static
     *         or read only
     */
    static IBoundNode makeNamedConstructor(ISyntaxNode node,
                                           ISyntaxNode[] namedArguments,
                                           IBindingContext bindingContext,
                                           IOpenClass type,
                                           ISyntaxNode typeNode) {
        if (type.getInstanceClass() == null) {
            return typeDefinedError(typeNode, bindingContext);
        }
        try (var values = NamedValues.bind(namedArguments, type, bindingContext)) {
            return makeNamedConstructor(node, values, type, typeNode);
        }
    }

    /**
     * Calls the default constructor of the type and assigns the values of the named arguments, bound already, to its
     * fields: the values are not bound again.
     *
     * @param values the values of the arguments, bound for the type
     * @return the call, or an error when the type cannot be created, or a field is named twice, is missing, static
     *         or read only
     */
    static IBoundNode makeNamedConstructor(ISyntaxNode node,
                                           NamedValues values,
                                           IOpenClass type,
                                           ISyntaxNode typeNode) {
        var bindingContext = values.bindingContext();
        bindingContext.pushMessages();
        bindingContext.pushErrors();
        try {
            var object = values.object();
            if (object == null) {
                return typeDefinedError(typeNode, bindingContext);
            }
            // what binding the values reported counts as reported here, so that an error of the type drops it
            values.report();
            var params = values.assignments(TypeBindingContext.create(bindingContext, object, 1));
            var namedParams = new HashMap<String, ISyntaxNode>();
            ISyntaxNode duplicatedParamSyntaxNode = null;
            for (var argument : values.arguments()) {
                var paramNameSyntaxNode = argument.getChild(0);
                var paramName = paramName(paramNameSyntaxNode);
                if (namedParams.containsKey(paramName)) {
                    duplicatedParamSyntaxNode = paramNameSyntaxNode;
                }
                namedParams.put(paramName, paramNameSyntaxNode);
            }
            var defaultConstructor = MethodSearch.findConstructor(IOpenClass.EMPTY, bindingContext, type);
            if (duplicatedParamSyntaxNode != null) {
                cleanErrorsAndMessages(bindingContext);
                return ANodeBinder.makeErrorNode("Field '%s' has already used.".formatted(
                        paramName(duplicatedParamSyntaxNode)), duplicatedParamSyntaxNode, bindingContext);
            }
            return makeNamedParamsConstructor(node,
                    type,
                    defaultConstructor,
                    object,
                    params,
                    namedParams,
                    bindingContext);
        } finally {
            bindingContext.popErrors().forEach(bindingContext::addError);
            bindingContext.popMessages().forEach(bindingContext::addMessage);
        }
    }

    /** The error of a type defined with errors, which no object can be created of. */
    private static IBoundNode typeDefinedError(ISyntaxNode typeNode, IBindingContext bindingContext) {
        return ANodeBinder.makeErrorNode(MessageUtils
                .getTypeDefinedErrorMessage(((IdentifierNode) typeNode).getIdentifier()), typeNode, bindingContext);
    }

    /**
     * The values of named arguments, bound once in the context of the call, with the errors and messages their
     * binding reported. The same values are then assigned to the variables of the caller, for a rule that may take
     * them, or to the fields of a new object, so a value is never bound twice.
     *
     * <p>The values are bound in a scope of local variables that declares the new object first, as a constructor
     * declares it before its values, so an object a value creates is kept apart from it. Closing the values ends the
     * scope.
     *
     * @param arguments      the named arguments, each an assignment
     * @param values         the values of the arguments, in their order
     * @param object         the variable of the new object, or {@code null} when the type cannot be created
     * @param bindingContext the context of the call
     */
    record NamedValues(List<ISyntaxNode> arguments,
                       List<IBoundNode> values,
                       @Nullable ILocalVar object,
                       List<SyntaxNodeException> errors,
                       Collection<OpenLMessage> messages,
                       IBindingContext bindingContext) implements AutoCloseable {

        /**
         * Opens the scope of the new object and binds the values of the named arguments in it, keeping apart what
         * their binding reports.
         *
         * @param type the type of the new object, or {@code null} when no type of the name is known
         */
        static NamedValues bind(ISyntaxNode[] namedArguments,
                                @Nullable IOpenClass type,
                                IBindingContext bindingContext) {
            bindingContext.pushLocalVarContext();
            ILocalVar object = null;
            if (type != null && type.getInstanceClass() != null) {
                var name = bindingContext.getTemporaryVarName();
                object = bindingContext.addVar(ISyntaxConstants.THIS_NAMESPACE, name, type);
            }
            bindingContext.pushErrors();
            bindingContext.pushMessages();
            var values = new ArrayList<IBoundNode>(namedArguments.length);
            for (var argument : namedArguments) {
                // an argument without a value is reported when it is assigned
                values.add(argument.getNumberOfChildren() == 2
                        ? ANodeBinder.bindChildNode(argument.getChild(1), bindingContext)
                        : new ErrorBoundNode(argument));
            }
            var errors = bindingContext.popErrors();
            return new NamedValues(List.of(namedArguments),
                    values,
                    object,
                    errors,
                    bindingContext.popMessages(),
                    bindingContext);
        }

        /** Reports in the context of the call what binding the values reported. */
        void report() {
            errors.forEach(bindingContext::addError);
            bindingContext.addMessages(messages);
        }

        /** Assigns the values to the targets the arguments name, resolved in the given context. */
        IBoundNode[] assignments(IBindingContext targetContext) {
            var assignments = new IBoundNode[values.size()];
            for (var i = 0; i < assignments.length; i++) {
                assignments[i] = AssignOperatorNodeBinder
                        .bind(arguments.get(i), targetContext, values.get(i), bindingContext);
            }
            return assignments;
        }

        /** Ends the scope the values are bound in. */
        @Override
        public void close() {
            bindingContext.popLocalVarContext();
        }
    }

    /**
     * Creates a call of the default constructor that assigns the named parameters to the fields of the type.
     *
     * <p>Returns an error node when the type has no default constructor, or when a parameter names a field that is
     * missing, static or read only.
     */
    private static IBoundNode makeNamedParamsConstructor(ISyntaxNode node,
                                                         IOpenClass type,
                                                         IMethodCaller defaultConstructor,
                                                         ILocalVar localVar,
                                                         IBoundNode[] params,
                                                         Map<String, ISyntaxNode> namedParams,
                                                         IBindingContext bindingContext) {
        if (defaultConstructor == null) {
            cleanErrorsAndMessages(bindingContext);
            return ANodeBinder.makeErrorNode("Default constructor is not found in type '%s'.".formatted(
                    type.getDisplayName(INamedThing.SHORT)), node, bindingContext);
        }
        for (var e : namedParams.entrySet()) {
            var f = type.getField(e.getKey());
            if (f == null || f.isStatic() || !f.isWritable()) {
                cleanErrorsAndMessages(bindingContext);
                if (f == null) {
                    return ANodeBinder.makeErrorNode("Field '%s' is not found.".formatted(e.getKey()),
                            e.getValue(),
                            bindingContext);
                }
                if (f.isStatic()) {
                    return ANodeBinder.makeErrorNode(
                            "Field '%s' is found, but it is declared with static modifier.".formatted(e.getKey()),
                            e.getValue(),
                            bindingContext);
                }
                if (!f.isWritable()) {
                    return ANodeBinder.makeErrorNode(
                            "Field '%s' is found, but it is read only.".formatted(e.getKey()),
                            e.getValue(),
                            bindingContext);
                }
            }
        }
        var methodBoundNode = new MethodBoundNode(node, defaultConstructor);
        return new ConstructorNamedParamsNode(localVar, methodBoundNode, params);
    }

    /**
     * The name of a named parameter as it is written, a dotted name such as {@code a.b} included.
     */
    private static String paramName(ISyntaxNode paramNameSyntaxNode) {
        var text = paramNameSyntaxNode.getText();
        var location = paramNameSyntaxNode.getSourceLocation();
        if (text != null || location == null || !location.isTextLocation()) {
            return text;
        }
        return paramNameSyntaxNode.getSourceCodeModule().getCode();
    }

    private static void cleanErrorsAndMessages(IBindingContext bindingContext) {
        bindingContext.popErrors();
        bindingContext.popMessages();
        bindingContext.pushErrors();
        bindingContext.pushMessages();
    }

}
