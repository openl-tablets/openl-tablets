package org.openl.excel.parser.event.style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFComment;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.hssf.usermodel.HSSFShapeContainer;

public final class CommentsCollector implements HSSFShapeContainer {
    private final List<HSSFComment> comments = new ArrayList<>();

    public List<HSSFComment> getComments() {
        return comments;
    }

    @Override
    public List<HSSFShape> getChildren() {
        return null;
    }

    @Override
    public void addShape(HSSFShape shape) {
        if (shape instanceof HSSFComment) {
            comments.add((HSSFComment) shape);
        }
    }

    @Override
    public void setCoordinates(int x1, int y1, int x2, int y2) {
    }

    @Override
    public void clear() {
    }

    @Override
    public int getX1() {
        return 0;
    }

    @Override
    public int getY1() {
        return 0;
    }

    @Override
    public int getX2() {
        return 0;
    }

    @Override
    public int getY2() {
        return 0;
    }

    @Override
    public boolean removeShape(HSSFShape shape) {
        return false;
    }

    @Override
    public Iterator<HSSFShape> iterator() {
        return Collections.emptyIterator();
    }
}
