package org.openl.rules.webstudio.web.test;

import com.fasterxml.jackson.core.JsonParseException;
import org.openl.base.INamedThing;
import org.openl.rules.serialization.JsonUtils;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.ui.Message;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.tablewizard.DomainTree;
import org.openl.rules.ui.tablewizard.WizardUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.StringUtils;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;
import org.richfaces.component.UITree;
import org.richfaces.model.SequenceRowKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@ManagedBean
@ViewScoped
public class InputArgsBean {
    private final Logger log = LoggerFactory.getLogger(InputArgsBean.class);

    private String uri;
    private UITree currentTreeNode;
    private ParameterWithValueDeclaration[] arguments;
    private ParameterDeclarationTreeNode[] argumentTreeNodes;
    private String className;
    private Map<String, ComplexParameterTreeNode> complexParameters = new HashMap<>();
    private InputTestCaseType inputTestCaseType = InputTestCaseType.BEAN;
    private String inputTextBean;

    enum InputTestCaseType {
        TEXT,
        BEAN
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getUri() {
        return uri;
    }

    public boolean isMethodHasParameters() {
        try {
            IOpenMethod testMethod = getTestedMethod();
            return testMethod != null && testMethod.getSignature().getNumberOfParameters() > 0;
        } catch (Exception | LinkageError e) {
            log.error(e.getMessage(), e);
            return false;
        }
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

    private IOpenMethod getTestedMethod() {
        ProjectModel projectModel = WebStudioUtils.getProjectModel();
        return projectModel.getMethod(uri);
    }

    private ParameterDeclarationTreeNode getCurrentNode() {
        return (ParameterDeclarationTreeNode) currentTreeNode.getRowData();
    }

    public void fillBean() {
        if (StringUtils.isNotBlank(inputTextBean) && InputTestCaseType.BEAN
                .equals(inputTestCaseType) && argumentTreeNodes != null) {
            try {
                Map<String, String> stringStringMap = JsonUtils.splitJSON(inputTextBean);
                if (stringStringMap.isEmpty()) {
                    validateFirstJsonSymbol(inputTextBean);
                }
                for (ParameterDeclarationTreeNode arg : argumentTreeNodes) {
                    String field = stringStringMap.get(arg.getName());
                    if (field != null) {
                        arg.setValueForced(JsonUtils.fromJSON(field, arg.getType().getInstanceClass()));
                    } else if (argumentTreeNodes.length == 1) {
                        argumentTreeNodes[0].setValueForced(JsonUtils.fromJSON(inputTextBean, argumentTreeNodes[0].getType().getInstanceClass()));
                    }
                }
            } catch (IOException e) {
                throw new Message(constructJsonExceptionMessage(e));
            }
        }
    }

    public Object[] getParams() {
        if (argumentTreeNodes == null) {
            return new Object[0];
        }
        Map<String, String> stringStringMap = null;
        if (InputTestCaseType.TEXT.equals(inputTestCaseType) && StringUtils.isNotBlank(inputTextBean)) {
            try {
                stringStringMap = JsonUtils.splitJSON(inputTextBean);
                if (stringStringMap.isEmpty()) {
                    validateFirstJsonSymbol(inputTextBean);
                }
            } catch (IOException e) {
                throw new Message(constructJsonExceptionMessage(e));
            }
        }
        Object[] parsedArguments = new Object[argumentTreeNodes.length];
        try {
            for (int i = 0; i < argumentTreeNodes.length; i++) {
                if (InputTestCaseType.TEXT.equals(inputTestCaseType) && stringStringMap != null) {
                    String field = stringStringMap.get(argumentTreeNodes[i].getName());
                    if (field != null) {
                        parsedArguments[i] = JsonUtils.fromJSON(field, argumentTreeNodes[i].getType().getInstanceClass());
                    } else if (argumentTreeNodes.length == 1) {
                        parsedArguments[i] = JsonUtils.fromJSON(inputTextBean, argumentTreeNodes[i].getType().getInstanceClass());
                    }
                } else {
                    parsedArguments[i] = argumentTreeNodes[i].getValueForced();
                }
            }
        } catch (RuntimeException e) {
            if (e instanceof IllegalArgumentException || e.getCause() instanceof IllegalArgumentException) {
                throw new Message("Input parameters are wrong.");
            } else {
                throw e;
            }
        } catch (IOException e) {
            throw new Message(constructJsonExceptionMessage(e));
        }
        return parsedArguments;
    }

    private void validateFirstJsonSymbol(String inputJson) {
        if (!inputJson.isEmpty() && (!inputJson.startsWith("{") && !inputJson.startsWith("["))) {
            // the only case for which jackson does not generate a clear error message
            throw new Message("Invalid JSON: should start with '{' or '[' symbol");
        }
    }

    private String constructJsonExceptionMessage(IOException e) {
        if (e instanceof JsonParseException) {
            return String.format("%s</br>[line: %s, column: %s]",
                    ((JsonParseException) e).getOriginalMessage(),
                    ((JsonParseException) e).getLocation().getLineNr(),
                    ((JsonParseException) e).getLocation().getColumnNr());
        }
        return "Input parameters are wrong.";
    }

    public void initObject() {
        ComplexParameterTreeNode currentNode = (ComplexParameterTreeNode) getCurrentNode();
        IOpenClass fieldType = currentNode.getTypeToCreate();
        if (fieldType == null) {
            fieldType = currentNode.getType();
        }

        ParameterDeclarationTreeNode parent = currentNode.getParent();
        Object value = ParameterTreeBuilder.canConstruct(fieldType)
                ? fieldType
                .newInstance(new SimpleVM().getRuntimeEnv())
                : null;
        ParameterRenderConfig config = new ParameterRenderConfig.Builder(fieldType, value)
                .fieldNameInParent(currentNode.getName())
                .parent(parent)
                .build();
        ParameterDeclarationTreeNode newNode = ParameterTreeBuilder.createNode(config);
        currentNode.setValueForced(newNode.getValueForced());

        if (parent != null) {
            parent.replaceChild(currentNode, newNode);
        }
    }

    public void initCollection() {
        ParameterDeclarationTreeNode currentnode = getCurrentNode();
        IOpenClass fieldType = currentnode.getType();

        IAggregateInfo info = fieldType.getAggregateInfo();

        Object ary = info.makeIndexedAggregate(info.getComponentType(fieldType), 0);

        currentnode.setValueForced(ary);
    }

    public void disposeObject() {
        ParameterDeclarationTreeNode currentnode = getCurrentNode();
        currentnode.setValueForced(null);
    }

    public void deleteFromCollection() {
        ParameterDeclarationTreeNode currentNode = getCurrentNode();
        ((CollectionParameterTreeNode) currentNode.getParent()).removeChild(currentNode);
    }

    public void addToCollection() {
        ParameterDeclarationTreeNode currentnode = getCurrentNode();
        currentnode.addChild(currentnode.getChildren().size() + 1, null);
    }

    private ParameterWithValueDeclaration[] initArguments() {
        IOpenMethod method = getTestedMethod();
        ParameterWithValueDeclaration[] args = new ParameterWithValueDeclaration[method.getSignature()
                .getNumberOfParameters()];
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        for (int i = 0; i < args.length; i++) {
            String parameterName = method.getSignature().getParameterName(i);
            IOpenClass parameterType = method.getSignature().getParameterType(i);
            Object parameterValue = null;
            try {
                // No need to instantiate the class with compile error and spam the logs.
                if (ParameterTreeBuilder.canConstruct(parameterType)) {
                    parameterValue = parameterType.newInstance(env);
                }
            } catch (Exception ignored) {
            }
            args[i] = new ParameterWithValueDeclaration(parameterName, parameterValue, parameterType);
        }
        return args;
    }

    public ParameterWithValueDeclaration[] getArguments() {
        try {
            if (arguments == null) {
                arguments = initArguments();
            }
            return arguments;
        } catch (Exception | LinkageError e) {
            log.error(e.getMessage(), e);
            return new ParameterWithValueDeclaration[0];
        }
    }

    private ParameterDeclarationTreeNode[] initArgumentTreeNodes() {
        ParameterWithValueDeclaration[] args = getArguments();
        ParameterDeclarationTreeNode[] argTreeNodes = new ParameterDeclarationTreeNode[args.length];
        for (int i = 0; i < args.length; i++) {
            ParameterRenderConfig config = new ParameterRenderConfig.Builder(args[i].getType(), args[i].getValue())
                    .fieldNameInParent(args[i].getName())
                    .build();
            argTreeNodes[i] = ParameterTreeBuilder.createNode(config);
        }
        return argTreeNodes;
    }

    public ParameterDeclarationTreeNode[] getArgumentTreeNodes() {
        try {
            if (argumentTreeNodes == null) {
                argumentTreeNodes = initArgumentTreeNodes();
            }
            return argumentTreeNodes;
        } catch (Exception | LinkageError e) {
            log.error(e.getMessage(), e);
            return new ParameterDeclarationTreeNode[0];
        }
    }

    public SelectItem[] getPossibleTypes(ParameterDeclarationTreeNode currentNode) {
        try {
            Collection<IOpenClass> allClasses = getAllClasses(currentNode);

            Collection<SelectItem> result = new ArrayList<>();
            for (IOpenClass type : allClasses) {
                result.add(new SelectItem(type.getName(), type.getDisplayName(INamedThing.SHORT)));
            }

            return result.toArray(new SelectItem[0]);
        } catch (Exception | LinkageError e) {
            log.error(e.getMessage(), e);
            return new SelectItem[0];
        }
    }

    private Collection<IOpenClass> getAllClasses(ParameterDeclarationTreeNode currentNode) {
        IOpenClass parentType = currentNode.getType();

        DomainTree domainTree = DomainTree.buildTree(WizardUtils.getProjectOpenClass());
        Collection<IOpenClass> allClasses = new ArrayList<>();

        for (IOpenClass type : domainTree.getAllOpenClasses()) {
            if (type.isAbstract() || !parentType.isAssignableFrom(type)) {
                continue;
            }

            allClasses.add(type);
        }

        for (IOpenClass type : WizardUtils.getImportedClasses()) {
            if (type.isAbstract() || !parentType.isAssignableFrom(type)) {
                continue;
            }
            allClasses.add(type);
        }
        return allClasses;
    }

    public String getRow(ComplexParameterTreeNode node, SequenceRowKey rowKey) {
        String row = StringUtils.join(rowKey.getSimpleKeys(), "_");
        complexParameters.put(row, node);
        return row;
    }

    public String getComplexNodeTypes() {
        return null;
    }

    public void setComplexNodeTypes(String nodeValuesString) {
        String[] nodeValues = nodeValuesString.split(",");
        for (String nodeValue : nodeValues) {
            if (StringUtils.isBlank(nodeValue)) {
                continue;
            }
            String[] parts = nodeValue.split("=");
            String row = parts[0];
            String typeName = parts[1];
            ComplexParameterTreeNode node = complexParameters.get(row);
            for (IOpenClass type : getAllClasses(node)) {
                if (typeName.equals(type.getName())) {
                    node.setTypeToCreate(type);
                    break;
                }
            }
        }
    }

    public InputTestCaseType getInputTestCaseType() {
        return inputTestCaseType;
    }

    public void setInputTestCaseType(InputTestCaseType inputTestCaseType) {
        this.inputTestCaseType = inputTestCaseType;
    }

    public String getInputTextBean() {
        return inputTextBean;
    }

    public void setInputTextBean(String inputTextBean) {
        this.inputTextBean = inputTextBean;
    }
}
