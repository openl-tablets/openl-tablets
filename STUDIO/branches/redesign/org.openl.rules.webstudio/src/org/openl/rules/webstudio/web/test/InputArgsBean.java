package org.openl.rules.webstudio.web.test;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.openl.rules.testmethod.ExecutionParamDescription;
import org.openl.rules.testmethod.ExecutionParamDescriptionWithType;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.SimpleVM;
import org.richfaces.component.UIRepeat;
import org.richfaces.component.UITree;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

@ManagedBean
@ViewScoped
public class InputArgsBean {
    private String uri;
    private UITree currentTreeNode;
    private ExecutionParamDescription[] arguments;
    private FieldDescriptionTreeNode[] argumentTreeNodes;

    public String getUri() {
        return uri;
    }

    public boolean isMethodHasParameters() {
        return getTestedMethod().getSignature().getNumberOfParameters() > 0;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public UITree getCurrentTreeNode() {
        return currentTreeNode;
    }

    public void setCurrentTreeNode(UITree currentTreeNode) {
        this.currentTreeNode = currentTreeNode;
    }

    public IOpenMethod getTestedMethod() {
        ProjectModel projectModel = WebStudioUtils.getProjectModel();
        return projectModel.getMethod(uri);
    }

    public FieldDescriptionTreeNode getCurrentNode() {
        return (FieldDescriptionTreeNode) currentTreeNode.getRowData();
    }

    public void makeTestSuite() {
        IOpenMethod method = getTestedMethod();
        Object[] arguments = new Object[method.getSignature().getNumberOfParameters()];
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = argumentTreeNodes[i].getValueForced();
        }
        TestDescription testDescription = new TestDescription(method, arguments);
        TestSuite testSuite = new TestSuite(testDescription);
        WebStudioUtils.getProjectModel().setLastTest(testSuite);
    }

    public void initObject() {
        FieldDescriptionTreeNode currentnode = getCurrentNode();
        IOpenClass fieldType = currentnode.getFieldType();
        currentnode.setValueForced(fieldType.newInstance(new SimpleVM().getRuntimeEnv()));
    }

    public void initCollection() {
        FieldDescriptionTreeNode currentnode = getCurrentNode();
        IOpenClass fieldType = currentnode.getFieldType();

        IAggregateInfo info = fieldType.getAggregateInfo();

        Object ary = info.makeIndexedAggregate(info.getComponentType(fieldType), new int[] { 0 });

        currentnode.setValueForced(ary);
    }

    public void disposeObject() {
        FieldDescriptionTreeNode currentnode = getCurrentNode();
        currentnode.setValueForced(null);
    }

    public void deleteFromCollection() {
        FieldDescriptionTreeNode currentNode = getCurrentNode();
        FieldDescriptionTreeNode parentNode = currentNode.getParent();
        IOpenClass arrayType = parentNode.getFieldType();

        IAggregateInfo info = arrayType.getAggregateInfo();
        int elementsCount = parentNode.getChildren().size();

        IOpenClass componentType = currentNode.getFieldType();
        Object ary = info.makeIndexedAggregate(componentType, new int[] { elementsCount - 1 });

        IOpenIndex index = info.getIndex(arrayType, JavaOpenClass.INT);

        int i = 0;
        for (FieldDescriptionTreeNode node : parentNode.getChildren()) {
            if (node != currentNode) {
                index.setValue(ary, new Integer(i), node.getValue());
                i++;
            }
        }
        parentNode.setValueForced(ary);
    }

    public void addToCollection() {
        FieldDescriptionTreeNode currentnode = getCurrentNode();
        IOpenClass fieldType = currentnode.getFieldType();

        IAggregateInfo info = fieldType.getAggregateInfo();
        int elementsCount = currentnode.getChildren().size();

        IOpenClass componentType = info.getComponentType(fieldType);
        Object ary = info.makeIndexedAggregate(componentType, new int[] { elementsCount + 1 });

        IOpenIndex index = info.getIndex(fieldType, JavaOpenClass.INT);

        for (int i = 0; i < elementsCount - 1; i++) {
            Object obj = index.getValue(currentnode.getValue(), new Integer(i));
            index.setValue(ary, new Integer(i), obj);
        }
        currentnode.setValueForced(ary);
    }

    public ExecutionParamDescription[] initArguments() {
        IOpenMethod method = getTestedMethod();
        ExecutionParamDescription[] args = new ExecutionParamDescription[method.getSignature().getNumberOfParameters()];
        for (int i = 0; i < args.length; i++) {
            args[i] = new ExecutionParamDescriptionWithType(method.getSignature().getParameterName(i),
                null,
                method.getSignature().getParameterType(i));
        }
        return args;
    }

    public ExecutionParamDescription[] getArguments() {
        if (arguments == null) {
            arguments = initArguments();
        }
        return arguments;
    }

    public FieldDescriptionTreeNode[] initArgumentTreeNodes() {
        ExecutionParamDescription[] args = getArguments();
        FieldDescriptionTreeNode[] argTreeNodes = new FieldDescriptionTreeNode[args.length];
        for (int i = 0; i < args.length; i++) {
            argTreeNodes[i] = TestTreeBuilder.createNode(args[i].getParamType(),
                args[i].getValue(),
                args[i].getParamName(),
                null);
        }
        return argTreeNodes;
    }

    public FieldDescriptionTreeNode[] getArgumentTreeNodes() {
        if (argumentTreeNodes == null) {
            argumentTreeNodes = initArgumentTreeNodes();
        }
        return argumentTreeNodes;
    }

    private UIRepeat currentArgTreeNode;// FieldDescriptionTreeNode

    public UIRepeat getCurrentArgTreeNode() {
        return currentArgTreeNode;
    }

    public void setCurrentArgTreeNode(UIRepeat currentArgTreeNode) {
        this.currentArgTreeNode = currentArgTreeNode;
    }

    public TreeNode getRoot() {
        FieldDescriptionTreeNode parameter = (FieldDescriptionTreeNode) currentArgTreeNode.getRowData();
        TreeNodeImpl root = new TreeNodeImpl();

        root.addChild(parameter.getFieldName(), parameter);

        return root;
    }
}
