package org.openl.studio.common.utils;

import static org.mockito.Mockito.mockStatic;

import java.util.ConcurrentModificationException;

import org.apache.poi.util.ThreadLocalUtil;
import org.junit.jupiter.api.Test;

class WorkbookThreadLocalsTest {

    @Test
    void requestEndClearsPoiAlsoWhenACleanerIsRegisteredMeanwhile() {
        try (var poi = mockStatic(ThreadLocalUtil.class)) {
            poi.when(ThreadLocalUtil::clearAllThreadLocals).thenThrow(ConcurrentModificationException.class);

            new WorkbookThreadLocals().requestDestroyed(null);

            poi.verify(ThreadLocalUtil::clearAllThreadLocals);
        }
    }
}
