/**
 * Created Jul 11, 2007
 */
package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.Iterator;

import org.openl.domain.IIntIterator;
import org.openl.domain.IntArrayIterator;

/**
 * @author snshor
 *
 */
public abstract class ADTRuleIndex {

    static class DTRuleNode {
        Object value;

        int[] rules;

        ADTRuleIndex nextIndex;

        /**
         * @param rules
         */
        public DTRuleNode(int[] rules) {
            // if (rules.length == 0)
            // throw new RuntimeException();

            this.rules = rules;
        }

        public ADTRuleIndex getNextIndex() {
            return nextIndex;
        }

        public int[] getRules() {
            return rules;
        }

        /**
         * @return
         */
        public IIntIterator getRulesIterator() {
            return new IntArrayIterator(rules);
        }

        /**
         * @return
         */
        public boolean hasIndex() {
            return nextIndex != null;
        }

    }

    static class DTRuleNodeBuilder {
        ArrayList<Integer> rules;

        public DTRuleNodeBuilder() {
            rules = new ArrayList<Integer>();
        }

        /**
         * @param emptyBuilder
         */
        public DTRuleNodeBuilder(DTRuleNodeBuilder emptyBuilder) {
            rules = new ArrayList<Integer>(emptyBuilder.rules);
        }

        void addRule(int rule) {
            rules.add(new Integer(rule));
        }

        DTRuleNode makeNode() {
            return new DTRuleNode(makeRulesAry());
        }

        int[] makeRulesAry() {
            int[] res = new int[rules.size()];
            for (int i = 0; i < res.length; i++) {
                res[i] = rules.get(i);
            }
            return res;
        }
    }

    DTRuleNode emptyOrFormulaNodes;

    public ADTRuleIndex(DTRuleNode emptyOrFormulaNodes) {
        this.emptyOrFormulaNodes = emptyOrFormulaNodes;
    }

    public DTRuleNode findNode(Object value) {
        if (value == null) {
            return emptyOrFormulaNodes;
        }

        DTRuleNode node = findNodeInIndex(value);

        return node == null || node.rules.length == 0 ? emptyOrFormulaNodes : node;
    }

    public abstract DTRuleNode findNodeInIndex(Object value);

    public abstract Iterator<DTRuleNode> nodes();

}
