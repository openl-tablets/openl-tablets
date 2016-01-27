package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.MatchNode;
import org.openl.rules.cmatch.matcher.IMatcher;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

public class WeightAlgorithmExecutor implements IMatchAlgorithmExecutor {

    public static final Object NO_MATCH = null;

    public Object invoke(Object target, Object[] params, IRuntimeEnv env, ColumnMatch columnMatch) {
        WScoreTraceObject wScore = new WScoreTraceObject(columnMatch, params);
        // score
        Tracer.begin(wScore);

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
                    int score = columnMatch.getColumnScores()[resultIndex] * node.getWeight();
                    sumScore += score;
                    wScore.setResult(sumScore);
                    MatchTraceObject mto = new MatchTraceObject(columnMatch, node.getRowIndex(), resultIndex);
                    mto.setResult(score);
                    Tracer.put(mto);
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
                // score
                Tracer.end();

                Tracer.put(new MatchTraceObject(columnMatch, 1, resultIndex));

                Tracer.put(new ResultTraceObject(columnMatch, resultIndex));

                return returnValues[resultIndex];
            }
        }

        // score
        Tracer.end();
        return NO_MATCH;
    }
}
