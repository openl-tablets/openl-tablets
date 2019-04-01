package org.openl.binding.impl.cast;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.domain.IDomain;
import org.openl.domain.StringDomain;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;

public class TypeToAliasCastTest {
    @Test
    public void testSingle() {
        IDomain<String> strDomain = new StringDomain(new String[] { "Val1", "Val2" });
        DomainOpenClass domain = new DomainOpenClass("TestDomain", JavaOpenClass.STRING, strDomain, null);
        TypeToAliasCast cast = new TypeToAliasCast(domain);

        Object value = cast.convert("Val1");
        assertNotNull(value);
        assertEquals("Val1", value);

        assertNull(cast.convert(null));

        try {
            cast.convert("Not Existing");
            fail("Should be exception");
        } catch (OutsideOfValidDomainException e) {
            assertEquals(e.getOriginalMessage(),
                "Object 'Not Existing' is outside of valid domain 'TestDomain'. Valid values: [Val1, Val2]");
        }
    }
}
