package org.openl.rules.cmatch.algorithm;

public class ScoreAlgorithmCompilerBuilder implements IMatchAlgorithmCompilerBuilder {
    public IMatchAlgorithmCompiler build() {
        return new ScoreAlgorithmCompiler();
    }
}
