package org.openl.rules.table.word;

import org.apache.poi.hwpf.usermodel.TableCell;
import org.openl.rules.table.syntax.XlsURLConstants;

public class WordCell {
    TableCell tcell;

    boolean merged;

    public WordCell(TableCell tc) {
        tcell = tc;
    }

    public int getParEnd() {
        return tcell.getParStart() + tcell.numParagraphs();
    }

    public int getParStart() {
        return tcell.getParStart() + 1;
    }

    public String getStringValue() {
        String text = tcell.text();
        int len = text.length();
        if (len == 0) {
            return text;
        }
        if (text.charAt(len - 1) == 7) {
            return text.substring(0, len - 1);
        }
        return text;
    }

    public String getUri() {
        return XlsURLConstants.PARAGRAPH_START + "=" + (tcell.getParStart() + 1) + "&" + XlsURLConstants.PARAGRAPH_END
                + "=" + (tcell.getParStart() + tcell.numParagraphs());
    }

    public boolean isMerged() {
        return merged;
    }

    public void setMerged(boolean merged) {
        this.merged = merged;
    }

}
