package org.openl.rules.table.properties.inherit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.openl.rules.BaseOpenlBuilderHelper;

/**
 * Checks where the problems of table properties are reported.
 *
 * @author Yury Molchan
 */
class PropertiesCheckerTest extends BaseOpenlBuilderHelper {

    PropertiesCheckerTest() {
        super("test/rules/property/DeprecatedProperty.xlsx");
    }

    @Test
    void deprecatedPropertyIsReportedOnItsNameCell() {
        var messages = getCompiledOpenClass().getAllMessages();

        assertEquals(1, messages.size());
        var warning = messages.iterator().next();
        assertEquals("Property 'name' was deprecated. Please remove it.", warning.getSummary());
        assertTrue(warning.getSourceLocation().endsWith("?sheet=Sheet1&cell=C4"), warning.getSourceLocation());
    }
}
