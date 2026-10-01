package org.openl.rules.dt.algorithm.evaluator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.openl.domain.IDomain;
import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.domain.IntRangeDomain;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.element.ConditionCasts;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.type.IRangeAdaptor;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.vm.IRuntimeEnv;

public abstract class ARangeIndexEvaluator extends AConditionEvaluator implements IConditionEvaluator {

    final IRangeAdaptor<Object, ? extends Comparable<Object>> rangeAdaptor;
    final int nparams;

    ARangeIndexEvaluator(IRangeAdaptor<Object, ? extends Comparable<Object>> rangeAdaptor,
                         int nparams,
                         ConditionCasts conditionCasts) {
        super(conditionCasts);
        this.rangeAdaptor = rangeAdaptor;
        this.nparams = nparams;
    }

    @Override
    public IOpenSourceCodeModule getFormalSourceCode(IBaseCondition condition) {
        if (rangeAdaptor != null && rangeAdaptor.useOriginalSource()) {
            return condition.getSourceCodeModule();
        }

        var params = condition.getParams();
        var conditionSource = condition.getSourceCodeModule();

        String code = params.length == 2 ? "%1$s<=(%2$s) && (%2$s) < %3$s".formatted(
                params[0].getName(),
                conditionSource.getCode(),
                params[1].getName()) : "%1$s.contains(%2$s)".formatted(params[0].getName(), conditionSource.getCode());
        return new StringSourceCodeModule(code, conditionSource.getUri());
    }

    @Override
    @SuppressWarnings("unchecked")
    public IIntSelector getSelector(ICondition condition, Object target, Object[] dtparams, IRuntimeEnv env) {
        var value = conditionCasts.castToConditionType(condition.getEvaluator().invoke(target, dtparams, env));
        return new RangeSelector(condition, value, target, dtparams, rangeAdaptor, env);
    }

    @Override
    protected IDomain<?> indexedDomain(IBaseCondition condition) throws DomainCanNotBeDefined {
        var min = Long.MAX_VALUE;
        var max = Long.MIN_VALUE;

        var nRules = condition.getNumberOfRules();
        for (var ruleN = 0; ruleN < nRules; ruleN++) {
            if (condition.isEmpty(ruleN)) {
                continue;
            }

            Comparable<?> vFrom;
            Comparable<?> vTo;
            if (nparams == 2) {
                vFrom = rangeFrom(condition.getParamValue(0, ruleN));
                vTo = rangeTo(condition.getParamValue(1, ruleN));
            } else {
                var range = condition.getParamValue(0, ruleN);
                vFrom = rangeFrom(range);
                vTo = rangeTo(range);
            }

            if (!(vFrom instanceof Long)) {
                throw new DomainCanNotBeDefined("Domain cannot be converted to Long", null);
            }

            min = Math.min(min, (Long) vFrom);
            max = Math.max(max, (Long) vTo - 1);
        }
        min = min < Integer.MIN_VALUE ? Integer.MIN_VALUE : min;
        min = min >= Integer.MAX_VALUE ? (Integer.MAX_VALUE - 1) : min;
        max = max < Integer.MIN_VALUE ? Integer.MIN_VALUE : max;
        max = max >= Integer.MAX_VALUE ? (Integer.MAX_VALUE - 1) : max;

        return new IntRangeDomain((int) min, (int) max);
    }

    private Comparable<?> rangeFrom(Object value) {
        return rangeAdaptor == null ? (Comparable<?>) value : rangeAdaptor.getMin(value);
    }

    private Comparable<?> rangeTo(Object value) {
        return rangeAdaptor == null ? (Comparable<?>) value : rangeAdaptor.getMax(value);
    }

    List<IndexNode> mergeRulesByValue(List<IndexNode> nodes) {
        nodes.sort(IndexNode.BY_VALUE);
        final var length = nodes.size();
        var builder = new DecisionTableRuleNodeBuilder();
        var result = new ArrayList<IndexNode>();
        for (var i = 0; i < length; i++) {
            var node = nodes.get(i);
            builder.addRule(node.getRuleN());
            if (i == length - 1 || IndexNode.BY_VALUE.compare(node, nodes.get(i + 1)) != 0) {
                result.add(new IndexNode(node.getValue(), builder.makeRulesAry()));
                builder = new DecisionTableRuleNodeBuilder();
            }
        }
        return result;
    }

    @Override
    public boolean isIndexed() {
        return true;
    }

    @Override
    public int countUniqueKeys(ICondition condition, IIntIterator it) {
        return 0;
    }

    @Override
    public int getPriority() {
        return IConditionEvaluator.RANGE_CONDITION_PRIORITY;
    }

    /**
     * Converts a value to an index node to search the index for.
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    protected static class RangeIndexNodeAdaptor implements Function<Object, IndexNode> {
        private final IRangeAdaptor<Object, ? extends Comparable<Object>> rangeAdaptor;

        @SuppressWarnings("unchecked")
        @Override
        public IndexNode apply(Object value) {
            if (value == null) {
                throw new IllegalArgumentException("Null values is not supported.");
            }
            if (rangeAdaptor != null) {
                value = rangeAdaptor.adaptValueType(value);
            }
            return new IndexNode((Comparable<Object>) value);
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    public static class IndexNode {
        /**
         * Orders the index nodes by their values. A node without a value goes first.
         */
        public static final Comparator<IndexNode> BY_VALUE = Comparator.comparing(IndexNode::getValue,
                Comparator.nullsFirst(Comparator.naturalOrder()));

        @Getter
        private final Comparable<Object> value;
        @Getter
        private int[] rules;
        @Getter(AccessLevel.PACKAGE)
        private int ruleN;

        IndexNode(Comparable<Object> value, int ruleN) {
            this.value = value;
            this.ruleN = ruleN;
        }

        IndexNode(Comparable<Object> value, int[] rules) {
            this.value = value;
            this.rules = rules;
        }
    }
}
