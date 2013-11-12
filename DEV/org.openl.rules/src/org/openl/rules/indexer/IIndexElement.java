package org.openl.rules.indexer;

public interface IIndexElement {
    String getCategory();

    String getDisplayName();

    String getIndexedText();

    // IIndexElement getParent();
    String getType();

    String getUri();

    // static final String
    // DOCUMENT = "Document", //workbook, .doc etc
    // WORKBOOK = "Workbook",
    //
    // DOCUMENT_PART="Document Part", // worksheet, top-level header in .doc
    // DOCUMENT_COMPONENT = "Component", //table, Paragraph
    // COMPONENT_PART = "Component Part", //header, properties, body
    // COMPONENT_ELEMENT = "Element"; // code, data, comment, column/row header,
    // text

}
