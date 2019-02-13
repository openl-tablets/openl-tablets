package org.openl.rules.dt.algorithm.evaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.openl.domain.IDomain;
import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.rules.dt.DecisionTableIndexedRuleNode;
import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.rules.dt.index.RangeIndex;
import org.openl.rules.dt.type.IRangeAdaptor;
import org.openl.rules.helpers.IntRange;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.types.IParameterDeclaration;
import org.openl.util.IOpenIterator;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 */
public class RangeIndexedEvaluator extends AConditionEvaluator implements IConditionEvaluator {

    private IRangeAdaptor<Object, ? extends Comparable<Object>> rangeAdaptor;

    int nparams; // 1 or 2

    public RangeIndexedEvaluator(IRangeAdaptor<Object, ? extends Comparable<Object>> adaptor, int nparams) {
        this.rangeAdaptor = adaptor;
        this.nparams = nparams;
    }

    public IOpenSourceCodeModule getFormalSourceCode(IBaseCondition condition) {
        if (rangeAdaptor != null && rangeAdaptor.useOriginalSource())
            return condition.getSourceCodeModule();

        IParameterDeclaration[] cparams = condition.getParams();

        IOpenSourceCodeModule conditionSource = condition.getSourceCodeModule();

        String code = cparams.length == 2 ? String.format("%1$s<=(%2$s) && (%2$s) < %3$s",
            cparams[0].getName(),
            conditionSource.getCode(),
            cparams[1].getName())
                                          : String.format("%1$s.contains(%2$s)",
                                              cparams[0].getName(),
                                              conditionSource.getCode());

        return new StringSourceCodeModule(code, conditionSource.getUri());
    }

    public IIntSelector getSelector(ICondition condition, Object target, Object[] dtparams, IRuntimeEnv env) {
        Object value = condition.getEvaluator().invoke(target, dtparams, env);

        return new RangeSelector(condition, value, target, dtparams, rangeAdaptor, env);
    }

    public boolean isIndexed() {
        return true;
    }

    @SuppressWarnings("unchecked")
    public ARuleIndex makeIndex(ICondition condition, IIntIterator iterator) {

        if (iterator.size() < 1) {
            return null;
        }

        int size = iterator.size();
        List<Point> points;
        if (size != IOpenIterator.UNKNOWN_SIZE) {
            points = new ArrayList<>(size);
        } else {
            points = new ArrayList<>();
        }
        DecisionTableRuleNodeBuilder emptyBuilder = new DecisionTableRuleNodeBuilder();
        while (iterator.hasNext()) {

            int i = iterator.nextInt();

            if (condition.isEmpty(i)) {
                emptyBuilder.addRule(i);
                continue;
            }

            Comparable<Object> vFrom;
            Comparable<Object> vTo;

            if (nparams == 2) {
                if (rangeAdaptor == null) {
                    vFrom = (Comparable<Object>) condition.getParamValue(0, i);
                    vTo = (Comparable<Object>) condition.getParamValue(1, i);
                } else {
                    vFrom = rangeAdaptor.getMin(condition.getParamValue(0, i));
                    vTo = rangeAdaptor.getMax(condition.getParamValue(1, i));
                }
            } else {
                // adapt border values for usage in IntervalMap
                // see IntervalMap description
                //
                if (rangeAdaptor == null) {
                    vFrom = (Comparable<Object>) condition.getParamValue(0, i);
                    vTo = (Comparable<Object>) condition.getParamValue(0, i);
                } else {
                    vFrom = rangeAdaptor.getMin(condition.getParamValue(0, i));
                    vTo = rangeAdaptor.getMax(condition.getParamValue(0, i));
                }
            }
            Point vFromPoint = new Point();
            vFromPoint.v = vFrom;
            vFromPoint.isToPoint = false;
            vFromPoint.value = i;
            vFromPoint.isPositiveInfinity = false;
            Point vToPoint = new Point();
            vToPoint.v = vTo;
            vToPoint.isToPoint = true;
            vToPoint.value = i;
            vToPoint.isPositiveInfinity = true;
            points.add(vToPoint);
            points.add(vFromPoint);
        }

        Collections.sort(points);
        List<DecisionTableIndexedRuleNode<?>> index = new ArrayList<>();
        DecisionTableRuleNode emptyNode = emptyBuilder.makeNode();

        // for each indexed value create a DecisionTableRuleNode with indexes of
        // rules,
        // that match given value
        //
        int length = points.size();
        SortedSet<Integer> values = new TreeSet<>();

        for (int i = 0; i < length; i++) {
            Point intervalPoint = points.get(i);
            if (!intervalPoint.isToPoint) {
                values.add(intervalPoint.value);
            } else {
                values.remove(intervalPoint.value);
            }
            if (i == length - 1 || intervalPoint.compareTo(points.get(i + 1)) != 0) {
                int[] rulesIndexesArray = collectionToPrimitiveArray(values);
                index.add(
                    new DecisionTableIndexedRuleNode<>(emptyNode.getRules(), rulesIndexesArray, intervalPoint));
            }
        }
        return new RangeIndex(emptyNode, index, new PointRangeAdaptor(rangeAdaptor));
    }

    @Override
    public int countUniqueKeys(ICondition condition, IIntIterator it) {
        return 0;
    }

    static class PointRangeAdaptor implements IRangeAdaptor<Point, Comparable<?>> {
        IRangeAdaptor<Object, ? extends Comparable<Object>> rangeAdaptor;

        PointRangeAdaptor(IRangeAdaptor<Object, ? extends Comparable<Object>> rangeAdaptor) {
            this.rangeAdaptor = rangeAdaptor;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Comparable<?> adaptValueType(Object value) {
            if (value == null) {
                throw new IllegalArgumentException("Null values doesn't supported!");
            }
            if (rangeAdaptor != null) {
                value = rangeAdaptor.adaptValueType(value);
            }
            Point point = new Point();
            point.v = (Comparable<Object>) value;
            return point;
        }

        @Override
        public Comparable<Object> getMax(Point param) {
            throw new UnsupportedOperationException("Operation not supported!");
        }

        @Override
        public Comparable<Object> getMin(Point param) {
            throw new UnsupportedOperationException("Operation not supported!");
        }

        @Override
        public boolean useOriginalSource() {
            throw new UnsupportedOperationException("Operation not supported!");
        }

        @Override
        public Class<?> getIndexType() {
            // if (rangeAdaptor != null)
            // return rangeAdaptor.getIndexType();
            throw new UnsupportedOperationException("getIndexType fpr empty rangeAdaptors");
        }
    }

    private int[] collectionToPrimitiveArray(Collection<Integer> rulesIndexesCollection) {
        if (rulesIndexesCollection.isEmpty()) {
            return DecisionTableRuleNode.ZERO_ARRAY;
        }
        int[] rulesIndexesArray = new int[rulesIndexesCollection.size()];
        int i = 0;
        for (Integer value : rulesIndexesCollection) {
            rulesIndexesArray[i] = value;
            i++;
        }
        return rulesIndexesArray;
    }

    protected IDomain<? extends Object> indexedDomain(IBaseCondition condition) throws DomainCanNotBeDefined {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        int nRules = condition.getNumberOfRules();
        for (int ruleN = 0; ruleN < nRules; ruleN++) {
            if (condition.isEmpty(ruleN))
                continue;

            Comparable<?> vFrom;
            Comparable<?> vTo;

            if (nparams == 2) {
                if (rangeAdaptor == null) {
                    vFrom = (Comparable<?>) condition.getParamValue(0, ruleN);
                    vTo = (Comparable<?>) condition.getParamValue(1, ruleN);
                } else {
                    vFrom = rangeAdaptor.getMin(condition.getParamValue(0, ruleN));
                    vTo = rangeAdaptor.getMax(condition.getParamValue(1, ruleN));
                }

            } else {
                Object range = condition.getParamValue(0, ruleN);
                vFrom = rangeAdaptor.getMin(range);
                vTo = rangeAdaptor.getMax(range);
            }

            if (!(vFrom instanceof Integer)) {
                throw new DomainCanNotBeDefined("Domain can't be converted to Integer", null);
            }

            min = Math.min(min, (Integer) vFrom);
            max = Math.max(max, (Integer) vTo - 1);
        }
        return new IntRange(min, max);
    }

    private final static class Point implements Comparable<Point> {
        private Comparable<Object> v;
        private boolean isPositiveInfinity = true;
        private int value;
        private boolean isToPoint;

        @Override
        public int compareTo(Point o) {
            if (this.v == null && o.v == null) {
                if (this.isPositiveInfinity == o.isPositiveInfinity) {
                    return 0;
                } else {
                    if (this.isPositiveInfinity) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            }
            if (this.v == null) {
                if (this.isPositiveInfinity) {
                    return 1;
                } else {
                    return -1;
                }
            }

            if (o.v == null) {
                if (o.isPositiveInfinity) {
                    return -1;
                } else {
                    return 1;
                }
            }

            return this.v.compareTo(o.v);
        }
    }

    public IRangeAdaptor<Object, ? extends Comparable<Object>> getRangeAdaptor() {
        return rangeAdaptor;
    }

    public int getNparams() {
        return nparams;
    }

    @Override
    public int getPriority() {
        return 110;
    }
}
