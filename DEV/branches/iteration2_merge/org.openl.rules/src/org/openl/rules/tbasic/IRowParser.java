package org.openl.rules.tbasic;

import java.util.List;

public interface IRowParser {
    List<AlgorithmTreeNode> parse() throws Exception;
}
