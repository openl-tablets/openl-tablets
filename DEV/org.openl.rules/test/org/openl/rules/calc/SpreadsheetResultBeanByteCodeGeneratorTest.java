package org.openl.rules.calc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.openl.gen.ByteCodeAnnotations.list;
import static org.openl.gen.ByteCodeAnnotations.map;
import static org.openl.gen.ByteCodeAnnotations.of;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.gen.ByteCodeAnnotations;
import org.openl.rules.calc.SpreadsheetResultBeanByteCodeGenerator.FieldDescription;

class SpreadsheetResultBeanByteCodeGeneratorTest {

    @Test
    void storesGeneratedSuffixForDuplicateFieldName() throws Exception {
        var fields = List.of(
                new FieldDescription(Integer.class.getCanonicalName(), "Code", null, null, null),
                new FieldDescription(Integer.class.getCanonicalName(), "code", null, null, null));
        var byteCode = SpreadsheetResultBeanByteCodeGenerator.byteCode("org.openl.generated.DuplicateFields", fields);

        var generatedClass = new GeneratedClassLoader().define(byteCode);

        assertEquals("", generatedClass.getMethod("getCode").getAnnotation(SpreadsheetCell.class).suffix());
        assertEquals("1", generatedClass.getMethod("getCode1").getAnnotation(SpreadsheetCell.class).suffix());
    }

    /**
     * A cell of a vocabulary type publishes the values the type allows, on the elements when the cell holds an
     * array. The annotations are read from the byte code: the swagger package is not on the class path here.
     */
    @Test
    void publishesTheValuesOfAVocabularyCell() {
        var values = new String[]{"bla1", "bla2", "bla3"};
        var fields = List.of(
                new FieldDescription(String.class.getCanonicalName(), "Kind", null, "The kind", values),
                new FieldDescription(String[].class.getCanonicalName(), "Kinds", null, null, values));
        var byteCode = SpreadsheetResultBeanByteCodeGenerator.byteCode("org.openl.generated.VocabularyCells", fields);

        var members = ByteCodeAnnotations.read(byteCode);
        var kind = of(members, "getKind", "Lio/swagger/v3/oas/annotations/media/Schema;");
        assertEquals("The kind", kind.get("description"));
        assertEquals(List.of(values), list(kind.get("allowableValues")));
        var kinds = of(members, "getKinds", "Lio/swagger/v3/oas/annotations/media/ArraySchema;");
        assertEquals(List.of(values), list(map(kinds.get("schema")).get("allowableValues")));
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
