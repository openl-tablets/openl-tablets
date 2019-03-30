package org.openl.rules.cmatch.algorithm;

public class WeightAlgorithmCompilerBuilder implements IMatchAlgorithmCompilerBuilder {
    @Override
    public IMatchAlgorithmCompiler build() {
        return new WeightAlgorithmCompiler();
    }
}
