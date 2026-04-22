package org.openl.rules.tbasic.compile;

import java.util.List;
import java.util.function.Consumer;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

@Getter
@Builder
public class ConversionRuleBean {
    private String operation;
    private boolean multiLine;
    @Singular
    private List<ConversionRuleStep> convertionSteps;

    public static class ConversionRuleBeanBuilder {
        public ConversionRuleBeanBuilder step(Consumer<ConversionRuleStep.ConversionRuleStepBuilder> step) {
            var builder = ConversionRuleStep.builder();
            step.accept(builder);
            return convertionStep(builder.build());
        }
    }
}
