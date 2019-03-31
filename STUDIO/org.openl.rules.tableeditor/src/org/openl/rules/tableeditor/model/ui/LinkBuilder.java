package org.openl.rules.tableeditor.model.ui;

public interface LinkBuilder {
    /**
     * Create link to other table from it's internal uri. Implementation should escape text shown to user.
     *
     * @param tableUri internal uri
     * @param text text shown to user
     * @return clickable link
     */
    String createLinkForTable(String tableUri, String text);
}
