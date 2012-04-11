package org.openl.rules.cmatch.algorithm;

public class MatchAlgorithmCompilerBuilder implements IMatchAlgorithmCompilerBuilder {
    public IMatchAlgorithmCompiler build() {
        return new MatchAlgorithmCompiler();
    }
}
