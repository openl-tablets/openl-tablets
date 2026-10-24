package org.openl.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLCheckedException;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenLRuntimeException;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLWarnMessage;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.SourceRange;
import org.openl.source.impl.CompositeSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.text.IPosition;
import org.openl.util.text.LocationUtils;
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
        final var expected = """
                Openl Code Fragment:\r
                =======================\r
                 SpreadsheetResult MyS1pr (Stri1ng currentAgeBand,  String SIC)\r
                                           ^^^^^^^\r
                =======================\r
                """;

        assertEquals(expected, actual);
    }

    private static final String B2 = "file:/wb.xlsx?sheet=Main&cell=B2";
    private static final String A5 = "file:/wb.xlsx?sheet=Main&cell=A5";
    private static final String A6 = "file:/wb.xlsx?sheet=Main&cell=A6";

    /** The code after the '=' a cell begins with. */
    private static final IOpenSourceCodeModule FORMULA = new SubTextSourceCodeModule(
            new StringSourceCodeModule("= format(x)", B2), 1);

    /** A Method table body of two cells, the first of which holds two lines. */
    private static final IOpenSourceCodeModule BODY = new CompositeSourceCodeModule(new IOpenSourceCodeModule[]{
            new StringSourceCodeModule("int a = 1;\nint b = 2;", A5),
            null,
            new StringSourceCodeModule("return format(a);", A6)}, "\n");

    @Test
    void aPieceAfterTheEqualsSignIsCountedInTheCell() {
        // " format(x)": the piece "format(x)" ends at its last character, as a syntax node does.
        var location = LocationUtils.createTextInterval(1, 9);

        assertEquals(new SourceRange(B2, "= format(x)", 2, 11), SourceCodeURLTool.rangeOf(location, FORMULA));
        assertEquals(B2 + "&start=2&end=10", SourceCodeURLTool.makeSourceLocationURL(location, FORMULA));
    }

    @Test
    void aPieceOfAPartOfAPartIsCountedInTheSource() {
        var inner = new SubTextSourceCodeModule(FORMULA, 1);

        assertEquals(new SourceRange(B2, "= format(x)", 2, 8),
                SourceCodeURLTool.rangeOf(LocationUtils.createTextInterval(0, 5), inner));
    }

    @Test
    void aPieceOfAMethodBodyIsCountedInTheCellThatHoldsIt() {
        // "int a = 1;\nint b = 2;\nreturn format(a);\n": the first cell takes two lines, so the piece on the
        // third line is in the second cell.
        var location = LocationUtils.createTextInterval(29, 37);

        assertEquals(new SourceRange(A6, "return format(a);", 7, 16), SourceCodeURLTool.rangeOf(location, BODY));
        assertEquals(A6 + "&start=7&end=15", SourceCodeURLTool.makeSourceLocationURL(location, BODY));
        assertEquals(new SourceRange(A5, "int a = 1;\nint b = 2;", 11, 14),
                SourceCodeURLTool.rangeOf(LocationUtils.createTextInterval(11, 13), BODY));
    }

    @Test
    void aPieceRunningOverSeveralCellsEndsWithTheFirstOne() {
        assertEquals(new SourceRange(A5, "int a = 1;\nint b = 2;", 4, 21),
                SourceCodeURLTool.rangeOf(LocationUtils.createTextInterval(4, 30), BODY));
    }

    @Test
    void aLocationWithoutTextCoversTheWholeCode() {
        assertEquals(new SourceRange(B2, "= format(x)", 1, 11), SourceCodeURLTool.rangeOf(null, FORMULA));
        assertEquals(B2, SourceCodeURLTool.makeSourceLocationURL(null, FORMULA));
        assertEquals("NO_MODULE", SourceCodeURLTool.makeSourceLocationURL(null, null));
    }

    @Test
    void aSourceWithoutUriOrPositionHasNoRange() {
        var module = new StringSourceCodeModule("x + y", null);
        assertNull(SourceCodeURLTool.rangeOf(LocationUtils.createTextInterval(0, 0), module));
        assertEquals("", SourceCodeURLTool.makeSourceLocationURL(LocationUtils.createTextInterval(0, 0), module));
        assertNull(SourceCodeURLTool.rangeOf(null, new StringSourceCodeModule(null, B2)));

        var position = mock(IPosition.class);
        when(position.getAbsolutePosition(any())).thenThrow(UnsupportedOperationException.class);
        var location = new TextInterval(position, position);
        assertNull(SourceCodeURLTool.rangeOf(location, FORMULA));
        assertEquals(B2 + "&start=-1&end=-1", SourceCodeURLTool.makeSourceLocationURL(location, FORMULA));
    }

    @Test
    void errorsAndWarningsKeepThePieceOfTheSource() {
        var location = LocationUtils.createTextInterval(29, 37);
        var expected = new SourceRange(A6, "return format(a);", 7, 16);
        var error = new OpenLCompilationException("Method is not found.", null, location, BODY);
        assertEquals(expected, error.getSourceRange());
        assertEquals(expected, new OpenLErrorMessage(error).getSourceRange());
        assertNull(new OpenLCompilationException("No source.").getSourceRange());

        var node = mock(ISyntaxNode.class);
        when(node.getModule()).thenReturn(BODY);
        when(node.getSourceLocation()).thenReturn(location);
        assertEquals(expected, new OpenLWarnMessage("Deprecated.", node).getSourceRange());
        var boundNode = mock(IBoundNode.class);
        when(boundNode.getSyntaxNode()).thenReturn(node);
        assertEquals(expected, new OpenLRuntimeException("Failed.", boundNode).getSourceRange());

        assertEquals(expected, new OpenlNotCheckedException("Failed.", null, location, BODY).getSourceRange());
        assertEquals(expected, new OpenLCheckedException("Failed.", null, location, BODY).getSourceRange());
        assertNull(new OpenlNotCheckedException("No source.").getSourceRange());
        assertNull(new OpenLCheckedException("No source.").getSourceRange());

        assertNull(new OpenLWarnMessage("Deprecated.", mock(ISyntaxNode.class)).getSourceRange());
        assertNull(new OpenLRuntimeException("No node.").getSourceRange());
    }
}
