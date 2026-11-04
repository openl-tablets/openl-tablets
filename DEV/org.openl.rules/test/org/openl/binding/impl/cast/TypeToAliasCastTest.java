package org.openl.binding.impl.cast;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Iterator;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.domain.IType;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.SimpleRuntimeEnv;

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

        var e = assertThrows(OutsideOfValidDomainException.class, () -> cast.convert("Not Existing"));
        assertEquals("Object 'Not Existing' is outside of valid domain 'TestDomain'. Valid values: [Val1, Val2]",
                e.getOriginalMessage());
    }

    @Test
    void theSameTextIsLookedUpOnceInAnExecution() {
        var domain = new CountingDomain(new EnumDomain<>(new Object[]{"Val1", "Val2"}));
        var cast = new TypeToAliasCast(new DomainOpenClass("TestDomain", JavaOpenClass.STRING, domain, null, null));
        var env = new SimpleRuntimeEnv();
        var value = "Val1";

        assertEquals(value, cast.convert(value, env));
        assertEquals(value, cast.convert(value, env));
        assertEquals(1, domain.lookups);

        assertEquals("Val2", cast.convert("Val2", env));
        assertEquals(2, domain.lookups);
        assertThrows(OutsideOfValidDomainException.class, () -> cast.convert("Not Existing", env));
        assertEquals(3, domain.lookups);
    }

    @Test
    void anotherExecutionLooksTheValueUpAgain() {
        var domain = new CountingDomain(new EnumDomain<>(new Object[]{"Val1"}));
        var cast = new TypeToAliasCast(new DomainOpenClass("TestDomain", JavaOpenClass.STRING, domain, null, null));
        var env = new SimpleRuntimeEnv();

        cast.convert("Val1", env);
        cast.convert("Val1", env.copy());
        cast.convert("Val1", new SimpleRuntimeEnv());
        cast.convert("Val1");
        cast.convert("Val1");
        // A copy, another environment and a conversion out of an execution know nothing of the first check
        assertEquals(5, domain.lookups);
    }

    @Test
    void theElementsOfAnArrayAreLookedUpOnceInAnExecution() {
        var domain = new CountingDomain(new EnumDomain<>(new Object[]{"Val1", "Val2"}));
        var aliasType = new DomainOpenClass("TestDomain", JavaOpenClass.STRING, domain, null, null);
        var cast = new ArrayCast(aliasType, new TypeToAliasCast(aliasType));
        var env = new SimpleRuntimeEnv();
        var values = new String[]{"Val1", "Val2"};

        assertArrayEquals(values, (Object[]) cast.convert(values, env));
        assertArrayEquals(values, (Object[]) cast.convert(values, env));
        assertEquals(2, domain.lookups);
        assertThrows(OutsideOfValidDomainException.class, () -> cast.convert(new String[]{"Val3"}, env));
    }

    @Test
    void withoutAnEnvironmentEveryValueIsLookedUp() {
        var domain = new CountingDomain(new EnumDomain<>(new Object[]{"Val1"}));
        var cast = new TypeToAliasCast(new DomainOpenClass("TestDomain", JavaOpenClass.STRING, domain, null, null));

        assertEquals("Val1", cast.convert("Val1", null));
        assertEquals("Val1", cast.convert("Val1", null));
        assertEquals(2, domain.lookups);
        assertThrows(OutsideOfValidDomainException.class, () -> cast.convert("Not Existing", null));
    }

    @Test
    void aValueThatCanChangeIsLookedUpEveryTime() {
        var value = new StringBuilder("Val1");
        var domain = new CountingDomain(new EnumDomain<>(new Object[]{value}));
        var cast = new TypeToAliasCast(new DomainOpenClass("TestDomain", JavaOpenClass.OBJECT, domain, null, null));
        var env = new SimpleRuntimeEnv();

        cast.convert(value, env);
        cast.convert(value, env);
        assertEquals(2, domain.lookups);
    }

    @Test
    void anAbsentPrimitiveIsCheckedAfterAValueWasAccepted() {
        var domain = new EnumDomain<>(new Integer[]{1, 2});
        var cast = new TypeToAliasCast(new DomainOpenClass("Grade", JavaOpenClass.INT, domain, null, null));
        var env = new SimpleRuntimeEnv();

        assertEquals(1, cast.convert(1, env));
        var e = assertThrows(OutsideOfValidDomainException.class, () -> cast.convert(null, env));
        assertEquals("Object '0' is outside of valid domain 'Grade'. Valid values: [1, 2]", e.getOriginalMessage());
    }

    /**
     * Counts the lookups of the domain it wraps.
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    private static final class CountingDomain implements IDomain<Object> {
        private final IDomain<Object> domain;
        private int lookups;

        @Override
        public IType getElementType() {
            return domain.getElementType();
        }

        @Override
        public boolean selectObject(Object obj) {
            lookups++;
            return domain.selectObject(obj);
        }

        @Override
        public Iterator<Object> iterator() {
            return domain.iterator();
        }
    }
}
