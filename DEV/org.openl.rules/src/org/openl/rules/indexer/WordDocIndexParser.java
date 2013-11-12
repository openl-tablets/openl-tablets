package org.openl.rules.indexer;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Section;
import org.apache.poi.hwpf.usermodel.Table;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.word.WordGridModel;
import org.openl.rules.word.WordDocSourceCodeModule;

public class WordDocIndexParser implements IIndexParser {

    public String getCategory() {
        return IDocumentType.WORD_DOC.getCategory();
    }

    public String getType() {
        return IDocumentType.WORD_DOC.getType();
    }

    public IIndexElement[] parse(IIndexElement root) {
        WordDocSourceCodeModule wdSrc = (WordDocSourceCodeModule) root;

        HWPFDocument doc = wdSrc.getDocument();

        Range r = doc.getRange();

        List<WordParagraphElement> v = new ArrayList<WordParagraphElement>();
        int nSections = r.numSections();
        int paragraphNum = 1;

        for (int i = 0; i < nSections; i++) {
            Section s = r.getSection(i);

            for (int y = 0; y < s.numParagraphs(); y++, paragraphNum++) {
                Paragraph p = s.getParagraph(y);
                {
                    v.add(new WordParagraphElement(p, paragraphNum, wdSrc));

                }
            }

        }

        return v.toArray(new WordParagraphElement[v.size()]);
    }

    public GridTable[] parseTables(WordDocSourceCodeModule wdSrc) {

        HWPFDocument doc = wdSrc.getDocument();

        Range r = doc.getRange();

        List<GridTable> v = new ArrayList<GridTable>();
        int nSections = r.numSections();

        for (int i = 0; i < nSections; i++) {
            Section s = r.getSection(i);

            for (int y = 0; y < s.numParagraphs(); y++) {
                Paragraph p = s.getParagraph(y);
                {
                    if (p.isInTable()) {
                        Table t = s.getTable(p);
                        WordTableElement wte = new WordTableElement(t, wdSrc);
                        WordGridModel wg = new WordGridModel(wte);
                        GridTable gt = new GridTable(0, 0, wg.getMaxRowIndex(), wg.getMaxColumnIndex(0), wg);
                        v.add(gt);

                        y += t.numParagraphs() - 1;
                    }

                }
            }

        }
        return (GridTable[]) v.toArray(new GridTable[0]);

    }

}