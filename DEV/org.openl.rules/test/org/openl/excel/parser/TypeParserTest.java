package org.openl.excel.parser;

import org.junit.Assert;
import org.junit.Test;
import org.openl.OpenL;
import org.openl.engine.OpenLParseManager;
import org.openl.source.SourceType;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.code.IParsedCode;

public class TypeParserTest {
    @Test
    public void type() {
        OpenL openl = OpenL.getInstance(OpenL.OPENL_J_NAME);
        IParsedCode result = new OpenLParseManager(openl).parseSource(new StringSourceCodeModule("String", ""),
            SourceType.TYPE);
        Assert.assertEquals(0, result.getErrors().length);
    }

    @Test
    public void typeWithSpaces() {
        OpenL openl = OpenL.getInstance(OpenL.OPENL_J_NAME);
        IParsedCode result = new OpenLParseManager(openl)
            .parseSource(new StringSourceCodeModule("String sadfa sadf", ""), SourceType.TYPE);
        Assert.assertEquals(1, result.getErrors().length);
    }
}