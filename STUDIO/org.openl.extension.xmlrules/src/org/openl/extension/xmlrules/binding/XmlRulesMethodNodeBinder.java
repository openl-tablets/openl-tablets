package org.openl.extension.xmlrules.binding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.poi.ss.formula.WorkbookEvaluator;
import org.apache.poi.ss.formula.atp.AnalysisToolPak;
import org.apache.poi.ss.formula.eval.FunctionEval;
import org.apache.poi.ss.formula.function.FunctionMetadata;
import org.apache.poi.ss.formula.function.FunctionMetadataRegistry;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.*;
import org.openl.extension.xmlrules.ProjectData;
import org.openl.extension.xmlrules.XmlRulesPath;
import org.openl.extension.xmlrules.model.Function;
import org.openl.extension.xmlrules.model.Table;
import org.openl.extension.xmlrules.model.single.Attribute;
import org.openl.extension.xmlrules.model.single.ParameterImpl;
import org.openl.extension.xmlrules.model.single.node.IfErrorNode;
import org.openl.extension.xmlrules.model.single.node.expression.ExpressionContext;
import org.openl.extension.xmlrules.syntax.SimpleCell;
import org.openl.extension.xmlrules.utils.HelperFunctions;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.ICell;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.CompositeSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenMethod;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlRulesMethodNodeBinder extends MethodNodeBinder {
    private final Logger log = LoggerFactory.getLogger(XmlRulesMethodNodeBinder.class);

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        int childrenCount = node.getNumberOfChildren();

        if (childrenCount < 1) {
            return makeErrorNode("Method node should have at least one subnode", node, bindingContext);
        }

        ISyntaxNode lastNode = node.getChild(childrenCount - 1);

        String methodName = ((IdentifierNode) lastNode).getIdentifier();

        IBoundNode[] children = bindChildren(node, bindingContext, 0, childrenCount - 1);
        if (hasErrorBoundNode(children)){
            return new ErrorBoundNode(node);
        }
        IOpenClass[] parameterTypes = getTypes(children);
        IOpenClass[] singleCallParameterTypes = new IOpenClass[parameterTypes.length];
        System.arraycopy(parameterTypes, 0, singleCallParameterTypes, 0, parameterTypes.length);

        IBoundNode xmlRulesNode = bindXmlRulesMethodNode(node, bindingContext, methodName, children, parameterTypes);
        if (xmlRulesNode != null) {
            return xmlRulesNode;
        }

        IMethodCaller methodCaller = null;

        List<Integer> arrayCallArguments = getArrayCallArguments(children, parameterTypes);
        boolean isArrayCall = !arrayCallArguments.isEmpty();
        if (isArrayCall) {
            for (Integer arrayCallArgument : arrayCallArguments) {
                singleCallParameterTypes[arrayCallArgument] = parameterTypes[arrayCallArgument].getComponentClass();
            }

            methodCaller = bindingContext.findMethodCaller(ISyntaxConstants.THIS_NAMESPACE, methodName, parameterTypes);
            if (methodCaller == null) {
                methodCaller = bindingContext.findMethodCaller(ISyntaxConstants.THIS_NAMESPACE, methodName, singleCallParameterTypes);
            }

            boolean isAggregateFunction = methodCaller != null &&
                    (isReturnTwoDimensionArray(methodCaller) || paramsAreArrays(methodCaller, arrayCallArguments));
            if (isAggregateFunction) {
                methodCaller = null;
                isArrayCall = false;
            }
        }

        // TODO Simplify the method

        if (methodCaller == null) {
            methodCaller = bindingContext.findMethodCaller(ISyntaxConstants.THIS_NAMESPACE, methodName, parameterTypes);

            if (methodCaller == null) {
                methodCaller = getPoiMethodCaller(methodName);
                boolean isAggregateFunction = methodCaller != null &&
                        (isReturnTwoDimensionArray(methodCaller) || paramsAreArrays(methodCaller, arrayCallArguments));
                if (methodCaller == null || isAggregateFunction) {
                    isArrayCall = false;
                }
            } else {
                isArrayCall = false;
            }
        }

        // can`t find directly the method with given name and parameters. so, try to bind it some additional ways
        //
        if (methodCaller == null) {
            IBoundNode boundNode = null;
            if (!arrayCallArguments.isEmpty()) {
                boundNode = bindWithAdditionalBinders(node,
                        bindingContext,
                        methodName,
                        singleCallParameterTypes,
                        children,
                        childrenCount);
                if (boundNode instanceof MethodBoundNode) {
                    return new ArrayCallMethodBoundNode(node,
                            children,
                            ((MethodBoundNode) boundNode).getMethodCaller(),
                            arrayCallArguments);
                }
            }
            if (boundNode == null || boundNode instanceof ErrorBoundNode) {
                boundNode = bindWithAdditionalBinders(node,
                        bindingContext,
                        methodName,
                        parameterTypes,
                        children,
                        childrenCount);
            }

            return boundNode;
        }

        if (isArrayCall) {
            return new ArrayCallMethodBoundNode(node, children, methodCaller, arrayCallArguments);
        }

        if (!bindingContext.isExecutionMode()) {
            addMetaInfo(methodName, node, children);
        }

        return new MethodBoundNode(node, children, methodCaller);
    }

    private void addMetaInfo(String methodName, ISyntaxNode node, IBoundNode[] children) {
        for (IBoundNode child : children) {
            if (!(child instanceof LiteralBoundNode)) {
                return;
            }
        }
        ExpressionContext expressionContext = ExpressionContext.getInstance();
        if (expressionContext == null || expressionContext.getCurrentPath() == null) {
            return;
        }
        XmlRulesPath path = expressionContext.getCurrentPath();
        if (methodName.equals("Cell")) {
            if (children.length == 2) {
                String workbook = path.getWorkbook();
                String sheet = path.getSheet();
                Integer row = (Integer) ((LiteralBoundNode) children[0]).getValue();
                Integer column = (Integer) ((LiteralBoundNode) children[1]).getValue();

                addMetaInfo(node, workbook, sheet, row, column);
            } else if (children.length == 3) {
                String workbook = path.getWorkbook();
                String sheet = (String) ((LiteralBoundNode) children[0]).getValue();
                Integer row = (Integer) ((LiteralBoundNode) children[1]).getValue();
                Integer column = (Integer) ((LiteralBoundNode) children[2]).getValue();

                addMetaInfo(node, workbook, sheet, row, column);
            } else if (children.length == 4) {
                String workbook = (String) ((LiteralBoundNode) children[0]).getValue();
                String sheet = (String) ((LiteralBoundNode) children[1]).getValue();
                Integer row = (Integer) ((LiteralBoundNode) children[2]).getValue();
                Integer column = (Integer) ((LiteralBoundNode) children[3]).getValue();

                addMetaInfo(node, workbook, sheet, row, column);
            }
        } else if (methodName.equals("CellRange")) {
            if (children.length == 4) {
                String workbook = path.getWorkbook();
                String sheet = path.getSheet();
                Integer row = (Integer) ((LiteralBoundNode) children[0]).getValue();
                Integer column = (Integer) ((LiteralBoundNode) children[1]).getValue();
                Integer endRow = row + (Integer) ((LiteralBoundNode) children[2]).getValue() - 1;
                Integer endColumn = column + (Integer) ((LiteralBoundNode) children[3]).getValue() - 1;

                addMetaInfo(node, workbook, sheet, row, column, endRow, endColumn);
            } else if (children.length == 5) {
                String workbook = path.getWorkbook();
                String sheet = (String) ((LiteralBoundNode) children[0]).getValue();
                Integer row = (Integer) ((LiteralBoundNode) children[1]).getValue();
                Integer column = (Integer) ((LiteralBoundNode) children[2]).getValue();
                Integer endRow = row + (Integer) ((LiteralBoundNode) children[3]).getValue() - 1;
                Integer endColumn = column + (Integer) ((LiteralBoundNode) children[4]).getValue() - 1;

                addMetaInfo(node, workbook, sheet, row, column, endRow, endColumn);
            } else if (children.length == 6) {
                String workbook = (String) ((LiteralBoundNode) children[0]).getValue();
                String sheet = (String) ((LiteralBoundNode) children[1]).getValue();
                Integer row = (Integer) ((LiteralBoundNode) children[2]).getValue();
                Integer column = (Integer) ((LiteralBoundNode) children[3]).getValue();
                Integer endRow = row + (Integer) ((LiteralBoundNode) children[4]).getValue() - 1;
                Integer endColumn = column + (Integer) ((LiteralBoundNode) children[5]).getValue() - 1;

                addMetaInfo(node, workbook, sheet, row, column, endRow, endColumn);
            }
        }
    }

    private void addMetaInfo(ISyntaxNode node, String workbook, String sheet, Integer row, Integer column) {
        addMetaInfo(node, workbook, sheet, row, column, null, null);
    }

    private void addMetaInfo(ISyntaxNode node, String workbook, String sheet, Integer row, Integer column, Integer endRow, Integer endColumn) {
        IOpenSourceCodeModule src = node.getModule();
        TextInfo text = new TextInfo(src.getCode());
        ILocation location = node.getSourceLocation();
        int absoluteStart = location.getStart().getAbsolutePosition(text);
        int absoluteEnd = location.getEnd().getAbsolutePosition(text);

        if (src instanceof CompositeSourceCodeModule) {
            int line = location.getStart().getLine(text);
            int lineStart = text.getPosition(line);
            src = ((CompositeSourceCodeModule) src).getModules()[line];
            addMetaInfo(workbook, sheet, row, column, endRow, endColumn, src, absoluteStart - lineStart, absoluteEnd - lineStart);
        } else {
            addMetaInfo(workbook, sheet, row, column, endRow, endColumn, src, absoluteStart, absoluteEnd);
        }
    }

    private void addMetaInfo(String workbook,
            String sheet,
            Integer row,
            Integer column,
            Integer endRow,
            Integer endColumn, IOpenSourceCodeModule src, int absoluteStart, int absoluteEnd) {
        int startIndex = 0;
        // extract original cell source
        while (src instanceof SubTextSourceCodeModule) {
            startIndex += src.getStartPosition();
            src = ((SubTextSourceCodeModule) src).getBaseModule();
        }
        if (src instanceof GridCellSourceCodeModule) {
            ICell cell = ((GridCellSourceCodeModule) src).getCell();
            if (!(cell instanceof SimpleCell)) {
                return;
            }

            // TODO: Refactor to use MetaInfoReader
            CellMetaInfo metaInfo = ((SimpleCell) cell).getMetaInfo();
            if (metaInfo == null) {
                metaInfo = new CellMetaInfo(JavaOpenClass.STRING, false,  new ArrayList<NodeUsage>());
                ((SimpleCell) cell).setMetaInfo(metaInfo);
            }
            List<NodeUsage> usedNodes = metaInfo.getUsedNodes() == null ? new ArrayList<NodeUsage>() :
                                        new ArrayList<>(metaInfo.getUsedNodes());
            metaInfo.setUsedNodes(usedNodes);

            int start = startIndex + absoluteStart;
            int end = startIndex + absoluteEnd;

            String description = "Workbook: " + workbook +
                    "\nSheet: " + sheet +
                    "\nCell: R" + row + "C" + column;
            if (endRow != null && endColumn != null) {
                description += ":R" + endRow + "C" + endColumn;
            }

            String uri = ProjectData.getCurrentInstance().getTableUri(workbook, sheet);
            usedNodes.add(new SimpleNodeUsage(start, end, description, uri, NodeType.OTHER));

            Collections.sort(usedNodes, new NodeUsageComparator());
        }
    }

    private IBoundNode bindXmlRulesMethodNode(ISyntaxNode methodNode,
            IBindingContext bindingContext,
            String methodName,
            IBoundNode[] children,
            IOpenClass[] argumentTypes) {
        ProjectData instance = ProjectData.getCurrentInstance();

        Function function = instance.getFirstFunction(methodName);
        if (function != null) {
            List<ParameterImpl> parameters = function.getParameters();
            if (parameters.size() == children.length) {
                return bindExactParametersMethodNode(methodNode,
                        bindingContext,
                        methodName,
                        children,
                        argumentTypes,
                        parameters,
                        "Object");
            } else {
                return bindModifiedAttributes(methodNode, bindingContext, methodName, argumentTypes, children);
            }
        }

        Table table = instance.getFirstTable(methodName);
        if (table != null) {
            List<ParameterImpl> parameters = table.getParameters();
            if (parameters.size() == children.length) {
                return bindExactParametersMethodNode(methodNode,
                        bindingContext,
                        methodName,
                        children,
                        argumentTypes,
                        parameters,
                        "String");
            } else {
                return bindModifiedAttributes(methodNode, bindingContext, methodName, argumentTypes, children);
            }
        }

        return null;
    }

    private MethodBoundNode bindExactParametersMethodNode(ISyntaxNode methodNode,
            IBindingContext bindingContext,
            String methodName,
            IBoundNode[] children,
            IOpenClass[] argumentTypes,
            List<ParameterImpl> parameters,
            String defaultType) {
        IOpenClass[] parameterTypes = new IOpenClass[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            String parameterType = HelperFunctions.convertToOpenLType(parameters.get(i).getType());
            if (parameterType == null) {
                parameterType = defaultType;
            }

            String[] split = parameterType.split("\\[]", -1);
            parameterType = split[0];
            int dimensions = split.length - 1;

            IOpenClass type = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, parameterType);
            if (type == null) {
                BindHelper.processError("Can't find type " + parameterType,
                        methodNode,
                        bindingContext);
            } else if (dimensions > 0) {
                type = type.getAggregateInfo().getIndexedAggregateType(type, dimensions);
            }

            parameterTypes[i] = type;
        }

        IMethodCaller methodCaller = bindingContext.findMethodCaller(ISyntaxConstants.THIS_NAMESPACE,
                methodName,
                parameterTypes);

        if (methodCaller == null) {
            return null;
        }

        List<Integer> arrayCallArguments = getArrayCallArguments(children, argumentTypes);
        boolean isArrayCall = !arrayCallArguments.isEmpty();
        boolean isAggregateFunction = isReturnTwoDimensionArray(methodCaller) || paramsAreArrays(
                methodCaller,
                arrayCallArguments);
        if (isArrayCall && !isAggregateFunction) {
            return new ArrayCallMethodBoundNode(methodNode, children, methodCaller, arrayCallArguments);
        } else {
            return new MethodBoundNode(methodNode, children, methodCaller);
        }
    }

    private IMethodCaller getPoiMethodCaller(String methodName) {
        if (WorkbookEvaluator.getSupportedFunctionNames().contains(methodName)) {
            log.info("POI implementation for '{}' method is used", methodName);

            if (AnalysisToolPak.isATPFunction(methodName)) {
                PoiMethodCaller.create(AnalysisToolPak.instance.findFunction(methodName));
            } else {
                FunctionMetadata metaData = FunctionMetadataRegistry.getFunctionByName(methodName);
                org.apache.poi.ss.formula.functions.Function function = FunctionEval.getBasicFunction(metaData.getIndex());

                return PoiMethodCaller.create(function, metaData);
            }
        }

        return null;
    }

    @Override
    protected IBoundNode bindWithAdditionalBinders(ISyntaxNode methodNode,
            IBindingContext bindingContext,
            String methodName,
            IOpenClass[] argumentTypes,
            IBoundNode[] children,
            int childrenCount) throws Exception {

        if (IfErrorNode.FUNCTION_NAME.equals(methodName) && children.length == IfErrorNode.ARGUMENTS_COUNT) {
            return new IfErrorFunctionBoundNode(methodNode, children);
        }

        IBoundNode methodWithModifiedAttributes = bindModifiedAttributes(methodNode,
                bindingContext,
                methodName,
                argumentTypes,
                children);

        if (methodWithModifiedAttributes != null) {
            return methodWithModifiedAttributes;
        }

        return super.bindWithAdditionalBinders(methodNode,
                bindingContext,
                methodName,
                argumentTypes,
                children,
                childrenCount);
    }

    // TODO Array calls support fot attributes
    private IBoundNode bindModifiedAttributes(ISyntaxNode methodNode,
            IBindingContext bindingContext,
            String methodName,
            IOpenClass[] argumentTypes, IBoundNode[] children) {
        IMethodCaller modifyContext = bindingContext.findMethodCaller(ISyntaxConstants.THIS_NAMESPACE,
                "modifyContext",
                new IOpenClass[] { JavaOpenClass.STRING, JavaOpenClass.OBJECT });
        IMethodCaller restoreContext = bindingContext.findMethodCaller(ISyntaxConstants.THIS_NAMESPACE,
                "restoreContext",
                IOpenClass.EMPTY);
        ProjectData instance = ProjectData.getCurrentInstance();

        List<Function> overloadedFunctions = instance.getOverloadedFunctions(methodName);
        for (Function function : overloadedFunctions) {
            int parameterCount = function.getParameters().size();
            List<String> attributeNames = getUniqueAttributeNames(function.getAttributes());
            int possibleParameterCount = parameterCount + attributeNames.size();
            if (parameterCount < children.length && possibleParameterCount >= children.length) {
                IOpenClass[] parameterTypes = Arrays.copyOfRange(argumentTypes, 0, parameterCount);
                IMethodCaller methodCaller = bindingContext.findMethodCaller(ISyntaxConstants.THIS_NAMESPACE,
                        methodName, parameterTypes);
                if (methodCaller == null) {
                    return null;
                }

                return new MethodWithAttributesBoundNode(methodNode,
                        children,
                        methodCaller,
                        modifyContext,
                        restoreContext,
                        attributeNames,
                        parameterCount);
            }
        }

        List<Table> tables = instance.getOverloadedTables(methodName);
        for (Table table : tables) {
            int parameterCount = table.getParameters().size();
            List<String> attributeNames = getUniqueAttributeNames(table.getAttributes());
            int possibleParameterCount = parameterCount + attributeNames.size();
            if (parameterCount < children.length && possibleParameterCount >= children.length) {
                IOpenClass[] parameterTypes = Arrays.copyOfRange(argumentTypes, 0, parameterCount);
                IMethodCaller methodCaller = bindingContext.findMethodCaller(ISyntaxConstants.THIS_NAMESPACE,
                        methodName, parameterTypes);
                if (methodCaller == null) {
                    return null;
                }

                return new MethodWithAttributesBoundNode(methodNode,
                        children,
                        methodCaller,
                        modifyContext,
                        restoreContext,
                        attributeNames,
                        parameterCount);
            }
        }

        return null;
    }

    private List<String> getUniqueAttributeNames(List<Attribute> attributes) {
        List<String> attributeNames = new ArrayList<>();
        for (Attribute attribute : attributes) {
            String attributeName = attribute.getName();
            if (!attributeNames.contains(attributeName)) {
                attributeNames.add(attributeName);
            }
        }
        return attributeNames;
    }

    private List<Integer> getArrayCallArguments(IBoundNode[] children, IOpenClass[] parameterTypes) {
        List<Integer> arrayCallArguments = new ArrayList<>();

        for (int i = 0; i < children.length; i++) {
            IBoundNode child = children[i];
            boolean isReturnsArray = child instanceof MethodBoundNode &&
                    isReturnTwoDimensionArray(((MethodBoundNode) child).getMethodCaller());
            boolean isCastToArray = child instanceof CastNode && child.getType().isArray() && child.getType().getComponentClass().isArray();
            if (isReturnsArray || isCastToArray || child instanceof ArrayCallMethodBoundNode || child instanceof ArrayInitializerNode) {
                // Found array call argument
                IOpenClass parameterType = parameterTypes[i];
                if (!parameterType.isArray()) {
                    throw new IllegalArgumentException("Parameter " + i + " has a type " + parameterType.getName() + " but array is expected");
                }

                arrayCallArguments.add(i);
            }
        }
        return arrayCallArguments;
    }

    private boolean isReturnTwoDimensionArray(IMethodCaller methodCaller) {
        if (methodCaller instanceof PoiMethodCaller) {
            return ((PoiMethodCaller) methodCaller).isReturnsArray();
        }
        IOpenClass type = methodCaller.getMethod().getType();
        return type.isArray() && type.getComponentClass().isArray();
    }

    private boolean paramsAreArrays(IMethodCaller methodCaller, List<Integer> arrayCallArguments) {
        if (methodCaller instanceof PoiMethodCaller) {
            return ((PoiMethodCaller) methodCaller).isHasArrayParameter();
        }

        if (!arrayCallArguments.isEmpty()) {
            return isArrayParam(methodCaller, arrayCallArguments.get(0));
        } else {
            int numberOfParameters = methodCaller.getMethod().getSignature().getNumberOfParameters();
            for (int i = 0; i < numberOfParameters; i++) {
                if (isArrayParam(methodCaller, i)) {
                    return true;
                }
            }

            return false;
        }
    }

    private boolean isArrayParam(IMethodCaller methodCaller, Integer paramNum) {
        IOpenClass arrayCallParameter = methodCaller.getMethod().getSignature().getParameterType(paramNum);
        return arrayCallParameter.isArray() ||
                JavaOpenClass.OBJECT.equals(arrayCallParameter) && methodCaller instanceof JavaOpenMethod;
    }
}
