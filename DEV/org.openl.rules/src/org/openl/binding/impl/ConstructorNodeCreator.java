package org.openl.binding.impl;

import java.util.Optional;

import org.openl.binding.IBoundNode;
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
        var constructorNode = (ConstructorNode) boundNode;
        var info = new TextInfo(sourceString);
        var methodBoundNode = constructorNode.getConstructor();
        var syntaxNode = methodBoundNode.getSyntaxNode();
        var method = methodBoundNode.getMethodCaller().getMethod();
        var location = syntaxNode.getSourceLocation();
        var pstart = location.getStart().getAbsolutePosition(info);
        var pend = pstart;
        while (pend < sourceString.length() && sourceString.charAt(pend) != '(') {
            pend++;
        }
        while (pend >= pstart && sourceString.charAt(pend) == ' ') {
            pend--;
        }
        var s = sourceString.substring(pstart);
        if (s.startsWith("new ")) {
            pstart += 4;
        }
        while (pstart < pend && sourceString.charAt(pstart) == ' ') {
            pstart++;
        }
        return Optional.of(new ConstructorUsage(constructorNode, pstart + startIndex, pend + startIndex, method));
    }

    private static class Holder {
        private static final ConstructorNodeCreator INSTANCE = new ConstructorNodeCreator();
    }

    public static ConstructorNodeCreator getInstance() {
        return Holder.INSTANCE;
    }
}
