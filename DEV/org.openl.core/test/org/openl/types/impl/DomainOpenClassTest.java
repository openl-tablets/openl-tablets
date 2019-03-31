package org.openl.types.impl;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.domain.IDomain;
import org.openl.domain.StringDomain;
import org.openl.types.DomainOpenClassAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

public class DomainOpenClassTest {

    @Test
    public void testNotArray() {
        IOpenClass baseClass = JavaOpenClass.STRING;
        IDomain<String> domain = new StringDomain(new String[] { "Value1", "Value2" });
        DomainOpenClass domainClass = new DomainOpenClass("TestClass", baseClass, domain, null);
        assertEquals(DomainOpenClassAggregateInfo.DOMAIN_AGGREGATE, domainClass.getAggregateInfo());

        assertEquals(baseClass, domainClass.getBaseClass());

        assertEquals(null, domainClass.getComponentClass());

        assertEquals(String.class, domainClass.getInstanceClass());

        assertFalse(domainClass.isArray());

        assertFalse(domainClass.isAbstract());

        assertTrue(domainClass.isSimple());
    }

    @Test
    public void testArray() {
        IOpenClass baseClass = JavaOpenClass.STRING.getAggregateInfo().getIndexedAggregateType(JavaOpenClass.STRING);
        IDomain<String> domain = new StringDomain(new String[] { "Value1", "Value2" });
        DomainOpenClass domainClass = new DomainOpenClass("TestClass[]", baseClass, domain, null);
        assertEquals(DomainOpenClassAggregateInfo.DOMAIN_AGGREGATE, domainClass.getAggregateInfo());

        assertTrue(domainClass.isArray());

        assertEquals(baseClass, domainClass.getBaseClass());

        assertEquals(new DomainOpenClass("TestClass", baseClass, domain, null), domainClass.getComponentClass());

        IOpenClass aggregateDomain = domainClass.getAggregateInfo().getIndexedAggregateType(domainClass);

        assertEquals("TestClass[][]", aggregateDomain.getName());

        assertEquals(domainClass, aggregateDomain.getComponentClass());

        assertTrue(aggregateDomain.isArray());

        assertEquals(domainClass, aggregateDomain.getAggregateInfo().getComponentType(aggregateDomain));
    }
}
