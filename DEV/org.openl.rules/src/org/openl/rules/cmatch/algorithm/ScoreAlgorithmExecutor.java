package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.MatchNode;
import org.openl.rules.cmatch.matcher.IMatcher;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

public class ScoreAlgorithmExecutor implements IMatchAlgorithmExecutor {
    private static class TraceHelper {
        private ColumnMatch columnMatch;
        private ColumnMatchTraceObject traceObject;

        public TraceHelper(ColumnMatch columnMatch, Object[] params) {
            if (Tracer.isTracerDefined()) {
                this.columnMatch = columnMatch;
                traceObject = new ColumnMatchTraceObject(columnMatch, params);
                Tracer tracer = Tracer.getTracer();
                tracer.push(traceObject);
            }
        }

        public void closeMatch(int sumScore) {
            if (Tracer.isTracerDefined()) {
                traceObject.setResult(sumScore);
                Tracer tracer = Tracer.getTracer();
                tracer.pop();
            }
        }

        public void nextScore(MatchNode node, int resultIndex) {
            if (Tracer.isTracerDefined()) {
                Tracer.put(new MatchTraceObject(columnMatch, node.getRowIndex(), resultIndex));
            }
        }
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env, ColumnMatch columnMatch) {
        TraceHelper t = new TraceHelper(columnMatch, params);

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
                    t.nextScore(node, resultIndex);
                    break;
                }
            }
        }

        t.closeMatch(sumScore);
        return sumScore;
    }

}
