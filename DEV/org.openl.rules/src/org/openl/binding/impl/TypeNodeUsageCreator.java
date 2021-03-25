package org.openl.binding.impl;

import java.lang.reflect.Modifier;
import java.util.Optional;

import org.openl.base.INamedThing;
import org.openl.binding.IBoundNode;
import org.openl.binding.MethodUtil;
import org.openl.meta.IMetaInfo;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.OpenClassUtils;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

/**
 * Converts {@link ArrayBoundNode}, {@link CastNode} or {@link ArrayInitializerNode} to type {@link SimpleNodeUsage}
 *
 * @author Vladyslav Pikus
 */
final class TypeNodeUsageCreator implements NodeUsageCreator {

    private TypeNodeUsageCreator() {
    }

    @Override
    public boolean accept(IBoundNode boundNode) {
        return boundNode instanceof ArrayBoundNode || boundNode instanceof ArrayInitializerNode
                || (boundNode instanceof CastNode && "type.cast".equals(boundNode.getSyntaxNode().getType()));
    }

    @Override
    public Optional<NodeUsage> create(IBoundNode boundNode, String sourceString, int startIndex) {
        ISyntaxNode syntaxNode = boundNode.getSyntaxNode();
        if (boundNode instanceof ArrayInitializerNode) {
            syntaxNode = syntaxNode.getParent();
        }
        IdentifierNode identifierNode = getIdentifierNode(syntaxNode);
        IOpenClass type = boundNode.getType();
        if (type == null || identifierNode == null || !"type.name".equals(identifierNode.getType())) {
            return Optional.empty();
        }
        ILocation location = identifierNode.getSourceLocation();
        if (location == null || !location.isTextLocation()) {
            return Optional.empty();
        }
        IOpenClass componentOpenClass = OpenClassUtils.getRootComponentClass(type);
        TextInfo textInfo = new TextInfo(sourceString);
        int pstart = location.getStart().getAbsolutePosition(textInfo) + startIndex;
        int pend = location.getEnd().getAbsolutePosition(textInfo) + startIndex;
        if (componentOpenClass instanceof JavaOpenClass) {
            StringBuilder description = new StringBuilder(componentOpenClass.getPackageName()).append('\n');
            printClassDeclaration(description, componentOpenClass.getInstanceClass());
            description.append(MethodUtil.printType(componentOpenClass));
            return Optional.of(new SimpleNodeUsage(pstart, pend, description.toString(), null, NodeType.OTHER));
        } else {
            IMetaInfo typeMeta = componentOpenClass.getMetaInfo();
            if (typeMeta == null) {
                return Optional.empty();
            }
            return Optional.of(new SimpleNodeUsage(pstart,
                pend,
                typeMeta.getDisplayName(INamedThing.SHORT),
                typeMeta.getSourceUrl(),
                NodeType.DATATYPE));
        }
    }

    private static void printClassDeclaration(StringBuilder builder, Class<?> cl) {
        if (cl.isEnum()) {
            builder.append("enum");
        } else if (cl.isInterface()) {
            if (cl.isAnnotation()) {
                builder.append('@');
            }
            builder.append("interface");
        } else {
            if (Modifier.isAbstract(cl.getModifiers())) {
                builder.append("abstract ");
            }
            builder.append("class");
        }
        builder.append(' ');
    }

    static IdentifierNode getIdentifierNode(ISyntaxNode syntaxNode) {
        ISyntaxNode res = syntaxNode;
        while (res.getNumberOfChildren() > 0 && !(res instanceof IdentifierNode)) {
            res = res.getChild(0);
        }
        return res instanceof IdentifierNode ? (IdentifierNode) res : null;
    }

    private static class Holder {
        private static final TypeNodeUsageCreator INSTANCE = new TypeNodeUsageCreator();
    }

    public static TypeNodeUsageCreator getInstance() {
        return Holder.INSTANCE;
    }
}
