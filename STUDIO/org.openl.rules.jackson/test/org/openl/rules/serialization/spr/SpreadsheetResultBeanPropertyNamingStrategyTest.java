package org.openl.rules.serialization.spr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.openl.rules.calc.SpreadsheetCell;

class SpreadsheetResultBeanPropertyNamingStrategyTest {

    @Test
    void keepsGeneratedSuffixAfterTransformingPropertyNames() {
        var objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(new LowerCamelCaseStrategy());

        var json = objectMapper.valueToTree(new DuplicateFields());

        assertEquals(2, json.size());
        assertEquals(1, json.get("code").intValue());
        assertEquals(2, json.get("code1").intValue());
    }

    @Test
    void namingStrategiesHandANamelessPropertyBackUnchanged() {
        var snake = new SnakeCaseStrategy();
        var lower = new LowerCaseStrategy();

        assertNull(snake.transform(null));
        assertEquals("", snake.transform(""));
        assertNull(lower.transform(null));
        assertEquals("", lower.transform(""));
    }

    @Test
    void namingStrategiesLowerCaseTheCellAndTheRow() {
        assertEquals("code_total", new SnakeCaseStrategy().transform("Code", "Total"));
        assertEquals("codetotal", new LowerCaseStrategy().transform("Code", "Total"));
    }

    @SuppressWarnings({"EffectivelyPrivate", "UnusedMethod"})
    private static final class DuplicateFields {

        @SpreadsheetCell(cell = "Code", row = "Code")
        public Integer getCode() {
            return 1;
        }

        @SpreadsheetCell(cell = "code", row = "code", suffix = "1")
        public Integer getCode1() {
            return 2;
        }
    }
}
