package org.openl.rules.ruleservice.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.ruleservice.core.ExceptionType;
import org.openl.rules.ruleservice.publish.jaxrs.JAXRSErrorResponse;

class JsonProcessingExceptionMapperTest {

    private JsonProcessingExceptionMapper mapper;
    private ObjectMapper jackson;

    @BeforeEach
    void setUp() {
        mapper = new JsonProcessingExceptionMapper();
        jackson = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Test
    void malformedJson_returnsGenericMessage() {
        assertResponse(capture("{ broken"), "Request body is malformed");
    }

    @Test
    void unrecognizedProperty_topLevel_includesFieldName() {
        assertResponse(capture("{\"unexpected\":1}"), "Unknown field 'unexpected'");
    }

    @Test
    void unrecognizedProperty_nested_includesFullPath() {
        assertResponse(capture("{\"nested\":{\"unknownInner\":1}}"), "Unknown field 'nested.unknownInner'");
    }

    @Test
    void unrecognizedProperty_withoutPath_returnsFallbackMessage() {
        var ex = new com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException(
                null, "x", null, Bean.class, null, null);
        assertResponse(ex, "Unknown field in request");
    }

    @Test
    void invalidFormat_dateField_returnsFriendlyDateMessage() {
        assertResponse(capture("{\"date\":\"not-a-date\"}"), "Invalid date format for field 'date'");
    }

    @Test
    void invalidFormat_integerField_returnsNumberMessage() {
        assertResponse(capture("{\"count\":\"abc\"}"), "Invalid number format for field 'count'");
    }

    @Test
    void invalidFormat_doubleField_returnsNumberMessage() {
        assertResponse(capture("{\"price\":\"abc\"}"), "Invalid number format for field 'price'");
    }

    @Test
    void invalidFormat_booleanField_returnsBooleanMessage() {
        assertResponse(capture("{\"flag\":\"maybe\"}"), "Invalid boolean format for field 'flag'");
    }

    @Test
    void invalidFormat_enumField_returnsEnumMessage() {
        assertResponse(capture("{\"color\":\"VIOLET\"}"), "Invalid enum format for field 'color'");
    }

    @Test
    void invalidFormat_arrayElement_includesArrayIndexInPath() {
        assertResponse(capture("{\"numbers\":[1,\"abc\",3]}"),
                "Invalid number format for field 'numbers[1]'");
    }

    @Test
    void invalidFormat_localDateTarget_returnsDateMessage() {
        var ex = new InvalidFormatException((JsonParser) null, "msg", "x", LocalDate.class);
        assertResponse(ex, "Invalid date format");
    }

    @Test
    void invalidFormat_primitiveInt_returnsNumberMessage() {
        var ex = new InvalidFormatException((JsonParser) null, "msg", "x", int.class);
        assertResponse(ex, "Invalid number format");
    }

    @Test
    void invalidFormat_primitiveBoolean_returnsBooleanMessage() {
        var ex = new InvalidFormatException((JsonParser) null, "msg", "x", boolean.class);
        assertResponse(ex, "Invalid boolean format");
    }

    @Test
    void invalidFormat_stringTarget_returnsStringMessage() {
        var ex = new InvalidFormatException((JsonParser) null, "msg", "x", String.class);
        assertResponse(ex, "Invalid string format");
    }

    @Test
    void invalidFormat_characterClass_returnsStringMessage() {
        var ex = new InvalidFormatException((JsonParser) null, "msg", "x", Character.class);
        assertResponse(ex, "Invalid string format");
    }

    @Test
    void invalidFormat_charPrimitive_returnsStringMessage() {
        var ex = new InvalidFormatException((JsonParser) null, "msg", "x", char.class);
        assertResponse(ex, "Invalid string format");
    }

    @Test
    void invalidFormat_voidPrimitive_returnsValueMessage() {
        var ex = new InvalidFormatException((JsonParser) null, "msg", "x", void.class);
        assertResponse(ex, "Invalid value format");
    }

    @Test
    void invalidFormat_nullTargetType_returnsValueMessage() {
        var ex = new InvalidFormatException((JsonParser) null, "msg", "x", null);
        assertResponse(ex, "Invalid value format");
    }

    @Test
    void invalidFormat_unknownClass_returnsValueMessage() {
        var ex = new InvalidFormatException((JsonParser) null, "msg", "x", Bean.class);
        assertResponse(ex, "Invalid value format");
    }

    @Test
    void mismatchedInput_topLevel_returnsGenericRequestBodyMessage() {
        assertResponse(capture("[1,2,3]"), "Invalid request body");
    }

    @Test
    void mismatchedInput_nestedField_includesFieldPath() {
        assertResponse(capture("{\"nested\":42}"), "Invalid value for field 'nested'");
    }

    @Test
    void mismatchedInput_arrayForScalarField_includesFieldPath() {
        assertResponse(capture("{\"count\":[1,2]}"), "Invalid value for field 'count'");
    }

    @Test
    void jsonMappingException_withoutPath_returnsGenericMessage() {
        var ex = JsonMappingException.from((JsonParser) null, "raw");
        assertResponse(ex, "Invalid request body");
    }

    @Test
    void response_hasBadRequestStatusAndJsonContentType() {
        var response = mapper.toResponse(capture("{ broken"));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        var entity = assertInstanceOf(JAXRSErrorResponse.class, response.getEntity());
        assertEquals(ExceptionType.BAD_REQUEST, entity.type());
    }

    private void assertResponse(JsonProcessingException ex, String expectedMessage) {
        var response = mapper.toResponse(ex);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        var entity = assertInstanceOf(JAXRSErrorResponse.class, response.getEntity());
        assertEquals(expectedMessage, entity.message());
        assertEquals(ExceptionType.BAD_REQUEST, entity.type());
    }

    private JsonProcessingException capture(String json) {
        try {
            jackson.readValue(json, Bean.class);
        } catch (JsonProcessingException e) {
            return e;
        } catch (Exception e) {
            throw new AssertionError("Expected JsonProcessingException, got " + e.getClass(), e);
        }
        throw new AssertionError("Expected JsonProcessingException for input: " + json);
    }

    public static class Bean {
        public String name;
        public Integer count;
        public Double price;
        public Boolean flag;
        public Date date;
        public Color color;
        public Nested nested;
        public List<Integer> numbers;
    }

    public static class Nested {
        public String label;
        public Integer value;
    }

    public enum Color {
        RED, GREEN, BLUE
    }
}
