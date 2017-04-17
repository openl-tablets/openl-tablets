package org.openl.engine;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.message.OpenLMessages;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.runtime.RulesEngineFactory;

public class OpenLMessagesTest {

    public static final String SRC1 = "test/rules/messages/project1.xls";
    public static final String SRC2 = "test/rules/messages/project2.xls";
    public static final String SRC3 = "test/rules/messages/merged-region.xlsx";

    @Test
    public void testInSeriesCompileMessages1() {
        // test using wrapper generation
        //
        BaseOpenlBuilderHelper helper1 = new BaseOpenlBuilderHelper() {
        };
        helper1.build(SRC1);
        OpenLMessages messages = OpenLMessages.getCurrentInstance();
        assertEquals("Should be one message from current module", 1, messages.getMessages().size());

        BaseOpenlBuilderHelper helper2 = new BaseOpenlBuilderHelper() {
        };
        helper2.build(SRC2);
        OpenLMessages messages1 = OpenLMessages.getCurrentInstance();
        assertEquals("Messages should be 5, just from current module", 2, messages1.getMessages().size());
    }

    public interface Project1Int {
        String hello(int hour);
    }

    public interface Project2Int {
        int test(int a);
    }

    @Test
    public void testInSeriesCompileMessages2() {
        // test using engine factory
        //

        RulesEngineFactory<Project1Int> engineFactory1 = new RulesEngineFactory<Project1Int>(SRC1, Project1Int.class);
        engineFactory1.setExecutionMode(false);
        try {
            engineFactory1.newEngineInstance();
        } catch (OpenlNotCheckedException ignored) {
        }
        OpenLMessages messages = OpenLMessages.getCurrentInstance();
        assertEquals("Should be one message from current module", 1, messages.getMessages().size());

        RulesEngineFactory<Project2Int> engineFactory2 = new RulesEngineFactory<Project2Int>(SRC2,
                Project2Int.class);
        engineFactory2.setExecutionMode(false);
        try {
            engineFactory2.newEngineInstance();
        } catch (OpenlNotCheckedException ignored) {
        }
        OpenLMessages messages1 = OpenLMessages.getCurrentInstance();
        assertEquals("Messages should be 5, just from current module", 2, messages1.getMessages().size());
    }

    @Test
    public void testErrorsInMergedRegions() {
        BaseOpenlBuilderHelper helper1 = new BaseOpenlBuilderHelper() { };
        helper1.build(SRC3);
        OpenLMessages messages = OpenLMessages.getCurrentInstance();
        assertEquals("Must be only one message from current module", 1, messages.getMessages().size());
    }
}
