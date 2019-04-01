package org.openl.rules.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.OpenLWarnMessage;
import org.openl.message.Severity;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

public class DimensionalPropertyValidatorTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/validation/DimensionalPropertyValidator.xls";

    public DimensionalPropertyValidatorTest() {
        super(SRC);
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

    private Collection<IOpenMethod> findMethods(IOpenClass openClass, String methodName) {
        Collection<IOpenMethod> methods = new ArrayList<>();
        for (IOpenMethod method : openClass.getMethods()) {
            if (methodName.equals(method.getName())) {
                if (method instanceof OpenMethodDispatcher) {
                    methods.addAll(((OpenMethodDispatcher) method).getCandidates());
                } else {
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    @Test
    public void testHello() {
        CompiledOpenClass compiledOpenClass = getCompiledOpenClass();
        IOpenClass openClass = compiledOpenClass.getOpenClassWithErrors();
        Collection<IOpenMethod> methods = findMethods(openClass, "Hello");
        Assert.assertEquals(2, methods.size());
        for (IOpenMethod method : methods) {
            List<OpenLMessage> messages = getWarningsForTable(compiledOpenClass.getMessages(),
                (TableSyntaxNode) method.getInfo().getSyntaxNode());
            Assert.assertEquals(1, messages.size());
            Assert.assertEquals(
                "Ambiguous definition of properties values. Details: (country={AL, CN}) and (country={AL, DZ, BY})",
                messages.get(0).getSummary());
        }
    }

    @Test
    public void testHello2() {
        CompiledOpenClass compiledOpenClass = getCompiledOpenClass();
        IOpenClass openClass = compiledOpenClass.getOpenClassWithErrors();
        Collection<IOpenMethod> methods = findMethods(openClass, "Hello2");
        Assert.assertEquals(2, methods.size());
        for (IOpenMethod method : methods) {
            List<OpenLMessage> messages = getWarningsForTable(compiledOpenClass.getMessages(),
                (TableSyntaxNode) method.getInfo().getSyntaxNode());
            Assert.assertEquals(1, messages.size());
            Assert.assertEquals(
                "Ambiguous definition of properties values. Details: (country={AL, DZ, AR}, state={AL}) and (country={AL}, state={AL, AK, AZ})",
                messages.get(0).getSummary());
        }
    }

    @Test
    public void testHello3() {
        CompiledOpenClass compiledOpenClass = getCompiledOpenClass();
        IOpenClass openClass = compiledOpenClass.getOpenClassWithErrors();
        Collection<IOpenMethod> methods = findMethods(openClass, "Hello3");
        Assert.assertEquals(3, methods.size());
        for (IOpenMethod method : methods) {
            ITableProperties props = PropertiesHelper.getTableProperties(method);
            List<OpenLMessage> messages = getWarningsForTable(compiledOpenClass.getMessages(),
                (TableSyntaxNode) method.getInfo().getSyntaxNode());
            if (props.getState().length == 1 && props.getCountry().length == 1) {
                Assert.assertEquals(0, messages.size());
            } else {
                Assert.assertEquals(1, messages.size());
            }
        }
    }

}