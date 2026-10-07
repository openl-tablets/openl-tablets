package org.openl.studio.projects.service.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.studio.projects.service.trace.TableInputParserService.ParseResult;

class TableInputParserServiceTest {

    @Test
    void resultsHoldingEqualParamsAreEqual() {
        var one = new ParseResult(new Object[]{"a", 1}, null);
        var other = new ParseResult(new Object[]{"a", 1}, null);

        assertEquals(one, other);
        assertEquals(one.hashCode(), other.hashCode());
    }

    @Test
    void resultsDifferingInParamsOrContextAreNotEqual() {
        var result = new ParseResult(new Object[]{"a", 1}, null);
        var context = RulesRuntimeContextFactory.buildRulesRuntimeContext();

        assertNotEquals(result, new ParseResult(new Object[]{"a", 2}, null));
        assertNotEquals(result, new ParseResult(new Object[]{"a", 1}, context));
        assertNotEquals(result, new Object());
    }

    @Test
    void toStringShowsTheContentOfTheParamsArray() {
        assertEquals("ParseResult[params=[a, 1], runtimeContext=null]",
                new ParseResult(new Object[]{"a", 1}, null).toString());
    }

    @Test
    void readsAnArrayParameterByItsContentRatherThanItsIdentity() {
        var one = new ParseResult(new Object[]{new int[]{1, 2}}, null);
        var other = new ParseResult(new Object[]{new int[]{1, 2}}, null);

        assertEquals(one, other);
        assertEquals(one.hashCode(), other.hashCode());
        assertEquals("ParseResult[params=[[1, 2]], runtimeContext=null]", one.toString());
        assertNotEquals(one, new ParseResult(new Object[]{new int[]{1, 3}}, null));
    }
}
