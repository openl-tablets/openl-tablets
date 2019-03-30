package org.openl.meta.number;

public class CastOperand {
    private final String type;
    private final boolean autocast;

    public CastOperand(Class<?> type, boolean autocast) {
        this(type.getSimpleName(), autocast);
    }

    public CastOperand(String type, boolean autocast) {
        this.type = type;
        this.autocast = autocast;
    }

    public String getType() {
        return type;
    }

    public boolean isAutocast() {
        return autocast;
    }
}
