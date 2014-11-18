package org.openl.rules.ruleservice.ws.databinding;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;
import org.openl.meta.StringValue;
import org.openl.rules.ruleservice.databinding.JacksonObjectMapperFactoryBean;
import org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.variation.ComplexVariationType;
import org.openl.rules.variation.ArgumentReplacementVariation;
import org.openl.rules.variation.ComplexVariation;
import org.openl.rules.variation.Variation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonObjectMapperFactoryBeanTest {

    private static class TestClass {
        ByteValue byteValue;
        ShortValue shortValue;
        IntValue intValue;
        LongValue longValue;
        FloatValue floatValue;
        DoubleValue doubleValue;
        StringValue stringValue;
        BigDecimalValue bigDecimalValue;
        BigIntegerValue bigIntegerValue;

        public TestClass() {
        }

        public ByteValue getByteValue() {
            return byteValue;
        }

        public void setByteValue(ByteValue byteValue) {
            this.byteValue = byteValue;
        }

        public ShortValue getShortValue() {
            return shortValue;
        }

        public void setShortValue(ShortValue shortValue) {
            this.shortValue = shortValue;
        }

        public IntValue getIntValue() {
            return intValue;
        }

        public void setIntValue(IntValue intValue) {
            this.intValue = intValue;
        }

        public LongValue getLongValue() {
            return longValue;
        }

        public void setLongValue(LongValue longValue) {
            this.longValue = longValue;
        }

        public FloatValue getFloatValue() {
            return floatValue;
        }

        public void setFloatValue(FloatValue floatValue) {
            this.floatValue = floatValue;
        }

        public DoubleValue getDoubleValue() {
            return doubleValue;
        }

        public void setDoubleValue(DoubleValue doubleValue) {
            this.doubleValue = doubleValue;
        }

        public StringValue getStringValue() {
            return stringValue;
        }

        public void setStringValue(StringValue stringValue) {
            this.stringValue = stringValue;
        }

        public BigDecimalValue getBigDecimalValue() {
            return bigDecimalValue;
        }

        public void setBigDecimalValue(BigDecimalValue bigDecimalValue) {
            this.bigDecimalValue = bigDecimalValue;
        }

        public BigIntegerValue getBigIntegerValue() {
            return bigIntegerValue;
        }

        public void setBigIntegerValue(BigIntegerValue bigIntegerValue) {
            this.bigIntegerValue = bigIntegerValue;
        }
    }

    @Test
    public void testOpenLTypes() throws JsonProcessingException, IOException {
        JacksonObjectMapperFactoryBean bean = new JacksonObjectMapperFactoryBean();
        bean.setSmartDefaultTyping(false);
        ObjectMapper objectMapper = bean.createJacksonDatabinding();
        TestClass testBean = new TestClass();
        ByteValue byteValue = new ByteValue((byte) 255);
        ShortValue shortValue = new ShortValue((short) 12345);
        IntValue intValue = new IntValue(12345);
        LongValue longValue = new LongValue(12345);
        FloatValue floatValue = new FloatValue(123.2f);
        DoubleValue doubleValue = new DoubleValue(123.2d);
        StringValue stringValue = new StringValue("someString");
        BigDecimalValue bigDecimalValue = new BigDecimalValue(new BigDecimal("1231241235235123612361235235325325.12345234"));
        BigIntegerValue bigIntegerValue = new BigIntegerValue(new BigInteger("1231241235235123612361235235325325"));

        testBean.setByteValue(byteValue);
        testBean.setShortValue(shortValue);
        testBean.setIntValue(intValue);
        testBean.setLongValue(longValue);
        testBean.setFloatValue(floatValue);
        testBean.setDoubleValue(doubleValue);
        testBean.setStringValue(stringValue);
        testBean.setBigIntegerValue(bigIntegerValue);
        testBean.setBigDecimalValue(bigDecimalValue);
        
        String text = objectMapper.writeValueAsString(testBean);
        
        testBean = objectMapper.readValue(text, TestClass.class);
        
        Assert.assertEquals(testBean.getByteValue(), byteValue);
        Assert.assertEquals(testBean.getShortValue(), shortValue);
        Assert.assertEquals(testBean.getIntValue(), intValue);
        Assert.assertEquals(testBean.getLongValue(), longValue);
        Assert.assertEquals(testBean.getFloatValue(), floatValue);
        Assert.assertEquals(testBean.getDoubleValue(), doubleValue);
        Assert.assertEquals(testBean.getStringValue(), stringValue);
        Assert.assertEquals(testBean.getBigDecimalValue(), bigDecimalValue);
        Assert.assertEquals(testBean.getBigIntegerValue(), bigIntegerValue);
        
    }
    
    @Test
    public void testVariations() throws JsonProcessingException, IOException {
        JacksonObjectMapperFactoryBean bean = new JacksonObjectMapperFactoryBean();
        bean.setSmartDefaultTyping(false);
        bean.setSupportVariations(true);
        ObjectMapper objectMapper = bean.createJacksonDatabinding();
        
        ArgumentReplacementVariation v = new ArgumentReplacementVariation("variationID", 1, new DoubleValue(123d));
        
        ComplexVariation complexVariation = new ComplexVariation("complexVariationId", v);
        
        String text = objectMapper.writeValueAsString(complexVariation);
        
        Variation v1 = objectMapper.readValue(text, Variation.class);
        
        System.out.println(complexVariation);
        
    }
}
