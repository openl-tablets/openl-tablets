package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.MatchNode;
import org.openl.rules.cmatch.matcher.IMatcher;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

public class WeightAlgorithmExecutor implements IMatchAlgorithmExecutor {
    private static class TraceHelper {
        private ColumnMatch columnMatch;
        private WColumnMatchTraceObject traceObject;
        private WScoreTraceObject wScore;

        public TraceHelper(ColumnMatch columnMatch, Object[] params) {
            if (Tracer.isTracerDefined()) {
                this.columnMatch = columnMatch;
                traceObject = new WColumnMatchTraceObject(columnMatch, params);
                // wcm
                Tracer tracer = Tracer.getTracer();
                tracer.push(traceObject);

                wScore = new WScoreTraceObject(columnMatch, params);
                // score
                tracer.push(wScore);
            }
        }

        public void closeMatch(int resultIndex) {
            if (Tracer.isTracerDefined()) {
                Tracer tracer = Tracer.getTracer();
                // score
                tracer.pop();

                Tracer.put(new MatchTraceObject(columnMatch, 1, resultIndex));

                Tracer.put(new ResultTraceObject(columnMatch, resultIndex));

                traceObject.setResult(columnMatch.getReturnValues()[resultIndex]);
                // wcm
                tracer.pop();
            }
        }

        public void closeNoMatch() {
            if (Tracer.isTracerDefined()) {
                // score
                Tracer tracer = Tracer.getTracer();
                tracer.pop();
                // wcm
                traceObject.setResult(NO_MATCH);
                tracer.pop();
            }
        }

        public void nextScore(MatchNode node, int resultIndex, int sumScore) {
            if (Tracer.isTracerDefined()) {
                wScore.setScore(sumScore);
                Tracer.put(new MatchTraceObject(columnMatch, node.getRowIndex(), resultIndex));
            }
        }
    }

    public static final Object NO_MATCH = null;

    public Object invoke(Object target, Object[] params, IRuntimeEnv env, ColumnMatch columnMatch) {
        TraceHelper t = new TraceHelper(columnMatch, params);

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
                    t.nextScore(node, resultIndex, sumScore);
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
                t.closeMatch(resultIndex);
                return returnValues[resultIndex];
            }
        }

        t.closeNoMatch();
        return NO_MATCH;
    }
}
