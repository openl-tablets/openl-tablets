package org.openl.rules.tbasic.compile;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import org.openl.rules.tbasic.AlgorithmTreeNode;

class ConversionRulesControllerTest {

    @Test
    void thereIsNoConversionRuleForNoOperations() {
        List<AlgorithmTreeNode> nodes = List.of();

        assertThrows(NoSuchElementException.class, () -> ConversionRulesController.getConvertionRule(nodes, null));
    }
}
