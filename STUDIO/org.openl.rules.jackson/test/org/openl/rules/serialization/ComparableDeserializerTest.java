package org.openl.rules.serialization;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * @author Yury Molchan
 */
class ComparableDeserializerTest {

    private static ObjectMapper mapper(DefaultTypingMode typing) throws ClassNotFoundException {
        var bean = new JacksonObjectMapperFactoryBean();
        bean.setDefaultTypingMode(typing);
        return bean.createJacksonObjectMapper();
    }

    @ParameterizedTest
    @EnumSource(DefaultTypingMode.class)
    void readsPlainValuesAsComparable(DefaultTypingMode typing) throws Exception {
        var mapper = mapper(typing);

        assertEquals("b", mapper.readValue("\"b\"", Comparable.class));
        assertEquals(1, mapper.readValue("1", Comparable.class));
        assertEquals(1.5, mapper.readValue("1.5", Comparable.class));
        assertEquals(true, mapper.readValue("true", Comparable.class));
    }

    /** The modes that type arrays expect an array as a pair of its type and its elements, so they are left out. */
    @ParameterizedTest
    @EnumSource(value = DefaultTypingMode.class, names = {"JAVA_LANG_OBJECT", "OBJECT_AND_NON_CONCRETE", "DISABLED"})
    void readsAnArrayOfPlainValues(DefaultTypingMode typing) throws Exception {
        assertArrayEquals(new Comparable<?>[]{"a", 2, false},
                mapper(typing).readValue("[\"a\", 2, false]", Comparable[].class));
    }

    @ParameterizedTest
    @EnumSource(value = DefaultTypingMode.class, names = {"JAVA_LANG_OBJECT", "DISABLED"})
    void refusesAnObjectOrAnArray(DefaultTypingMode typing) throws Exception {
        var mapper = mapper(typing);

        assertThrows(MismatchedInputException.class, () -> mapper.readValue("{\"a\": 1}", Comparable.class));
        assertThrows(MismatchedInputException.class, () -> mapper.readValue("[\"a\"]", Comparable.class));
    }
}
