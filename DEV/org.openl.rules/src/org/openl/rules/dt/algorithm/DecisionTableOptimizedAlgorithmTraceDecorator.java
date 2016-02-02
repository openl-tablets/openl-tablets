package org.openl.rules.dt.algorithm;

import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.DecisionTableIndexedRuleNode;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.rules.dt.index.RangeIndex;
import org.openl.rules.dtx.trace.DTConditionTraceObject;
import org.openl.rules.dtx.trace.DTIndexedTraceObject;
import org.openl.rules.dtx.trace.DecisionTableTraceObject;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

import java.util.Comparator;

public class DecisionTableOptimizedAlgorithmTraceDecorator extends DecisionTableOptimizedAlgorithm {
    private final DecisionTableOptimizedAlgorithm algorithmDelegate;
    private final ExecutableRulesMethod method;

    public DecisionTableOptimizedAlgorithmTraceDecorator(DecisionTableOptimizedAlgorithm delegate,
            ExecutableRulesMethod method, IndexInfo info) {
        super(delegate.getEvaluators(), delegate.getTable(), info);
        this.algorithmDelegate = delegate;
        this.method = method;
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
            public ARuleIndex create(ARuleIndex index, final ICondition condition) {
                if (index instanceof RangeIndex) {
                    RangeIndex rIndex = (RangeIndex) index;
                    rIndex.comparator = createComparator(condition);
                }

                return index;
            }

            @Override
            public IIntSelector create(final IIntSelector selector, final ICondition condition) {
                return new IIntSelector() {
                    @Override
                    public boolean select(int rule) {
                        boolean successful = selector.select(rule);
                        Tracer.put(new DTConditionTraceObject(method, condition, rule, successful));
                        return successful;
                    }
                };
            }
        });
    }

    private Comparator<Object> createComparator(final ICondition condition) {
        return new Comparator<Object>() {
            @Override public int compare(Object o1, Object o2) {
                DecisionTableIndexedRuleNode rule;
                Object value;
                if (o1 instanceof DecisionTableIndexedRuleNode) {
                    rule = (DecisionTableIndexedRuleNode) o1;
                    value = o2;
                } else {
                    rule = (DecisionTableIndexedRuleNode) o2;
                    value = o1;
                }
                if (rule.getRulesIterator().hasNext()) {
                    // Do not trace index value that is not mapped to any rule. This can
                    // be an excluding boundary for example.
                    Tracer.put(new DTIndexedTraceObject(method, condition, rule.getRules(), false));
                }
                return rule.compareTo(value);
            }
        };
    }

}
