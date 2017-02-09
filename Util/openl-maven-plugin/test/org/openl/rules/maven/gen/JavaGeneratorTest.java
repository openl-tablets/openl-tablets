package org.openl.rules.maven.gen;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import org.openl.util.StringUtils;

public class JavaGeneratorTest {

    @Test
    public void testGetFieldName() {
        JavaGenerator gen = new JavaGenerator(String.class) {

            @Override
            public String generateJavaClass() {
                // default implementation, don`t need
                return null;
            }
        };

        Set<String> fieldNames = new HashSet<String>();

        String field1 = "field1";
        String field2 = "FIELD2";
        String field3 = "Field3";
        String field4 = "fIeLd4";
        fieldNames.add(field1);

        fieldNames.add(field2);
        fieldNames.add(field3);

        fieldNames.add(field4);

        assertEquals(field1, gen.getFieldName("getField1", fieldNames));
        assertEquals(field2, gen.getFieldName("getFIELD2", fieldNames));
        assertEquals(field3, gen.getFieldName("getField3", fieldNames));
        assertEquals(field4, gen.getFieldName("getfIeLd4", fieldNames));

        assertEquals("Empty sting for non existing field", StringUtils.EMPTY, gen.getFieldName("getField5", fieldNames));

        assertEquals("Empty sting for null function name", StringUtils.EMPTY, gen.getFieldName(null, fieldNames));

        assertEquals("Empty sting for null function name and null fields", StringUtils.EMPTY,
                gen.getFieldName(null, null));

    }

}
