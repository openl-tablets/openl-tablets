package org.openl.rules.dt.algorithm.evaluator;

import java.util.List;

import org.openl.domain.IDomain;
import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.element.ConditionCasts;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.ContainsInInputArrayIndexV2;
import org.openl.rules.dt.index.IRuleIndex;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.vm.IRuntimeEnv;

/**
 * An evaluator for a condition that looks for the condition column value inside an array passed to the decision
 * table, for example {@code contains(codes, code)} where {@code codes} is a table input and {@code code} is a column
 * value.
 *
 * <p>The condition column values are indexed as single keys. Every element of the input array is looked up in that
 * index and the matching rules are merged, so the number of lookups is the length of the input array.
 *
 * @author Vladyslav Pikus
 */
public class ContainsInInputArrayIndexedEvaluator extends AEqualsIndexedEvaluator {

    private final List<ConditionParameter> values;

    public ContainsInInputArrayIndexedEvaluator(ConditionCasts conditionCasts) {
        this(conditionCasts, List.of());
    }

    /**
     * @param values the column parameters the index is built from; an empty list means the first parameter of the
     *               condition the index belongs to
     */
    public ContainsInInputArrayIndexedEvaluator(ConditionCasts conditionCasts, List<ConditionParameter> values) {
        super(conditionCasts);
        this.values = List.copyOf(values);
    }

    @Override
    public IRuleIndex makeIndex(ICondition condition, IIntIterator it) {
        if (it.size() < 1 || values.stream().anyMatch(value -> value.condition().hasFormulas())) {
            // a column with formulas has no value to index until the table is evaluated
            return null;
        }

        var builder = new ContainsInInputArrayIndexV2.Builder();
        builder.setConditionCasts(conditionCasts);
        while (it.hasNext()) {
            var ruleN = it.nextInt();
            builder.addRule(ruleN);

            if (condition.isEmpty(ruleN)) {
                builder.putEmptyRule(ruleN);
                continue;
            }

            if (values.isEmpty()) {
                putValue(builder, condition.getParamValue(0, ruleN), ruleN);
            } else {
                for (ConditionParameter value : values) {
                    putValue(builder, value.getValue(ruleN), ruleN);
                }
            }
        }

        return builder.build();
    }

    private void putValue(ContainsInInputArrayIndexV2.Builder builder, Object value, int ruleN) {
        if (value != null) {
            builder.putValueToRule(conditionCasts.castToInputType(value), ruleN);
        }
    }

    /**
     * Answers with the condition expression itself.
     *
     * <p>The evaluator is chosen only for a condition without formulas, so it is always indexed and the selector
     * is a safety net rather than a path taken during the evaluation.
     */
    @Override
    public IIntSelector getSelector(ICondition condition, Object target, Object[] dtparams, IRuntimeEnv env) {
        return new DefaultConditionSelector(condition, target, dtparams, env);
    }

    @Override
    public IOpenSourceCodeModule getFormalSourceCode(IBaseCondition condition) {
        return condition instanceof ICondition cond ? cond.getIndexSourceCodeModule() : condition.getSourceCodeModule();
    }

    /**
     * The table input is an array, so the values of the condition column do not describe it.
     *
     * @throws DomainCanNotBeDefined always
     */
    @Override
    public IDomain<?> getRuleParameterDomain(IBaseCondition condition) throws DomainCanNotBeDefined {
        throw new DomainCanNotBeDefined("The input of the condition is an array",
                getFormalSourceCode(condition).getCode());
    }

    @Override
    public int countUniqueKeys(ICondition condition, IIntIterator it) {
        return 0;
    }

    @Override
    public int getPriority() {
        return IConditionEvaluator.CONTAINS_IN_INPUT_ARRAY_CONDITION_PRIORITY_V2;
    }
}
