package org.openl.rules.indexer;

import org.apache.poi.hwpf.usermodel.RangeHack;
import org.apache.poi.hwpf.usermodel.Table;
import org.openl.rules.table.syntax.XlsURLConstants;
import org.openl.rules.word.WordDocSourceCodeModule;

public class WordTableElement implements IIndexElement {
    Table table;
    WordDocSourceCodeModule document;

    String uri;

    public WordTableElement(Table table, WordDocSourceCodeModule document) {
        this.table = table;
        this.document = document;
    }

    public String getCategory() {
        return IDocumentType.WORD_TABLE.getCategory();
    }

    public String getDisplayName() {
        return null;
    }

    // public IIndexElement getParent()
    // {
    // return document;
    // }

    public WordDocSourceCodeModule getDocument() {
        return document;
    }

    public String getIndexedText() {
        return null;
    }

    public int getParagraphNum() {
//		return table.getParStart() + 1;
        return RangeHack.getParStart(table) + 1;
    }

    public Table getTable() {
        return table;
    }

    public String getType() {
        return IDocumentType.WORD_TABLE.getType();
    }

    public String getUri() {
        if (uri == null) {
            uri = document.getUri() + "?" + XlsURLConstants.WD_PAR_START + "=" + getParagraphNum() + '&'
                    + XlsURLConstants.WD_PAR_END + '=' + (getParagraphNum() + table.numParagraphs() - 1);
        }
        return uri;
    }

}
