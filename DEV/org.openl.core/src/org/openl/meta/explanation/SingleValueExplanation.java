package org.openl.meta.explanation;

import java.util.Collections;

import org.openl.meta.IMetaInfo;
import org.openl.meta.ValueMetaInfo;
import org.openl.util.tree.ITreeElement;

/**
 * Explanation implementation for number values.
 * 
 * @author DLiauchuk
 *
 * @param <T> type that extends {@link ExplanationNumberValue}
 */
public class SingleValueExplanation<T extends ExplanationNumberValue<T>> implements ExplanationForNumber<T> {

    private IMetaInfo metaInfo;

    public IMetaInfo getMetaInfo() {
        return metaInfo;
    }

    public void setMetaInfo(IMetaInfo info) {
        metaInfo = info;

    }

    public String getDisplayName(int mode) {
        if (mode == SHORT) {
            return printValue();
        } else {
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

    public Iterable<? extends ITreeElement<T>> getChildren() {
        return Collections.EMPTY_LIST;
    }

    public boolean isLeaf() {
        return true;
    }

    public String getType() {
        return "value";
    }

    /**
     * default implementation. right implementation should be in the object that will be shown.
     */
    public T getObject() {
        throw new UnsupportedOperationException("Should be overriden in childs");
    }
}
