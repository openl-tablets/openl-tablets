package org.openl.rules.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.openl.message.Severity;
import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.constraints.RegexpValueConstraint;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;

/**
 * @author Yury Molchan
 */
class PropertyMessagesTest {

    @Test
    void takesTheSeverityTheDefinitionDeclares() {
        assertEquals(Severity.WARN, ValidationUtils.severityOf("version", Severity.ERROR));
        assertEquals(Severity.ERROR, ValidationUtils.severityOf("id", Severity.WARN));
    }

    @Test
    void takesTheGivenSeverityWhenTheDefinitionDeclaresNone() {
        assertEquals(Severity.ERROR, ValidationUtils.severityOf("precision", Severity.ERROR));
        assertEquals(Severity.WARN, ValidationUtils.severityOf("unknownProperty", Severity.WARN));
    }

    @Test
    void reportsAnErrorOrAWarning() {
        assertEquals(Severity.ERROR, ValidationUtils.messageOf("Wrong", Severity.ERROR, null).getSeverity());
        assertEquals(Severity.WARN, ValidationUtils.messageOf("Odd", Severity.WARN, null).getSeverity());
        assertEquals("Odd", ValidationUtils.messageOf("Odd", Severity.INFO, null).getSummary());
    }

    @Test
    void readsTheVersionFormatFromItsDefinition() {
        var version = TablePropertyDefinitionUtils.getPropertyByName("version");
        var regexp = RegexpValueConstraint.regexpOf(version.getConstraints());

        assertEquals("([0-9]{1,9})\\.([0-9]{1,9})\\.([0-9]{1,9})", regexp);
        assertEquals(Severity.WARN, version.getErrorSeverity());
    }

    @Test
    void findsNoFormatWithoutARegexpConstraint() {
        assertNull(RegexpValueConstraint.regexpOf(null));
        assertNull(RegexpValueConstraint.regexpOf(new Constraints("unique in:module")));
    }
}
