package org.openl.binding.impl.cast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.openl.domain.IDomain;
import org.openl.domain.StringDomain;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;

public class AliasToTypeCastTest {
    @Test
    public void testSingle() {
        IDomain<String> strDomain = new StringDomain(new String[] { "Val1", "Val2" });
        DomainOpenClass domain = new DomainOpenClass("TestDomain", JavaOpenClass.STRING, strDomain, null);
        AliasToTypeCast cast = new AliasToTypeCast(domain);

        Object value = cast.convert("Val1");
        assertNotNull(value);
        assertEquals("Val1", value);

        assertNull(cast.convert(null));

        try {
            cast.convert("Not Existing");
            fail("Should be exception");
        } catch (RuntimeException e) {
            assertEquals(e.getMessage(), "Object 'Not Existing' is outside of a valid domain");
        }
    }
}
