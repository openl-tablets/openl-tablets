package org.openl.rules.dataformat.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * TODO description
 *
 * @author Vladyslav Pikus
 * @since
 */
public class YamlMapperFactoryTest {

    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();

    @BeforeAll
    public static void setUp() {
        Locale.setDefault(Locale.US);
        // set +2 as default
        TimeZone defaultTimeZone = TimeZone.getTimeZone("Europe/Helsinki");
        TimeZone.setDefault(defaultTimeZone);
    }

    @AfterAll
    public static void tearDown() {
        Locale.setDefault(DEFAULT_LOCALE);
        TimeZone.setDefault(DEFAULT_TIMEZONE);
    }

    @Test
    public void testConfiguration() throws IOException {
        var mapper = YamlMapperFactory.getYamlMapper();
        MyBean myBean = null;
        try (var stream = getClass().getResourceAsStream("/myBean.yaml")) {
            myBean = mapper.readValue(stream, MyBean.class);
            assertTheSame(myBean);
        }
        assertNotNull(myBean);
        myBean.setTransientField5("!!! Must not be serialized !!!");
        assertTheSame(mapper.readValue(mapper.writeValueAsBytes(myBean), MyBean.class));
    }

    private static void assertTheSame(MyBean myBean) {
        assertEquals("foo bar", myBean.getField1());
        assertEquals(2, myBean.getField3().size());
        assertEquals("foo", myBean.getField3().get(0));
        assertEquals("bar", myBean.getField3().get(1));
        assertEquals(createDate(2023, 2, 14), myBean.getField2());
        assertTrue(myBean.getField4());
        assertNull(myBean.getTransientField5());
    }

    private static Date createDate(int year, int month, int dayOfMonth) {
        Calendar cal = new GregorianCalendar();
        cal.set(year, month - 1, dayOfMonth, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static class MyBean {

        private String field1;
        private Date field2;
        private List<String> field3;
        private Boolean field4;
        private transient String transientField5;

        public String getField1() {
            return field1;
        }

        public void setField1(String field1) {
            this.field1 = field1;
        }

        public Date getField2() {
            return field2;
        }

        public void setField2(Date field2) {
            this.field2 = field2;
        }

        public List<String> getField3() {
            return field3;
        }

        public void setField3(List<String> field3) {
            this.field3 = field3;
        }

        public Boolean getField4() {
            return field4;
        }

        public void setField4(Boolean field4) {
            this.field4 = field4;
        }

        public String getTransientField5() {
            return transientField5;
        }

        public void setTransientField5(String transientField5) {
            this.transientField5 = transientField5;
        }
    }
}
