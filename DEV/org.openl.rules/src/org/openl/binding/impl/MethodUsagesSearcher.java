package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openl.base.INamedThing;
import org.openl.binding.IBoundNode;
import org.openl.binding.MethodUtil;
import org.openl.meta.IMetaInfo;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.lang.xls.types.DatatypeOpenConstructor;
import org.openl.rules.types.IUriMember;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.rules.types.impl.OverloadedMethodsDispatcherTable;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.ExecutableMethod;
import org.openl.types.impl.MethodDelegator;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenConstructor;
import org.openl.util.CollectionUtils;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

/**
 * Helps to find all used OpenL methods in compiled code by {@link IBoundNode}.
 *
 * @author PUdalau
 */
public final class MethodUsagesSearcher {

    private MethodUsagesSearcher() {
    }

    public static class MethodUsage implements NodeUsage {
        private final int startPos;
        private final int endPos;
        private final IOpenMethod method;

        public MethodUsage(int startPos, int endPos, IOpenMethod method) {
            this.startPos = startPos;
            this.endPos = endPos;
            this.method = method;
        }

        /**
         * @return the start position of the method in code.
         */
        @Override
        public int getStart() {
            return startPos;
        }

        /**
         * @return the end position of the method in code.
         */
        @Override
        public int getEnd() {
            return endPos;
        }

        public IOpenMethod getMethod() {
            return method;
        }

        private static String getTableUri(IOpenMethod method) {
            try {
                if (method instanceof IUriMember) {
                    return ((IUriMember) method).getUri();
                } else if (method instanceof OverloadedMethodsDispatcherTable) {
                    return ((OverloadedMethodsDispatcherTable) method).getDispatcherTable().getUri();
                } else if (method instanceof MatchingOpenMethodDispatcher) {
                    MatchingOpenMethodDispatcher matchingOpenMethodDispatcher = (MatchingOpenMethodDispatcher) method;
                    if (matchingOpenMethodDispatcher.getCandidates().size() == 1) {
                        return getTableUri(matchingOpenMethodDispatcher.getCandidates().get(0));
                    } else {
                        return matchingOpenMethodDispatcher.getDispatcherTable().getUri();
                    }
                } else if (method instanceof DatatypeOpenConstructor && method
                    .getDeclaringClass() instanceof DatatypeOpenClass) {
                    IMetaInfo metaInfo = method.getDeclaringClass().getMetaInfo();
                    return metaInfo == null ? null : metaInfo.getSourceUrl();
                } else if (method.getInfo() != null) {
                    return method.getInfo().getSourceUrl();
                } else {
                    return null;
                }
            } catch (Exception e) {
                return null;
            }
        }

        /**
         *
         * @return uri of the table representing used method or <code>null</code> if this method is not represented by
         *         some OpenL component.
         */
        @Override
        public String getUri() {
            return getTableUri(method);
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.RULE;
        }

        /**
         * @return String description of the method signature.
         */
        @Override
        public String getDescription() {
            StringBuilder buff = new StringBuilder();
            if (method instanceof JavaOpenConstructor && method.getDeclaringClass() instanceof JavaOpenClass) {
                buff.append(method.getDeclaringClass().getPackageName()).append('\n');
            }
            MethodUtil.printMethod(method, buff);
            return buff.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MethodUsage that = (MethodUsage) o;
            return startPos == that.startPos && endPos == that.endPos && Objects.equals(method, that.method);
        }

        @Override
        public int hashCode() {
            return Objects.hash(startPos, endPos, method);
        }
    }

    /**
     * Find all OpenL methods used in the code.
     *
     * @param boundNode Compiled OpenL expression.
     * @param sourceString Source of OpenL expression.
     * @param startIndex Position in the <code>sourceString</code> which defines start of OpenL expression.
     */
    public static List<MethodUsage> findAllMethods(IBoundNode boundNode, String sourceString, int startIndex) {
        List<MethodUsage> methods = new ArrayList<>();
        findAllMethods(boundNode, methods, sourceString, startIndex);
        return methods;
    }

    private static void findAllMethods(IBoundNode boundNode,
            List<MethodUsage> methods,
            String sourceString,
            int startIndex) {
        if (boundNode == null) {
            return;
        }
        if (boundNode instanceof MethodBoundNode) {
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
                    methods.add(new MethodUsage(pstart, pend, method));
                } else if (method instanceof JavaOpenConstructor && methodBoundNode.getSyntaxNode()
                    .getNumberOfChildren() > 0) {
                    TextInfo info = new TextInfo(sourceString);
                    // get constructor syntax node location
                    location = methodBoundNode.getSyntaxNode().getChild(0).getSourceLocation();
                    if (location != null && location.isTextLocation()) {
                        int pstart = location.getStart().getAbsolutePosition(info) + startIndex;
                        if (sourceString.indexOf(method.getDeclaringClass().getPackageName()) == pstart - 1) {
                            // shift start index if constructor calling start with packageName
                            pstart += method.getDeclaringClass().getPackageName().length() + 1;
                        }
                        int pend = pstart + method.getDeclaringClass().getDisplayName(INamedThing.SHORT).length() - 1;
                        methods.add(new MethodUsage(pstart, pend, method));
                    }
                }
            }
        }
        IBoundNode[] children = boundNode.getChildren();
        if (CollectionUtils.isNotEmpty(children)) {
            for (IBoundNode child : children) {
                findAllMethods(child, methods, sourceString, startIndex);
            }
        }
        if (boundNode instanceof ATargetBoundNode) {
            IBoundNode targetNode = boundNode.getTargetNode();
            if (targetNode != null) {
                findAllMethods(targetNode, methods, sourceString, startIndex);
            }
        }
    }

}
