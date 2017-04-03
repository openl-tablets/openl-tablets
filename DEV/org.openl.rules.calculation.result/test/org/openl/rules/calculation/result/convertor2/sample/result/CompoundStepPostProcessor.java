package org.openl.rules.calculation.result.convertor2.sample.result;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.calculation.result.convertor2.CalculationStep;
import org.openl.rules.calculation.result.convertor2.CompoundStep;

/**
 * Maps ID and Total premium from sub-steps. By default, ID is taken from
 * step#1, and Total from step#SIZE-1
 * 
 * @author tkrivickas
 * 
 */
public class CompoundStepPostProcessor {

    private int idStepNumber;

    public CompoundStepPostProcessor() {
        this(0);
    }

    public CompoundStepPostProcessor(int idStepNumber) {
        this.idStepNumber = idStepNumber;
    }

    public void process(CompoundStep compoundStep) {
        if (!checkPreconditions(compoundStep)) {
            return;
        }
        List<CalculationStep> nestedSteps = compoundStep.getSteps();

        if (!allNestedCompounds(nestedSteps)) {
            /** Nested steps are simple */
            CalculationStep idStep = nestedSteps.get(idStepNumber);
            if (idStep instanceof SimpleStep) {
                SimpleStep autoIdStep = (SimpleStep) idStep;
                /**
                 * Trim original Id (text) field; Copy to the compound.
                 */
                String id = StringUtils.trimToNull(autoIdStep.getText());
                autoIdStep.setText(id);
                compoundStep.setId(id);
            }
            CalculationStep totalStep = nestedSteps.get(nestedSteps.size() - 1);
            if (totalStep instanceof SimpleStep) {
                compoundStep.setFormula(((SimpleStep) totalStep).getFormula());
            }
        }
    }

    protected boolean checkPreconditions(CompoundStep compoundStep) {
        return (compoundStep.getSteps() != null && compoundStep.getSteps().size() >= 2);
    }

    private boolean allNestedCompounds(List<CalculationStep> nestedSteps) {
        if (nestedSteps.get(0) instanceof CompoundStep
                && nestedSteps.get(nestedSteps.size() - 1) instanceof CompoundStep) {
            return true;
        }
        return false;
    }

}
