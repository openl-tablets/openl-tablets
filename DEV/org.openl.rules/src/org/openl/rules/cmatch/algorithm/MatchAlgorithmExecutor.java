package org.openl.rules.cmatch.algorithm;

import java.util.List;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.MatchNode;
import org.openl.rules.cmatch.matcher.IMatcher;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.Tracer;

public class MatchAlgorithmExecutor implements IMatchAlgorithmExecutor {
    public static final Object NO_MATCH = null;

    @Override
    public Object invoke(ColumnMatch target, Object[] params, IRuntimeEnv env) {
        MatchNode checkTree = target.getCheckTree();
        Object returnValues[] = target.getReturnValues();

        // iterate over linearized nodes
        for (MatchNode line : checkTree.getChildren()) {
            if (line.getRowIndex() >= 0) {
                throw new IllegalArgumentException("Linearized MatchNode tree expected.");
            }

            // find matching result value from left to right
            for (int resultIndex = 0; resultIndex < returnValues.length; resultIndex++) {
                boolean success = true;
                // check that all children are MATCH at resultIndex element
                List<MatchNode> children = line.getChildren();
                for (MatchNode node : children) {
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
                    Object result = returnValues[resultIndex];
                    for (MatchNode node : line.getChildren()) {
                        Tracer.put(this, "match", target, node, resultIndex, null);
                    }
                    Tracer.put(this, "result", target, resultIndex, result);
                    return result;
                }
            }
        }
        return NO_MATCH;
    }
}
