/**
 *
 */
package org.openl.rules.tbasic.compile;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.openl.rules.tbasic.TBasicSpecificationKey;

/**
 * @author User
 *
 */
public class LabelManager {
    private class LabelType {
        private boolean loopLabel;
        private String name;

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof LabelType)) {
                return false;
            }

            LabelType otherLabelType = (LabelType) other;

            return name.equals(otherLabelType.name) && loopLabel == otherLabelType.loopLabel;
        }

        /**
         * @return the labelType
         */
        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            // FIXME
            return name.hashCode() + (loopLabel ? 11 : 0);
        }

        /**
         * @return the isLoopLabel
         */
        public boolean isLoopLabel() {
            return loopLabel;
        }

        /**
         * @param labelType the labelType to set
         */
        public void setLabelType(String labelType) {
            name = labelType;
        }

        /**
         * @param isLoopLabel the isLoopLabel to set
         */
        public void setLoopLabel(boolean isLoopLabel) {
            loopLabel = isLoopLabel;
        }
    }

    private static final String LABEL_INSTRUCTION_PREFIX = "gen_label_";

    private Map<LabelType, String> currentLabels;
    private boolean isLoopOperationSet;
    private Stack<Map<LabelType, String>> labelsStack = new Stack<>();

    private int nextLabelNumber;

    public void finishOperationsSet() {
        currentLabels = null;
        if (!labelsStack.isEmpty()) {
            currentLabels = labelsStack.pop();
        }
    }

    public void generateAllLabels(String[] labelInstructions) {
        for (String labelInstruction : labelInstructions) {
            if (labelInstruction != null) {
                LabelType labelType = getLabelType(labelInstruction);
                generateLabel(labelType);
            }
        }
    }

    public String generateLabel(LabelType labelType) {
        String namePrefix = labelType.getName();
        String label = namePrefix + "Label" + nextLabelNumber++;

        // TODO register label in cash, code mess
        currentLabels.put(labelType, label);

        return label;
    }

    private String getExistingLabel(Map<LabelType, String> existingLabels, LabelType labelType) {
        String label = null;

        if (existingLabels.containsKey(labelType)) {
            label = existingLabels.get(labelType);
        } else if (!isLoopOperationSet && labelType.isLoopLabel()) {
            // TODO not very good we use field for recursive action
            label = getLabelFromStack(labelType);
        }

        return label;
    }

    public String getLabelByInstruction(String labelInstruction) {
        LabelType labelType = getLabelType(labelInstruction);

        String label = getExistingLabel(currentLabels, labelType);

        if (label == null) {
            label = generateLabel(labelType);
        }

        return label;
    }

    private String getLabelFromStack(LabelType labelType) {
        Map<LabelType, String> stackedLabels; // get from stack previous piece

        // FIXME eliminate pop and push to stack, just iterate
        if (!labelsStack.isEmpty()) {
            stackedLabels = labelsStack.pop();
        } else {
            throw new RuntimeException("Smth wrong in labels.....");
        }
        String label = getExistingLabel(stackedLabels, labelType);

        labelsStack.push(stackedLabels);

        return label;
    }

    /**
     * @param labelInstruction
     * @return
     */
    private LabelType getLabelType(String labelInstruction) {
        if (!isLabelInstruction(labelInstruction)) {
            // FIXME
            throw new RuntimeException("Smth wrong.........");
        }

        LabelType labelType = getLabelTypeByInstruction(labelInstruction);

        return labelType;
    }

    // TODO
    private LabelType getLabelTypeByInstruction(String labelInstruction) {
        String instruction = labelInstruction.substring(LABEL_INSTRUCTION_PREFIX.length());

        String loopKeyword = "loop";
        String[] instructionParts = instruction.split("_");

        LabelType labelType = new LabelType();

        // label should contain 1 or 2 parts, first with label name, second with
        // loop keyword
        if (instructionParts.length < 1 || instructionParts.length > 2 || instructionParts.length == 2 && !loopKeyword
            .equals(instructionParts[1])) {
            // FIXME
            throw new RuntimeException("Bad gen label instruction....");
        }

        labelType.setLabelType(instructionParts[0]);
        if (instructionParts.length > 1) {
            labelType.setLoopLabel(loopKeyword.equals(instructionParts[1]));
        }

        return labelType;
    }

    public boolean isLabelInstruction(String labelInstruction) {
        return labelInstruction.startsWith(LABEL_INSTRUCTION_PREFIX);
    }

    public void startOperationsSet(boolean isLoopOperationSet) {
        if (currentLabels != null) {
            labelsStack.push(currentLabels);
        }
        currentLabels = new HashMap<>();
        this.isLoopOperationSet = isLoopOperationSet;

    }

    /**
     * Checks if the label instructions represents are return instruction.
     *
     * @param labelInstruction
     * @return true if label instruction is a return one.
     */
    public boolean isReturnInstruction(String labelInstruction) {
        return labelInstruction.startsWith(TBasicSpecificationKey.RETURN.toString());
    }

}
