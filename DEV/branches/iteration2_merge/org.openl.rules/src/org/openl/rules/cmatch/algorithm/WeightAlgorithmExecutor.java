package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.MatchNode;
import org.openl.rules.cmatch.matcher.IMatcher;
import org.openl.vm.IRuntimeEnv;

public class WeightAlgorithmExecutor implements IMatchAlgorithmExecutor {
    public static final Object NO_MATCH = null;

    public Object invoke(Object target, Object[] params, IRuntimeEnv env, ColumnMatch columnMatch) {
        MatchNode checkTree = columnMatch.getCheckTree();
        Object returnValues[] = columnMatch.getReturnValues();

        int sumScore = 0;
        // iterate over linearized nodes
        for (MatchNode node : checkTree.getChildren()) {
            if (!node.isLeaf()) {
                throw new IllegalArgumentException("Sub node are prohibited here!");
            }

            Argument arg = node.getArgument();
            Object var = arg.extractValue(target, params, env);
            IMatcher matcher = node.getMatcher();

            // find all matching scores from left to right
            for (int resultIndex = 0; resultIndex < returnValues.length; resultIndex++) {
                Object checkValue = node.getCheckValues()[resultIndex];
                if (matcher.match(var, checkValue)) {
                    sumScore += columnMatch.getColumnScores()[resultIndex] * node.getWeight();
                    break;
                }
            }
        }

        MatchNode totalScore = columnMatch.getTotalScore();
        IMatcher matcher = totalScore.getMatcher();
        // totalScore -> resultValue
        for (int resultIndex = 0; resultIndex < returnValues.length; resultIndex++) {
            Object checkValue = totalScore.getCheckValues()[resultIndex];
            if (matcher.match(sumScore, checkValue)) {
                return returnValues[resultIndex];
            }
        }

        return NO_MATCH;
    }
}
