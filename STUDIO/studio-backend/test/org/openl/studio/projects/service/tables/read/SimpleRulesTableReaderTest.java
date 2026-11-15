package org.openl.studio.projects.service.tables.read;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.openl.studio.projects.service.tables.TableTestProjects.merge;
import static org.openl.studio.projects.service.tables.TableTestProjects.row;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * @author Yury Molchan
 */
class SimpleRulesTableReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readsARuleForEveryRowUnderAMergedCondition() throws Exception {
        var model = TableTestProjects.projectModel(tempDir, "Rules", sheet -> {
            row(sheet, 1, 1, "SimpleRules String myRule(int a, int b)");
            row(sheet, 2, 1, "a", "b", "res");
            row(sheet, 3, 1, "1", "10", "aaa");
            row(sheet, 4, 1, "", "20", "bbb");
            row(sheet, 5, 1, "2", "30", "ccc");
            merge(sheet, 3, 4, 1, 1);
        });

        var view = new SimpleRulesTableReader().read(TableTestProjects.table(model, "myRule"));

        assertEquals("[{a=1, b=10, res=aaa}, {a=1, b=20, res=bbb}, {a=2, b=30, res=ccc}]", view.rules.toString());
    }

    @Test
    void readsTheColumnsAfterATitleMergedOverTwoCells() throws Exception {
        var model = TableTestProjects.projectModel(tempDir, "Rules", sheet -> {
            row(sheet, 1, 1, "SimpleRules String myRule(int a, int b)");
            row(sheet, 2, 1, "a", "", "b", "res");
            row(sheet, 3, 1, "1", "", "10", "aaa");
            row(sheet, 4, 1, "2", "", "20", "bbb");
            merge(sheet, 1, 1, 1, 4);
            merge(sheet, 2, 2, 1, 2);
            merge(sheet, 3, 3, 1, 2);
            merge(sheet, 4, 4, 1, 2);
        });

        var view = new SimpleRulesTableReader().read(TableTestProjects.table(model, "myRule"));

        assertEquals("[{a=1, b=10, res=aaa}, {a=2, b=20, res=bbb}]", view.rules.toString());
    }
}
