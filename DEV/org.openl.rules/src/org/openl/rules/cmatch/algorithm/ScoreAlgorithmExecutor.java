package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.MatchNode;
import org.openl.rules.cmatch.matcher.IMatcher;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

public class ScoreAlgorithmExecutor implements IMatchAlgorithmExecutor {

    public Object invoke(Object target, Object[] params, IRuntimeEnv env, ColumnMatch columnMatch) {

        MatchNode checkTree = columnMatch.getCheckTree();
        int[] scores = columnMatch.getColumnScores();

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
            for (int resultIndex = 0; resultIndex < scores.length; resultIndex++) {
                Object checkValue = node.getCheckValues()[resultIndex];
                if (matcher.match(var, checkValue)) {
                    int score = scores[resultIndex] * node.getWeight();
                    sumScore += score;
                    Tracer.put(this, "match", columnMatch, node, resultIndex, score);
                    break;
                }
            }
        }
        return sumScore;
    }
}
