package org.openl.rules.webstudio.web;

import org.apache.commons.lang.StringUtils;
import org.openl.message.OpenLMessage;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.jsf.WebContext;
import org.openl.util.StringTool;

public class MessageHandler {
    
    /**
     * Gets the url to the source of message.
     * 
     * @param message {@link OpenLMessage} instance
     * @param model project model for current module.
     * @return url to error source.
     */
    public String getSourceUrl(OpenLMessage message, ProjectModel model) {
        String url = null;
        String errorUri = getUri(message);
        String tableUri = errorUri;//WebStudioUtils.getWebStudio().getModel().findTableUri(errorUri);
        if (StringUtils.isNotBlank(tableUri)) {
            url = getUrl(model, tableUri, errorUri, message);
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
        return WebContext.getContextPath() + "/pages/common/message.xhtml"
            + "?type" + "=" + message.getSeverity().name()
            + "&summary" + "=" + StringTool.encodeURL(message.getSummary());
    }
    
    protected String getUri(OpenLMessage message) {
        //Default implementation
        return null;
    }
    
    protected String getUrl(ProjectModel model, String tableUri, String errorUri, OpenLMessage message) {
        String url = null;
        if (model.tableBelongsToCurrentModule(tableUri)) { // table belongs to current module.
            url = getUrlForCurrentModule(errorUri, tableUri);
        } else { // table belongs to dependency module.                    
            url = getUrlForDependencyModule(message);
        }
        return url;
    }    
    
    private String getUrlForCurrentModule(String errorUri, String tableUri) {
        String url = null;        
        
        XlsUrlParser uriParser = new XlsUrlParser();
        uriParser.parse(errorUri);
        url = WebContext.getContextPath() + "/faces/pages/modules/rulesEditor/showTable.xhtml"
            + "?uri=" + StringTool.encodeURL(tableUri);
        if (StringUtils.isNotBlank(uriParser.cell)) {
            url += "&errorCell=" + uriParser.cell;
        }
        return url;
    }
    
    private String getUrlForDependencyModule(OpenLMessage message) {
        return DependencyModuleUrlStub.getUrlForError(message);
    }

}
