package org.openl.studio.settings.model.constraint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PeriodConstraintValidatorTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "null", value = {
            "null   | true",
            "''     | true",
            "'  '   | true",
            "90d    | true",
            "2W     | true",
            "6m     | true",
            "1y     | true",
            "P1Y6M  | true",
            "90     | false",
            "0d     | false",
            "-1d    | false",
            "1 year | false",
            "abc    | false"})
    void acceptsAPeriodLongerThanZeroOrNone(String value, boolean valid) {
        assertEquals(valid, new PeriodConstraintValidator().isValid(value, null));
    }
}
