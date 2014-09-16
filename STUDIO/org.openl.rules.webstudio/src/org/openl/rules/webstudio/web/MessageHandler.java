package org.openl.rules.webstudio.web;

import org.apache.commons.lang3.StringUtils;
import org.openl.message.OpenLMessage;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringTool;

public class MessageHandler {

    /**
     * Gets the url to the source of message.
     *
     * @param message {@link OpenLMessage} instance
     * @param model   project model for current module.
     * @return url to error source.
     */
    public String getSourceUrl(OpenLMessage message, ProjectModel model) {
        String url = null;
        String errorUri = getUri(message);
        if (StringUtils.isNotBlank(errorUri)) {
            url = getUrl(model, errorUri, message);
        }
        return url;
    }

    /**
     * Gets the url for messages that don`t have any sources.
     *
     * @param message
     * @return
     */
    public String getUrlForEmptySource(OpenLMessage message) {
        return WebStudioUtils.getWebStudio().url("message" + "?type" + "=" + message.getSeverity().name()
                + "&summary" + "=" + StringTool.encodeURL(message.getSummary()));
    }

    protected String getUri(OpenLMessage message) {
        // Default implementation
        return null;
    }

    protected String getUrl(ProjectModel model, String errorUri, OpenLMessage message) {
        String url;
        TableSyntaxNode node = model.getNode(errorUri);
        if (node != null) {
            // Table belongs to current module
            url = getUrlForCurrentModule(errorUri, node.getId());
        } else {
            // Table belongs to dependent module
            url = getErrorUrlForDependency(message);
        }
        return url;
    }

    private String getUrlForCurrentModule(String errorUri, String tableId) {
        XlsUrlParser uriParser = new XlsUrlParser();
        uriParser.parse(errorUri);
        String url = "table?id=" + tableId;
        if (StringUtils.isNotBlank(uriParser.cell)) {
            url += "&errorCell=" + uriParser.cell;
        }
        return WebStudioUtils.getWebStudio().url(url);
    }

    private String getErrorUrlForDependency(OpenLMessage message) {
        return WebStudioUtils.getWebStudio().url("message" + "?type" + "=" + message.getSeverity().name() + "&summary" + "="
                + StringTool.encodeURL(String.format("Dependency error: %s", message.getSummary())));
    }

}
