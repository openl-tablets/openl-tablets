package org.openl.rules.dt.index;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.helpers.NumberUtils;
import org.openl.util.math.MathUtils;

public class EqualsIndex extends ARuleIndex {

    private HashMap<Object, DecisionTableRuleNode> valueNodes = new HashMap<Object, DecisionTableRuleNode>();

    public EqualsIndex(DecisionTableRuleNode emptyOrFormulaNodes, HashMap<Object, DecisionTableRuleNode> valueNodes) {
        super(emptyOrFormulaNodes);

        this.valueNodes = valueNodes;
    }

    @Override
    public DecisionTableRuleNode findNodeInIndex(Object value) {

        if (value != null) {
            DecisionTableRuleNode result = valueNodes.get(value);
			
            if (result == null && NumberUtils.isFloatPointNumber(value)) {
            	Set<Object> keys = valueNodes.keySet();
            	Double objectValue =  NumberUtils.convertToDouble(value);
            	
            	for (Object key : keys) {
            		if (NumberUtils.isFloatPointNumber(value)) {
            			Double keyValue = NumberUtils.convertToDouble(key);
            			
            			if (MathUtils.eq(objectValue.doubleValue(), keyValue.doubleValue()))
            				return valueNodes.get(key);
            		}
            	}
            }
            
            return result;
            
        }

        return null;
    }

    @Override
    public Iterator<DecisionTableRuleNode> nodes() {
        return valueNodes.values().iterator();
    }
}
