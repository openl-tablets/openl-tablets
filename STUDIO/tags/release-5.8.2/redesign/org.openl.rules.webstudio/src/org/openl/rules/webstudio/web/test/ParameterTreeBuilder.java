package org.openl.rules.webstudio.web.test;

import java.util.Date;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.types.IOpenClass;
import org.openl.types.java.OpenClassHelper;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

/**
 * @author DLiauchuk
 */
@ManagedBean
@RequestScoped
public class ParameterTreeBuilder {

    public static ParameterDeclarationTreeNode createNode(IOpenClass fieldType,Object value,
            String fieldName, ParameterDeclarationTreeNode parent) {
        if (OpenClassHelper.isCollection(fieldType)) {
            return new CollectionParameterTreeNode(fieldName, value, fieldType, parent);
        } else if (!fieldType.isSimple()) {
            return new ComplexParameterTreeNode(fieldName, value, fieldType, parent);
        } else {
            return new SimpleParameterTreeNode(fieldName, value, fieldType, parent);
        }

    }

    public TreeNode getRoot(Object objParam) {
        ParameterWithValueDeclaration param = (ParameterWithValueDeclaration) objParam;
        TreeNodeImpl root = new TreeNodeImpl();

        ParameterDeclarationTreeNode treeNode = null;
        if (param != null) {
            treeNode = createNode(param.getType(), param.getValue(), null, null);
            root.addChild(param.getName(), treeNode);
        }
        return root;
    }

    public boolean isDateParameter(Object value) {
        return value instanceof Date;
    }

}
