package org.openl.rules.serialization.spr;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultBeanPropertyNamingStrategy;

/**
 * Default SpreadsheetResult serializer.
 *
 * @author Yury Molchan
 */
public class SpreadsheetSerializer extends StdSerializer<SpreadsheetResult> {

    private SpreadsheetResultBeanPropertyNamingStrategy namingStrategy;

    public SpreadsheetSerializer(SpreadsheetResultBeanPropertyNamingStrategy namingStrategy) {
        super(SpreadsheetResult.class);
        this.namingStrategy = namingStrategy;
    }

    @Override
    public void serialize(SpreadsheetResult spr, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        if (spr == null || spr.getColumnNames() == null || spr.getRowNames() == null) {
            return;
        }
        jgen.writeStartObject();
        if (spr.getWidth() == 1) {
            for (int r = 0; r < spr.getHeight(); r++) {
                String rowName = spr.getRowName(r);
                writeNonNull(jgen, provider, namingStrategy.transform(rowName), spr.getValue(r, 0));
            }
        } else if (spr.getHeight() == 1) {
            for (int c = 0; c < spr.getWidth(); c++) {
                String columnName = spr.getColumnName(c);
                writeNonNull(jgen, provider, namingStrategy.transform(columnName), spr.getValue(0, c));
            }
        } else {
            for (int c = 0; c < spr.getWidth(); c++) {
                for (int r = 0; r < spr.getHeight(); r++) {
                    String columnName = spr.getColumnName(c);
                    String rowName = spr.getRowName(r);
                    Object value = spr.getValue(r, c);
                    writeNonNull(jgen, provider, namingStrategy.transform(columnName, rowName), value);
                }
            }
        }
        jgen.writeEndObject();
    }

    private static void writeNonNull(JsonGenerator jgen, SerializerProvider provider, String fieldName, Object value) throws IOException {
        if (value != null) {
            provider.defaultSerializeField(fieldName, value, jgen);
        }
    }
}
