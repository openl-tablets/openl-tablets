package org.openl.rules.calc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.calc.SpreadsheetResultBeanByteCodeGenerator.FieldDescription;

class SpreadsheetResultBeanByteCodeGeneratorTest {

    @Test
    void storesGeneratedSuffixForDuplicateFieldName() throws Exception {
        var fields = List.of(
                new FieldDescription(Integer.class.getCanonicalName(), "Code", null, null),
                new FieldDescription(Integer.class.getCanonicalName(), "code", null, null));
        var byteCode = SpreadsheetResultBeanByteCodeGenerator.byteCode("org.openl.generated.DuplicateFields", fields);

        var generatedClass = new GeneratedClassLoader().define(byteCode);

        assertEquals("", generatedClass.getMethod("getCode").getAnnotation(SpreadsheetCell.class).suffix());
        assertEquals("1", generatedClass.getMethod("getCode1").getAnnotation(SpreadsheetCell.class).suffix());
    }

    private static final class GeneratedClassLoader extends ClassLoader {

        private GeneratedClassLoader() {
            super(SpreadsheetResultBeanByteCodeGeneratorTest.class.getClassLoader());
        }

        private Class<?> define(byte[] byteCode) {
            return defineClass(null, byteCode, 0, byteCode.length);
        }
    }
}
