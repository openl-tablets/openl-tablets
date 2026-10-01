package org.openl.excel.parser.event.style;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CommentsCollectorTest {

    @Test
    void collectsCommentsWithoutKeepingChildShapes() {
        var collector = new CommentsCollector();

        assertTrue(collector.getChildren().isEmpty());
        assertTrue(collector.getComments().isEmpty());
    }
}
