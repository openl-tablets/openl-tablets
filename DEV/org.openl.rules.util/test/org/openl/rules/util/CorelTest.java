package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CorelTest {

    @Test
    public void testCorrelationCoefficient() {
        assertNull(Corel.correl((Number[]) null, null));
        assertNull(Corel.correl(new Double[2], new Double[2]));
        assertNull(Corel.correl(new Double[]{1.0, null}, new Double[]{null, null}));
        assertNull(Corel.correl(new Double[]{1.0, 2.0}, new Double[]{null, null}));

        assertNull(Corel.correl(new Double[]{9.5}, new Double[]{5.0}));
        Assertions.assertEquals(0.4193139346887673, Corel.correl(new Double[]{3.0, 4.0, 8.0}, new Double[]{3.0, 10.0, 8.0}));
        Assertions.assertEquals(0.9999999999999999, Corel.correl(new Double[]{3.0, null, 8.0}, new Double[]{3.0, 10.0, 8.0}));
        Assertions.assertEquals(0.11597947774365458, Corel.correl(new Double[]{-3.0, 4.0, 8.0}, new Double[]{3.0, -10.0, 8.0}));

        Assertions.assertEquals(0.9999999999999998, Corel.correl(new Long[]{3L, 4L}, new Long[]{3L, 4L}));
        Assertions.assertEquals(0.4193139346887673, Corel.correl(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Byte[]{(byte) 3, (byte) 10, (byte) 8}));
        Assertions.assertEquals(0.4193139346887673, Corel.correl(new Short[]{(short) 3, (short) 4, (short) 8}, new Short[]{(short) 3, (short) 10, (short) 8}));
        Assertions.assertEquals(0.4193139346887673, Corel.correl(new Integer[]{3, 4, 8}, new Integer[]{3, 10, 8}));
        Assertions.assertEquals(0.4193139346887673, Corel.correl(new Long[]{3L, 4L, 8L}, new Long[]{3L, 10L, 8L}));
        Assertions.assertEquals(0.4193139346887673, Corel.correl(new Float[]{3f, 4f, 8f}, new Float[]{3f, 10f, 8f}));
        Assertions.assertEquals(0.4193139346887673, Corel.correl(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Short[]{(short) 3, (short) 10, (short) 8}));
        Assertions.assertEquals(new BigDecimal("0.4193139346887673183088446531970227"), Corel.correl(new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)}, new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(10), BigInteger.valueOf(8)}));
        Assertions.assertEquals(new BigDecimal("0.4193139346887673183088446531970227"), Corel.correl(new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4), BigDecimal.valueOf(8)}, new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(10), BigDecimal.valueOf(8)}));
    }

    @Test
    public void testRSQ() {
        assertNull(Corel.rsq((Number[]) null, null));
        assertNull(Corel.rsq(new Double[2], new Double[2]));
        assertNull(Corel.rsq(new Double[]{1.0, null}, new Double[]{null, null}));
        assertNull(Corel.rsq(new Double[]{1.0, 2.0}, new Double[]{null, null}));

        assertNull(Corel.rsq(new Double[]{9.5}, new Double[]{5.0}));
        Assertions.assertEquals(0.17582417582417584, Corel.rsq(new Double[]{3.0, 4.0, 8.0}, new Double[]{3.0, 10.0, 8.0}));
        Assertions.assertEquals(0.9999999999999998, Corel.rsq(new Double[]{3.0, null, 8.0}, new Double[]{3.0, 10.0, 8.0}));
        Assertions.assertEquals(0.013451239257690869, Corel.rsq(new Double[]{-3.0, 4.0, 8.0}, new Double[]{3.0, -10.0, 8.0}));

        Assertions.assertEquals(0.17582417582417584, Corel.rsq(new Long[]{3L, 4L, 8L}, new Long[]{3L, 10L, 8L}));
        Assertions.assertEquals(0.17582417582417584, Corel.rsq(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Byte[]{(byte) 3, (byte) 10, (byte) 8}));
        Assertions.assertEquals(0.17582417582417584, Corel.rsq(new Short[]{(short) 3, (short) 4, (short) 8}, new Short[]{(short) 3, (short) 10, (short) 8}));
        Assertions.assertEquals(0.17582417582417584, Corel.rsq(new Integer[]{3, 4, 8}, new Integer[]{3, 10, 8}));
        Assertions.assertEquals(0.17582417582417584, Corel.rsq(new Long[]{3L, 4L, 8L}, new Long[]{3L, 10L, 8L}));
        Assertions.assertEquals(0.17582417582417584, Corel.rsq(new Float[]{3f, 4f, 8f}, new Float[]{3f, 10f, 8f}));
        Assertions.assertEquals(0.17582417582417584, Corel.rsq(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Short[]{(short) 3, (short) 10, (short) 8}));
        Assertions.assertEquals(new BigDecimal("0.1758241758241758241758241758241759"), Corel.rsq(new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)}, new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(10), BigInteger.valueOf(8)}));
        Assertions.assertEquals(new BigDecimal("0.1758241758241758241758241758241759"), Corel.rsq(new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4), BigDecimal.valueOf(8)}, new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(10), BigDecimal.valueOf(8)}));
    }
}
