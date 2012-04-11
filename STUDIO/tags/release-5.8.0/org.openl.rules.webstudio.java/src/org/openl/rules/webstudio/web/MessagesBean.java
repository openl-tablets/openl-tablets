package org.openl.rules.webstudio.web;

import org.ajax4jsf.component.UIRepeat;
import org.apache.commons.lang.StringUtils;
import org.openl.exception.OpenLException;
import org.openl.exception.OpenLExceptionUtils;
import org.openl.main.SourceCodeURLTool;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

public class MessagesBean {

    private UIRepeat messages;

    public MessagesBean() {
    }

    public UIRepeat getMessages() {
        return messages;
    }

    public void setMessages(UIRepeat messages) {
        this.messages = messages;
    }

    public String getSummary() {
        OpenLMessage message = (OpenLMessage) messages.getRowData();
        String summary = message.getSummary();
        if (StringUtils.isNotBlank(summary)) {
            return summary.replaceAll("\\r\\n", "<br>");
        }
        return StringUtils.EMPTY;
    }

    public String[] getErrorCode() {
        OpenLMessage message = (OpenLMessage) messages.getRowData();

        if (message instanceof OpenLErrorMessage) {
            OpenLErrorMessage errorMessage = (OpenLErrorMessage) message;
            OpenLException error = errorMessage.getError();

            return OpenLExceptionUtils.getErrorCode(error);
        }

        return new String[0];
    }

    public String getTableUri() {
        OpenLErrorMessage message = (OpenLErrorMessage) messages.getRowData();
        OpenLException error = message.getError();

        String errorUri = SourceCodeURLTool.makeSourceLocationURL(error.getLocation(), error.getSourceModule(), "");

        WebStudio studio = WebStudioUtils.getWebStudio();

        return studio.getModel().findTableUri(errorUri);
    }

    public String getErrorCell() {
        OpenLErrorMessage message = (OpenLErrorMessage) messages.getRowData();
        OpenLException error = message.getError();

        String errorUri = SourceCodeURLTool.makeSourceLocationURL(error.getLocation(), error.getSourceModule(), "");

        XlsUrlParser uriParser = new XlsUrlParser();
        uriParser.parse(errorUri);

        return uriParser.cell;
    }

}
