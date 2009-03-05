package org.openl.rules.tbasic;

import java.util.List;

import org.openl.binding.impl.BoundError;

public interface IRowParser {
    List<AlgorithmTreeNode> parse() throws BoundError;
}
