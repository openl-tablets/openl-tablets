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

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * TODO description
 *
 * @author Vladyslav Pikus
 * @since
 */
class YamlMapperFactoryTest {

    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();

    @BeforeAll
    static void setUp() {
        Locale.setDefault(Locale.US);
        // set +2 as default
        TimeZone defaultTimeZone = TimeZone.getTimeZone("Europe/Helsinki");
        TimeZone.setDefault(defaultTimeZone);
    }

    @AfterAll
    static void tearDown() {
        Locale.setDefault(DEFAULT_LOCALE);
        TimeZone.setDefault(DEFAULT_TIMEZONE);
    }

    @Test
    void testConfiguration() throws IOException {
        var mapper = YamlMapperFactory.getYamlMapper();
        MyBean myBean;
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
        assertEquals("foo", myBean.getField3().getFirst());
        assertEquals("bar", myBean.getField3().get(1));
        assertEquals(createDate(2023, 2, 14), myBean.getField2());
        assertTrue(myBean.getField4());
        assertNull(myBean.getTransientField5());
    }

    private static Date createDate(int year, int month, int dayOfMonth) {
        var cal = new GregorianCalendar();
        cal.set(year, month - 1, dayOfMonth, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static class MyBean {

        @Getter
        @Setter
        private String field1;
        @Getter
        @Setter
        private Date field2;
        @Getter
        @Setter
        private List<String> field3;
        @Getter
        @Setter
        private Boolean field4;
        @Getter
        @Setter
        private transient String transientField5;
    }
}
