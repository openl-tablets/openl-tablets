/**
 * Created Jul 8, 2007
 */
package org.openl.rules.dt.algorithm;

import org.openl.binding.BindingDependencies;
import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.domain.IntRangeDomain;
import org.openl.rules.binding.RulesBindingDependencies;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt.data.ConditionOrActionParameterField;
import org.openl.rules.dt.element.ICondition;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

/**
 * The basic algorithm for decision table (DT) evaluation is straightforward
 * (let's consider table with conditions and actions as columns and rules as
 * rows - you remember that OpenL Tablets allow both this and transposed
 * orientation):
 * 
 * <ol>
 * <li>For each rule (row) from the top to the bottom of the table evaluate
 * conditions from the left to the right</li>
 * <li>If all conditions are true, execute all the actions in the rule from the
 * left to the right, if any condition is false stop evaluating conditions, go
 * to the next rule</li>
 * <li>If the action is non-empty return action then return the value of the
 * action (stops the evaluation)</li>
 * <li>If no rules left then return <code>null</code></li>
 * </ol>
 * The logic of the algorithm must be kept intact in all optimization
 * implementations, unless some permutations are explicitly allowed.
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
 * 
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
 * <li>Condition Sharing algorithm.<br>
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
 * <h3>Applicability of the Algorithms</h3>
 * 
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
 * the approach, one needs to relize that<br/>
 * a) indexing is possible only for some very well defined operations, like
 * equality or range checks<br/>
 * and<br/>
 * b) the index has to be calculated in advance
 * 
 * <p>
 * For example we have condition
 * <p/>
 * <code>driver.type == type</code>
 * <p/>
 * 
 * where <code>driver.type</code> is tested value and <code>type</code> is rule
 * condition parameter against which the tested value is being checked. We could
 * parse the code and figure it out automatically - but we decided to simplify
 * our task by putting a bit more responsibility (and control too) into
 * developer's hands. If the condition expression will be just
 * <p/>
 * <code>driver.type</code>
 * <p/>
 * the parser will easily recognize that it does not depend on rule parameters
 * at all. This can be used as the hint that the condition needs to be
 * optimized. The structure of parameters will determine the type of index using
 * this table:
 * 
 * <table border="1">
 * <tbody>
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
 * <td align="center">1</td>
 * <td>T[] ary</td>
 * <td><code>contains(ary, x)</code></td>
 * <td>Constant(HashMap) performance</td>
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
 * </tbody>
 * </table>
 * 
 * The OpenL Tablets Decision Table Optimizer will automatically recognize these
 * conditions and create indexes for them.<br/>
 * The advantages of the suggested approach are summarized here:
 * 
 * <ul>
 * <li>there is less code to type and therefore less to read, less places to
 * make typos etc.</li>
 * <li>there is less to compile and parse, the less work for the compiler or
 * optimizer to determine programmer's intentions == better performance and less
 * errors
 * <li>optimization algorithm is easy to turn on or off for any condition, it is
 * easy to predict what kind of optimization will be used for particular kind of
 * data, there would not be any "black box magic". Eventually we are going not
 * only publish all the optimization algorithms, but also provide formula-based
 * estimates for their expected performance. This will provide the basis for
 * static compile-time performance analysis</li>
 * </ul>
 * <p>
 * 
 * @author sshor
 */
// TODO: rename, it is common implementation for DT algorithm 
public class DecisionTableOptimizedAlgorithm {

    /**
     * There is one evaluator per condition in DT
     */
    private IConditionEvaluator[] evaluators;
    private DecisionTable table;
    private BindingDependencies dependencies;

    public DecisionTableOptimizedAlgorithm(IConditionEvaluator[] evaluators, DecisionTable table) {
        this.evaluators = evaluators;
        this.table = table;
        dependencies = new RulesBindingDependencies();
        table.updateDependency(dependencies);
    }

    public IConditionEvaluator[] getEvaluators() {
        return evaluators;
    }
    
    public DecisionTable getTable() {
        return table;
    }

    protected Object evaluateTestValue(ICondition condition, Object target, Object[] dtparams, IRuntimeEnv env) {
        return condition.getEvaluator().invoke(target, dtparams, env);
    }

    /**
     * Clears condition's param values.
     * 
     * Memory optimization: clear condition values because this values will be
     * used in index(only if it condition is not used).
     */
    public void removeParamValuesForIndexedConditions() {
        for (int i = 0; i < evaluators.length; i++) {
            if (evaluators[i].isIndexed()) {
                if (!isDependecyOnConditionExists(table.getConditionRows()[i])) {
                    table.getConditionRows()[i].clearParamValues();
                }
            } else {
                break;
            }
        }
        // we do not need dependencies after clearing conditions
        dependencies = null;
    }
    
    private boolean isDependecyOnConditionExists(ICondition condition){
        for (IOpenField field : dependencies.getFieldsMap().values()) {
            if (field instanceof ConditionOrActionParameterField
                    && ((ConditionOrActionParameterField) field).getConditionOrAction() == condition) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method produces the iterator over the set of rules in DT. It has to
     * retain the order of the rules.
     * 
     * An optimized algorithm has 2 distinct steps:
     * 
     * 1) Create initial discriminate rules set using indexing in initial
     * conditions.
     * 
     * 2) Iterate over the initial set using remaining conditions as selectors;
     * not-optimized algorithm has the whole rules set as initial.
     * 
     * Performance. From the algorithm definition it is clear, that step 1 of
     * algorithm is performed with constant or near constant speed with regard
     * to the number of the rules. The performance of the part 2 is largely
     * dependent the size of the resulting rules set. The order of initial
     * indexed conditions does not seem to affect performance much (//TODO this
     * statement needs verification)
     * 
     * @return iterator over <b>rule indexes</b> - integer iterator.
     */
    public IIntIterator checkedRules(Object target, Object[] params, IRuntimeEnv env) {
       
        ICondition[] conditions = table.getConditionRows();

        IIntIterator iterator = null;
        int conditionNumber = 0;

        iterator = new IntRangeDomain(0, table.getNumberOfRules() - 1).intIterator();

        for (; conditionNumber < evaluators.length; conditionNumber++) {

            ICondition condition = conditions[conditionNumber];
            IConditionEvaluator evaluator = evaluators[conditionNumber];

            IIntSelector sel = evaluator.getSelector(condition, target, params, env);

            iterator = iterator.select(sel);
        }

        return iterator;
    }

}
