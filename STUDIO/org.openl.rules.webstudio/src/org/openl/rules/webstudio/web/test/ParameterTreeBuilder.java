package org.openl.rules.webstudio.web.test;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.base.INamedThing;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultOpenClass;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.SubGridTable;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.ui.ParameterRegistry;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;
import org.openl.vm.SimpleVM;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

/**
 * @author DLiauchuk
 */
@ManagedBean
@RequestScoped
public class ParameterTreeBuilder {
    public static ParameterDeclarationTreeNode createNode(ParameterRenderConfig config) {
        ParameterDeclarationTreeNode customNode = getOpenLCustomNode(config.getType(),
            config.getValue(),
            config.getFieldNameInParent(),
            config.getParent());
        if (customNode != null) {
            return customNode;
        }

        if (Utils.isCollection(config.getType())) {
            if (ClassUtils.isAssignable(config.getType().getInstanceClass(), Map.class)) {
                return new MapParameterTreeNode(config);
            }
            return new CollectionParameterTreeNode(config);
        } else if (isSpreadsheetResult(config.getValue())) {
            return new SpreadsheetResultTreeNode(config);
        } else if (!config.getType().isSimple()) {
            return createComplexBeanNode(config);
        } else {
            return createSimpleNode(config);
        }
    }

    private static ParameterDeclarationTreeNode createComplexBeanNode(ParameterRenderConfig config) {
        if (config.getType().getInstanceClass() != null && IRulesRuntimeContext.class
            .isAssignableFrom(config.getType().getInstanceClass())) {
            return new ContextParameterTreeNode(config);
        }
        if (canConstruct(config.getType())) {
            return new ComplexParameterTreeNode(config);
        } else {
            UnmodifiableParameterTreeNode node = new UnmodifiableParameterTreeNode(config.getFieldNameInParent(),
                config.getValue(),
                config.getType(),
                config.getParent());
            node.setWarnMessage(String.format(
                "Can not construct bean of type '%s'. Make sure that it has public constructor without parameters.",
                config.getType().getDisplayName(INamedThing.SHORT)));
            return node;
        }
    }

    /**
     * TODO: Refactor. Not a good way to check if it is possible to instantiate
     */
    public static boolean canConstruct(IOpenClass type) {
        if (type instanceof SpreadsheetResultOpenClass || type instanceof DomainOpenClass) {
            return false;
        }
        if (type.getInstanceClass() == null) {
            return false;
        }

        try {
            type.newInstance(new SimpleVM().getRuntimeEnv());
            return true;
        } catch (Exception | LinkageError ex) {
            return false;
        }
    }

    static ParameterDeclarationTreeNode createSimpleNode(ParameterRenderConfig config) {
        ParameterDeclarationTreeNode parent = config.getParent();
        String fieldName = config.getFieldNameInParent();
        Object value = config.getValue();
        IOpenClass fieldType = config.getType();

        if (parent == null || Utils.isCollection(parent.getType()) || parent.getType()
            .getField(fieldName)
            .isWritable()) {
            return new SimpleParameterTreeNode(fieldName, value, fieldType, parent);
        } else {
            UnmodifiableParameterTreeNode node = new UnmodifiableParameterTreeNode(fieldName, value, fieldType, parent);
            node.setWarnMessage(String.format("Field '%s' is not writable.", fieldName));
            return node;
        }
    }

    private static ParameterDeclarationTreeNode getOpenLCustomNode(IOpenClass fieldType,
            Object value,
            String fieldName,
            ParameterDeclarationTreeNode parent) {
        Class<?> instanceClass = fieldType.getInstanceClass();
        if (IntRange.class.equals(instanceClass)) {
            return new IntRangeDeclarationTreeNode(fieldName, value, fieldType, parent);
        } else if (DoubleRange.class.equals(instanceClass)) {
            return new DoubleRangeDeclarationTreeNode(fieldName, value, fieldType, parent);
        }

        return null;
    }

    public String getRoot(ParameterWithValueDeclaration param) {
        if (param == null) {
            return "null";
        }

        Object value = param.getValue();

        if (value != null) {
            IOpenClass fieldType = param.getType();
            if (Utils.isCollection(fieldType)) {
                boolean empty = !fieldType.getAggregateInfo().getIterator(value).hasNext();
                return Utils.displayNameForCollection(fieldType, empty);
            } else if (!fieldType.isSimple()) {
                ParameterRenderConfig config = new ParameterRenderConfig.Builder(fieldType, value)
                    .keyField(param.getKeyField())
                    .build();

                return createComplexBeanNode(config).getDisplayedValue();
            }
        }

        return "null";
    }

    public TreeNode getTree(String requestId, ParameterWithValueDeclaration param, boolean hasExplainLinks) {
        TreeNodeImpl root = new TreeNodeImpl();

        if (param != null) {
            ParameterRenderConfig config = new ParameterRenderConfig.Builder(param.getType(), param.getValue())
                .keyField(param.getKeyField())
                .hasExplainLinks(hasExplainLinks)
                .requestId(requestId)
                .build();
            ParameterDeclarationTreeNode treeNode = createNode(config);
            root.addChild(param.getName(), treeNode);
        }
        return root;
    }

    public String getParameterId(String requestId, ParameterWithValueDeclaration param) {
        return String.valueOf(ParameterRegistry.getUniqueId(requestId, param));
    }

    public ParameterWithValueDeclaration getParam(String requestId, String parameterRootId) {
        if (StringUtils.isEmpty(parameterRootId)) {
            return null;
        }
        return ParameterRegistry.getParameter(requestId, parameterRootId);
    }

    public boolean isDateParameter(Object value) {
        return value instanceof Date;
    }

    public static boolean isSpreadsheetResult(Object value) {
        return value != null && ClassUtils.isAssignable(value.getClass(), SpreadsheetResult.class);
    }

    public String formattedResult(Object value) {
        String str = "NOW I CANNOT FIND RESULT";
        if (value != null) {
            str = FormattersManager.format(value);
        }
        return str;
    }

    public boolean isHtmlTable(Object value) {
        if (value instanceof ParameterWithValueDeclaration) {
            Object singlValue = value;
            if (Utils.isCollection(((ParameterWithValueDeclaration) value).getType())) {
                Iterator<Object> iterator = ((ParameterWithValueDeclaration) value).getType()
                    .getAggregateInfo()
                    .getIterator(((ParameterWithValueDeclaration) value).getValue());

                if (iterator.hasNext()) {
                    singlValue = iterator.next();
                }
            }

            return checkTableObjectType(singlValue);
        }

        return false;
    }

    private boolean checkTableObjectType(Object value) {
        return value instanceof SubGridTable || value instanceof GridTable;
    }

    public String tableToHtml(Object value) {
        if (value instanceof ParameterWithValueDeclaration) {
            StringBuilder result = new StringBuilder();
            if (Utils.isCollection(((ParameterWithValueDeclaration) value).getType())) {
                Iterator<Object> iterator = ((ParameterWithValueDeclaration) value).getType()
                    .getAggregateInfo()
                    .getIterator(((ParameterWithValueDeclaration) value).getValue());
                ProjectModel model = WebStudioUtils.getWebStudio().getModel();
                while (iterator.hasNext()) {
                    Object singleValue = iterator.next();

                    if (singleValue instanceof GridTable) {
                        GridTable gridTable = (GridTable) singleValue;
                        MetaInfoReader metaInfoReader = model.getNode(gridTable.getUri()).getMetaInfoReader();
                        int numRows = HTMLRenderer.getMaxNumRowsToDisplay(gridTable);
                        HTMLRenderer.TableRenderer tableRenderer = new HTMLRenderer.TableRenderer(
                            TableModel.initializeTableModel(gridTable, numRows, metaInfoReader));

                        result.append(tableRenderer.render(false, null, "testId", null)).append("<br/>");
                    } else if (singleValue instanceof SubGridTable) {
                        SubGridTable sgTable = (SubGridTable) singleValue;
                        GridTable gridTable = new GridTable(sgTable.getRegion(), sgTable.getGrid());
                        MetaInfoReader metaInfoReader = model.getNode(gridTable.getUri()).getMetaInfoReader();
                        int numRows = HTMLRenderer.getMaxNumRowsToDisplay(gridTable);
                        HTMLRenderer.TableRenderer tableRenderer = new HTMLRenderer.TableRenderer(
                            TableModel.initializeTableModel(gridTable, numRows, metaInfoReader));

                        result.append(tableRenderer.render(false, null, "testId", null)).append("<br/>");
                    }
                }
            }

            return result.toString();
        }

        return value == null ? "null" : value.toString();
    }
}
