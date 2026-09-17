package org.openl.studio.projects.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.types.java.JavaOpenClass;

class ExecutionValueMapperTest {

    /** A value is known by the key a test case refers to it by, so values of one type are told apart unread. */
    @Test
    void aValueIsKnownByTheKeyItIsReferredToBy() {
        var driverClass = JavaOpenClass.getOpenClass(Driver.class);
        var driver = new Driver();
        driver.setName("Sara");
        driver.setLicense(new License());

        var byName = new ParameterWithValueDeclaration("driver", driver, driverClass, driverClass.getField("name"));
        assertEquals("Sara", ExecutionValueMapper.keyOf(byName));
        // A key that is itself a structure names nothing readable, so the value reads by its type alone.
        var byLicense = new ParameterWithValueDeclaration("driver", driver, driverClass, driverClass.getField("license"));
        assertNull(ExecutionValueMapper.keyOf(byLicense));
        // A value no test case refers to by a key has none, whatever fields its type declares first.
        assertNull(ExecutionValueMapper.keyOf(new ParameterWithValueDeclaration("driver", driver, driverClass)));
        // A value that is not there has no key to be found by.
        var absent = new ParameterWithValueDeclaration("driver", null, driverClass, driverClass.getField("name"));
        assertNull(ExecutionValueMapper.keyOf(absent));
    }

    public static class License {
    }

    @Getter
    @Setter
    public static class Driver {
        private String name;
        private License license;
    }
}
