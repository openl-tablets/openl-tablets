package org.openl.binding.impl;

import java.util.Optional;

import org.openl.base.INamedThing;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
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
        TextInfo info = new TextInfo(sourceString);
        MethodBoundNode methodBoundNode = constructorNode.getConstructor();
        ISyntaxNode syntaxNode = methodBoundNode.getSyntaxNode();
        IOpenMethod method = methodBoundNode.getMethodCaller().getMethod();
        ILocation location = syntaxNode.getSourceLocation();
        int pstart = startIndex + location.getStart().getAbsolutePosition(info);
        int pend = pstart;
        if (sourceString.indexOf(method.getDeclaringClass().getPackageName()) == pstart) {
            // shift end index if constructor calling start with packageName
            pend += method.getDeclaringClass().getPackageName().length() + 1; // +1 - is a dot
        }
        pend += method.getDeclaringClass().getDisplayName(INamedThing.SHORT).length();
        return Optional.of(new ConstructorUsage(constructorNode, pstart, pend, method));
    }

    private static class Holder {
        private static final ConstructorNodeCreator INSTANCE = new ConstructorNodeCreator();
    }

    public static ConstructorNodeCreator getInstance() {
        return Holder.INSTANCE;
    }
}
