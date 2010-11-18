package org.openl.mapper;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.openl.exception.OpenLRuntimeException;
import org.openl.mapper.model.*;
import org.openl.rules.runtime.ApiBasedRulesEngineFactory;

public class RulesBeanMapperTest {

    @Test
    public void testMapper1() {

        File source = new File("src/test/resources/org/openl/mapper/RulesBeanMapperTest.xlsx");
        ApiBasedRulesEngineFactory factory = new ApiBasedRulesEngineFactory(source);
        Class<?> instanceClass;
        Object instance;
        
        try {
            instanceClass = factory.getInterfaceClass();
            instance = factory.makeInstance();
        } catch (Exception e) {
            throw new OpenLRuntimeException("Cannot load rules project", e);
        }

        RulesBeanMapper mapper = new RulesBeanMapper(instanceClass, instance);

        A a = new A();
        a.setA("string");
        a.setB(10);
        a.setX(new String[] { "x", null, "y" });

        C c = mapper.map(a, C.class);

        A a1 = new A();
        mapper.map(c, a1);

        F f = new F();
        f.setA(a);

        E e = mapper.map(f, E.class);

        assertEquals(10, e.getD().getI());

        B b = new B();
        b.setFirst("string");
        A a2 = new A();
        mapper.map(b, a2);

        B b1 = mapper.map(a2, B.class);
        assertEquals("string", b1.getFirst());
    }

}
