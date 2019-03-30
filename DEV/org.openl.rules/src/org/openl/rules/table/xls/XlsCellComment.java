package org.openl.rules.table.xls;

import org.apache.poi.ss.usermodel.Comment;
import org.openl.rules.table.ICellComment;

/**
 * @author Andrei Astrouski
 */
public class XlsCellComment implements ICellComment {

    private Comment xlxComment;

    public XlsCellComment(Comment xlsComment) {
        this.xlxComment = xlsComment;
    }

    @Override
    public String getAuthor() {
        return xlxComment.getAuthor();
    }

    @Override
    public String getText() {
        return xlxComment.getString().getString();
    }

    public Comment getXlxComment() {
        return xlxComment;
    }

}
