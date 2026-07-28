package org.openl.rules.testmethod.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.openl.rules.testmethod.TestStatus;

@RequiredArgsConstructor
public class ComparedResult {

    @Getter
    private final String fieldName;
    @Getter
    private final Object expectedValue;
    @Getter
    private final Object actualValue;
    @Getter
    private final TestStatus status;
}
