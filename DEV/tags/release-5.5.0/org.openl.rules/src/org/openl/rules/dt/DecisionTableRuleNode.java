package org.openl.rules.dt;

import java.io.CharArrayWriter;
import java.util.Iterator;

import org.openl.domain.IIntIterator;
import org.openl.domain.IntArrayIterator;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.util.ArrayTool;

public class DecisionTableRuleNode {

    private int[] rules;

    private Object value;
    private ARuleIndex nextIndex;

    public DecisionTableRuleNode(int[] rules, Object value) {
        this.rules = rules;
        this.value = value;
    }

    public ARuleIndex getNextIndex() {
        return nextIndex;
    }
    
    public void setNextIndex(ARuleIndex nextIndex) {
        this.nextIndex = nextIndex;
    }

    public int[] getRules() {
        return rules;
    }

    public IIntIterator getRulesIterator() {
        return new IntArrayIterator(rules);
    }

    public boolean hasIndex() {
        return nextIndex != null;
    }

    @Override
    public String toString() {
        CharArrayWriter w = new CharArrayWriter(100);
        print(1, w);
        return w.toString();
    }

    private void print(int level, CharArrayWriter writer) {
        for (int i = 0; i < level; i++) {
            writer.append("--");
        }
        writer.append(" " + value + ArrayTool.asString(rules)).append('\n');
        if (nextIndex != null)
            for (Iterator<DecisionTableRuleNode> it = nextIndex.nodes(); it.hasNext();)
                it.next().print(level + 1, writer);
        if (nextIndex.getEmptyOrFormulaNodes() != null)
            nextIndex.getEmptyOrFormulaNodes().print(level + 1, writer);
    }



}
