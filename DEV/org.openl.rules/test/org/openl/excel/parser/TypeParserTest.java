package org.openl.excel.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.openl.OpenL;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.code.IParsedCode;

public class TypeParserTest {
    @Test
    public void type() {
        OpenL openl = OpenL.getInstance();
        IParsedCode result = openl.getParser().parseAsType(new StringSourceCodeModule("String", ""));
        assertEquals(0, result.getErrors().length);
    }

    @Test
    public void typeWithSpaces() {
        OpenL openl = OpenL.getInstance();
        IParsedCode result = openl.getParser().parseAsType(new StringSourceCodeModule("String sadfa sadf", ""));
        assertEquals(1, result.getErrors().length);
    }
}
