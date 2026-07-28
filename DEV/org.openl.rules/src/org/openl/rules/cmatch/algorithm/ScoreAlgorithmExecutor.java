package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.MatchNode;
import org.openl.vm.IRuntimeEnv;

public class ScoreAlgorithmExecutor implements IMatchAlgorithmExecutor {

    @Override
    public Object invoke(ColumnMatch target, Object[] params, IRuntimeEnv env) {

        var checkTree = target.getCheckTree();
        var scores = target.getColumnScores();

        var sumScore = 0;
        // iterate over linearized nodes
        for (MatchNode node : checkTree.getChildren()) {
            if (!node.isLeaf()) {
                throw new IllegalArgumentException("Sub node are prohibited here.");
            }

            var arg = node.getArgument();
            var var = arg.extractValue(target, params, env);
            var matcher = node.getMatcher();

            // find all matching scores from left to right
            for (var resultIndex = 0; resultIndex < scores.length; resultIndex++) {
                var checkValue = node.getCheckValues()[resultIndex];
                if (matcher.match(var, checkValue)) {
                    var score = scores[resultIndex] * node.getWeight();
                    sumScore += score;
                    env.getTracer().put(this, "match", target, node, resultIndex, score);
                    break;
                }
            }
        }
        return sumScore;
    }
}
