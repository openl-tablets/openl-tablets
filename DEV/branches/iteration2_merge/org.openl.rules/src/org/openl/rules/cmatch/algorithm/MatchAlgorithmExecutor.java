package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.MatchNode;
import org.openl.rules.cmatch.matcher.IMatcher;
import org.openl.vm.IRuntimeEnv;

public class MatchAlgorithmExecutor implements IMatchAlgorithmExecutor {

    public Object invoke(Object target, Object[] params, IRuntimeEnv env, ColumnMatch columnMatch) {
        MatchNode checkTree = columnMatch.getCheckTree();
        Object returnValues[] = columnMatch.getReturnValues();

        for (MatchNode node : checkTree.getChildren()) {
            Argument arg = node.getArgument();
            Object var = arg.extractValue(target, params, env);
            IMatcher matcher = node.getMatcher();

            for (int i = 0; i < returnValues.length; i++) {
                Object checkValue = node.getCheckValues()[i];
                if (matcher.match(var, checkValue)) {
                    // check that all children are MATCH at i-th element
                    if (childrenMatch(target, params, env, node, i)) {
                        return returnValues[i];
                    }
                }
            }
        }

        return null;
    }

    protected boolean childrenMatch(Object target, Object[] params, IRuntimeEnv env, MatchNode parent, int index) {
        for (MatchNode node : parent.getChildren()) {
            Argument arg = node.getArgument();
            Object var = arg.extractValue(target, params, env);
            IMatcher matcher = node.getMatcher();

            Object checkValue = node.getCheckValues()[index];
            if (matcher.match(var, checkValue)) {
                // check that all children are MATCH at i-th element
                if (!childrenMatch(target, params, env, node, index)) {
                    return false;
                }
            } else {
                // fail fast
                return false;
            }
        }

        // all TRUE or no children
        return true;
    }
}
