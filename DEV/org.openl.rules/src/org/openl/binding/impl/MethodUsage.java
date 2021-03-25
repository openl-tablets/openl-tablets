package org.openl.binding.impl;

import java.util.Objects;

import org.openl.binding.MethodUtil;
import org.openl.meta.IMetaInfo;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.lang.xls.types.DatatypeOpenConstructor;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.rules.types.impl.OverloadedMethodsDispatcherTable;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenConstructor;

public class MethodUsage implements NodeUsage {
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
            if (method instanceof ExecutableRulesMethod) {
                return ((ExecutableRulesMethod) method).getSyntaxNode().getUri();
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
