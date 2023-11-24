package org.openl.rules.serialization;

import java.io.IOException;
import java.math.BigDecimal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.NumberSerializers;

/**
 * Double type serializer for preventing output of the float point numbers in the scientific notation.
 *
 * @author Yury Molchan
 */
class DoubleSerializer extends NumberSerializers.DoubleSerializer {
    public DoubleSerializer(Class<?> cls) {
        super(cls);
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        Double number = (Double) value;
        if (Double.isFinite(number)) {
            // Serialize Float without scientific notation
            gen.writeNumber(new BigDecimal(value.toString()).toPlainString());
        } else {
            gen.writeNumber(number);
        }
    }

    @Override
    public void serializeWithType(Object value,
                                  JsonGenerator gen,
                                  SerializerProvider serializers,
                                  TypeSerializer typeSer) throws IOException {
        Double number = (Double) value;
        if (Double.isFinite(number)) {
            serialize(value, gen, serializers);
        } else {
            WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen,
                    typeSer.typeId(value, JsonToken.VALUE_NUMBER_FLOAT));
            serialize(value, gen, serializers);
            typeSer.writeTypeSuffix(gen, typeIdDef);
        }
    }
}
