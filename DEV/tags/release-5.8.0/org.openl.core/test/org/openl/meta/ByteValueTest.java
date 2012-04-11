package org.openl.meta;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.exception.OpenlNotCheckedException;

public class ByteValueTest {
    
    @Test
    public void testAdd() {
        ByteValue bval1 = new ByteValue((byte) 1);
        ByteValue bval2 = new ByteValue((byte) 4);
        assertEquals(new ByteValue((byte) 5), ByteValue.add(bval1, bval2));
        
        ByteValue nullValue = null;
        try {
            assertEquals(bval2, ByteValue.add(nullValue, bval2));
            fail();
        } catch (OpenlNotCheckedException e) {
            assertTrue(true);
        }
        try {
            assertEquals(bval1, ByteValue.add(bval1, nullValue));
        } catch (OpenlNotCheckedException e) {
            assertTrue(true);
        }
        
        
        ByteValue zeroValue = new ByteValue((byte) 0);
        assertEquals(bval2, ByteValue.add(zeroValue, bval2));
        assertEquals(bval1, ByteValue.add(bval1, zeroValue));
    }

}
