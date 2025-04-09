package org.openl.rules.webstudio.web.test;

import static org.openl.types.java.JavaOpenClass.BOOLEAN;
import static org.openl.types.java.JavaOpenClass.BYTE;
import static org.openl.types.java.JavaOpenClass.CHAR;
import static org.openl.types.java.JavaOpenClass.DOUBLE;
import static org.openl.types.java.JavaOpenClass.FLOAT;
import static org.openl.types.java.JavaOpenClass.INT;
import static org.openl.types.java.JavaOpenClass.LONG;
import static org.openl.types.java.JavaOpenClass.SHORT;
import static org.openl.types.java.JavaOpenClass.STRING;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.model.SelectItem;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.richfaces.component.UITree;
import org.richfaces.model.SequenceRowKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import org.openl.base.INamedThing;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.serialization.JsonUtils;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.ui.Message;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.tablewizard.WizardUtils;
import org.openl.rules.webstudio.web.jsf.annotation.ViewScope;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.StringUtils;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

@Service
@ViewScope
public class InputArgsBean {
    private final Logger log = LoggerFactory.getLogger(InputArgsBean.class);

    @Autowired
    private Environment environment;

    private String uri;
    private UITree currentTreeNode;
    private ParameterWithValueDeclaration[] arguments;
    private ParameterDeclarationTreeNode[] argumentTreeNodes;
    private IRulesRuntimeContext runtimeContext;
    private String className;
    private final Map<String, ComplexParameterTreeNode> complexParameters = new HashMap<>();
    private InputTestCaseType inputTestCaseType = InputTestCaseType.BEAN;
    private String inputTextBean;

    private static final List<IOpenClass> predefinedTypes;

    static {
        predefinedTypes = new ArrayList<>();

        // The most popular
        predefinedTypes.add(STRING);
        predefinedTypes.add(JavaOpenClass.getOpenClass(Double.class));
        predefinedTypes.add(JavaOpenClass.getOpenClass(Integer.class));
        predefinedTypes.add(JavaOpenClass.getOpenClass(Boolean.class));
        predefinedTypes.add(JavaOpenClass.getOpenClass(Date.class));

        predefinedTypes.add(JavaOpenClass.getOpenClass(BigInteger.class));
        predefinedTypes.add(JavaOpenClass.getOpenClass(BigDecimal.class));

        predefinedTypes.add(JavaOpenClass.getOpenClass(IntRange.class));
        predefinedTypes.add(JavaOpenClass.getOpenClass(DoubleRange.class));

        predefinedTypes.add(JavaOpenClass.getOpenClass(Long.class));
        predefinedTypes.add(JavaOpenClass.getOpenClass(Float.class));
        predefinedTypes.add(JavaOpenClass.getOpenClass(Short.class));
        predefinedTypes.add(JavaOpenClass.getOpenClass(Character.class));

        // Less popular
        predefinedTypes.add(BYTE);
        predefinedTypes.add(SHORT);
        predefinedTypes.add(INT);
        predefinedTypes.add(LONG);
        predefinedTypes.add(FLOAT);
        predefinedTypes.add(DOUBLE);
        predefinedTypes.add(BOOLEAN);
        predefinedTypes.add(CHAR);
    }

    public InputArgsBean(Environment environment) {
        this.environment = environment;
    }

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

    private boolean isProvideRuntimeContext() {
        RulesDeploy rulesDeploy = WebStudioUtils.getWebStudio().getCurrentProjectRulesDeploy();
        if (rulesDeploy == null) {
            return true;
        } else {
            return rulesDeploy.isProvideRuntimeContext();
        }
    }

    private ObjectMapper configureObjectMapper() {
        try {
            var objectMapperFactory = WebStudioUtils.getWebStudio().getCurrentProjectJacksonObjectMapperFactoryBean();
            objectMapperFactory.setEnvironment(environment);
            return objectMapperFactory.createJacksonObjectMapper();
        } catch (ClassNotFoundException e) {
            if (StringUtils.isNotBlank(e.getMessage())) {
                throw new Message("Invalid rules deploy configuration: " + e.getMessage());
            }
            throw new Message("Invalid rules deploy configuration.");
        }
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
                    ObjectMapper objectMapper = configureObjectMapper();
                    if (field != null) {
                        arg.setValueForced(JsonUtils.fromJSON(field, arg.getType().getInstanceClass(), objectMapper));
                    } else if (argumentTreeNodes.length == 1) {
                        argumentTreeNodes[0].setValueForced(JsonUtils
                                .fromJSON(inputTextBean, argumentTreeNodes[0].getType().getInstanceClass(), objectMapper));
                    }
                }
            } catch (JsonParseException e) {
                throw new Message(constructJsonExceptionMessage(e));
            } catch (IOException e) {
                if (StringUtils.isNotBlank(e.getMessage())) {
                    throw new Message("Input parameters are wrong. " + e.getMessage());
                }
                throw new Message("Input parameters are wrong.");
            }
        }
    }

    public Object[] getParams() {
        runtimeContext = null;
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
            } catch (JsonParseException e) {
                throw new Message(constructJsonExceptionMessage(e), e);
            } catch (IOException e) {
                if (StringUtils.isNotBlank(e.getMessage())) {
                    throw new Message("Input parameters are wrong. " + e.getMessage(), e);
                }
                throw new Message("Input parameters are wrong.");
            }
        }
        Object[] parsedArguments = new Object[argumentTreeNodes.length];
        try {
            if (InputTestCaseType.TEXT.equals(inputTestCaseType) && stringStringMap != null) {
                ObjectMapper objectMapper = configureObjectMapper();
                if (argumentTreeNodes.length == 1 && !isProvideRuntimeContext()) {
                    parsedArguments[0] = JsonUtils
                            .fromJSON(inputTextBean, argumentTreeNodes[0].getType().getInstanceClass(), objectMapper);
                } else {
                    for (int i = 0; i < argumentTreeNodes.length; i++) {
                        String field = stringStringMap.get(argumentTreeNodes[i].getName());
                        if (field != null) {
                            parsedArguments[i] = tryParseJson(field,
                                    argumentTreeNodes[i].getType().getInstanceClass(),
                                    objectMapper);
                            stringStringMap.remove(argumentTreeNodes[i].getName());
                        }
                    }
                    if (!stringStringMap.isEmpty() && isProvideRuntimeContext()) {
                        String contextJson = stringStringMap.values().iterator().next();
                        runtimeContext = tryParseJson(contextJson, IRulesRuntimeContext.class, objectMapper);
                        if (runtimeContext == null) {
                            runtimeContext = new DefaultRulesRuntimeContext();
                        }
                    }
                }
            } else {
                for (int i = 0; i < argumentTreeNodes.length; i++) {
                    parsedArguments[i] = argumentTreeNodes[i].getValueForced();
                }
            }
        } catch (Message e) {
            throw e;
        } catch (IOException e) {
            if (StringUtils.isNotBlank(e.getMessage())) {
                throw new Message("Input parameters are wrong. " + e.getMessage(), e);
            }
            throw new Message("Input parameters are wrong.", e);
        } catch (RuntimeException e) {
            if (e instanceof IllegalArgumentException || e.getCause() instanceof IllegalArgumentException) {
                throw new Message("Input parameters are wrong.", e);
            } else {
                throw e;
            }
        }
        return parsedArguments;
    }

    private void validateFirstJsonSymbol(String inputJson) {
        if (!inputJson.isEmpty() && (!inputJson.startsWith("{") && !inputJson.startsWith("["))) {
            // the only case for which jackson does not generate a clear error message
            throw new Message("Invalid JSON: should start with '{' or '[' symbol");
        }
    }

    private String constructJsonExceptionMessage(JsonParseException e) {
        return String.format("%s</br>[line: %s, column: %s]",
                e.getOriginalMessage(),
                e.getLocation().getLineNr(),
                e.getLocation().getColumnNr());
    }

    public void initObject() {
        ComplexParameterTreeNode currentNode = (ComplexParameterTreeNode) getCurrentNode();
        IOpenClass fieldType = currentNode.getTypeToCreate();
        if (fieldType == null) {
            fieldType = currentNode.getType();
        }

        ParameterDeclarationTreeNode parent = currentNode.getParent();
        Object value = ParameterTreeBuilder.canInstantiate(fieldType)
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
        ParameterDeclarationTreeNode currentNode = getCurrentNode();
        IOpenClass fieldType = currentNode.getType();

        IAggregateInfo info = fieldType.getAggregateInfo();

        Object ary = info.makeIndexedAggregate(info.getComponentType(fieldType), 0);

        currentNode.setValueForced(ary);
    }

    public void disposeObject() {
        ParameterDeclarationTreeNode currentNode = getCurrentNode();
        currentNode.setValueForced(null);
    }

    public void deleteFromCollection() {
        ParameterDeclarationTreeNode currentNode = getCurrentNode();
        ((CollectionParameterTreeNode) currentNode.getParent()).removeChild(currentNode);
    }

    public void addToCollection() {
        ParameterDeclarationTreeNode currentNode = getCurrentNode();
        currentNode.addChild(currentNode.getChildren().size() + 1, null);
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
                if (ParameterTreeBuilder.canInstantiate(parameterType)) {
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
            return ParameterWithValueDeclaration.EMPTY_ARRAY;
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
            IOpenClass parentType = currentNode.getType();
            Collection<IOpenClass> allClasses = getAllClasses(parentType);

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

    private Collection<IOpenClass> getAllClasses(IOpenClass parentType) {

        Collection<IOpenClass> allClasses = new ArrayList<>();

        for (IOpenClass type : predefinedTypes) {
            if (!parentType.isAssignableFrom(type)) {
                continue;
            }
            allClasses.add(type);
        }

        for (IOpenClass type : WizardUtils.getProjectOpenClass().getTypes()) {
            if (!parentType.isAssignableFrom(type)) {
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
            IOpenClass parentType = node.getType();
            for (IOpenClass type : getAllClasses(parentType)) {
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

    public IRulesRuntimeContext getRuntimeContext() {
        return runtimeContext;
    }

    private static <T> T tryParseJson(String json, Class<T> clazz, ObjectMapper mapper) throws IOException {
        try {
            return JsonUtils.fromJSON(json, clazz, mapper);
        } catch (MismatchedInputException ignored) {
            // can be happened if plain text is passed instead of JSON.
            return null;
        }
    }
}
