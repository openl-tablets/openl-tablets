package org.openl.binding.impl;

import java.util.Optional;

import org.openl.binding.IBoundNode;
import org.openl.types.IOpenMethod;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

public class ShortConstructorNodeCreator implements NodeUsageCreator {

    private ShortConstructorNodeCreator() {
    }

    @Override
    public boolean accept(IBoundNode boundNode) {
        return boundNode instanceof ShortConstructor;
    }

    @Override
    public Optional<NodeUsage> create(IBoundNode boundNode, String sourceString, int startIndex) {
        MethodBoundNode methodBoundNode =((ShortConstructor) boundNode).getConstructor();
        ILocation location = methodBoundNode.getSyntaxNode().getSourceLocation();
        TextInfo info = new TextInfo(sourceString);
        IOpenMethod method = methodBoundNode.getMethodCaller().getMethod();
        int pstart = location.getStart().getAbsolutePosition(info) + startIndex;
        int pend = pstart + method.getName().length() - 1;
        return Optional.of(new MethodUsage(pstart, pend, method));
    }

    private static class Holder {
        private static final ShortConstructorNodeCreator INSTANCE = new ShortConstructorNodeCreator();
    }

    public static ShortConstructorNodeCreator getInstance() {
        return ShortConstructorNodeCreator.Holder.INSTANCE;
    }
}
