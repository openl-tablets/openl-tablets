package org.openl.rules.validation.properties.dimentional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;

public class TableSyntaxNodeDispatcherBuilderTest {

    @Test
    public void testEmpty() {
        MatchingOpenMethodDispatcher dispatcher = mock(MatchingOpenMethodDispatcher.class);
        RulesModuleBindingContext context = mock(RulesModuleBindingContext.class);
        XlsModuleOpenClass moduleOpenClass = mock(XlsModuleOpenClass.class);
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
