package org.openl.rules.tbasic;

import java.util.List;

public class AlgorithmTreeNode {

    private AlgorithmRow algorithmRow;
    private List<AlgorithmTreeNode> children;
    private TableParserSpecificationBean specification;

    public AlgorithmRow getAlgorithmRow() {
        return algorithmRow;
    }

    public void setAlgorithmRow(AlgorithmRow algorithmRow) {
        this.algorithmRow = algorithmRow;
    }

    public List<AlgorithmTreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<AlgorithmTreeNode> children) {
        this.children = children;
    }

    public TableParserSpecificationBean getSpecification() {
        return specification;
    }

    public void setSpecification(TableParserSpecificationBean specification) {
        this.specification = specification;
    }
}
