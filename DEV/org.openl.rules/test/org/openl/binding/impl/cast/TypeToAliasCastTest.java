package org.openl.binding.impl.cast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import org.openl.domain.EnumDomain;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;

class TypeToAliasCastTest {
    @Test
    void testSingle() {
        var strDomain = new EnumDomain<String>(new String[]{"Val1", "Val2"});
        var domain = new DomainOpenClass("TestDomain", JavaOpenClass.STRING, strDomain, null, null);
        var cast = new TypeToAliasCast(domain);

        var value = cast.convert("Val1");
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
