package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.MatchNode;
import org.openl.rules.cmatch.matcher.IMatcher;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

public class WeightAlgorithmExecutor implements IMatchAlgorithmExecutor {

    public static final Object NO_MATCH = null;
    private ScoreAlgorithmExecutor scoreAlgorithmExecutor = new ScoreAlgorithmExecutor();

    public Object invoke(Object target, Object[] params, IRuntimeEnv env, ColumnMatch columnMatch) {
        WScoreTraceObject wScore = new WScoreTraceObject(columnMatch, params);
        // score
        Integer sumScore;

        Tracer.begin(wScore);
        try {
            sumScore = (Integer) scoreAlgorithmExecutor.invoke(target, params, env, columnMatch);
            wScore.setResult(sumScore);
        } finally {
            Tracer.end();
        }

        MatchNode totalScore = columnMatch.getTotalScore();
        IMatcher matcher = totalScore.getMatcher();
        // totalScore -> resultValue
        Object[] returnValues = columnMatch.getReturnValues();
        for (int resultIndex = 0; resultIndex < returnValues.length; resultIndex++) {
            Object checkValue = totalScore.getCheckValues()[resultIndex];
            if (matcher.match(sumScore, checkValue)) {
                Object result = returnValues[resultIndex];

                Tracer.put(this, "match", columnMatch, totalScore, resultIndex, null);
                Tracer.put(this, "result", columnMatch, resultIndex, result);
                return result;
            }
        }

        return NO_MATCH;
    }
}
