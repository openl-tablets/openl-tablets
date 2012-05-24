package org.openl.rules.dt.algorithm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.rules.dt.trace.DTConditionTraceObject;
import org.openl.rules.dt.trace.DTIndexedTraceObject;
import org.openl.rules.dt.trace.DecisionTableTraceObject;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.ITracerObject;
import org.openl.vm.trace.Tracer;

public class DecisionTableOptimizedAlgorithmTraceDecorator extends DecisionTableOptimizedAlgorithm {
    private final DecisionTableOptimizedAlgorithm algorithmDelegate;
    private final DecisionTableTraceObject baseTraceObject;
    private final Tracer tracer;
    private int pushedTraces = 0;
    
    private final List<ICondition> indexedConditions = new ArrayList<ICondition>();
    private final Set<Integer> tracedRules = new HashSet<Integer>();

    public DecisionTableOptimizedAlgorithmTraceDecorator(DecisionTableOptimizedAlgorithm delegate, DecisionTableTraceObject baseTraceObject) {
        super(delegate.getEvaluators(), delegate.getTable());
        this.algorithmDelegate = delegate;
        this.baseTraceObject = baseTraceObject;
        this.tracer = Tracer.getTracer();
    }
    
    public void popAll() {
        while (pushedTraces > 0) {
            pop();
        }
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

    public void buildIndex() throws Exception {
        algorithmDelegate.buildIndex();
    }

    public void removeParamValuesForIndexedConditions() {
        algorithmDelegate.removeParamValuesForIndexedConditions();
    }

    public IIntIterator checkedRules(Object target, Object[] params, IRuntimeEnv env) {
        return algorithmDelegate.checkedRules(target, params, env, new DefaultAlgorithmDecoratorFactory() {

            @Override
            public ARuleIndex create(ARuleIndex index, ICondition condition) {
                indexedConditions.add(condition);
                return index;
            }

            @Override
            public IIntSelector create(IIntSelector selector, ICondition condition) {
                return new SelectorTracer(selector, condition);
            }
        });
    }
    
    private void push(ITracerObject tracerObject) {
        tracer.push(tracerObject);
        pushedTraces++;
    }
    
    private void pop() {
        tracer.pop();
        pushedTraces--;
    }

    private class SelectorTracer implements IIntSelector {
        private final IIntSelector delegate;
        private final ICondition condition;

        public SelectorTracer(IIntSelector delegate, ICondition condition) {
            this.delegate = delegate;
            this.condition = condition;
        }

        @Override
        public boolean select(int rule) {
            traceIndexedConditions(rule);
            
            boolean successful = delegate.select(rule);
            push(new DTConditionTraceObject(baseTraceObject, condition, rule, successful));
            
            if (!successful) {
                popAll();
            }
            
            return successful;
        }

        private void traceIndexedConditions(int rule) {
            if (!tracedRules.contains(rule)) {
                for (ICondition indexedCondition : indexedConditions) {
                    push(new DTIndexedTraceObject(baseTraceObject, indexedCondition, rule));
                }
                tracedRules.add(rule);
            }
        }

    }
}
