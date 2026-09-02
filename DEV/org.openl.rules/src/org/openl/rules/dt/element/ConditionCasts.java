package org.openl.rules.dt.element;

import lombok.Getter;

import org.openl.binding.impl.cast.IOpenCast;

public final class ConditionCasts {
    @Getter
    private final IOpenCast castToInputType;
    @Getter
    private final IOpenCast castToConditionType;

    ConditionCasts(IOpenCast castToInputType, IOpenCast castToConditionType) {
        this.castToInputType = castToInputType;
        this.castToConditionType = castToConditionType;
    }

    public boolean atLeastOneExists() {
        return castToInputType != null || castToConditionType != null;
    }

    public boolean isCastToConditionTypeExists() {
        return castToConditionType != null;
    }

    public boolean isCastToInputTypeExists() {
        return castToInputType != null;
    }

    public Object castToInputType(Object value) {
        if (castToInputType != null) {
            return castToInputType.convert(value);
        }
        return value;
    }

    public Object castToConditionType(Object value) {
        if (castToConditionType != null) {
            return castToConditionType.convert(value);
        }
        return value;
    }

}
