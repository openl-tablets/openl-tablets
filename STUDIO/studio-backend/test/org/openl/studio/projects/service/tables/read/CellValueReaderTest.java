package org.openl.studio.projects.service.tables.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.types.java.JavaOpenClass;

class CellValueReaderTest {

    /** A number a double holds, kept as a text in a numeric cell, is read as the number. */
    @ParameterizedTest
    @CsvSource({"0.5, 0.5", "1000, 1000", "0.6666666666666666, 0.6666666666666666"})
    void readsTextOfNumberDoubleHoldsAsNumber(String text, String number) {
        var value = read(text, BigDecimal.class);

        assertEquals(0, new BigDecimal(number).compareTo(new BigDecimal(value.toString())));
    }

    /** A number of more digits than a double holds stays a text, so that no reader loses its digits. */
    @ParameterizedTest
    @ValueSource(strings = {"0.6666666666666666666666666666666667", "0.66666666666666663", "9007199254740993"})
    void keepsTextOfNumberBeyondDouble(String text) {
        assertEquals(text, read(text, BigDecimal.class));
        assertEquals(text, read(text, Double.class));
    }

    /** A text of another type is read as a value of that type. */
    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void readsTextOfBoolean(String text) {
        assertEquals(Boolean.valueOf(text), read(text, Boolean.class));
    }

    /** A text that is no number stays the text. */
    @ParameterizedTest
    @ValueSource(strings = {"abc", "12abc"})
    void keepsTextOfNoNumber(String text) {
        assertEquals(text, read(text, BigDecimal.class));
    }

    private static Object read(String text, Class<?> type) {
        var cell = mock(ICell.class);
        when(cell.getObjectValue()).thenReturn(text);
        when(cell.getAbsoluteRow()).thenReturn(3);
        when(cell.getAbsoluteColumn()).thenReturn(2);
        var metaInfoReader = mock(MetaInfoReader.class);
        when(metaInfoReader.getMetaInfo(3, 2)).thenReturn(new CellMetaInfo(JavaOpenClass.getOpenClass(type), false));
        return new CellValueReader(metaInfoReader).apply(cell);
    }
}
