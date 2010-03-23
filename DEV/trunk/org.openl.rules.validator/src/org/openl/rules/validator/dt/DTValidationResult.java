/**
 * Created Feb 11, 2007
 */
package org.openl.rules.validator.dt;

import java.util.Arrays;

import org.openl.rules.dt.DTOverlapping;
import org.openl.rules.dt.DTUncovered;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.validator.IValidationResult;
import org.openl.util.ArrayOfNamedValues;

import com.exigen.ie.constrainer.consistencyChecking.Overlapping;
import com.exigen.ie.constrainer.consistencyChecking.Uncovered;

/**
 * @author snshor
 *
 */
public class DTValidationResult implements IValidationResult {

    private DecisionTable decisionTable;
    private DTOverlapping[] overlappings;
    private DTUncovered[] uncovered;

    public DTValidationResult(DecisionTable decisionTable, DTOverlapping[] overlappings, DTUncovered[] uncovered) {
        this.decisionTable = decisionTable;
        this.overlappings = overlappings;

        this.uncovered = uncovered;
    }
    
    public DTValidationResult(DecisionTable decisionTable, Overlapping[] ov, Uncovered[] un, IConditionTransformer transformer,
            DTAnalyzer dtan) {
        this.decisionTable = decisionTable;
        overlappings = convertOverlappings(ov, transformer, dtan);
        uncovered = convertUncovered(un, transformer, dtan);
    }
    
    private DTOverlapping[] convertOverlappings(Overlapping[] ov, IConditionTransformer transformer, DTAnalyzer dtan) {
        DTOverlapping[] ov2 = new DTOverlapping[ov.length];
        for (int i = 0; i < ov.length; i++) {
            String[] names = ov[i].getSolutionNames();
            Object[] values = new Object[names.length];
            for (int j = 0; j < values.length; j++) {
                values[j] = transformer.transformSignatureValueBack(names[j], ov[i].getSolutionValues()[j], dtan);
            }

            ov2[i] = new DTOverlapping(ov[i].getOverlapped(), new ArrayOfNamedValues(names, values));
        }
        return ov2;
    }
    
    private DTUncovered[] convertUncovered(Uncovered[] un, IConditionTransformer transformer, DTAnalyzer dtan) {
        DTUncovered[] un2 = new DTUncovered[un.length];
        for (int i = 0; i < un.length; i++) {
            String[] names = un[i].getSolutionNames();
            Object[] values = new Object[names.length];
            for (int j = 0; j < values.length; j++) {
                values[j] = transformer.transformSignatureValueBack(names[j], un[i].getSolutionValues()[j], dtan);
            }

            un2[i] = new DTUncovered(new ArrayOfNamedValues(names, values));
        }
        return un2;
    }

    public DecisionTable getDT() {
        return decisionTable;
    }

    public DTOverlapping[] getOverlappings() {
        return overlappings;
    }

    public DTUncovered[] getUncovered() {
        return uncovered;
    }

    public boolean hasProblems() {
        return overlappings != null && overlappings.length > 0 || uncovered != null && uncovered.length > 0;
    }

    @Override
    public String toString() {
        return "Uncovered: " + Arrays.asList(getUncovered()) + "\n" + "Overlapped: " + Arrays.asList(getOverlappings());
    }

}
