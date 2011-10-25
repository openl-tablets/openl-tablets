package org.openl.rules.webstudio.web.test;

import org.openl.base.INameSpacedThing;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.testmethod.ExecutionParamDescription;
import org.openl.types.IOpenClass;
import org.richfaces.model.TreeNodeImpl;

public class FieldDescriptionTreeNode extends TreeNodeImpl {
    private IOpenClass fieldType;
    private String fieldName;
    private Object value;
    private TreeNodeType nodeType;

    public FieldDescriptionTreeNode(String fieldName, Object value, IOpenClass fieldType) {
        this.fieldName = fieldName;
        this.value = value;
        this.fieldType = fieldType;
        initNodeType();
    }

    public FieldDescriptionTreeNode(ExecutionParamDescription paramDescription) {
        this.fieldName = paramDescription.getParamName();
        this.value = paramDescription.getValue();
        this.fieldType = paramDescription.getParamType();
        initNodeType();
    }

    @Override
    public boolean isLeaf() {
        return fieldType.isSimple() || value == null;
    }

    public void initNodeType() {
        if (fieldType.getAggregateInfo().isAggregate(fieldType)) {
            nodeType = new CollectionNodeType(this);
        } else if (isLeaf()) {
            nodeType = new SimpleNodeType(this);
        } else {
            nodeType = new ComplexNodeType(this);
        }
    }

    public IOpenClass getFieldType() {
        return fieldType;
    }

    public String getFieldName() {
        return fieldName;
    }

    protected Object getValue() {
        return value;
    }

    public String getNodeType() {
        return nodeType.getType();
    }

    public String getTreeText() {
        return nodeType.getNodeText();
    }

    private abstract class TreeNodeType {
        private FieldDescriptionTreeNode treeNode;

        public TreeNodeType(FieldDescriptionTreeNode treeNode) {
            this.treeNode = treeNode;
        }

        protected FieldDescriptionTreeNode getTreeNode() {
            return treeNode;
        }

        public String getNodeText() {
            StringBuilder buff = new StringBuilder();
            if (treeNode.getFieldName() != null) {
                buff.append(treeNode.getFieldName());
                buff.append(" = ");
            }
            buff.append(getDisplayedValue());
            return buff.toString();
        }

        public abstract String getType();

        protected abstract String getDisplayedValue();
    }

    private class ComplexNodeType extends TreeNodeType {
        public static final String COMPLEX_TYPE = "complex";

        public ComplexNodeType(FieldDescriptionTreeNode treeNode) {
            super(treeNode);
        }

        @Override
        public String getType() {
            return COMPLEX_TYPE;
        }

        @Override
        protected String getDisplayedValue() {
            return getTreeNode().getFieldType().getDisplayName(INameSpacedThing.SHORT);
        }

    }

    private class SimpleNodeType extends TreeNodeType {
        public static final String SIMPLE_TYPE = "simple";

        public SimpleNodeType(FieldDescriptionTreeNode treeNode) {
            super(treeNode);
        }

        @Override
        public String getType() {
            return SIMPLE_TYPE;
        }

        @Override
        protected String getDisplayedValue() {
            Object value = getTreeNode().getValue();
            return FormattersManager.getFormatter(value).format(value);
        }
    }

    private class CollectionNodeType extends TreeNodeType {
        public static final String COLLECTION_TYPE = "collection";

        public CollectionNodeType(FieldDescriptionTreeNode treeNode) {
            super(treeNode);
        }

        @Override
        public String getType() {
            return COLLECTION_TYPE;
        }

        @Override
        protected String getDisplayedValue() {
            return String.format("Collection of %s",
                getTreeNode().getFieldType().getComponentClass().getDisplayName(INameSpacedThing.SHORT));
        }

    }
}