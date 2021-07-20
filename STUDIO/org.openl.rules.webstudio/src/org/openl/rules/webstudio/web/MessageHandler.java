package org.openl.rules.webstudio.web;

import org.openl.message.OpenLMessage;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.StringUtils;

public class MessageHandler {

    public String getSourceUrl(String sourceLocation, String severity, long id, ProjectModel model) {
        String url = null;
        if (StringUtils.isNotBlank(sourceLocation)) {
            url = getUrl(model, sourceLocation, severity, id);
        }
        return url;
    }

    /**
     * Gets the url for messages that don`t have any sources.
     */
    public String getUrlForEmptySource(OpenLMessage message) {
        return WebStudioUtils.getWebStudio(WebStudioUtils.getSession())
            .url("message?type=" + message.getSeverity().name() + "&summary=" + message.getId());
    }

    protected String getUrl(ProjectModel model, String sourceLocation, String severity, long id) {
        TableSyntaxNode node = model.getNode(sourceLocation);
        if (isCurrentModule(model, node)) {
            return getUrlForCurrentModule(sourceLocation, node.getId());
        } else {
            if (node != null) {
                return getUrlToDependentModule(sourceLocation, model.getMessageNodeId(sourceLocation));
            } else {
                return WebStudioUtils.getWebStudio(WebStudioUtils.getSession())
                    .url("message?type=" + severity + "&summary=" + id);
            }
        }
    }

    private boolean isCurrentModule(ProjectModel model, TableSyntaxNode node) {
        ISyntaxNode referencedModule = getModuleNode(node);

        XlsWorkbookSourceCodeModule currentModuleWorkbook = model.getCurrentModuleWorkbook();

        return referencedModule != null && currentModuleWorkbook != null && currentModuleWorkbook.getSource()
            .equals(referencedModule.getModule());
    }

    private ISyntaxNode getModuleNode(TableSyntaxNode node) {
        ISyntaxNode moduleNode = node;
        while (moduleNode != null) {
            if (moduleNode instanceof XlsModuleSyntaxNode) {
                break;
            } else {
                moduleNode = moduleNode.getParent();
            }
        }
        return moduleNode;
    }

    private String getUrlToDependentModule(String uri, String id) {
        String url = WebStudioUtils.getWebStudio(WebStudioUtils.getSession()).url("table", uri);
        if (url != null && url.endsWith("table")) {
            url += "?" + Constants.REQUEST_PARAM_ID + "=" + id;
        }
        return url;
    }

    private String getUrlForCurrentModule(String errorUri, String tableId) {
        XlsUrlParser uriParser = new XlsUrlParser(errorUri);
        String url = "table?id=" + tableId;
        if (StringUtils.isNotBlank(uriParser.getCell())) {
            url += "&errorCell=" + uriParser.getCell();
        }
        return WebStudioUtils.getWebStudio(WebStudioUtils.getSession()).url(url);
    }
}
