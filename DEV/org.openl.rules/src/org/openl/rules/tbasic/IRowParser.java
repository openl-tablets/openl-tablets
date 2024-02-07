package org.openl.rules.tbasic;

import java.util.List;

import org.openl.syntax.exception.SyntaxNodeException;

public interface IRowParser {
    List<AlgorithmTreeNode> parse() throws SyntaxNodeException;
}
