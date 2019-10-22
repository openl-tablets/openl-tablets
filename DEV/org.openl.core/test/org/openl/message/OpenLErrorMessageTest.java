package org.openl.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenLRuntimeException;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.util.text.LocationUtils;

public class OpenLErrorMessageTest {

    @Test
    public void differentErrorTypesEquality() {
        IOpenSourceCodeModule module1 = new StringSourceCodeModule("Module1", "uri1");
        IOpenSourceCodeModule module2 = new StringSourceCodeModule("Module2", "uri2");

        OpenLErrorMessage e1 = new OpenLErrorMessage(
            new OpenLCompilationException("test", null, LocationUtils.createTextInterval(0, 3), module1));
        OpenLErrorMessage e2 = new OpenLErrorMessage(
            new OpenLCompilationException("test", null, LocationUtils.createTextInterval(0, 3), module1));
        OpenLErrorMessage e3 = new OpenLErrorMessage(
            new OpenLCompilationException("test", null, LocationUtils.createTextInterval(1, 4), module2));
        OpenLErrorMessage e4 = new OpenLErrorMessage(new OpenLRuntimeException("test"));
        OpenLErrorMessage e5 = new OpenLErrorMessage(new OpenlNotCheckedException("test"));

        // If error message contains location, it should be considered in equality comparison.
        // Otherwise only message should be considered.

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());

        if (e1.hashCode() != e3.hashCode()) {
            assertNotEquals(e1, e3);
        }

        if (e1.hashCode() != e4.hashCode()) {
            assertNotEquals(e1, e4);
        }

        assertEquals(e4, e5);
        assertEquals(e4.hashCode(), e5.hashCode());
    }
}