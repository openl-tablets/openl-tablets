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
        children = new ArrayList<AlgorithmTreeNode>();
        labels = new LinkedList<StringValue>();
    }

    public void add(AlgorithmTreeNode node) {
        children.add(node);
    }

    public void addLabel(StringValue label) {
        if (!label.equals("")) {
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

    public void setAlgorithmRow(AlgorithmRow algorithmRow) {
        this.algorithmRow = algorithmRow;
    }

    public void setSpecification(TableParserSpecificationBean specification) {
        this.specification = specification;
    }
}
