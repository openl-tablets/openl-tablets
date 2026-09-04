package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.vm.IRuntimeEnv;

public class WeightAlgorithmExecutor implements IMatchAlgorithmExecutor {

    public static final Object NO_MATCH = null;
    private final ScoreAlgorithmExecutor scoreAlgorithmExecutor = new ScoreAlgorithmExecutor();

    @Override
    public Object invoke(ColumnMatch target, Object[] params, IRuntimeEnv env) {
        var sumScore = env.getTracer().invoke(scoreAlgorithmExecutor, target, params, env, this);

        var totalScore = target.getTotalScore();
        var matcher = totalScore.getMatcher();
        // totalScore -> resultValue
        var returnValues = target.getReturnValues();
        for (var resultIndex = 0; resultIndex < returnValues.length; resultIndex++) {
            var checkValue = totalScore.getCheckValues()[resultIndex];
            if (matcher.match(sumScore, checkValue)) {
                var result = returnValues[resultIndex];

                env.getTracer().put(this, "match", target, totalScore, resultIndex, null);
                env.getTracer().put(this, "result", target, resultIndex, result);
                return result;
            }
        }

        return NO_MATCH;
    }
}
