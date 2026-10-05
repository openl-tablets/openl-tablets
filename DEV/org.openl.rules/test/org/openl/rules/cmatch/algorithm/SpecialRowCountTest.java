package org.openl.rules.cmatch.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SpecialRowCountTest {

    @ParameterizedTest
    @CsvSource({"MATCH, 1", "SCORE, 1", "WEIGHTED, 3"})
    void countsTheRowsAnAlgorithmReadsBeforeItsConditions(String algorithm, int rows) {
        assertEquals(rows, MatchAlgorithmFactory.getAlgorithm(algorithm).getSpecialRowCount());
    }

    @Test
    void readsOneRowBeforeTheConditionsWhenTheHeaderNamesNoAlgorithm() {
        assertEquals(1, MatchAlgorithmFactory.getAlgorithm(null).getSpecialRowCount());
    }
}
