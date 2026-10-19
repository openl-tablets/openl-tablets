package org.openl.binding.impl;

import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.cast.JavaNoCast;
import org.openl.vm.IRuntimeEnv;

/**
 * The variables an array index operator declares for every element: the element itself and, when the operator names
 * it, the index of the element.
 * <p>
 * The operator goes through the elements in their order and skips the empty ones. The index is the position of the
 * element in the array or the collection, starting from 0. The skipped elements keep their positions, so the element
 * at index {@code i} is {@code array[i]}.
 *
 * @param element      the variable of the element
 * @param elementCast  the conversion of an element to the declared type of its variable
 * @param index        the variable of the index, or {@code null} when the operator does not name it
 * @param indexCast    the conversion of an Integer index to the declared type of its variable
 * @param declarations the declarations of the names, such as {@code (m, i)}, empty when the operator names nothing
 */
record ElementVariables(ILocalVar element,
                        IOpenCast elementCast,
                        @Nullable ILocalVar index,
                        IOpenCast indexCast,
                        List<IBoundNode> declarations) {

    /**
     * The variables of an operator that does not name the index.
     */
    ElementVariables(ILocalVar element, IOpenCast elementCast, List<IBoundNode> declarations) {
        this(element, elementCast, null, JavaNoCast.getInstance(), declarations);
    }

    /**
     * Lists the children of the operator node: its operands, then the declarations of the names. A walk over the
     * bound nodes, such as the one that finds the hints of a cell, reaches the declarations this way.
     */
    IBoundNode[] children(IBoundNode... operands) {
        return Stream.concat(Stream.of(operands), declarations.stream()).toArray(IBoundNode[]::new);
    }

    /**
     * Calls the action for every element that is not empty, after it assigns the element and its index to the
     * variables.
     * <p>
     * The action gets the element and the value of the element variable, which is the element converted to the
     * declared type.
     */
    void forEach(Iterator<Object> elements, IRuntimeEnv env, BiConsumer<Object, Object> action) {
        findFirst(elements, env, (element, value) -> {
            action.accept(element, value);
            return false;
        });
    }

    /**
     * Goes through the elements like {@link #forEach} and stops at the first one the condition holds for.
     *
     * @return that element, or {@code null} when the condition holds for none
     */
    @Nullable Object findFirst(Iterator<Object> elements, IRuntimeEnv env, BiPredicate<Object, Object> condition) {
        for (var position = 0; elements.hasNext(); position++) {
            var element = elements.next();
            if (element != null && condition.test(element, assign(element, position, env))) {
                return element;
            }
        }
        return null;
    }

    /**
     * Assigns an element and its index to the variables.
     *
     * @return the element converted to the declared type of its variable
     */
    private Object assign(Object element, int position, IRuntimeEnv env) {
        var value = elementCast.convert(element);
        this.element.set(null, value, env);
        if (index != null) {
            index.set(null, indexCast.convert(position), env);
        }
        return value;
    }
}
