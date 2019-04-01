package org.openl.rules.tbasic.compile;

public enum SuitablityAsReturn {
    RETURN(2),
    SUITABLE(1),
    NONE(0);
    private int value;

    public static SuitablityAsReturn lessSuitable(SuitablityAsReturn first, SuitablityAsReturn second) {
        if (first.value < second.value) {
            return first;
        } else {
            return second;
        }
    }

    private SuitablityAsReturn(int value) {
        this.value = value;
    }
}
