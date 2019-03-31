package org.openl.rules.ui.tree.richfaces;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.ui.IProjectTypes;
import org.openl.rules.ui.tree.ProjectTreeNode;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.CollectionUtils;
import org.openl.util.tree.ITreeElement;

public class ProjectTreeBuilder extends TreeBuilder {

    public ProjectTreeBuilder(CollectionUtils.Predicate<ITreeElement> utilityTablePredicate) {
        super(utilityTablePredicate);
    }

    @Override
    @Deprecated
    int getState(ITreeElement<?> element) {
        ProjectTreeNode pte = (ProjectTreeNode) element;
        if (pte.getTableSyntaxNode() != null && WebStudioUtils.getProjectModel().isTestable(pte.getTableSyntaxNode())) {
            return 2; // has tests
        }
        return super.getState(element);
    }

    @Override
    int getNumErrors(ITreeElement<?> element) {
        ProjectTreeNode pte = (ProjectTreeNode) element;
        return pte.getNumErrors();
    }

    @Override
    boolean isActive(ITreeElement<?> element) {
        ProjectTreeNode projectNode = (ProjectTreeNode) element;
        TableSyntaxNode syntaxNode = projectNode.getTableSyntaxNode();
        if (syntaxNode != null) {
            ITableProperties tableProperties = syntaxNode.getTableProperties();
            if (tableProperties != null) {
                Boolean active = tableProperties.getActive();
                if (active != null) {
                    return active;
                }
            }
        }
        return super.isActive(element);
    }

    @Override
    String getUrl(ITreeElement<?> element) {
        String elementType = element.getType();
        if (elementType.startsWith(IProjectTypes.PT_TABLE + ".")) {
            TableSyntaxNode tsn = ((ProjectTreeNode) element).getTableSyntaxNode();
            return WebStudioUtils.getWebStudio().url("table?" + Constants.REQUEST_PARAM_ID + "=" + tsn.getId());
        }
        return null;
    }
}
