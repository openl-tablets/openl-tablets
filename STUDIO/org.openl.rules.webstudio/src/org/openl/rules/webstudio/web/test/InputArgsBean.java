package org.openl.rules.webstudio.web.test;

import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.ui.Message;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.*;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;
import org.richfaces.component.UITree;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

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
        IOpenMethod testMethod = getTestedMethod();
        return testMethod != null && testMethod.getSignature().getNumberOfParameters() > 0;
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

    public Object[] getParams() {
        if (argumentTreeNodes == null) {
            return new Object[0];
        }
        Object[] arguments = new Object[argumentTreeNodes.length];
        try {
            for (int i = 0; i < argumentTreeNodes.length; i++) {
                arguments[i] = argumentTreeNodes[i].getValueForced();
            }
        } catch (RuntimeException e) {
            if (e instanceof IllegalArgumentException || e.getCause() instanceof IllegalArgumentException) {
                throw new Message("Input parameters are wrong.");
            } else {
                throw e;
            }
        }
        return arguments;
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

        Object ary = info.makeIndexedAggregate(info.getComponentType(fieldType), new int[]{0});

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
        Object ary = info.makeIndexedAggregate(componentType, new int[]{elementsCount - 1});

        IOpenIndex index = info.getIndex(arrayType, JavaOpenClass.INT);

        int i = 0;
        for (ParameterDeclarationTreeNode node : parentNode.getChildren()) {
            if (node != currentNode) {
                index.setValue(ary, i, node.getValue());
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
        Object ary = info.makeIndexedAggregate(componentType, new int[]{elementsCount + 1});

        IOpenIndex index = info.getIndex(fieldType, JavaOpenClass.INT);

        for (int i = 0; i < elementsCount - 1; i++) {
            Object obj = index.getValue(currentnode.getValue(), i);
            index.setValue(ary, i, obj);
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
            } catch (Exception ignored) {
            }
            args[i] = new ParameterWithValueDeclaration(parameterName, parameterValue, parameterType);
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
}
