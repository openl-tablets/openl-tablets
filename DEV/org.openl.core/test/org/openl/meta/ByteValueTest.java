package org.openl.meta;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ByteValueTest {

    @Test
    public void testAdd() {
        ByteValue bval1 = new ByteValue((byte) 1);
        ByteValue bval2 = new ByteValue((byte) 4);
        assertEquals(new ByteValue((byte) 5), ByteValue.add(bval1, bval2));

        ByteValue nullValue = null;
        assertEquals(4, ByteValue.add(nullValue, bval2).intValue());

        assertEquals(1, ByteValue.add(bval1, nullValue).intValue());

        ByteValue zeroValue = new ByteValue((byte) 0);
        assertEquals(bval2, ByteValue.add(zeroValue, bval2));
        assertEquals(bval1, ByteValue.add(bval1, zeroValue));

        assertEquals("0", ByteValue.add((ByteValue) null, new ByteValue((byte) 0)).toString());
        assertEquals("0", ByteValue.add(new ByteValue((byte) 0), (ByteValue) null).toString());
    }

}
