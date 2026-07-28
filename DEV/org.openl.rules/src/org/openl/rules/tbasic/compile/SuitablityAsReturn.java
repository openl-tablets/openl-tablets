package org.openl.rules.tbasic.compile;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum SuitablityAsReturn {
    RETURN(2),
    SUITABLE(1),
    NONE(0);

    private final int value;

    public static SuitablityAsReturn lessSuitable(SuitablityAsReturn first, SuitablityAsReturn second) {
        if (first.value < second.value) {
            return first;
        } else {
            return second;
        }
    }
}
