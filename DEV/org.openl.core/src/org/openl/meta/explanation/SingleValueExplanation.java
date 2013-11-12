package org.openl.meta.explanation;

import java.util.Iterator;

import org.openl.meta.IMetaInfo;
import org.openl.meta.ValueMetaInfo;
import org.openl.meta.number.NumberValue;
import org.openl.meta.number.NumberValue.ValueType;
import org.openl.util.AOpenIterator;
import org.openl.util.tree.ITreeElement;

/**
 * Explanation implementation for number values that are of type {@link ValueType#SINGLE_VALUE}, see also
 * {@link NumberValue#getValueType()}.
 * 
 * @author DLiauchuk
 *
 * @param <T> type that extends {@link ExplanationNumberValue} 
 */
public class SingleValueExplanation<T extends ExplanationNumberValue<T>> implements ExplanationForNumber<T> {
    
    protected static final int VALUE = 0x01;
    protected static final int SHORT_NAME = 0x02;
    protected static final int LONG_NAME = 0x04;
    protected static final int URL = 0x08;
    protected static final int EXPAND_FORMULA = 0x10;
    protected static final int EXPAND_FUNCTION = 0x20;
    protected static final int PRINT_VALUE_IN_EXPANDED = 0x40;
    protected static final int EXPAND_ALL = EXPAND_FORMULA | EXPAND_FUNCTION | PRINT_VALUE_IN_EXPANDED;
    protected static final int PRINT_ALL = EXPAND_ALL | LONG_NAME;

    private IMetaInfo metaInfo;
    
    public SingleValueExplanation() {        
    }
    
    public SingleValueExplanation(IMetaInfo metaInfo) {
        this.metaInfo = metaInfo;
    }

    public SingleValueExplanation(String name) {
        ValueMetaInfo mi = new ValueMetaInfo();
        mi.setShortName(name);
        metaInfo = mi;
    }
    
    public IMetaInfo getMetaInfo() {
        return metaInfo;
    }

    public void setMetaInfo(IMetaInfo info) {
        metaInfo = info;

    }

    public String getDisplayName(int mode) {
        switch (mode) {
            case SHORT:
                return printValue();
            default:
                String name = metaInfo == null ? null : getMetaInfo().getDisplayName(mode);
                return name == null ? printValue() : name + "(" + printValue() + ")";
        }
    }

    public String getName() {
        if (metaInfo == null) {
            return null;
        }

        return metaInfo.getDisplayName(IMetaInfo.LONG);
    }

    public String printValue() {
        return getName();
    }
    
    public void setFullName(String name) {
        if (metaInfo == null) {
            metaInfo = new ValueMetaInfo();
        }
        if (metaInfo instanceof ValueMetaInfo) {
            ((ValueMetaInfo) metaInfo).setFullName(name);
        }
    }

    public void setName(String name) {
        if (metaInfo == null) {
            metaInfo = new ValueMetaInfo();
        }
        if (metaInfo instanceof ValueMetaInfo) {
            ((ValueMetaInfo) metaInfo).setShortName(name);
        }
    }

    public Iterator<? extends ITreeElement<T>> getChildren() {
        return AOpenIterator.empty();
    }

    public boolean isLeaf() {
        return true;
    }

    public String getType() {
        return ValueType.SINGLE_VALUE.toString();
    }
    
    /**
     * default implementation. right implementation should be in the object that will be shown.  
     */
    public T getObject() {
        throw new UnsupportedOperationException("Should be overriden in childs");
    }
}
