package org.openl.rules.dt.algorithm.evaluator;

import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.helpers.NumberUtils;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.types.IParameterDeclaration;
import org.openl.vm.IRuntimeEnv;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public abstract class AContainsInArrayIndexedEvaluator extends AConditionEvaluator {

    private int uniqueKeysSize = -1;
    private int maxArrayLength = -1;

    @Override
    public IOpenSourceCodeModule getFormalSourceCode(IBaseCondition condition) {
        IParameterDeclaration[] cparams = condition.getParams();

        IOpenSourceCodeModule conditionSource = condition.getSourceCodeModule();

        String code = String.format("containsCtr(%1$s, %2$s)", cparams[0].getName(), conditionSource.getCode());

        return new StringSourceCodeModule(code, conditionSource.getUri());
    }

    @Override
    public IIntSelector getSelector(ICondition condition, Object target, Object[] params, IRuntimeEnv env) {
        Object value = condition.getEvaluator().invoke(target, params, env);

        return new ContainsInArraySelector(condition, value);
    }

    @Override
    public boolean isIndexed() {
        return true;
    }

    @Override
    public int countUniqueKeys(ICondition condition, IIntIterator it) {
        if (uniqueKeysSize < 0) {
            countUniqueKeysAndMaxArrayLength(condition, it);
        }
        return uniqueKeysSize;
    }

    public int getMaxArrayLength(ICondition condition, IIntIterator it) {
        if (maxArrayLength < 0) {
            countUniqueKeysAndMaxArrayLength(condition, it);
        }
        return maxArrayLength;
    }

    private void countUniqueKeysAndMaxArrayLength(ICondition condition, IIntIterator it) {
        Set<Object> uniqueVals = null;
        while (it.hasNext()) {
            int i = it.nextInt();
            if (condition.isEmpty(i)) {
                continue;
            }
            Object values = condition.getParamValue(0, i);
            int length = Array.getLength(values);
            maxArrayLength = Math.max(length, maxArrayLength);
            for (int j = 0; j < length; j++) {
                Object val = Array.get(values, j);
                if (uniqueVals == null) {
                    if (NumberUtils.isFloatPointNumber(val)) {
                        if (val instanceof BigDecimal) {
                            uniqueVals = new HashSet<>();
                        } else {
                            uniqueVals = new TreeSet<>(FloatTypeComparator.getInstance());
                        }
                    } else {
                        uniqueVals = new HashSet<>();
                    }
                }
                uniqueVals.add(val);
            }
        }
        uniqueKeysSize = uniqueVals == null ? 0 : uniqueVals.size();
    }

    @Override
    protected IDomain<Object> indexedDomain(IBaseCondition condition) {
        int len = condition.getNumberOfRules();
        ArrayList<Object> list = new ArrayList<>(len);
        HashSet<Object> set = new HashSet<>(len);

        for (int ruleN = 0; ruleN < len; ruleN++) {
            if (condition.isEmpty(ruleN)) {
                continue;
            }
            Object ary = condition.getParamValue(0, ruleN);

            int plen = Array.getLength(ary);

            for (int j = 0; j < plen; j++) {
                Object key = Array.get(ary, j);
                if (key == null || !set.add(key)) {
                    continue;
                }
                list.add(key);
            }
        }

        return new EnumDomain<>(list.toArray());
    }

}
