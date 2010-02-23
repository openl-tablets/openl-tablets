/**
 * Created Jul 8, 2007
 */
package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.Iterator;

import org.openl.binding.impl.BoundError;
import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.domain.IntRangeDomain;
import org.openl.rules.dt.ADTRuleIndex.DTRuleNode;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 */

/**
 * The basic algorithm for decision table (DT) evaluation is straightforward
 * (let's consider table with conditions and actions as columns and rules as
 * rows - you remember that OpenL Tablets allow both this and transposed
 * orientation):
 *
 *
 *
 * <ol>
 *
 * <li> For each rule (row) from the top to the bottom of the table evaluate
 * conditions from the left to the right</li>
 * <li> If all conditions are true, execute all the actions in the rule from the
 * left to the right, if any condition is false stop evaluating conditions, go
 * to the next rule</li>
 * <li>If the action is non-empty return action then return the value of the
 * action (stops the evaluation)</li>
 * <li>If no rules left then return <code>null</code></li>
 * </ol>
 * The logic of the algorithm must be kept intact in all optimization
 * implementations, unless some permutations are explicitly allowed.
 *
 *
 * <p>
 * The goal of optimizations is to decrease the number of condition checking
 * required to determine which rule actions need to be executed. Action
 * optimizations are not considered, even though some improvements could be
 * possible. Sometimes the best results may be achieved by changing the order of
 * conditions(columns) and rules(rows) but these approaches will change the
 * algorithm's logic and must be used with caution. We are not going to
 * implement these approaches at this point, but will give some guidelines to
 * users on how to re-arrange rule tables to achieve better performance.
 * <p>
 * Out of the class of optimization algorithms that do not change the order of
 * conditions or rules we are going to consider ones that optimize condition
 * checking in one condition (column) at the time. In decision table with
 * multiple conditions the algorithm will create a tree of the optimized nodes.
 * The time of the of the tree traversing will equal the sum of times required
 * to calculate each node.
 *
 * <p>
 * The optimization algorithms that deal with single condition can be classified
 * as follows:
 * <li>Condition Sharing algorithm. <br>
 * Merge all the rows that share the same condition data into one node. Then
 * calculate condition only once for all rules that share the same condition.
 * The algorithm can be extended to achieve even better results if we allow the
 * pseudo-data keyword <i>else</i>. The advantage of this algorithm is in it's
 * universal applicability - it does not depend on the nature of condition
 * expression. The disadvantage is in it's low performance improvement - it's
 * expected average performance is n/s - where n is the total number of
 * conditions and s = n/u where u is the number of unique conditions. We see
 * that the more unique conditions the column has, the less is performance
 * advantage of this method.
 *
 * <p>
 * <li>Indexing<br>
 * Calculate condition input value and determine which rules to fire using some
 * kind of index. The performance will be determined by the index speed. For
 * example, if index is implemented as a HashMap (for equality checks) the
 * performance is expected to be constant, if as a TreeMap - log (u), where u is
 * number of unique conditions. Interestingly, for indexing algorithms, the more
 * is the number of shared conditions or empty conditions, the less is
 * performance improvement - quite the opposite to the Condition Sharing
 * algorithm.<br>
 *
 *
 * <h3>Applicability of the Algorithms</h3>
 * <p>
 * Both algorithms are single-condition(column) based. Both work only if rule
 * condition expression does not change it's value during the course of DT
 * evaluation. In other words, the Decision Table rules do not change attributes
 * that participate in condition evaluation. For indexing the additional
 * requirement is necessary - the index value should be known at compile time.
 * It excludes conditions with dynamic formulas as cell values.
 *
 * <p>
 * If a Decision Table does not conform to these assumptions, you should not use
 * optimizations. It is also recommended that you take another look at your
 * design and ask yourself: was it really necessary to produce such a twisted
 * logic?</span>
 *
 *
 * <h3>Explicit Indexing Optimization</h3>
 * <p>
 * Generally speaking, it would be nice to have system automatically apply
 * optimizations, when appropriate, would not it? In reality there are always
 * the cases where one doesn't want optimization happen for some reason, for
 * example condition calls a function with side effects.. This would require us
 * to provide some facility to suppress optimization on column and/or table
 * level, and this might unnecessary complicate DT structure.
 *
 * <p>
 * Fortunately, we were lucky to come up with an approach that gives the
 * developer an explicit control over (indexing) optimization at the same time
 * <i>reducing</i> the total amount of the code in the condition. To understand
 * the approach, one needs to relize that<br/> a) indexing is possible only for
 * some very well defined operations, like equality or range checks<br/> and<br/>
 * b) the index has to be calculated in advance
 *
 * <p>
 *
 * For example we have condition <p/> <code>driver.type == type</code> <p/>
 *
 * where <code>driver.type</code> is tested value and <code>type</code> is
 * rule condition parameter against which the tested value is being checked. We
 * could parse the code and figure it out automatically - but we decided to
 * simplify our task by putting a bit more responsibility (and control too) into
 * developer's hands. If the condition expression will be just <p/>
 * <code>driver.type</code> <p/> the parser will easily recognize that it does
 * not depend on rule parameters at all. This can be used as the hint that the
 * condition needs to be optimized. The structure of parameters will determine
 * the type of index using this table:
 *
 *
 * <table border="1"><tbody>
 * <tr>
 * <th>Expr Type</th>
 * <th>N Params</th>
 * <th>Param Type</th>
 * <th>Condition</th>
 * <th>Index Performance</th>
 * </tr>
 * <tr>
 * <td>any T x</td>
 * <td align="center">1</td>
 * <td>T value</td>
 * <td><code>x == value</code></td>
 * <td>Constant(HashMap) performance</td>
 * </tr>
 * <tr>
 * <td>any T x</td>
 * <td align="center">1 </td>
 * <td>T[] ary</td>
 * <td><code>contains(ary, x)</code> </td>
 * <td>Constant(HashMap) performance </td>
 * </tr>
 * <tr>
 * <td>Comparable T x</td>
 * <td align="center">2</td>
 * <td>T min, T max</td>
 * <td><code>min &lt;= x &&amp; x &lt;  max</code></td>
 * <td><code>log(n)</code>(TreeMap) performance</td>
 * </tr>
 * <tr>
 * <td>any T x</td>
 * <td align="center">2</td>
 * <td>&lt;in|not in&gt; Enum isIn, T[] ary</td>
 * <td><code>isIn == in ? contains(ary, x) : !contains(ary, x) </code></td>
 * <td>Constant(HashMap) performance</td>
 * </tr>
 *
 * </tbody></table>
 *
 *
 *
 * The OpenL Tablets Decision Table Optimizer will automatically recognize these
 * condititions and create indexes for them.<br/> The advantages of the
 * suggested approach are summarized here:
 *
 * <ul>
 * <li>there is less code to type and therefore less to read, less places to
 * make typos etc.</li>
 * <li>there is less to compile and parse, the less work for the compiler or
 * optimizer to determine programmer's intentions == better performance and less
 * errors
 * <li>optimization algorithm is easy to turn on or off for any condition, it
 * is easy to predict what kind of optimization will be used for particular kind
 * of data, there would not be any "black box magic". Eventually we are going
 * not only publish all the optimization algorithms, but also provide
 * formula-based estimates for their expected performance. This will provide the
 * basis for static compile-time performance analysis</li>
 * </ul>
 * <p>
 *
 *
 *
 *
 */

public class DTOptimizedAlgorithm {

    static class ConditionNode {
        DecisionTable table;

        DTCondition condition;

        int[] rules;

    }

    static public class RuleParamHolder {

        int rule;
        int param;

        public RuleParamHolder(int rule) {
            this.rule = rule;
        }

        public int getRule() {
            return rule;
        }

        // public Object calculateDynamicParameter(IDTCondition condition,
        // Object target, Object[] dtparams, IRuntimeEnv env)
        // {
        // //TODO add implementation
        // return null;
        // }

        /**
         *
         * @return true if paramater requires a dynamic evaluation - in case if
         *         paramter presented as formula. This is highly unlikely case
         *         for Condition parameters - but we need to address this
         *         possibility
         */
        public boolean isDynamic() {
            return false;
        }

    }

    /**
     * There is one evaluator per condition in DT
     */
    IDTConditionEvaluator[] evaluators;

    DecisionTable table;

    ADTRuleIndex indexRoot;

    private boolean allowConditionReordering;
    /**
     * @param condition
     * @param target
     * @param dtparams
     * @param env
     * @return
     */
    static public Object evaluateTestValue(IDTCondition condition, Object target, Object[] dtparams, IRuntimeEnv env) {
        return condition.getEvaluator().invoke(target, dtparams, env);
    }

    /**
     * @param methodType
     * @param paramType
     * @return
     */
    @SuppressWarnings("unchecked")
    private static IRangeAdaptor getRangeAdaptor(IOpenClass methodType, IOpenClass paramType) {
        if (methodType == JavaOpenClass.INT && paramType.getInstanceClass() == org.openl.rules.helpers.IntRange.class) {
            return new IRangeAdaptor.IntRangeAdaptor();
        }
        return null;
    }
    
    
//TODO    to do - fix _NO_PARAM_ issue
    
    /**
     * @param condition
     * @param methodType
     * @return
     */
    @SuppressWarnings("unchecked")
    public static IDTConditionEvaluator makeEvaluator(IDTCondition condition, IOpenClass methodType) throws BoundError {
        IParameterDeclaration[] params = condition.getParams();

        switch (params.length) {
            case 1:
                IOpenClass paramType = params[0].getType();
                if (methodType == paramType || methodType.getInstanceClass() == paramType.getInstanceClass()) {
                    return new EqualsIndexedEvaluator();
                }
                IAggregateInfo aggr = paramType.getAggregateInfo();
                if (aggr.isAggregate(paramType) && aggr.getComponentType(paramType) == methodType) {
                    return new ContainsInAryIndexedEvaluator();
                }

                IRangeAdaptor<Object, Object> ra = getRangeAdaptor(methodType, paramType);
                if (ra != null) {
                    return new RangeIndexedEvaluator(ra);
                }
                
                if (methodType == JavaOpenClass.BOOLEAN)
                    return new DefaultConditionEvaluator();

                break;
            case 2:
                IOpenClass paramType0 = params[0].getType();
                IOpenClass paramType1 = params[1].getType();
                if (methodType == paramType0 && methodType == paramType1) {

                    Class<?> c = methodType.getInstanceClass();
                    if (c != int.class && c != long.class && c != double.class && c != float.class
                            && !Comparable.class.isAssignableFrom(c)) {
                        throw new BoundError(null, "Type " + methodType.getName() + " is not Comparable", null,
                                condition.getSourceCodeModule());
                    }

                    return new RangeIndexedEvaluator(null);
                }

                aggr = paramType1.getAggregateInfo();
                if (aggr.isAggregate(paramType1) && aggr.getComponentType(paramType1) == methodType) {
                    BooleanTypeAdaptor bta = BooleanTypeAdaptor.getAdaptor(paramType0);
                    if (bta != null) {
                        return new ContainsInOrNotInAryIndexedEvaluator(bta);
                    }
                }

                break;

        }

        String paramStr = "";
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                paramStr += ",";
            }
            paramStr += params[i].getType().getName();
        }

        throw new BoundError(null, "Can not make a Condition Evaluator for parameter " + methodType.getName()
                + " and [" + paramStr + "]", null, condition.getSourceCodeModule());
    }

    public DTOptimizedAlgorithm(IDTConditionEvaluator[] evaluators, DecisionTable table) {
        this.evaluators = evaluators;
        this.table = table;
    }

    /**
     *
     */
    public void buildIndex() {

        if (allowConditionReordering) {
            reorderConditions();
        }
        ArrayList<Object[][]> params = new ArrayList<Object[][]>();

        for (int i = 0; i < evaluators.length; i++) {
            if (evaluators[i].isIndexed()) {
                Object[][] values = table.getConditionRows()[i].getParamValues();
                Object[][] precalculatedParams = prepareIndexedParams(values);
                params.add(precalculatedParams);
            } else {
                break;
            }
        }

        int nIndexedCond = params.size();

        if (nIndexedCond == 0) {
            return;
        }
        Object[][] params0 = params.get(0);
        indexRoot = evaluators[0].makeIndex(params0, new IntRangeDomain(0, params0.length - 1).intIterator());

        indexNodes(indexRoot, params, 1);

    }

    /**
     * This method produces the iterator over the set of rules in DT. It has to
     * retain the order of the rules.
     *
     * An optimized algorithm has 2 distinct steps: 1) Create initial
     * discriminate ruleset using indexing in initial conditions. 2) Iterate
     * over the initial set using remaining conditions as selectors;
     * not-optimized algorithm has the whole ruleset as initial.
     *
     * 3)Performance. From the algorithm definition it is clear, that step 1 of
     * algoritm is performed with constant or near constant speed with regard to
     * the number of the rules. The performance of the part 2 is largely
     * dependend the size of the resulting ruleset. The order of initial indexed
     * conditions does not seem to affect performance much (//TODO this
     * atatement needs verification)
     *
     *
     * @return iterator over <b>rule indexes</b> - integer iterator.
     *
     */

    public IIntIterator checkedRules(Object target, Object[] dtparams, IRuntimeEnv env) {

        // select ruleset using indexed mode

        IIntIterator rules = null;
        int nCond = 0;
        IDTCondition[] cond = table.getConditionRows();

        if (indexRoot == null) {
            rules = new IntRangeDomain(0, table.getNumberOfRules() - 1).intIterator();
        } else {
            ADTRuleIndex index = indexRoot;
            for (; nCond < evaluators.length; nCond++) {
                // IDTConditionEvaluator dtcev = evaluators[nCond];

                Object testValue = evaluateTestValue(cond[nCond], target, dtparams, env);

                DTRuleNode node = index.findNode(testValue);
                if (!node.hasIndex()) {
                    rules = node.getRulesIterator();
                    ++nCond;
                    break;
                }

                index = node.getNextIndex();
            }
        }

        for (; nCond < evaluators.length; nCond++) {
            IDTCondition condition = cond[nCond];
            IDTConditionEvaluator ev = evaluators[nCond];

            IIntSelector sel = ev.getSelector(condition, target, dtparams, env);

            rules = rules.select(sel);
        }

        return rules;

    }

    public IDTConditionEvaluator[] getEvaluators() {
        return evaluators;
    }

    void indexNode(DTRuleNode node, ArrayList<Object[][]> params, int level) {
        ADTRuleIndex nodeIndex = evaluators[level].makeIndex(params.get(level), node.getRulesIterator());
        node.nextIndex = nodeIndex;

        indexNodes(nodeIndex, params, level + 1);
    }

    // public void buildIndex2()
    // {
    //
    // if (allowConditionReordering)
    // reorderConditions();
    //
    // if (evaluators.length > 0)
    // {
    // if (evaluators[0].isIndexed())
    // {
    // Object[][] values = table.getConditionRows()[0].getParamValues();
    // Object[][] indexedparams = prepareIndexedParams(values);
    // indexRoot = evaluators[0].makeIndex(indexedparams);
    // }
    // }
    //
    // if (indexRoot == null)
    // return;
    //
    //
    // // for (int i = 1; i < evaluators.length; i++)
    // // {
    // // if (!evaluators[i].isIndexed())
    // // break;
    // //
    // // Object[][] values = table.getConditionRows()[i].getParamValues();
    // // Object[][] indexedparams = prepareIndexedParams(values);
    // // evaluators[i].makeIndex(indexedparams);
    // // }
    //
    //
    // }

    /**
     * @param indexRoot2
     * @param params
     * @param i
     */
    void indexNodes(ADTRuleIndex index, ArrayList<Object[][]> params, int level) {
        if (index == null) {
            return;
        }

        if (params.size() <= level) {
            return;
        }

        for (Iterator<DTRuleNode> iter = index.nodes(); iter.hasNext();) {
            DTRuleNode node = iter.next();

            indexNode(node, params, level);
        }
        indexNode(index.emptyOrFormulaNodes, params, level);
    }

    Object[][] prepareIndexedParams(Object[][] params) {
        Object[][] indexedparams = new Object[params.length][];

        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                indexedparams[i] = null;
            } else {
                Object[] pary = new Object[params[i].length];
                for (int j = 0; j < pary.length; j++) {
                    Object x = params[i][j];
                    if (x instanceof IOpenMethod) {
                        throw new RuntimeException("Can not index conditions with formulas");
                    }
                    pary[j] = x;
                    indexedparams[i] = pary;
                }
            }
        }

        return indexedparams;
    }

    /**
     *
     */
    private void reorderConditions() {
        // TODO Auto-generated method stub

    }

}
