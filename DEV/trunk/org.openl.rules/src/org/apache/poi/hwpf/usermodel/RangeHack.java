package org.apache.poi.hwpf.usermodel;

/**
 * Fast fix of compatibility issue.
 * 
 * @author Aleh Bykhavets
 *
 */


//TODO please rework
public class RangeHack extends Range {

    // it is hack ;)
    private RangeHack(int start, int end, Range parent) {
        super(start, end, parent);
    }

    public static int getParStart(Range range) {
      // force initParagraphs();
        range.numParagraphs();
      // return start of paragraph
      return range._parStart;
    }
}
