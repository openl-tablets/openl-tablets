package org.openl.binding.impl;

import java.util.Optional;

import org.openl.binding.IBoundNode;
import org.openl.binding.impl.FieldBoundNodeUsageCreator.FieldUsage;
import org.openl.binding.impl.IndexParameterDeclarationBinder.IndexParameterNode;
import org.openl.types.NullOpenClass;

/**
 * Describes a name an array index operator declares, as {@code m} and {@code i} in
 * {@code members[(m, i) select all having i > 0]}.
 * <p>
 * The hint tells where the value comes from, then gives the hint of a use of the name in the expression:
 * {@code Element of Member[]} and {@code Member m}, with the link to the table that declares the type.
 */
final class IndexParameterNodeUsageCreator implements NodeUsageCreator {

    private IndexParameterNodeUsageCreator() {
    }

    @Override
    public boolean accept(IBoundNode boundNode) {
        return boundNode instanceof IndexParameterNode;
    }

    @Override
    public Optional<NodeUsage> create(IBoundNode boundNode, String sourceString, int startIndex) {
        var parameter = (IndexParameterNode) boundNode;
        // A local variable is read from no type, as a use of the name in the expression is.
        var use = FieldBoundNodeUsageCreator.describe(parameter.getVariable(), NullOpenClass.the,
                parameter.getNameNode());
        return new FieldUsage(use.syntaxNode(), parameter.getOrigin() + "\n" + use.description(), use.uri())
                .toNodeUsage(sourceString, startIndex);
    }

    private static class Holder {
        private static final IndexParameterNodeUsageCreator INSTANCE = new IndexParameterNodeUsageCreator();
    }

    static IndexParameterNodeUsageCreator getInstance() {
        return Holder.INSTANCE;
    }
}
