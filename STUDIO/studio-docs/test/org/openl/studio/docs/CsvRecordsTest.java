package org.openl.studio.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.studio.docs.CsvRecords.Cell;
import org.openl.studio.docs.CsvRecords.Row;

class CsvRecordsTest {

    @Test
    void readsValuesSeparatedByCommas() {
        var records = CsvRecords.parse("Name, Value\none,1\n");

        assertEquals(List.of(
                new Row(0, List.of(new Cell("Name", false), new Cell("Value", false))),
                new Row(1, List.of(new Cell("one", false), new Cell("1", false)))), records.rows());
        assertEquals(List.of(), records.issues());
    }

    @Test
    void readsQuotedValuesLiterally() {
        var records = CsvRecords.parse("\"a, b\",\"say \"\"hi\"\"\", \"<\" ,\"two\nlines\"\nnext");

        assertEquals(List.of(
                new Row(0, List.of(new Cell("a, b", true), new Cell("say \"hi\"", true), new Cell("<", true),
                        new Cell("two\nlines", true))),
                new Row(2, List.of(new Cell("next", false)))), records.rows());
        assertEquals(List.of(), records.issues());
    }

    @Test
    void skipsBlankLinesAndKeepsEmptyValues() {
        var records = CsvRecords.parse("a,,\n\n  \n,b");

        assertEquals(List.of(
                new Row(0, List.of(new Cell("a", false), new Cell("", false), new Cell("", false))),
                new Row(3, List.of(new Cell("", false), new Cell("b", false)))), records.rows());
    }

    @Test
    void reportsBrokenQuotes() {
        var records = CsvRecords.parse("say \"hi\",x\n\"done\" here,y\n\"open,\nz");

        assertEquals(List.of(
                new Issue(0, "A quote inside an unquoted value. Quote the whole value and double the quote."),
                new Issue(1, "Text after a closing quote. Quote the whole value."),
                new Issue(2, "A quoted value is not closed.")), records.issues());
    }

    @Test
    void tellsMarkersFromValues() {
        assertEquals(true, new Cell("<", false).is("<"));
        assertEquals(false, new Cell("<", true).is("<"));
        assertEquals(false, new Cell("x", false).is("<"));
    }
}
