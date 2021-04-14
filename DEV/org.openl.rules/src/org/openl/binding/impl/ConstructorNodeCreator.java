package org.openl.binding.impl;

import java.util.Optional;

import org.openl.base.INamedThing;
import org.openl.binding.IBoundNode;
import org.openl.types.IOpenMethod;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

/**
 * Converts {@link ConstructorNamedParamsNode} or {@link ConstructorNamedParamsNode} to type {@link ConstructorUsage}
 *
 * @author Eugene Biruk
 */
public class ConstructorNodeCreator implements NodeUsageCreator {

    private ConstructorNodeCreator() {
    }

    @Override
    public boolean accept(IBoundNode boundNode) {
        return boundNode instanceof ConstructorNode;
    }

    @Override
    public Optional<NodeUsage> create(IBoundNode boundNode, String sourceString, int startIndex) {
        ConstructorNode constructorNode = (ConstructorNode) boundNode;
        MethodBoundNode methodBoundNode = constructorNode.getConstructor();
        if (constructorNode.isShort()) {
            IOpenMethod method = methodBoundNode.getMethodCaller().getMethod();
            TextInfo info = new TextInfo(sourceString);
            // get constructor syntax node location
            ILocation location = methodBoundNode.getSyntaxNode().getChild(0).getSourceLocation();
            int pstart = location.getStart().getAbsolutePosition(info) + startIndex;
            if (sourceString.indexOf(method.getDeclaringClass().getPackageName()) == pstart - 1) {
                // shift start index if constructor calling start with packageName
                pstart += method.getDeclaringClass().getPackageName().length() + 1;
            }
            int pend = pstart + method.getDeclaringClass().getDisplayName(INamedThing.SHORT).length() - 1;
            return Optional.of(new ConstructorUsage(constructorNode, pstart, pend, method));
        } else {
            ILocation location = constructorNode.isShort() ? methodBoundNode.getSyntaxNode().getChild(0).getSourceLocation() : methodBoundNode.getSyntaxNode().getSourceLocation();
            TextInfo info = new TextInfo(sourceString);
            IOpenMethod method = methodBoundNode.getMethodCaller().getMethod();
            int pstart = location.getStart().getAbsolutePosition(info) + startIndex;
            int pend = pstart + method.getName().length() - 2;
            return Optional.of(new ConstructorUsage(constructorNode, pstart, pend, method));
        }
    }

    private static class Holder {
        private static final ConstructorNodeCreator INSTANCE = new ConstructorNodeCreator();
    }

    public static ConstructorNodeCreator getInstance() {
        return ConstructorNodeCreator.Holder.INSTANCE;
    }
}
