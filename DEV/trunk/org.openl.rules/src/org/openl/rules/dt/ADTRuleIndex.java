/**
 * Created Jul 11, 2007
 */
package org.openl.rules.dt;

import java.io.CharArrayWriter;
import java.util.ArrayList;
import java.util.Iterator;

import org.openl.domain.IIntIterator;
import org.openl.domain.IntArrayIterator;
import org.openl.util.ArrayTool;

/**
 * @author snshor
 *
 */
public abstract class ADTRuleIndex {

    static class DTRuleNode {
        Object value;

        int[] rules;

        ADTRuleIndex nextIndex;
        
        
        
        
        @Override
        public String toString() {
            CharArrayWriter w = new CharArrayWriter(100);
            print(1, w);
            return w.toString();
        }

        
        
        void print(int level, CharArrayWriter writer)
        {
            for (int i = 0; i < level; i++) {
                writer.append("--");
            }
            writer.append(" " + value + ArrayTool.asString(rules)).append('\n');
            if (nextIndex != null)
                for (Iterator<DTRuleNode> it = nextIndex.nodes(); it.hasNext();)
                    it.next().print(level+1, writer);
            if (nextIndex.emptyOrFormulaNodes != null)
                nextIndex.emptyOrFormulaNodes.print(level+1, writer);
        }

        /**
         * @param rules
         */
        public DTRuleNode(int[] rules, Object value) {
            // if (rules.length == 0)
            // throw new RuntimeException();

            this.rules = rules;
            this.value = value;
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

        DTRuleNode makeNode(Object value) {
            return new DTRuleNode(makeRulesAry(), value);
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
