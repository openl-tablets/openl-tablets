package org.openl.rules.tbasic.compile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.tbasic.AlgorithmTreeNode;

class AlgorithmCompilerToolTest {

    @Test
    void groupsTheOperationsClosingAKeyword() {
        assertArrayEquals(new String[]{"ELSE", "END IF"}, AlgorithmCompilerTool.whatOperationsToGroup("IF"));
        assertNull(AlgorithmCompilerTool.whatOperationsToGroup("SET"));
        assertNull(AlgorithmCompilerTool.whatOperationsToGroup(null));
    }

    @Test
    void anOperationWithoutSpecificationIsTheLastExecutableOne() {
        var operation = new AlgorithmTreeNode();

        assertSame(operation, AlgorithmCompilerTool.getLastExecutableOperation(List.of(operation)));
    }
}
