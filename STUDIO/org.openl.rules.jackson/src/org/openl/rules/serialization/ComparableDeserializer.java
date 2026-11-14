package org.openl.rules.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

/**
 * Reads a value declared as {@link Comparable}, such as a rule parameter {@code Comparable b}.
 *
 * <p>A text is read as a {@link String}, a number as the number type Jackson reads it as, and {@code true} or
 * {@code false} as a {@link Boolean}. Each of them is comparable.
 *
 * <p>An object or an array cannot be read as a comparable value and is refused.
 *
 * @author Yury Molchan
 */
@SuppressWarnings("rawtypes")
class ComparableDeserializer extends StdScalarDeserializer<Comparable> {

    ComparableDeserializer() {
        super(Comparable.class);
    }

    @Override
    public Comparable deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return switch (p.currentToken()) {
            case VALUE_STRING -> p.getText();
            case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> (Comparable) p.getNumberValue();
            case VALUE_TRUE -> Boolean.TRUE;
            case VALUE_FALSE -> Boolean.FALSE;
            case null, default -> (Comparable) ctxt.handleUnexpectedToken(Comparable.class, p);
        };
    }

    /** A plain value carries no type information even when the default typing is on. */
    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer)
            throws IOException {
        return p.currentToken().isScalarValue()
                ? deserialize(p, ctxt)
                : typeDeserializer.deserializeTypedFromAny(p, ctxt);
    }
}
