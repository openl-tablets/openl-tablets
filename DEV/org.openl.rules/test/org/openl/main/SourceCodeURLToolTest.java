package org.openl.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import org.openl.util.text.IPosition;
import org.openl.util.text.TextInterval;

class SourceCodeURLToolTest {

    @Test
    void printCodeAndErrorTest() {
        IPosition start = mock(IPosition.class);
        when(start.getColumn(any())).thenReturn(28);
        when(start.getLine(any())).thenReturn(0);

        IPosition end = mock(IPosition.class);
        when(end.getColumn(any())).thenReturn(34);
        when(end.getLine(any())).thenReturn(0);

        var location = new TextInterval(start, end);

        var stringWriter = new StringWriter();
        try (var printWriter = new PrintWriter(stringWriter)) {
            SourceCodeURLTool.printCodeAndError(location,
                    " SpreadsheetResult MyS1pr (Stri1ng currentAgeBand,  String SIC)",
                    printWriter);
        }

        final var actual = stringWriter.toString();
        final var expected = "Openl Code Fragment:\r\n" + "=======================\r\n" + " SpreadsheetResult MyS1pr (Stri1ng currentAgeBand,  String SIC)\r\n" + "                           ^^^^^^^\r\n" + "=======================\r\n";

        assertEquals(expected, actual);
    }

}
