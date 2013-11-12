package org.openl.rules.dt.index;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.helpers.NumberUtils;
import org.openl.util.math.MathUtils;

public class EqualsIndex extends ARuleIndex {

    protected HashMap<Object, DecisionTableRuleNode> valueNodes = new HashMap<Object, DecisionTableRuleNode>();

    public EqualsIndex(DecisionTableRuleNode emptyOrFormulaNodes, HashMap<Object, DecisionTableRuleNode> valueNodes) {
        super(emptyOrFormulaNodes);

        this.valueNodes = valueNodes;
    }

    @Override
    public DecisionTableRuleNode findNodeInIndex(Object value) {

        if (value != null) {
            DecisionTableRuleNode result = valueNodes.get(value);
			
            if (result == null && isFloatPointNumber(value)) {
            	Set<Object> keys = valueNodes.keySet();
            	Double objectValue = convertToDouble(value);
            	
            	for (Object key : keys) {
        			if (equalDoubleValues(objectValue, key)) {
        				return valueNodes.get(key);
        			}
            	}
            }
            
            return result;
            
        }

        return null;
    }
    
    protected boolean isFloatPointNumber(Object value) {
        return NumberUtils.isFloatPointNumber(value);
    }
    
    protected Double convertToDouble(Object value) {
        return NumberUtils.convertToDouble(value);
    }
    
    protected boolean equalDoubleValues(Double inputParamValue, Object indexValue) {
        Double indexDoubleValue = NumberUtils.convertToDouble(indexValue);
        return MathUtils.eq(inputParamValue, indexDoubleValue);
    }

    @Override
    public Iterator<DecisionTableRuleNode> nodes() {
        return valueNodes.values().iterator();
    }
}
