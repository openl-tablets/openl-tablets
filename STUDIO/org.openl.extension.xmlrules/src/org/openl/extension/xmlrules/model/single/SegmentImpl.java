package org.openl.extension.xmlrules.model.single;

import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.model.Segment;

@XmlType(name = "segment")
public class SegmentImpl implements Segment {
    private int segmentNumber;
    private int totalSegments;
    private boolean columnSegment = false;

    @Override
    public int getSegmentNumber() {
        return segmentNumber;
    }

    public void setSegmentNumber(int segmentNumber) {
        this.segmentNumber = segmentNumber;
    }

    @Override
    public int getTotalSegments() {
        return totalSegments;
    }

    public void setTotalSegments(int totalSegments) {
        this.totalSegments = totalSegments;
    }

    @Override
    public boolean isColumnSegment() {
        return columnSegment;
    }

    public void setColumnSegment(boolean columnSegment) {
        this.columnSegment = columnSegment;
    }
}
