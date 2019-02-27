package org.openl.rules.validation.properties.dimentional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.mockito.Mockito;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;

public class TableSyntaxNodeDispatcherBuilderTest {

    @Test
    public void testEmpty() {
        MatchingOpenMethodDispatcher dispatcher = Mockito.mock(MatchingOpenMethodDispatcher.class);
        RulesModuleBindingContext context = Mockito.mock(RulesModuleBindingContext.class);
        XlsModuleOpenClass moduleOpenClass = Mockito.mock(XlsModuleOpenClass.class);
        TableSyntaxNodeDispatcherBuilder builder = new TableSyntaxNodeDispatcherBuilder(context,
            moduleOpenClass,
            dispatcher);
        assertNull(builder.build());
    }

    @Test
    public void testNull() {
        try {
            new TableSyntaxNodeDispatcherBuilder(null, null, null);
            fail("Exception should be thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("None of the constructor parameters can be null", e.getMessage());
        }
    }
}
