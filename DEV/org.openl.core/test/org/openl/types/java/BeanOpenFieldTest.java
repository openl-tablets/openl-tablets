package org.openl.types.java;

import org.junit.Test;
import org.openl.types.IOpenField;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Created by dl on 12/1/14.
 */
public class BeanOpenFieldTest {

    @Test
    public void testBeanJavaSpecification() {
        Map<String, IOpenField> fieldsMap = new HashMap<String, IOpenField>();
        BeanOpenField.collectFields(fieldsMap, BeanJavaSpecification.class, null, null);

        assertEquals(2, fieldsMap.size());
        assertTrue(fieldsMap.containsKey("fieldName"));
        assertTrue(fieldsMap.containsKey("flag"));
    }

    @Test
    public void testBeanNONJavaSpecification() {
        Map<String, IOpenField> fieldsMap = new HashMap<String, IOpenField>();
        BeanOpenField.collectFields(fieldsMap, BeanNONJavaSpecification.class, null, null);

        assertEquals(2, fieldsMap.size());
        assertTrue(fieldsMap.containsKey("FieldName"));
        assertTrue(fieldsMap.containsKey("Flag"));
    }

    class BeanJavaSpecification {
        private String fieldName;
        private boolean flag;

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public boolean isFlag() {
            return flag;
        }

        public void setFlag(boolean flag) {
            this.flag = flag;
        }
    }

    class BeanNONJavaSpecification {
        // The name starts from the upper case
        // it is against java bean specification
        // See http://www.oracle.com/technetwork/java/javase/documentation/spec-136004.html
        // section 'Capitalization of inferred names'
        // OpenL allows properties started from the upper case
        //
        private String FieldName;
        private boolean Flag;

        public String getFieldName() {
            return FieldName;
        }

        public void setFieldName(String fieldName) {
            FieldName = fieldName;
        }

        public boolean isFlag() {
            return Flag;
        }

        public void setFlag(boolean flag) {
            Flag = flag;
        }
    }
}
