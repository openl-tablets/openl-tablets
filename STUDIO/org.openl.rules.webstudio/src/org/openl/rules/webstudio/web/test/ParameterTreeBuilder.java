package org.openl.rules.webstudio.web.test;

import org.openl.base.INamedThing;
import org.openl.rules.calc.result.SpreadsheetResultHelper;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.SubGridTable;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.webstudio.web.trace.TracerObjectDecorator;
import org.openl.types.IOpenClass;
import org.openl.types.java.OpenClassHelper;
import org.openl.vm.SimpleVM;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import java.util.Date;
import java.util.Iterator;

/**
 * @author DLiauchuk
 */
@ManagedBean
@RequestScoped
public class ParameterTreeBuilder {

    public static ParameterDeclarationTreeNode createNode(IOpenClass fieldType, Object value,
                                                          String fieldName, ParameterDeclarationTreeNode parent) {
        return createNode(fieldType, value, null, fieldName, parent);
    }

    private static ParameterDeclarationTreeNode createNode(IOpenClass fieldType, Object value, Object parameterPreview,
                                                           String fieldName, ParameterDeclarationTreeNode parent) {
        if (OpenClassHelper.isCollection(fieldType)) {
            return new CollectionParameterTreeNode(fieldName, value, fieldType, parent);
        } else if (isSpreadsheetResult(value)) {
            return createSpreadsheetResultTreeNode(fieldType, value, fieldName, parent);
        } else if (!fieldType.isSimple()) {
            return createComplexBeanNode(fieldType, value, parameterPreview, fieldName, parent);
        } else {
            return createSimpleNode(fieldType, value, fieldName, parent);
        }
    }

    private static ParameterDeclarationTreeNode createComplexBeanNode(IOpenClass fieldType,
                                                                      Object value,
                                                                      Object parameterPreview,
                                                                      String fieldName,
                                                                      ParameterDeclarationTreeNode parent) {
        if (canConstruct(fieldType)) {
            String valuePreview = parameterPreview == null ? null : createSimpleNode(fieldType, parameterPreview, null, null).getDisplayedValue();
            return new ComplexParameterTreeNode(fieldName, value, fieldType, parent, valuePreview);
        } else {
            UnmodifiableParameterTreeNode node = new UnmodifiableParameterTreeNode(fieldName, value, fieldType, parent);
            node.setWarnMessage(String.format("Can not construct bean of type '%s'. Make sure that it has public constructor without parameters.",
                    fieldType.getDisplayName(INamedThing.SHORT)));
            return node;
        }
    }

    public static ParameterDeclarationTreeNode createSpreadsheetResultTreeNode(IOpenClass fieldType,
                                                                               Object value,
                                                                               String fieldName,
                                                                               ParameterDeclarationTreeNode parent) {
        return new SpreadsheetResultTreeNode(fieldName, value, fieldType, parent);
    }

    /**
     * TODO: Refactor. Not a good way to check if it is possible to instantiate
     */
    public static boolean canConstruct(IOpenClass type) {
        boolean result = true;
        try {
            type.newInstance(new SimpleVM().getRuntimeEnv());
        } catch (Exception ex) {
            result = false;
        }
        return result;
    }

    public static ParameterDeclarationTreeNode createSimpleNode(IOpenClass fieldType,
                                                                Object value,
                                                                String fieldName,
                                                                ParameterDeclarationTreeNode parent) {
        if (parent == null || OpenClassHelper.isCollection(parent.getType()) || parent.getType().getField(fieldName).isWritable()) {
            return new SimpleParameterTreeNode(fieldName, value, fieldType, parent);
        } else {
            UnmodifiableParameterTreeNode node = new UnmodifiableParameterTreeNode(fieldName, value, fieldType, parent);
            node.setWarnMessage(String.format("Field '%s' is not writable.", fieldName));
            return node;
        }
    }

    public String getRoot(ParameterWithValueDeclaration param) {
        if (param == null) {
            return "null";
        }

        Object value = param.getValue();

        if (value != null) {
            IOpenClass fieldType = param.getType();
            if (OpenClassHelper.isCollection(fieldType)) {
                boolean empty = !fieldType.getAggregateInfo().getIterator(value).hasNext();
                return OpenClassHelper.displayNameForCollection(fieldType, empty);
            } else if (!fieldType.isSimple()) {
                Object parameterPreview = null;
                if (param instanceof ParameterWithValueAndPreviewDeclaration) {
                    parameterPreview = ((ParameterWithValueAndPreviewDeclaration) param).getPreview();
                }
                return createComplexBeanNode(fieldType, value, parameterPreview, null, null).getDisplayedValue();
            }
        }

        return "null";
    }

    public TreeNode getTree(ParameterWithValueDeclaration param) {
        TreeNodeImpl root = new TreeNodeImpl();

        if (param != null) {
            Object parameterPreview = null;
            if (param instanceof ParameterWithValueAndPreviewDeclaration) {
                parameterPreview = ((ParameterWithValueAndPreviewDeclaration) param).getPreview();
            }
            ParameterDeclarationTreeNode treeNode = createNode(param.getType(), param.getValue(), parameterPreview, null, null);
            root.addChild(param.getName(), treeNode);
        }
        return root;
    }

    public boolean isDateParameter(Object value) {
        return value instanceof Date;
    }

    public static boolean isSpreadsheetResult(Object value) {
        return value != null && SpreadsheetResultHelper.isSpreadsheetResult(value.getClass());
    }

    public String formattedResult(Object value) {
        return TracerObjectDecorator.format(value);
    }

    public boolean isHtmlTable(Object value) {
        if (value instanceof ParameterWithValueDeclaration) {
            Object singlValue = value;
            if (OpenClassHelper.isCollection(((ParameterWithValueDeclaration) value).getType())) {
                Iterator<Object> iterator = ((ParameterWithValueDeclaration) value).getType().getAggregateInfo()
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
        if (value != null && value instanceof ParameterWithValueDeclaration) {
            String returnString = "";
            if (OpenClassHelper.isCollection(((ParameterWithValueDeclaration) value).getType())) {
                Iterator<Object> iterator = ((ParameterWithValueDeclaration) value).getType().getAggregateInfo()
                        .getIterator(((ParameterWithValueDeclaration) value).getValue());
                while (iterator.hasNext()) {
                    Object singleValue = iterator.next();

                    if (singleValue instanceof GridTable) {
                        int numRows = HTMLRenderer.getMaxNumRowsToDisplay((GridTable) singleValue);
                        HTMLRenderer.TableRenderer tableRenderer = new HTMLRenderer.
                                TableRenderer(TableModel.initializeTableModel((GridTable) singleValue, numRows));

                        returnString = returnString + tableRenderer.render(false, null, "testId", null) + "<br/>";
                    } else if (singleValue instanceof SubGridTable) {
                        SubGridTable sgTable = (SubGridTable) singleValue;
                        GridTable gridTable = new GridTable(sgTable.getRegion(), sgTable.getGrid());
                        int numRows = HTMLRenderer.getMaxNumRowsToDisplay(gridTable);
                        HTMLRenderer.TableRenderer tableRenderer = new HTMLRenderer.
                                TableRenderer(TableModel.initializeTableModel(gridTable, numRows));

                        returnString = returnString + tableRenderer.render(false, null, "testId", null) + "<br/>";
                    }
                }
            }

            return returnString;
        }

        return value == null ? "null" : value.toString();
    }

}
