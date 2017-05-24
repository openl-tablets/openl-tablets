package org.openl.rules.webstudio.web;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.exception.OpenLException;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLWarnMessage;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.StringUtils;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;
import org.richfaces.component.UIRepeat;

@ManagedBean
@RequestScoped
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

        ILocation location = null;
        IOpenSourceCodeModule module = null;
        if (message instanceof OpenLErrorMessage) {
            OpenLErrorMessage errorMessage = (OpenLErrorMessage) message;
            OpenLException error = errorMessage.getError();
            location = error.getLocation();
            module = error.getSourceModule();
        } else if (message instanceof OpenLWarnMessage) {
            OpenLWarnMessage warnMessage = (OpenLWarnMessage) message;
            ISyntaxNode source = warnMessage.getSource();
            location = source.getSourceLocation();
            module = source.getModule();
        }
        return getErrorCode(location, module);
    }

    public String getTableId() {
        String errorUri = getErrorUri();

        ProjectModel model = WebStudioUtils.getProjectModel();

        return TableUtils.makeTableId(model.findTableUri(errorUri));
    }

    public String getErrorCell() {
        String errorUri = getErrorUri();

        XlsUrlParser uriParser = new XlsUrlParser();
        uriParser.parse(errorUri);

        return uriParser.cell;
    }

    private String getErrorUri() {
        String errorUri;
        Object rowData = messages.getRowData();

        if (rowData instanceof OpenLMessage) {
            OpenLMessage message = (OpenLMessage) rowData;
            errorUri = message.getSourceLocation();
        } else {
            errorUri = null;
        }

        return errorUri;
    }


    private String[] getErrorCode(ILocation location, IOpenSourceCodeModule sourceModule) {
        String code = StringUtils.EMPTY;
        if (sourceModule != null) {
            code = sourceModule.getCode();
            if (StringUtils.isBlank(code)) {
                code = StringUtils.EMPTY;
            }
        }

        int pstart = 0;
        int pend = code.length();

        if (StringUtils.isNotBlank(code)
                && location != null && location.isTextLocation()) {
            TextInfo info = new TextInfo(code);
            pstart = location.getStart().getAbsolutePosition(info);
            pend = Math.min(location.getEnd().getAbsolutePosition(info) + 1, code.length());
        }

        if (pend != 0) {
            return new String[] {
                    code.substring(0, pstart),
                    code.substring(pstart, pend),
                    code.substring(pend, code.length())};
        }

        return new String[0];
    }

}
