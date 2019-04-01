package org.openl.rules.calculation.result.convertor2.sample.result;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.calculation.result.convertor2.CalculationStep;
import org.openl.rules.calculation.result.convertor2.CompoundStep;

/**
 * Maps ID and Total premium from sub-steps. By default, ID is taken from step#1, and Total from step#SIZE-1
 *
 * @author tkrivickas
 *
 */
public class CompoundStepPostProcessor {

    private int idStepNumber;

    CompoundStepPostProcessor() {
        this(0);
    }

    CompoundStepPostProcessor(int idStepNumber) {
        this.idStepNumber = idStepNumber;
    }

    public void process(CompoundStep compoundStep) {
        if (compoundStep.getSteps() == null || compoundStep.getSteps().size() < 2) {
            return;
        }
        List<CalculationStep> nestedSteps = compoundStep.getSteps();

        if (!allNestedCompounds(nestedSteps)) {
            /* Nested steps are simple */
            CalculationStep idStep = nestedSteps.get(idStepNumber);
            if (idStep instanceof SimpleStep) {
                SimpleStep autoIdStep = (SimpleStep) idStep;
                /*
                 * Trim original Id (text) field; Copy to the compound.
                 */
                String id = StringUtils.trimToNull(autoIdStep.getText());
                autoIdStep.setText(id);
                compoundStep.setId(id);
            }
            CalculationStep totalStep = nestedSteps.get(nestedSteps.size() - 1);
            if (totalStep instanceof SimpleStep) {
                compoundStep.setFormula((totalStep).getFormula());
            }
        }
    }

    private boolean allNestedCompounds(List<CalculationStep> nestedSteps) {
        return nestedSteps.get(0) instanceof CompoundStep && nestedSteps
            .get(nestedSteps.size() - 1) instanceof CompoundStep;
    }

}
