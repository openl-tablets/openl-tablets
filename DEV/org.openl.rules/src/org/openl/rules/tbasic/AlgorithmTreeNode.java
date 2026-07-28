package org.openl.rules.tbasic;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.openl.meta.StringValue;

public class AlgorithmTreeNode {

    @Getter
    @Setter
    private AlgorithmRow algorithmRow;
    @Getter
    @Setter
    private TableParserSpecificationBean specification;
    @Getter
    private final List<StringValue> labels;
    @Getter
    private final List<AlgorithmTreeNode> children;

    public AlgorithmTreeNode() {
        children = new ArrayList<>();
        labels = new LinkedList<>();
    }

    public void add(AlgorithmTreeNode node) {
        children.add(node);
    }

    public void addLabel(StringValue label) {
        if (!"".equals(label.getValue())) {
            labels.add(label);
        }
    }

    public String getSpecificationKeyword() {
        if (specification != null) {
            return specification.getKeyword();
        }
        return null;
    }

    @Override
    public String toString() {
        if (specification != null) {
            var buf = new StringBuilder();
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
