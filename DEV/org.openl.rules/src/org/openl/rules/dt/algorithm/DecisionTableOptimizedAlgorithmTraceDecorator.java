package org.openl.rules.dt.algorithm;

import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.rules.dt.index.EqualsIndex;
import org.openl.rules.dt.index.RangeIndex;
import org.openl.rules.dt.index.TraceableEqualsIndex;
import org.openl.rules.dt.index.TraceableRangeIndex;
import org.openl.rules.dtx.trace.DTConditionTraceObject;
import org.openl.rules.dtx.trace.IDecisionTableTraceObject;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.ChildTraceStack;
import org.openl.vm.trace.TraceStack;

public class DecisionTableOptimizedAlgorithmTraceDecorator extends DecisionTableOptimizedAlgorithm {
    private final DecisionTableOptimizedAlgorithm algorithmDelegate;
    private final IDecisionTableTraceObject baseTraceObject;
    private final TraceStack conditionsStack;

    public DecisionTableOptimizedAlgorithmTraceDecorator(DecisionTableOptimizedAlgorithm delegate,
            TraceStack conditionsStack, IDecisionTableTraceObject baseTraceObject, IndexInfo info) {
        super(delegate.getEvaluators(), delegate.getTable(), info, delegate.getIndexRoot());
        this.algorithmDelegate = delegate;
        this.baseTraceObject = baseTraceObject;
        this.conditionsStack = conditionsStack;
    }

    public int hashCode() {
        return algorithmDelegate.hashCode();
    }

    public boolean equals(Object obj) {
        return algorithmDelegate.equals(obj);
    }

    public String toString() {
        return algorithmDelegate.toString();
    }

    public IConditionEvaluator[] getEvaluators() {
        return algorithmDelegate.getEvaluators();
    }

    public DecisionTable getTable() {
        return algorithmDelegate.getTable();
    }


    public void removeParamValuesForIndexedConditions() {
        algorithmDelegate.removeParamValuesForIndexedConditions();
    }

    public IIntIterator checkedRules(Object target, Object[] params, IRuntimeEnv env) {
        return algorithmDelegate.checkedRules(target, params, env, new DefaultAlgorithmDecoratorFactory() {
            @Override
            public ARuleIndex create(ARuleIndex index, ICondition condition) {
                if (index instanceof RangeIndex) {
                    index = new TraceableRangeIndex((RangeIndex) index, condition, baseTraceObject, conditionsStack);
                } else if (index instanceof EqualsIndex) {
                    index = new TraceableEqualsIndex((EqualsIndex) index, condition, baseTraceObject, conditionsStack);
                }

                return index;
            }

            @Override
            public IIntSelector create(IIntSelector selector, ICondition condition) {
                return new SelectorTracer(selector, condition, baseTraceObject, new ChildTraceStack(conditionsStack));
            }
        });
    }

    private static class SelectorTracer implements IIntSelector {
        private final IDecisionTableTraceObject baseTraceObject;
        private final IIntSelector delegate;
        private final ICondition condition;
        private final TraceStack conditionsStack;

        public SelectorTracer(IIntSelector delegate, ICondition condition, IDecisionTableTraceObject baseTraceObject,
                TraceStack conditionsStack) {
            this.baseTraceObject = baseTraceObject;
            this.delegate = delegate;
            this.condition = condition;
            this.conditionsStack = conditionsStack;
        }

        @Override
        public boolean select(int rule) {
            boolean successful = delegate.select(rule);
            conditionsStack.push(new DTConditionTraceObject(baseTraceObject, condition, rule, successful));

            if (!successful) {
                conditionsStack.reset();
            }

            return successful;
        }
    }

}
