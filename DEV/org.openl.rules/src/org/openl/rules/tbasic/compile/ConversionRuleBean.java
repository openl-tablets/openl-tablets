package org.openl.rules.tbasic.compile;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConversionRuleBean {
    private String operation;
    private boolean multiLine;
    private List<ConversionRuleStep> convertionSteps;

}
