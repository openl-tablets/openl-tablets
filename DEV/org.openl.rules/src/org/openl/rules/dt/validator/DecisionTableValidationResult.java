/**
 * Created Feb 11, 2007
 */
package org.openl.rules.dt.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.Getter;

import org.openl.ie.constrainer.consistencyChecking.Overlapping;
import org.openl.ie.constrainer.consistencyChecking.Overlapping.OverlappingStatus;
import org.openl.ie.constrainer.consistencyChecking.Uncovered;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.validator.IValidationResult;
import org.openl.util.ArrayOfNamedValues;

/**
 * @author snshor
 */
public class DecisionTableValidationResult implements IValidationResult {

    @Getter
    private final IDecisionTable decisionTable;
    @Getter
    private final DecisionTableOverlapping[] overlappings;
    @Getter
    private final DecisionTableUncovered[] uncovered;

    public DecisionTableValidationResult(IDecisionTable decisionTable) {
        this.decisionTable = decisionTable;
        this.overlappings = new DecisionTableOverlapping[]{};
        this.uncovered = new DecisionTableUncovered[]{};
    }

    public DecisionTableValidationResult(IDecisionTable decisionTable,
                                         Overlapping[] overlappings,
                                         Uncovered[] uncovered,
                                         IConditionTransformer transformer,
                                         DecisionTableAnalyzer analyzer) {

        this.decisionTable = decisionTable;
        this.overlappings = convertOverlappings(overlappings, transformer, analyzer);
        this.uncovered = convertUncovered(uncovered, transformer, analyzer);
    }

    private DecisionTableOverlapping[] convertOverlappings(Overlapping[] overlappings,
                                                           IConditionTransformer transformer,
                                                           DecisionTableAnalyzer analyzer) {

        DecisionTableOverlapping[] tableOverlappings = new DecisionTableOverlapping[overlappings.length];

        for (var i = 0; i < overlappings.length; i++) {

            var names = overlappings[i].getSolutionNames();
            Object[] values = new Object[names.length];

            for (var j = 0; j < values.length; j++) {
                values[j] = transformer
                        .transformSignatureValueBack(names[j], overlappings[i].getSolutionValues()[j], analyzer);
            }

            var tableOverlapping = new DecisionTableOverlapping(overlappings[i].getOverlapped(),
                    new ArrayOfNamedValues(names, values),
                    overlappings[i].getStatus());

            tableOverlappings[i] = tableOverlapping;
        }

        return tableOverlappings;
    }

    private DecisionTableUncovered[] convertUncovered(Uncovered[] uncovered,
                                                      IConditionTransformer transformer,
                                                      DecisionTableAnalyzer analyzer) {

        DecisionTableUncovered[] tableUncovered = new DecisionTableUncovered[uncovered.length];

        for (var i = 0; i < uncovered.length; i++) {

            var names = uncovered[i].getSolutionNames();
            Object[] values = new Object[names.length];

            for (var j = 0; j < values.length; j++) {
                values[j] = transformer
                        .transformSignatureValueBack(names[j], uncovered[i].getSolutionValues()[j], analyzer);
            }

            tableUncovered[i] = new DecisionTableUncovered(new ArrayOfNamedValues(names, values));
        }

        return tableUncovered;
    }

    public boolean hasProblems() {

        return hasErrors() || hasWarnings();
        // return overlappings != null && overlappings.length > 0 || uncovered != null && uncovered.length > 0;
    }

    public boolean hasErrors() {
        return !getOverlappingBlocks().isEmpty() || uncovered != null && uncovered.length > 0;
    }

    public boolean hasWarnings() {
        return !getOverlappingPartialOverlaps().isEmpty();
    }

    public List<DecisionTableOverlapping> getOverlappingBlocks() {
        return selectOverlappings(OverlappingStatus.BLOCK);
    }

    public List<DecisionTableOverlapping> getOverlappingPartialOverlaps() {
        return selectOverlappings(OverlappingStatus.PARTIAL);
    }

    public List<DecisionTableOverlapping> getOverlappingOverrides() {
        return selectOverlappings(OverlappingStatus.OVERRIDE);
    }

    private List<DecisionTableOverlapping> selectOverlappings(OverlappingStatus status) {
        var res = new ArrayList<DecisionTableOverlapping>();
        if (overlappings == null) {
            return res;
        }
        for (DecisionTableOverlapping overlapping : overlappings) {
            if (overlapping.getStatus() == status) {
                res.add(overlapping);
            }
        }
        return res;
    }

    @Override
    public String toString() {
        var validationResultDetails = new StringBuilder();

        if (getUncovered().length > 0) {
            validationResultDetails
                    .append("There is an uncovered case for values: %s\r\n".formatted(Arrays.asList(getUncovered())));
        }

        // if (getOverlappings().length > 0) {
        // validationResultDetails.append(String.format("There is an overlap: %s", Arrays.asList(getOverlappings())));
        // }

        var maxCounter = 3;
        var cnt = 0;

        for (DecisionTableOverlapping ovl : getOverlappingBlocks()) {
            if (++cnt < maxCounter) {
                validationResultDetails.append(ovl.toString()).append("\r\n");
            }
        }

        for (DecisionTableOverlapping ovl : getOverlappingPartialOverlaps()) {
            if (++cnt <= maxCounter) {
                validationResultDetails.append(ovl.toString()).append("\r\n");
            }
        }

        for (DecisionTableOverlapping ovl : getOverlappingOverrides()) {
            if (++cnt <= maxCounter) {
                validationResultDetails.append(ovl.toString()).append("\r\n");
            }
        }

        if (cnt > maxCounter) {
            validationResultDetails.append("  %d more ...".formatted(cnt - maxCounter));
        }

        return validationResultDetails.toString();
    }

}
