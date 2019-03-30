package org.openl.rules.cmatch.algorithm;

public class ScoreAlgorithmCompilerBuilder implements IMatchAlgorithmCompilerBuilder {
    @Override
    public IMatchAlgorithmCompiler build() {
        return new ScoreAlgorithmCompiler();
    }
}
