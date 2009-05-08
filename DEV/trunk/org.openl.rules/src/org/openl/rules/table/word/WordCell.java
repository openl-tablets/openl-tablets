package org.openl.rules.table.word;

import org.apache.poi.hwpf.usermodel.RangeHack;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.openl.rules.table.syntax.XlsURLConstants;

public class WordCell {
    TableCell tcell;

    boolean merged;

    public WordCell(TableCell tc) {
        tcell = tc;
    }

    public int getParEnd() {
//		return tcell.getParStart() + tcell.numParagraphs();
        return RangeHack.getParStart(tcell) + tcell.numParagraphs();
    }

    public int getParStart() {
//		return tcell.getParStart() + 1;
        return RangeHack.getParStart(tcell) + 1;
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
		return XlsURLConstants.PARAGRAPH_START + "=" + getParStart() + 
			"&" + XlsURLConstants.PARAGRAPH_END + "=" + getParEnd();
    }

    public boolean isMerged() {
        return merged;
    }

    public void setMerged(boolean merged) {
        this.merged = merged;
    }

}
