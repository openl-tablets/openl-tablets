package org.openl.rules.indexer;

import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.openl.rules.table.syntax.XlsURLConstants;
import org.openl.rules.word.WordDocSourceCodeModule;

public class WordParagraphElement implements IIndexElement {
    Paragraph paragraph;
    int paragraphNum;
    WordDocSourceCodeModule document;

    String indexedText = null;

    String uri;

    public WordParagraphElement(Paragraph paragraph, int paragraphNum, WordDocSourceCodeModule document) {
        this.paragraph = paragraph;
        this.paragraphNum = paragraphNum;
        this.document = document;
    }

    public String getCategory() {
        return IDocumentType.WORD_PARAGRAPH.getCategory();
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
        if (indexedText != null) {
            return indexedText;
        }

        indexedText = paragraph.getCharacterRun(0).text();

        for (int i = 1; i < paragraph.numCharacterRuns(); i++) {
            CharacterRun chr = paragraph.getCharacterRun(i);
            indexedText += chr.text();
        }

        return indexedText;
    }

    public Paragraph getParagraph() {
        return paragraph;
    }

    public int getParagraphNum() {
        return paragraphNum;
    }

    public String getType() {
        return IDocumentType.WORD_PARAGRAPH.getType();
    }

    public String getUri() {
        if (uri == null) {
            uri = document.getUri() + "?" + XlsURLConstants.PARAGRAPH_START + "=" + paragraphNum + "&"
                    + XlsURLConstants.PARAGRAPH_END + "=" + paragraphNum;
        }
        return uri;
    }

}
