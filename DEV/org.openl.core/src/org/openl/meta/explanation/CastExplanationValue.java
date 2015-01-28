package org.openl.meta.explanation;

import java.util.ArrayList;
import java.util.List;

import org.openl.meta.number.NumberCast;

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
        return String.format("cast.%s", castHolder.getOperand().getType());
    }

    @Override
    public boolean isLeaf() {
        return false;
    }
}
