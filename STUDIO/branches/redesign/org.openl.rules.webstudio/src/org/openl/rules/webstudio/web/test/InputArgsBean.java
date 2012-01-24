package org.openl.rules.webstudio.web.test;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;
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
    private ParameterWithValueDeclaration[] arguments;
    private ParameterDeclarationTreeNode[] argumentTreeNodes;

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

    public ParameterDeclarationTreeNode getCurrentNode() {
        return (ParameterDeclarationTreeNode) currentTreeNode.getRowData();
    }

    public void makeTestSuite() {
        IOpenMethod method = getTestedMethod();
        Object[] arguments = new Object[method.getSignature().getNumberOfParameters()];
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = argumentTreeNodes[i].getValueForced();
        }
        TestDescription testDescription = new TestDescription(method, arguments);
        TestSuite testSuite = new TestSuite(testDescription);
        WebStudioUtils.getProjectModel().addTestSuiteToRun(testSuite);
    }

    public void initObject() {
        ParameterDeclarationTreeNode currentnode = getCurrentNode();
        IOpenClass fieldType = currentnode.getType();
        currentnode.setValueForced(fieldType.newInstance(new SimpleVM().getRuntimeEnv()));
    }

    public void initCollection() {
        ParameterDeclarationTreeNode currentnode = getCurrentNode();
        IOpenClass fieldType = currentnode.getType();

        IAggregateInfo info = fieldType.getAggregateInfo();

        Object ary = info.makeIndexedAggregate(info.getComponentType(fieldType), new int[] { 0 });

        currentnode.setValueForced(ary);
    }

    public void disposeObject() {
        ParameterDeclarationTreeNode currentnode = getCurrentNode();
        currentnode.setValueForced(null);
    }

    public void deleteFromCollection() {
        ParameterDeclarationTreeNode currentNode = getCurrentNode();
        ParameterDeclarationTreeNode parentNode = currentNode.getParent();
        IOpenClass arrayType = parentNode.getType();

        IAggregateInfo info = arrayType.getAggregateInfo();
        int elementsCount = parentNode.getChildren().size();

        IOpenClass componentType = currentNode.getType();
        Object ary = info.makeIndexedAggregate(componentType, new int[] { elementsCount - 1 });

        IOpenIndex index = info.getIndex(arrayType, JavaOpenClass.INT);

        int i = 0;
        for (ParameterDeclarationTreeNode node : parentNode.getChildren()) {
            if (node != currentNode) {
                index.setValue(ary, new Integer(i), node.getValue());
                i++;
            }
        }
        parentNode.setValueForced(ary);
    }

    public void addToCollection() {
        ParameterDeclarationTreeNode currentnode = getCurrentNode();
        IOpenClass fieldType = currentnode.getType();

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

    public ParameterWithValueDeclaration[] initArguments() {
        IOpenMethod method = getTestedMethod();
        ParameterWithValueDeclaration[] args = new ParameterWithValueDeclaration[method.getSignature().getNumberOfParameters()];
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        for (int i = 0; i < args.length; i++) {
            String parameterName = method.getSignature().getParameterName(i);
            IOpenClass parameterType = method.getSignature().getParameterType(i);
            Object parameterValue = null;
            try {
                parameterValue = parameterType.newInstance(env);
            } catch (Exception e) {

            }
            args[i] = new ParameterWithValueDeclaration(parameterName, parameterValue, parameterType, IParameterDeclaration.IN);
        }
        return args;
    }

    public ParameterWithValueDeclaration[] getArguments() {
        if (arguments == null) {
            arguments = initArguments();
        }
        return arguments;
    }

    public ParameterDeclarationTreeNode[] initArgumentTreeNodes() {
        ParameterWithValueDeclaration[] args = getArguments();
        ParameterDeclarationTreeNode[] argTreeNodes = new ParameterDeclarationTreeNode[args.length];
        for (int i = 0; i < args.length; i++) {
            argTreeNodes[i] = ParameterTreeBuilder.createNode(args[i].getType(),
                args[i].getValue(),
                args[i].getName(),
                null);
        }
        return argTreeNodes;
    }

    public ParameterDeclarationTreeNode[] getArgumentTreeNodes() {
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
        ParameterDeclarationTreeNode parameter = (ParameterDeclarationTreeNode) currentArgTreeNode.getRowData();
        TreeNodeImpl root = new TreeNodeImpl();

        root.addChild(parameter.getName(), parameter);

        return root;
    }
}
