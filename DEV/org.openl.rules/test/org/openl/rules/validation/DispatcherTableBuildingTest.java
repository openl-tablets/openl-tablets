package org.openl.rules.validation;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.engine.OpenLSystemProperties;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.OpenLWarnMessage;
import org.openl.message.Severity;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.ISyntaxNode;

public class DispatcherTableBuildingTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "test/rules/overload/DispatcherTest.xlsx";

    public DispatcherTableBuildingTest() {
        super(SRC);
    }

    private static String csr;

    @BeforeClass
    public static void before() {
        csr = System.getProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY, "");
        System.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY, OpenLSystemProperties.DISPATCHING_MODE_JAVA);
    }

    @AfterClass
    public static void after() {
        System.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY, csr);
    }

    private static List<OpenLMessage> getWarningsForTable(Collection<OpenLMessage> messages, TableSyntaxNode tsn) {
        List<OpenLMessage> warningsForTable = new ArrayList<>();
        Collection<OpenLMessage> warnMessages = OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.WARN);
        for (OpenLMessage message : warnMessages) {
            if (message instanceof OpenLWarnMessage) {// there can be simple
                // OpenLMessages with
                // severity WARN
                OpenLWarnMessage warning = (OpenLWarnMessage) message;
                ISyntaxNode syntaxNode = warning.getSource();
                if (syntaxNode == tsn) {
                    warningsForTable.add(warning);
                }
            }
        }
        return warningsForTable;
    }

    @Test
    public void checkArraysInSignature() {
        TableSyntaxNode dispatcherTable = findDispatcherForMethod("arraysTest");
        assertNotNull(dispatcherTable);
        assertFalse(dispatcherTable.hasErrors());
        assertTrue(getWarningsForTable(getCompiledOpenClass().getMessages(), dispatcherTable).size() == 0);
    }

    @Test
    public void checkKeywordsInSignature() {
        TableSyntaxNode dispatcherTable = findDispatcherForMethod("keywordsTest");
        assertNotNull(dispatcherTable);
        assertFalse(dispatcherTable.hasErrors());
        assertTrue(getWarningsForTable(getCompiledOpenClass().getMessages(), dispatcherTable).size() == 0);
    }
}
