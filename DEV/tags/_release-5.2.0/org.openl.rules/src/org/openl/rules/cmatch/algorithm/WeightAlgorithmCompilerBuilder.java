package org.openl.rules.cmatch.algorithm;

public class WeightAlgorithmCompilerBuilder implements IMatchAlgorithmCompilerBuilder {
    public IMatchAlgorithmCompiler build() {
        return new WeightAlgorithmCompiler();
    }
}
