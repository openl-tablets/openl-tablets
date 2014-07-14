package org.openl.meta.explanation;

import java.util.ArrayList;
import java.util.List;

import org.openl.meta.number.NumberCast;
import org.openl.meta.number.NumberValue.ValueType;

@SuppressWarnings("rawtypes")
public class CastExplanationValue extends SingleValueExplanation {
    private NumberCast castHolder;
    
    public CastExplanationValue(NumberCast castHolder) {
        this.castHolder = castHolder;
    }
    
    @Override
    public Iterable<? extends org.openl.util.tree.ITreeElement> getChildren() {
        List<ExplanationNumberValue<?>> list = new ArrayList<ExplanationNumberValue<?>>();
        list.add(castHolder.getValue());
        return list;
    }
    
    @Override
    public String getType() {
        return String.format("%s.%s", ValueType.CAST, castHolder.getOperand().getType());
    }

    @Override
    public boolean isLeaf() {
        return false;
    }
}
