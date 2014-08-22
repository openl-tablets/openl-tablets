package org.openl.rules.webstudio.web.test;

import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

/**
 * A helper class which contains utility methods.
 */
@ManagedBean
@RequestScoped
public final class Helper {

    Helper() {
        // THIS CONSTRUCTOR MUST BE EMPTY!!!
    }

    public TreeNode getRoot(ParameterDeclarationTreeNode parameter) {
        TreeNodeImpl root = new TreeNodeImpl();
        root.addChild(parameter.getName(), parameter);
        return root;
    }
}
