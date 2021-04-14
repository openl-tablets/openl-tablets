package org.openl.binding.impl;

import java.util.Optional;

import org.openl.binding.IBoundNode;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.ExecutableMethod;
import org.openl.types.impl.MethodDelegator;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

/**
 * Helps to find all used OpenL methods in compiled code by {@link IBoundNode}.
 *
 * @author PUdalau, Vladyslav Pikus
 */
final class MethodBoundNodeUsageCreator implements NodeUsageCreator {

    private MethodBoundNodeUsageCreator() {
    }

    @Override
    public boolean accept(IBoundNode boundNode) {
        return boundNode instanceof MethodBoundNode;
    }

    @Override
    public Optional<NodeUsage> create(IBoundNode boundNode, String sourceString, int startIndex) {
        MethodBoundNode methodBoundNode = (MethodBoundNode) boundNode;
        ILocation location = methodBoundNode.getSyntaxNode().getSourceLocation();
        IMethodCaller methodCaller = methodBoundNode.getMethodCaller();
        if (methodCaller != null && location != null && location.isTextLocation()) {
            IOpenMethod method;
            if (methodCaller instanceof IOpenMethod) {
                method = (IOpenMethod) methodCaller;
            } else {
                method = methodCaller.getMethod();
            }
            if (method instanceof ExecutableMethod || method instanceof MatchingOpenMethodDispatcher || method instanceof MethodDelegator) {
                TextInfo info = new TextInfo(sourceString);
                int pstart = location.getStart().getAbsolutePosition(info) + startIndex;
                int pend = pstart + method.getName().length() - 1;
                return Optional.of(new MethodUsage(pstart, pend, method));
            }
        }
        return Optional.empty();
    }

    private static class Holder {
        private static final MethodBoundNodeUsageCreator INSTANCE = new MethodBoundNodeUsageCreator();
    }

    public static MethodBoundNodeUsageCreator getInstance() {
        return Holder.INSTANCE;
    }

}
