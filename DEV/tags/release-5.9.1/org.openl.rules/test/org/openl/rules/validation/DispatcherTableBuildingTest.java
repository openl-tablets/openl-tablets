package org.openl.rules.validation;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.engine.OpenLSystemProperties;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.OpenLWarnMessage;
import org.openl.message.Severity;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

public class DispatcherTableBuildingTest extends BaseOpenlBuilderHelper {
    private static String __src = "test/rules/overload/DispatcherTest.xlsx";

    public DispatcherTableBuildingTest() {
        super(__src);
    }

    @BeforeClass
    public static void init() {
        System.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY, OpenLSystemProperties.DISPATCHING_MODE_JAVA);
    }

    private TableSyntaxNode findDispatcherForMethod(String methodName) {
        IOpenClass moduleOpenClass = getJavaWrapper().getOpenClass();
        for (IOpenMethod method : moduleOpenClass.getMethods()) {
            if (method.getInfo() != null && method.getInfo().getSyntaxNode() instanceof TableSyntaxNode) {
                TableSyntaxNode tsn = (TableSyntaxNode) method.getInfo().getSyntaxNode();
                if (DispatcherTablesBuilder.isDispatcherTable(tsn) && method.getName().endsWith(methodName)) {
                    return tsn;
                }
            }
        }
        return null;
    }

    private static List<OpenLMessage> getWarningsForTable(List<OpenLMessage> allMessages, TableSyntaxNode tsn) {
        List<OpenLMessage> warningMessages = OpenLMessagesUtils.filterMessagesBySeverity(allMessages, Severity.WARN);
        List<OpenLMessage> warningsForTable = new ArrayList<OpenLMessage>();
        for (OpenLMessage message : warningMessages) {
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
        assertTrue(getWarningsForTable(getJavaWrapper().getCompiledClass().getMessages(), dispatcherTable).size() == 0);
    }

    @Test
    public void checkKeywordsInSignature() {
        TableSyntaxNode dispatcherTable = findDispatcherForMethod("keywordsTest");
        assertNotNull(dispatcherTable);
        assertFalse(dispatcherTable.hasErrors());
        assertTrue(getWarningsForTable(getJavaWrapper().getCompiledClass().getMessages(), dispatcherTable).size() == 0);
    }

}
