package org.openl.rules.tbasic;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.openl.meta.StringValue;

public class AlgorithmTreeNode {

    private AlgorithmRow algorithmRow;
    private TableParserSpecificationBean specification;
    private final List<StringValue> labels;
    private final List<AlgorithmTreeNode> children;

    public AlgorithmTreeNode() {
        children = new ArrayList<>();
        labels = new LinkedList<>();
    }

    public void add(AlgorithmTreeNode node) {
        children.add(node);
    }

    public void addLabel(StringValue label) {
        if (!label.getValue().equals("")) {
            labels.add(label);
        }
    }

    public AlgorithmRow getAlgorithmRow() {
        return algorithmRow;
    }

    public List<AlgorithmTreeNode> getChildren() {
        return children;
    }

    public List<StringValue> getLabels() {
        return labels;
    }

    public TableParserSpecificationBean getSpecification() {
        return specification;
    }

    public String getSpecificationKeyword() {
        if (specification != null) {
            return specification.getKeyword();
        }
        return null;
    }

    public void setAlgorithmRow(AlgorithmRow algorithmRow) {
        this.algorithmRow = algorithmRow;
    }

    public void setSpecification(TableParserSpecificationBean specification) {
        this.specification = specification;
    }

    @Override
    public String toString() {
        if (specification != null) {
            StringBuilder buf = new StringBuilder();
            buf.append("Specification Keyword : ");
            buf.append(specification.getKeyword());
            if (algorithmRow != null) {
                buf.append(". Row : ");
                buf.append(algorithmRow);
            }
            return buf.toString();
        }
        return super.toString();
    }
}
