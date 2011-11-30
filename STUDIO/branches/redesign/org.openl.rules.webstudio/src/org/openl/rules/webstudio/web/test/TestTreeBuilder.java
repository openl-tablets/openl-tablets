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
/**
 * TODO: refactor, rename to ParameterTreeBuilder
 * @author DLiauchuk
 *
 */
public class TestTreeBuilder {
    
    public TestTreeBuilder() {
        System.out.println();
    }
    private UIRepeat executionParam;// ExecutionParamDescription

    public UIRepeat getExecutionParam() {
        return executionParam;
    }

    public void setExecutionParam(UIRepeat parameter) {
        this.executionParam = parameter;
    }

    public static FieldDescriptionTreeNode createNode(IOpenClass fieldType,
            Object value,
            String fieldName,
            FieldDescriptionTreeNode parent) {
        if (fieldType.getAggregateInfo()!= null && fieldType.getAggregateInfo().isAggregate(fieldType)) {
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
        if (parameter != null) {
            treeNode = createNode(parameter.getType(), parameter.getValue(), null, null);
            root.addChild(parameter.getName(), treeNode);
        }
        return root;
    }

}
