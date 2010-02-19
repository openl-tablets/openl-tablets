package org.openl.rules.ui.tree;

import org.openl.rules.ui.IProjectTypes;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.util.StringTool;
import org.openl.util.tree.ITreeElement;

public class ProjectRichFacesTreeBuilder extends RichFacesTreeBuilder {

    private ProjectModel projectModel;

    public ProjectRichFacesTreeBuilder(ITreeElement<?> root, ProjectModel projectModel) {
        super(root);
        this.projectModel = projectModel;
    }

    @Override
    protected int getState(ITreeElement<?> element) {
        ProjectTreeNode pte = (ProjectTreeNode) element;
        if (pte.hasProblems()) {
            return 1; // has errors
        } else if (pte.getTableSyntaxNode() != null
                && projectModel.isTestable(pte.getTableSyntaxNode())) {
            return 2; // has tests
        }
        return super.getState(element);
    }

    @Override
    protected String getUrl(ITreeElement<?> element) {
        String elementType = element.getType();
        if (elementType.startsWith(IProjectTypes.PT_TABLE + ".")) {
            String uri = ((ProjectTreeNode) element).getUri();
            return "tableeditor/showTable.xhtml?" + Constants.REQUEST_PARAM_URI + "=" + StringTool.encodeURL(uri);
        } else if (elementType.startsWith(IProjectTypes.PT_PROBLEM)) {
            return "tableeditor/showError.xhtml?" + Constants.REQUEST_PARAM_ID + "="
                + projectModel.getTreeNodeId(element);
        }
        return null;
    }
}
