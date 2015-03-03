package org.openl.rules.dt2;

import org.openl.domain.IIntIterator;
import org.openl.domain.IntArrayIterator;
import org.openl.rules.dt2.index.ARuleIndex;

public class DecisionTableRuleNode {

    public static final int[] ZERO_ARRAY = new int[0];
    private int[] rules;

    private ARuleIndex nextIndex;

    // private boolean saveRulesMetaInfo;

    public DecisionTableRuleNode(int[] rules) {
        this.rules = rules;
    }

    // public boolean isSaveRulesMetaInfo() {
    // return saveRulesMetaInfo;
    // }
    //
    // public void setSaveRulesMetaInfo(boolean saveRulesMetaInfo) {
    // this.saveRulesMetaInfo = saveRulesMetaInfo;
    // }

    public ARuleIndex getNextIndex() {
        return nextIndex;
    }

    public void setNextIndex(ARuleIndex nextIndex) {
        this.nextIndex = nextIndex;
        if (nextIndex != null) {
            rules = null;
            // memory optimization: we do not need rule numbers for current
            // index if we have next index
        }
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

}
