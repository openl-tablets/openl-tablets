package org.openl.rules.webstudio.web.test;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.types.IOpenClass;
import org.openl.types.java.OpenClassHelper;
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

    public static ParameterDeclarationTreeNode createNode(IOpenClass fieldType,
            Object value,
            String fieldName,
            ParameterDeclarationTreeNode parent) {
        if (OpenClassHelper.isCollection(fieldType)) {
            return new CollectionParameterTreeNode(fieldName, value, fieldType, parent);
        } else if (!fieldType.isSimple()) {
            return new ComplexParameterTreeNode(fieldName, value, fieldType, parent);
        } else {
            return new SimpleParameterTreeNode(fieldName, value, fieldType, parent);
        }

    }

    public TreeNode getRoot() {
        ParameterWithValueDeclaration parameter = (ParameterWithValueDeclaration) executionParam.getRowData();
        TreeNodeImpl root = new TreeNodeImpl();

        ParameterDeclarationTreeNode treeNode = null;
        if (parameter != null) {
            treeNode = createNode(parameter.getType(), parameter.getValue(), null, null);
            root.addChild(parameter.getName(), treeNode);
        }
        return root;
    }

}
