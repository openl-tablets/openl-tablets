package org.openl.rules.webstudio.web.test;

import java.util.Iterator;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import org.openl.rules.testmethod.ExecutionParamDescription;
import org.openl.rules.webstudio.web.tableeditor.ShowTableBean;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.richfaces.component.UIRepeat;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

@ManagedBean
@RequestScoped
public class TestTreeBuilder {
    private UIRepeat executionParam;// ExecutionParamDescription

    @ManagedProperty(value = "#{showTableBean}")
    private ShowTableBean showTableBean;

    public ShowTableBean getShowTableBean() {
        return showTableBean;
    }

    public void setShowTableBean(ShowTableBean showTableBean) {
        this.showTableBean = showTableBean;
    }

    public UIRepeat getExecutionParam() {
        return executionParam;
    }

    public void setExecutionParam(UIRepeat tests) {
        this.executionParam = tests;
    }

    private void processNode(FieldDescriptionTreeNode node, IOpenClass type, Object value) {
        if (type.getAggregateInfo().isAggregate(type)) {
            processCollectionNode(node, type, value);
        }
        if (!type.isSimple() && value != null) {
            processSimpleNode(node, type, value);
        }
    }

    // non collection nodes
    private void processSimpleNode(FieldDescriptionTreeNode node, IOpenClass type, Object value) {
        for (String fieldName : type.getFields().keySet()) {
            IOpenField field = type.getField(fieldName);
            if (!field.isConst()) {
                Object fieldValue = field.get(value, null);
                FieldDescriptionTreeNode treeNode = new FieldDescriptionTreeNode(fieldName, fieldValue, field.getType());
                node.addChild(fieldName, treeNode);
                processNode(treeNode, field.getType(), fieldValue);
            }
        }
    }

    private void processCollectionNode(FieldDescriptionTreeNode node, IOpenClass type, Object value) {
        Iterator<Object> iterator = type.getAggregateInfo().getIterator(value);
        IOpenClass arrayElementType = type.getComponentClass();
        while (iterator.hasNext()) {
            Object arrayElement = iterator.next();
            FieldDescriptionTreeNode treeNode = new FieldDescriptionTreeNode(null, arrayElement, arrayElementType);
            node.addChild(arrayElement, treeNode);
            processNode(treeNode, arrayElementType, arrayElement);
        }
    }

    public TreeNode getRoot() {
        ExecutionParamDescription parameter = (ExecutionParamDescription) executionParam.getRowData();
        TreeNodeImpl root = new TreeNodeImpl();

        FieldDescriptionTreeNode treeNode = new FieldDescriptionTreeNode(parameter);
        processNode(treeNode, parameter.getParamType(), parameter.getValue());
        root.addChild(parameter.getParamName(), treeNode);

        return root;
    }

}
