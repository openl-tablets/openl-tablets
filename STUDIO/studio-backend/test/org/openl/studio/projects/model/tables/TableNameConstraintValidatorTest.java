package org.openl.studio.projects.model.tables;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TableNameConstraintValidatorTest {

    private final TableNameConstraintValidator validator = new TableNameConstraintValidator();

    @Test
    void acceptsANameOpenLCompilesATableUnder() {
        assertTrue(validator.isValid("BankLimitIndex", null));
        assertTrue(validator.isValid("_rate$2", null));
        // OpenL compiles a name of any script, so the dialog is not the only thing refusing one.
        assertTrue(validator.isValid("Привет", null));
    }

    @Test
    void refusesANameThatWouldStopTheModuleFromCompiling() {
        assertFalse(validator.isValid("2ndRate", null));
        assertFalse(validator.isValid("Bank Limit", null));
        // The name reaches the header as it is given, so a padded one is a padded header.
        assertFalse(validator.isValid("  Greeting  ", null));
        assertFalse(validator.isValid("rate-2", null));
    }

    @Test
    void leavesAMissingNameToTheConstraintThatRequiresOne() {
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("   ", null));
    }
}
