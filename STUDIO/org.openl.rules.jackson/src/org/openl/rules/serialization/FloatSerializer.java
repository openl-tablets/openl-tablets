package org.openl.rules.serialization;

import java.io.IOException;
import java.math.BigDecimal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.NumberSerializers;

/**
 * Float type serializer for preventing output of the float point numbers in the scientific notation.
 *
 * @author Yury Molchan
 */
class FloatSerializer extends NumberSerializers.FloatSerializer {
    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        Float number = (Float) value;
        if (Float.isFinite(number)) {
            // Serialize Float without scientific notation
            gen.writeNumber(new BigDecimal(value.toString()).toPlainString());
        } else {
            gen.writeNumber(number);
        }
    }
}
