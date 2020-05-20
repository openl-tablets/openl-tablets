package org.openl.rules.webstudio.web.repository;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.rules.webstudio.web.repository.tree.AbstractTreeNode;

/**
 * Returns Tree Node shown in the current request. If for setNode() isn't invoked for the request, returns null. It's
 * needed to disable long history rendering when it is not shown to a user actually. For this reason this bean must be
 * request scoped. View scope or session scope isn't allowed because it can worsen performance.
 */
@ManagedBean
@RequestScoped
public class NodeVersionsBean {
    private AbstractTreeNode node;

    public AbstractTreeNode getNode() {
        return node;
    }

    public void setNode(AbstractTreeNode node) {
        this.node = node;
    }
}
