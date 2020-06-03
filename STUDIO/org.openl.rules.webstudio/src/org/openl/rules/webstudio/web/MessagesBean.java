package org.openl.rules.webstudio.web;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.exception.OpenLException;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLWarnMessage;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.StringUtils;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;
import org.richfaces.component.UIRepeat;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.annotation.RequestScope;

@Controller
@RequestScope
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
            return summary;
        }
        return StringUtils.EMPTY;
    }

    public String getStacktrace() {
        OpenLMessage message = (OpenLMessage) messages.getRowData();
        if (message instanceof OpenLErrorMessage) {
            OpenLErrorMessage errorMessage = (OpenLErrorMessage) message;
            return ExceptionUtils.getStackTrace((Throwable) errorMessage.getError());
        }
        return StringUtils.EMPTY;
    }

    public boolean isHasStacktrace() {
        OpenLMessage message = (OpenLMessage) messages.getRowData();
        if (message instanceof OpenLErrorMessage) {
            OpenLErrorMessage errorMessage = (OpenLErrorMessage) message;
            return errorMessage.getError() != null;
        }
        return false;
    }

    public String[] getErrorCode() {
        OpenLMessage message = (OpenLMessage) messages.getRowData();

        ILocation location = null;
        String sourceCode = null;
        if (message instanceof OpenLErrorMessage) {
            OpenLErrorMessage errorMessage = (OpenLErrorMessage) message;
            OpenLException error = errorMessage.getError();
            location = error.getLocation();
            sourceCode = error.getSourceCode();
        } else if (message instanceof OpenLWarnMessage) {
            OpenLWarnMessage warnMessage = (OpenLWarnMessage) message;
            ISyntaxNode source = warnMessage.getSource();
            location = source.getSourceLocation();
            sourceCode = source.getModule() == null ? null : source.getModule().getCode();
        }
        return getErrorCode(location, sourceCode);
    }

    public boolean isHasLinkToCell() {
        OpenLMessage message = (OpenLMessage) messages.getRowData();

        IOpenSourceCodeModule module = null;
        String code = null;
        if (message instanceof OpenLErrorMessage) {
            OpenLException error = ((OpenLErrorMessage) message).getError();
            code = error.getSourceCode();
        } else if (message instanceof OpenLWarnMessage) {
            ISyntaxNode source = ((OpenLWarnMessage) message).getSource();
            module = source.getModule();
        }
        if (module != null) {
            code = module.getCode();
        }

        // Support the case when cell with error is empty (code == ""). See example in EPBDS-2481 (need to add column
        // Condition).
        // But error message containing link to table entirely must not show "Edit cell containing error" link.
        return getErrorUri() != null && (code != null || module instanceof StringSourceCodeModule);
    }

    public String getTableId() {
        String errorUri = getErrorUri();

        ProjectModel model = WebStudioUtils.getProjectModel();

        return TableUtils.makeTableId(model.findTableUri(errorUri));
    }

    public String getErrorCell() {
        String errorUri = getErrorUri();
        if (errorUri == null) {
            return null;
        }

        XlsUrlParser uriParser = new XlsUrlParser();
        uriParser.parse(errorUri);

        return uriParser.getCell();
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

    private String[] getErrorCode(ILocation location, String sourceCode) {
        String code = StringUtils.isBlank(sourceCode) ? StringUtils.EMPTY : sourceCode;

        int pstart = 0;
        int pend = code.length();

        if (StringUtils.isNotBlank(code) && location != null && location.isTextLocation()) {
            TextInfo info = new TextInfo(code);
            pstart = location.getStart().getAbsolutePosition(info);
            pend = Math.min(location.getEnd().getAbsolutePosition(info) + 1, code.length());
        }

        if (pend != 0) {
            return new String[] { code.substring(0, pstart), code.substring(pstart, pend), code.substring(pend) };
        }

        return new String[0];
    }

}
