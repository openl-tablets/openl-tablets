package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.MatchNode;
import org.openl.rules.cmatch.matcher.IMatcher;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

public class MatchAlgorithmExecutor implements IMatchAlgorithmExecutor {
    public static final Object NO_MATCH = null;

    public Object invoke(Object target, Object[] params, IRuntimeEnv env, ColumnMatch columnMatch) {
        MatchNode checkTree = columnMatch.getCheckTree();
        Object returnValues[] = columnMatch.getReturnValues();

        // iterate over linearized nodes
        for (MatchNode line : checkTree.getChildren()) {
            if (line.getRowIndex() >= 0) {
                throw new IllegalArgumentException("Linearized MatchNode tree expected!");
            }

            // find matching result value from left to right
            for (int resultIndex = 0; resultIndex < returnValues.length; resultIndex++) {
                boolean success = true;
                // check that all children are MATCH at resultIndex element
                for (MatchNode node : line.getChildren()) {
                    Argument arg = node.getArgument();
                    Object var = arg.extractValue(target, params, env);
                    IMatcher matcher = node.getMatcher();
                    Object checkValue = node.getCheckValues()[resultIndex];
                    if (!matcher.match(var, checkValue)) {
                        success = false;
                        break;
                    }
                }

                if (success) {
                    for (MatchNode node : line.getChildren()) {
                        Tracer.put(new MatchTraceObject(columnMatch, node.getRowIndex(), resultIndex));
                    }
                    Object result = returnValues[resultIndex];
                    MatchUtil.trace(columnMatch, resultIndex, result);
                    return result;
                }
            }
        }
        return NO_MATCH;
    }
}
