package org.openl.rules.ui.tree.richfaces;

import org.openl.base.INamedThing;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.meta.number.Formulas;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;

public class ExplainTreeBuilder {

    private static final String SHOW_TABLE_PAGE = "/faces/pages/modules/explain/showExplainTable.xhtml?";

    public TreeNode buildWithRoot(ExplanationNumberValue root) {
        TreeNode subNode = buildNode(root);
        // Wrap to root node
        TreeNode node = new TreeNode();
        node.addChild(0, subNode);
        return node;
    }

    private TreeNode buildNode(ExplanationNumberValue<?> element) {
        if (element == null) {
            return createNullNode();
        }
        TreeNode node = createNode(element);
        Iterable<ExplanationNumberValue<?>> children = element.getChildren();
        for (ExplanationNumberValue<?> child : children) {
            TreeNode rfChild = buildNode(child);
            node.addChild(rfChild, rfChild);
        }
        return node;
    }

    private TreeNode createNode(ExplanationNumberValue<?> element) {
        boolean leaf = element.isLeaf();
        TreeNode node = new TreeNode(leaf);

        String name = getDisplayName(element, INamedThing.SHORT);
        node.setName(name);

        String title = getDisplayName(element, INamedThing.REGULAR);
        node.setTitle(title);

        String url = getUrl(element);
        node.setUrl(url);

        String type = getType(element);
        node.setType(type);

        return node;
    }

    private TreeNode createNullNode() {
        TreeNode dest = new TreeNode(true);
        dest.setName("null");
        dest.setTitle("null");
        dest.setUrl(getUrl(null));
        dest.setType("value");
        return dest;
    }

    private String getType(ExplanationNumberValue<?> element) {
        String type = element.getType();
        if (type == null) {
            return StringUtils.EMPTY;
        }
        return type.replace('.', '_')
            .replace(Formulas.ADD.toString(), "plus")
            .replace(Formulas.SUBTRACT.toString(), "minus")
            .replace(Formulas.MULTIPLY.toString(), "mul")
            .replace(Formulas.DIVIDE.toString(), "div")
            .replace(Formulas.REM.toString(), "rem");
    }

    private String getDisplayName(ExplanationNumberValue<?> obj, int mode) {
        String result = FormattersManager.format(obj);
        if (obj.isFunction() && mode == INamedThing.SHORT) {
            String functionName = obj.getFunction().getFunctionName().toUpperCase();
            result = functionName + " = " + result;
        }
        return result;
    }

    private String getUrl(ExplanationNumberValue<?> element) {
        String url = element == null || element.getMetaInfo() == null ? null : element.getMetaInfo().getSourceUrl();
        if (StringUtils.isNotBlank(url)) {
            return getUrlToElement(element, url);
        }
        return WebStudioUtils.getExternalContext().getRequestContextPath() + SHOW_TABLE_PAGE;
    }

    private String getUrlToElement(ExplanationNumberValue<?> element, String url) {
        return WebStudioUtils.getExternalContext().getRequestContextPath() + SHOW_TABLE_PAGE + "uri=" + StringTool
            .encodeURL("" + url) + "&text=" + StringTool.encodeURL(getDisplayName(element, INamedThing.REGULAR));
    }
}
