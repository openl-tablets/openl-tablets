package org.openl.rules.cmatch.algorithm;

public class MatchAlgorithmCompilerBuilder implements IMatchAlgorithmCompilerBuilder {
    @Override
    public IMatchAlgorithmCompiler build() {
        return new MatchAlgorithmCompiler();
    }
}
