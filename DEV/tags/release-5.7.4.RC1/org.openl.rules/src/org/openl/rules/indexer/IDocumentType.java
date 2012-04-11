package org.openl.rules.indexer;

public interface IDocumentType {

    class Instance implements IDocumentType {
        String type, category;

        Instance(String type, String category) {
            this.type = type;
            this.category = category;
        }

        public String getCategory() {
            return category;
        }

        public String getType() {
            return type;
        }
    }

    String DOCUMENT = "Document", // workbook, .doc etc
            WORKBOOK_TYPE = "Workbook", WORDDOC_TYPE = "Word.doc",
            WORKSHEET_TABLE_TYPE = "Worksheet Table",
            WORKSHEET_CATEGORY = "Worksheet", WORKSHEET_TYPE = "Worksheet",
            TABLE_CATEGORY = "Table",
            WORKSHEET_CELL_TYPE = "Worksheet Cell", CELL_CATEGORY = "Cell",
            PARAGRAPH = "Paragraph",
            WORD_TABLE_TYPE = "Word Table", WORD_PARAGRAPH_TYPE = "Word.Paragraph";

    IDocumentType WORKBOOK = new Instance(WORKBOOK_TYPE, DOCUMENT), WORKSHEET = new Instance(
            WORKSHEET_TYPE, WORKSHEET_CATEGORY), WORKSHEET_TABLE = new Instance(WORKSHEET_TABLE_TYPE, TABLE_CATEGORY),
            WORKSHEET_CELL = new Instance(WORKSHEET_CELL_TYPE, CELL_CATEGORY),

            WORD_DOC = new Instance(WORDDOC_TYPE, DOCUMENT), WORD_PARAGRAPH = new Instance(WORD_PARAGRAPH_TYPE,
                    PARAGRAPH), WORD_TABLE = new Instance(WORD_TABLE_TYPE, TABLE_CATEGORY)

            ;

    String getCategory();

    String getType();

}
