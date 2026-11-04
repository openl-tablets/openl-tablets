package org.openl.vm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Test;

import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.domain.EnumDomain;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;

class CheckedAliasValuesTest {

    private static final DomainOpenClass TEXTS = alias(JavaOpenClass.STRING, "Val1", "Val2");

    @Test
    void theVeryValueIsRememberedForItsAliasType() {
        var values = new CheckedAliasValues();
        var value = "Val1";
        var otherAlias = alias(JavaOpenClass.STRING, "Val1");

        values.check(value, TEXTS);

        assertTrue(values.contains(value, TEXTS));
        assertFalse(values.contains(value, otherAlias));
        assertFalse(values.contains(new StringBuilder(value).toString(), TEXTS));
    }

    @Test
    void aValueOutOfTheDomainFailsAndIsNotRemembered() {
        var values = new CheckedAliasValues();
        var value = "Val3";

        var e = assertThrows(OutsideOfValidDomainException.class, () -> values.check(value, TEXTS));

        assertEquals("Object 'Val3' is outside of valid domain 'Texts'. Valid values: [Val1, Val2]",
                e.getOriginalMessage());
        assertFalse(values.contains(value, TEXTS));
    }

    @Test
    void aValueThatCanChangeIsNotRemembered() {
        var date = new Date();
        var dates = alias(JavaOpenClass.OBJECT, date);
        var values = new CheckedAliasValues();

        values.check(date, dates);

        assertFalse(values.contains(date, dates));
    }

    @Test
    void theValuesAreForgottenPastTheLimit() {
        var numbers = new Object[CheckedAliasValues.MAX_VALUES + 1];
        for (var i = 0; i < numbers.length; i++) {
            // Boxed once: the values are remembered as objects
            numbers[i] = Integer.valueOf(1_000_000 + i);
        }
        var aliasType = alias(JavaOpenClass.INT, numbers);
        var values = new CheckedAliasValues();
        for (var i = 0; i < CheckedAliasValues.MAX_VALUES; i++) {
            values.check(numbers[i], aliasType);
        }
        assertTrue(values.contains(numbers[0], aliasType));

        values.check(numbers[CheckedAliasValues.MAX_VALUES], aliasType);

        assertFalse(values.contains(numbers[0], aliasType));
        assertTrue(values.contains(numbers[CheckedAliasValues.MAX_VALUES], aliasType));
    }

    @Test
    void noneRemembersNothing() {
        CheckedAliasValues.NONE.check("Val1", TEXTS);

        assertFalse(CheckedAliasValues.NONE.contains("Val1", TEXTS));
    }

    @Test
    void anEnvironmentKeepsItsValuesAndACopyStartsWithout() {
        var env = new SimpleRuntimeEnv();

        assertSame(env.getCheckedAliasValues(), env.getCheckedAliasValues());
        assertNotSame(env.getCheckedAliasValues(), env.copy().getCheckedAliasValues());
    }

    private static DomainOpenClass alias(IOpenClass baseType, Object... values) {
        return new DomainOpenClass("Texts", baseType, new EnumDomain<>(values), null, null);
    }
}
