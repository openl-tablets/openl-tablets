package org.openl.rules.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import org.openl.types.IOpenField;

class PrecisionFieldChainTest {

    @Test
    void deltaIsThePowerOfTen() {
        // Math.pow(10.0, -5) is 9.999999999999999E-6
        assertEquals(new BigDecimal("0.00001"), chain(5).getDelta());
        assertEquals(new BigDecimal("1E-11"), chain(11).getDelta());
        assertEquals(new BigDecimal("0.01"), chain(2).getDelta());
        assertEquals(BigDecimal.ONE, chain(0).getDelta());
        assertEquals(new BigDecimal("1E+2"), chain(-2).getDelta());
        assertTrue(chain(5).hasDelta());
    }

    @Test
    void deltaBeyondDouble() {
        assertEquals(new BigDecimal("1E+309"), chain(-309).getDelta());
        assertEquals(new BigDecimal("1E-324"), chain(324).getDelta());
    }

    @Test
    void noPrecisionGivesNoDelta() {
        assertNull(chain(null).getDelta());
        assertFalse(chain(null).hasDelta());
    }

    private static PrecisionFieldChain chain(Integer precision) {
        return new PrecisionFieldChain(null, new IOpenField[0], precision);
    }
}
