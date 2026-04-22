package org.openl.rules.tbasic.compile;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
class ConversionRuleStep {

    private String operationType;
    private String operationParam1;
    private String operationParam2;
    private String labelInstruction;
    private String nameForDebug;
}
