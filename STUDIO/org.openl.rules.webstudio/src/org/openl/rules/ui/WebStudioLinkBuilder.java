package org.openl.rules.ui;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.tableeditor.model.ui.LinkBuilder;

public final class WebStudioLinkBuilder implements LinkBuilder {
    private final WebStudio webStudio;

    public WebStudioLinkBuilder(WebStudio webStudio) {
        this.webStudio = webStudio;
    }

    @Override
    public String createLinkForTable(String tableUri, String text) {
        String urlToTable;
        String moduleUri = webStudio.url("table", tableUri);
        if (moduleUri == null) {
            moduleUri = webStudio.url("table");
        }
        urlToTable = moduleUri + "?id=" + TableUtils.makeTableId(tableUri);
        return "<a href=\"" + urlToTable + "\">" + escapeHtml4(text) + "</a>";
    }
}
