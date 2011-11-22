package org.openl.rules.webstudio.web.test;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.rules.testmethod.ExecutionParamDescription;
import org.openl.types.IOpenClass;
import org.richfaces.component.UIRepeat;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

@ManagedBean
@RequestScoped
public class TestTreeBuilder {
    private UIRepeat executionParam;// ExecutionParamDescription

    public UIRepeat getExecutionParam() {
        return executionParam;
    }

    public void setExecutionParam(UIRepeat tests) {
        this.executionParam = tests;
    }

    public static FieldDescriptionTreeNode createNode(IOpenClass fieldType,
            Object value,
            String fieldName,
            FieldDescriptionTreeNode parent) {
        if (fieldType.getAggregateInfo().isAggregate(fieldType)) {
            return new CollectionFieldNode(fieldName, value, fieldType, parent);
        } else if (!fieldType.isSimple()) {
            return new ComplexFieldNode(fieldName, value, fieldType, parent);
        } else {
            return new SimpleFieldNode(fieldName, value, fieldType, parent);
        }

    }

    public TreeNode getRoot() {
        ExecutionParamDescription parameter = (ExecutionParamDescription) executionParam.getRowData();
        TreeNodeImpl root = new TreeNodeImpl();

        FieldDescriptionTreeNode treeNode = null;
        treeNode = createNode(parameter.getParamType(), parameter.getValue(), null, null);
        root.addChild(parameter.getParamName(), treeNode);

        return root;
    }

}
