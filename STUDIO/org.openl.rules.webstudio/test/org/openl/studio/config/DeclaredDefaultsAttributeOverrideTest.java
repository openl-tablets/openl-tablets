package org.openl.studio.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import jakarta.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import org.junit.jupiter.api.Test;

class DeclaredDefaultsAttributeOverrideTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectSchemaGeneratorConfiguration configuration = new ObjectSchemaGeneratorConfiguration();
    private final SchemaGenerator generator = configuration.inputSchemaGenerator(objectMapper);

    /** Stands in for a generated datatype bean. Some fields declare defaults, one does not. */
    @XmlType
    public static class QualityIndicators {
        private Date reportDate;
        private boolean lossesInThisYear = false;
        private boolean adequateLiquidityRatio = true;
        private Bank bank = new Bank();

        public Date getReportDate() {
            return reportDate;
        }

        public void setReportDate(Date reportDate) {
            this.reportDate = reportDate;
        }

        public boolean isLossesInThisYear() {
            return lossesInThisYear;
        }

        public void setLossesInThisYear(boolean lossesInThisYear) {
            this.lossesInThisYear = lossesInThisYear;
        }

        public boolean isAdequateLiquidityRatio() {
            return adequateLiquidityRatio;
        }

        public void setAdequateLiquidityRatio(boolean adequateLiquidityRatio) {
            this.adequateLiquidityRatio = adequateLiquidityRatio;
        }

        public Bank getBank() {
            return bank;
        }

        public void setBank(Bank bank) {
            this.bank = bank;
        }
    }

    @XmlType
    public static class Bank {
        private String countryCode = "DE";

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }
    }

    /** A datatype bean without a way to create it keeps its schema without defaults. */
    @XmlType
    public static class Uncreatable {
        private final String code;

        public Uncreatable(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    @Test
    void writesTheDeclaredDefaultsOfEveryNestedDatatype() {
        var schema = generator.generateSchema(QualityIndicators.class);

        var properties = schema.get("properties");
        assertFalse(properties.get("reportDate").has("default"));
        assertFalse(properties.get("lossesInThisYear").get("default").asBoolean());
        assertTrue(properties.get("adequateLiquidityRatio").get("default").asBoolean());
        assertEquals("DE", properties.get("bank").get("properties").get("countryCode").get("default").asText());
        assertEquals("DE", properties.get("bank").get("default").get("countryCode").asText());
    }

    /** A class that is not a generated datatype bean is never created, whatever it declares. */
    public static class PlainJavaType {
        private String code = "X";

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

    @Test
    void leavesAnyOtherClassAlone() {
        var schema = generator.generateSchema(PlainJavaType.class);

        assertFalse(schema.get("properties").get("code").has("default"));
    }

    /** The generator of the published values keeps its schemas as they were. Defaults are for the input only. */
    @Test
    void theOutputGeneratorDescribesNoDefaults() {
        var schema = configuration.schemaGenerator(objectMapper).generateSchema(QualityIndicators.class);

        assertFalse(schema.get("properties").get("lossesInThisYear").has("default"));
    }

    @Test
    void leavesAClassItCannotCreateWithoutDefaults() {
        var schema = generator.generateSchema(Uncreatable.class);

        assertFalse(schema.get("properties").get("code").has("default"));
    }
}
