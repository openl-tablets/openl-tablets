package org.openl.extension.xmlrules.project;

import java.util.Collection;

import org.openl.extension.xmlrules.ProjectData;
import org.openl.rules.lang.xls.syntax.OpenlSyntaxNode;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.source.IOpenSourceCodeModule;

public class XmlRulesModuleSyntaxNode extends XlsModuleSyntaxNode {
    private final ProjectData projectData;

    public XmlRulesModuleSyntaxNode(WorkbookSyntaxNode[] nodes,
            IOpenSourceCodeModule module,
            OpenlSyntaxNode openlNode,
            Collection<String> imports,
            ProjectData projectData) {
        super(nodes, module, openlNode, imports);
        this.projectData = projectData;
    }

    public ProjectData getProjectData() {
        return projectData;
    }
}
